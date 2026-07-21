package com.ak.momapp.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ak.momapp.problem.Difficulty
import com.ak.momapp.problem.DifficultyTracker
import com.ak.momapp.problem.ProblemTopic
import com.ak.momapp.widget.BrainBreakWidget
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** One day of the activity chart. */
data class DayCount(val date: LocalDate, val count: Int)

data class BrainBreakStats(
    val solvedToday: Int = 0,
    val solvedThisWeek: Int = 0,
    val solvedAllTime: Int = 0,
    /** Fastest first-try solve in millis, or null before the first one. */
    val fastestMs: Long? = null,
    /** Problems answered right on the first try, and all first tries. */
    val firstTryCorrect: Int = 0,
    val firstTrySeen: Int = 0,
    /** The same first-try record, split by exercise type. */
    val topicTallies: Map<ProblemTopic, TopicTally> = emptyMap(),
    /** Solves per day, oldest first, ending today. As long as [DailyCounts.KEEP_DAYS]. */
    val lastTwoWeeks: List<DayCount> = emptyList(),
) {
    /** First-try accuracy in percent, or null before the first problem. */
    val accuracyPercent: Int?
        get() = if (firstTrySeen == 0) null else (firstTryCorrect * 100 + firstTrySeen / 2) / firstTrySeen
}

/**
 * Adaptive-difficulty progress and light solve stats, persisted so they
 * survive app restarts.
 */
class ProgressRepository(private val context: Context) {

    object Keys {
        val CURRENT_DIFFICULTY = stringPreferencesKey("current_difficulty")
        val CORRECT_STREAK = intPreferencesKey("correct_streak")
        val MISS_STREAK = intPreferencesKey("miss_streak")
        val DAILY_SOLVED = stringPreferencesKey("daily_solved")
        val TOTAL_SOLVED = intPreferencesKey("total_solved")
        val FASTEST_MS = longPreferencesKey("fastest_ms")
        val FIRST_TRY_CORRECT = intPreferencesKey("first_try_correct")
        val FIRST_TRY_SEEN = intPreferencesKey("first_try_seen")
        /** Per-topic first-try tallies; see [TopicAccuracy]. */
        val TOPIC_ACCURACY = stringPreferencesKey("topic_accuracy")

        /** Per-topic adaptive levels; see [TopicLadders]. */
        val TOPIC_LADDERS = stringPreferencesKey("topic_ladders")
    }

    /** Falls back to the starting level until the first answer is recorded. */
    val currentDifficulty: Flow<Difficulty> =
        context.brainBreakDataStore.data.map(::readDifficulty)

    /**
     * The level each topic is dealt at right now, with the Relaxed cap
     * already applied. This is the number the Exercises rows show and the
     * number the generator deals at, so the two can't drift.
     */
    val topicLevels: Flow<Map<ProblemTopic, Difficulty>> =
        context.brainBreakDataStore.data.map { prefs ->
            val overall = readDifficulty(prefs)
            val maxLevel = readMaxLevel(prefs)
            val ladders = TopicLadders.decode(prefs[Keys.TOPIC_LADDERS])
            ProblemTopic.entries.associateWith {
                TopicLadders.levelFor(ladders, it, overall, maxLevel)
            }
        }


    val stats: Flow<BrainBreakStats> = context.brainBreakDataStore.data.map { prefs ->
        val daily = DailyCounts.decode(prefs[Keys.DAILY_SOLVED])
        val today = LocalDate.now()
        BrainBreakStats(
            solvedToday = DailyCounts.countOn(daily, today),
            solvedThisWeek = DailyCounts.weekTotal(daily, today),
            solvedAllTime = prefs[Keys.TOTAL_SOLVED] ?: 0,
            fastestMs = prefs[Keys.FASTEST_MS],
            firstTryCorrect = prefs[Keys.FIRST_TRY_CORRECT] ?: 0,
            firstTrySeen = prefs[Keys.FIRST_TRY_SEEN] ?: 0,
            topicTallies = TopicAccuracy.decode(prefs[Keys.TOPIC_ACCURACY]),
            lastTwoWeeks = (DailyCounts.KEEP_DAYS - 1 downTo 0).map { back ->
                val day = today.minusDays(back)
                DayCount(day, DailyCounts.countOn(daily, day))
            },
        )
    }

    /**
     * A solved problem always counts toward the stats; difficulty and the
     * fastest time only move on a first-try success.
     *
     * [countsTowardLevel] is false for practice problems: she chose the
     * type and the level herself, so drilling a hard one shouldn't drag
     * her break level around, and an untimed drill can't set a speed
     * record either. The solve and the accuracy tally still count.
     */
    suspend fun recordCorrect(
        firstAttempt: Boolean,
        solveTimeMs: Long,
        topic: ProblemTopic,
        countsTowardLevel: Boolean = true,
    ) {
        context.brainBreakDataStore.edit { prefs ->
            if (firstAttempt) {
                if (countsTowardLevel) {
                    runTracker(prefs, correct = true)
                    runTopicLadder(prefs, topic, correct = true)
                }
                // Each problem casts exactly one first-try vote: here on a
                // clean solve, in recordIncorrect on the first stumble.
                prefs[Keys.FIRST_TRY_CORRECT] = (prefs[Keys.FIRST_TRY_CORRECT] ?: 0) + 1
                prefs[Keys.FIRST_TRY_SEEN] = (prefs[Keys.FIRST_TRY_SEEN] ?: 0) + 1
                prefs[Keys.TOPIC_ACCURACY] =
                    TopicAccuracy.record(prefs[Keys.TOPIC_ACCURACY], topic, correct = true)
            }

            val daily = DailyCounts.decode(prefs[Keys.DAILY_SOLVED])
            prefs[Keys.DAILY_SOLVED] = DailyCounts.encode(DailyCounts.increment(daily, LocalDate.now()))
            prefs[Keys.TOTAL_SOLVED] = (prefs[Keys.TOTAL_SOLVED] ?: 0) + 1

            val fastest = prefs[Keys.FASTEST_MS]
            if (countsTowardLevel && firstAttempt && solveTimeMs > 0 &&
                (fastest == null || solveTimeMs < fastest)
            ) {
                prefs[Keys.FASTEST_MS] = solveTimeMs
            }
        }
        // The one place the day's count changes, so the one place the
        // home-screen widget has to be told about it.
        BrainBreakWidget.refresh(context)
    }

    /**
     * A fresh round starts over: back to the chosen starting level (the
     * adaptive level is dropped, so [currentDifficulty] falls back to it)
     * with both streaks cleared.
     *
     * The per-topic ladders go with it: a fresh round that left half the
     * topics where they had climbed to would not be a fresh round.
     */
    suspend fun resetToStartingDifficulty() {
        context.brainBreakDataStore.edit { prefs ->
            prefs.remove(Keys.CURRENT_DIFFICULTY)
            prefs.remove(Keys.TOPIC_LADDERS)
            prefs[Keys.CORRECT_STREAK] = 0
            prefs[Keys.MISS_STREAK] = 0
        }
    }

    /** Only called for a miss on the first attempt (wrong answer, skip, or timeout). */
    suspend fun recordIncorrect(topic: ProblemTopic, countsTowardLevel: Boolean = true) {
        context.brainBreakDataStore.edit { prefs ->
            if (countsTowardLevel) {
                runTracker(prefs, correct = false)
                runTopicLadder(prefs, topic, correct = false)
            }
            prefs[Keys.FIRST_TRY_SEEN] = (prefs[Keys.FIRST_TRY_SEEN] ?: 0) + 1
            prefs[Keys.TOPIC_ACCURACY] =
                TopicAccuracy.record(prefs[Keys.TOPIC_ACCURACY], topic, correct = false)
        }
    }

    /**
     * Seeded with the OVERALL level, deliberately: a topic she has never
     * answered should start where she already is, not back at Easy.
     */
    private fun runTopicLadder(prefs: MutablePreferences, topic: ProblemTopic, correct: Boolean) {
        prefs[Keys.TOPIC_LADDERS] = TopicLadders.record(
            raw = prefs[Keys.TOPIC_LADDERS],
            topic = topic,
            correct = correct,
            seedLevel = readDifficulty(prefs),
            maxLevel = readMaxLevel(prefs),
        )
    }

    private fun runTracker(prefs: MutablePreferences, correct: Boolean) {
        val tracker = DifficultyTracker(
            start = readDifficulty(prefs),
            correctInARow = prefs[Keys.CORRECT_STREAK] ?: 0,
            missesInARow = prefs[Keys.MISS_STREAK] ?: 0,
            maxLevel = readMaxLevel(prefs),
        )
        if (correct) tracker.recordCorrect() else tracker.recordIncorrect()
        prefs[Keys.CURRENT_DIFFICULTY] = tracker.current.name
        prefs[Keys.CORRECT_STREAK] = tracker.correctInARow
        prefs[Keys.MISS_STREAK] = tracker.missesInARow
    }

    private fun readDifficulty(prefs: Preferences): Difficulty {
        val stored = SettingsSerialization.decodeDifficulty(
            prefs[Keys.CURRENT_DIFFICULTY]
                ?: prefs[SettingsRepository.Keys.STARTING_DIFFICULTY],
        )
        return minOf(stored, readMaxLevel(prefs))
    }

    /** The Relaxed preset caps the climb; a missing key means no cap. */
    private fun readMaxLevel(prefs: Preferences): Difficulty =
        SettingsSerialization.decodeDifficulty(
            prefs[SettingsRepository.Keys.MAX_DIFFICULTY],
            default = Difficulty.HARD,
        )
}
