package com.ak.momapp.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ak.momapp.problem.Difficulty
import com.ak.momapp.problem.Level
import com.ak.momapp.problem.LevelLadder
import com.ak.momapp.problem.ProblemTopic
import com.ak.momapp.problem.ReviewPick
import com.ak.momapp.problem.toLevel
import com.ak.momapp.widget.BrainBreakWidget
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        /** Where she is on the fine scale, and the name last shown for it. */
        val CURRENT_POINTS = intPreferencesKey("current_level_points")
        val CURRENT_BAND = stringPreferencesKey("current_level_band")

        /**
         * The pre-scale level, read only to migrate someone who last used
         * a tier build: her Easy/Normal/Hard becomes the middle of that
         * band, which is the closest true statement about where she was.
         */
        val CURRENT_DIFFICULTY = stringPreferencesKey("current_difficulty")
        val DAILY_SOLVED = stringPreferencesKey("daily_solved")
        val TOTAL_SOLVED = intPreferencesKey("total_solved")
        val FASTEST_MS = longPreferencesKey("fastest_ms")
        val FIRST_TRY_CORRECT = intPreferencesKey("first_try_correct")
        val FIRST_TRY_SEEN = intPreferencesKey("first_try_seen")
        /** Per-topic first-try tallies; see [TopicAccuracy]. */
        val TOPIC_ACCURACY = stringPreferencesKey("topic_accuracy")

        /**
         * Per-topic adaptive levels; see [TopicLadders]. Version two of the
         * encoding, on its own key: the tier-era rows carried streak counts
         * that have no meaning on a points ladder, and a fresh key discards
         * them cleanly instead of half-reading them.
         */
        val TOPIC_LADDERS = stringPreferencesKey("topic_ladders_v2")

        /** Missed shapes waiting to come round again; see [ReviewQueue]. */
        val REVIEW_QUEUE = stringPreferencesKey("review_queue")
    }

    /**
     * A shape she once got wrong that is due today, or null. Read fresh
     * before each problem is dealt.
     */
    suspend fun dueReview(enabledTopics: Set<ProblemTopic>): ReviewPick? {
        val prefs = context.brainBreakDataStore.data.first()
        return ReviewQueue.due(
            raw = prefs[Keys.REVIEW_QUEUE],
            today = LocalDate.now().toEpochDay(),
            enabledTopics = enabledTopics,
        )
    }

    /** Falls back to the starting level until the first answer is recorded. */
    val currentLevel: Flow<Level> = context.brainBreakDataStore.data.map(::readLevel)

    /** The name for that level, sticky across band boundaries. */
    val currentBand: Flow<Difficulty> = context.brainBreakDataStore.data.map(::readBand)

    /**
     * The level each topic is dealt at right now, with the Relaxed cap
     * already applied. This is what the generator deals at.
     */
    val topicLevels: Flow<Map<ProblemTopic, Level>> =
        context.brainBreakDataStore.data.map { prefs ->
            val overall = readLevel(prefs)
            val ceiling = readCeiling(prefs)
            val ladders = TopicLadders.decode(prefs[Keys.TOPIC_LADDERS])
            ProblemTopic.entries.associateWith {
                TopicLadders.levelFor(ladders, it, overall, ceiling)
            }
        }

    /**
     * The name each topic shows on the Exercises screen. Derived from the
     * same ladders as [topicLevels], so a row can never name a level she
     * isn't actually being dealt.
     */
    val topicBands: Flow<Map<ProblemTopic, Difficulty>> =
        context.brainBreakDataStore.data.map { prefs ->
            val overall = readLevel(prefs)
            val overallBand = readBand(prefs)
            val ceiling = readCeiling(prefs)
            val ladders = TopicLadders.decode(prefs[Keys.TOPIC_LADDERS])
            ProblemTopic.entries.associateWith {
                TopicLadders.bandFor(ladders, it, overall, overallBand, ceiling)
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
        /** The shape solved, so the review queue can space it out further. */
        shape: String? = null,
    ) {
        context.brainBreakDataStore.edit { prefs ->
            if (firstAttempt) {
                if (countsTowardLevel) {
                    advance(prefs, topic, correct = true, solveTimeMs = solveTimeMs)
                }
                // A drill still counts here. She chose to practise this and
                // got it right, which is exactly the evidence the queue
                // wants, whatever it does to the break level.
                prefs[Keys.REVIEW_QUEUE] = ReviewQueue.recordCorrect(
                    raw = prefs[Keys.REVIEW_QUEUE],
                    shape = shape,
                    today = LocalDate.now().toEpochDay(),
                )
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
            prefs.remove(Keys.CURRENT_POINTS)
            prefs.remove(Keys.CURRENT_BAND)
            prefs.remove(Keys.CURRENT_DIFFICULTY)
            prefs.remove(Keys.TOPIC_LADDERS)
        }
    }

    /** Only called for a miss on the first attempt (wrong answer, skip, or timeout). */
    suspend fun recordIncorrect(
        topic: ProblemTopic,
        countsTowardLevel: Boolean = true,
        /** The shape missed, so it can come round again in a day or so. */
        shape: String? = null,
    ) {
        context.brainBreakDataStore.edit { prefs ->
            if (countsTowardLevel) {
                advance(prefs, topic, correct = false, solveTimeMs = 0)
            }
            prefs[Keys.REVIEW_QUEUE] = ReviewQueue.recordMiss(
                raw = prefs[Keys.REVIEW_QUEUE],
                topic = topic,
                shape = shape,
                today = LocalDate.now().toEpochDay(),
            )
            prefs[Keys.FIRST_TRY_SEEN] = (prefs[Keys.FIRST_TRY_SEEN] ?: 0) + 1
            prefs[Keys.TOPIC_ACCURACY] =
                TopicAccuracy.record(prefs[Keys.TOPIC_ACCURACY], topic, correct = false)
        }
    }

    /**
     * Moves both ladders on one first-try answer.
     *
     * How quickly the answer came is judged ONCE, against her usual pace on
     * this topic, and the same verdict feeds both ladders. Classifying
     * separately for each would let them disagree about whether the very
     * same answer was quick.
     *
     * The topic's ladder is seeded with the OVERALL level, deliberately: a
     * topic she has never answered should start where she already is,
     * rather than back at the floor.
     */
    private fun advance(
        prefs: MutablePreferences,
        topic: ProblemTopic,
        correct: Boolean,
        solveTimeMs: Long,
    ) {
        val ladders = TopicLadders.decode(prefs[Keys.TOPIC_LADDERS])
        val pace = TopicLadders.paceOf(ladders, topic, solveTimeMs)
        val ceiling = readCeiling(prefs)
        val level = readLevel(prefs)
        val band = readBand(prefs)

        val moved = LevelLadder.next(level, correct, pace, ceiling)
        prefs[Keys.CURRENT_POINTS] = moved.points
        prefs[Keys.CURRENT_BAND] = LevelLadder.shownBand(minOf(moved, ceiling), band).name

        prefs[Keys.TOPIC_LADDERS] = TopicLadders.record(
            raw = prefs[Keys.TOPIC_LADDERS],
            topic = topic,
            correct = correct,
            pace = pace,
            solveTimeMs = solveTimeMs,
            seedLevel = level,
            seedBand = band,
            ceiling = ceiling,
        )
    }

    /**
     * Where she is, before the cap. The cap is applied when a level is
     * DEALT rather than when it is stored, so that turning the Relaxed
     * preset off gives back the ground she had already earned instead of
     * making her climb it twice.
     */
    private fun readLevel(prefs: Preferences): Level {
        val stored = prefs[Keys.CURRENT_POINTS]
        if (stored != null) return Level.of(stored)
        // Nothing on the fine scale yet: either a tier-era level to carry
        // over, or the starting level she picked.
        val band = SettingsSerialization.decodeDifficulty(
            prefs[Keys.CURRENT_DIFFICULTY]
                ?: prefs[SettingsRepository.Keys.STARTING_DIFFICULTY],
        )
        return band.toLevel()
    }

    private fun readBand(prefs: Preferences): Difficulty =
        SettingsSerialization.decodeDifficulty(
            prefs[Keys.CURRENT_BAND],
            default = minOf(readLevel(prefs), readCeiling(prefs)).band,
        )

    /**
     * The Relaxed preset caps the climb at the TOP of its band, not the
     * middle: capping at Normal should still let her use all of Normal.
     */
    private fun readCeiling(prefs: Preferences): Level =
        when (
            SettingsSerialization.decodeDifficulty(
                prefs[SettingsRepository.Keys.MAX_DIFFICULTY],
                default = Difficulty.HARD,
            )
        ) {
            Difficulty.EASY -> Level.of(Level.EASY_TOP)
            Difficulty.MEDIUM -> Level.of(Level.MEDIUM_TOP)
            Difficulty.HARD -> Level.CEILING
        }
}
