package com.ak.momapp.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ak.momapp.i18n.AppLanguage
import com.ak.momapp.problem.Difficulty
import com.ak.momapp.problem.ProblemTopic
import com.ak.momapp.ui.theme.AppPalette
import java.time.DayOfWeek
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class BrainBreakSettings(
    val remindersEnabled: Boolean = true,
    val intervalHours: Int = DEFAULT_INTERVAL_HOURS,
    val activeStartMinutes: Int = DEFAULT_ACTIVE_START,
    val activeEndMinutes: Int = DEFAULT_ACTIVE_END,
    val activeDays: Set<DayOfWeek> = DEFAULT_ACTIVE_DAYS,
    val startingDifficulty: Difficulty = Difficulty.EASY,
    /** Problems allowed per sitting; [UNLIMITED_PROBLEMS] means no cap. */
    val problemsPerBreak: Int = UNLIMITED_PROBLEMS,
    /** Minutes allowed per problem; [TIMER_OFF] disables the countdown. */
    val timerMinutes: Int = TIMER_OFF,
    /** Problem families the mix may draw from; never below [ProblemTopic.MIN_ENABLED]. */
    val enabledTopics: Set<ProblemTopic> = ProblemTopic.ALL,
    /** The look picked under Settings → P ersonalize. */
    val palette: AppPalette = AppPalette.CLAY,
    /** The one-time setup guide has been answered. */
    val guideShown: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH,
    /** A soft chime on a correct answer; silent mode always wins. */
    val successSound: Boolean = true,
) {
    companion object {
        const val MIN_INTERVAL_HOURS = 1
        const val MAX_INTERVAL_HOURS = 4
        const val DEFAULT_INTERVAL_HOURS = 2
        const val UNLIMITED_PROBLEMS = 0
        const val MAX_PROBLEMS_PER_BREAK = 99
        /** Quick-pick options offered in settings. */
        val PROBLEM_LIMIT_PRESETS = listOf(5, 10, 20)
        const val TIMER_OFF = 0
        /** Off by default; tricky problems stretch these via a multiplier. */
        val TIMER_OPTIONS = listOf(TIMER_OFF, 3, 5, 10)
        const val DEFAULT_ACTIVE_START = 9 * 60
        const val DEFAULT_ACTIVE_END = 17 * 60
        val DEFAULT_ACTIVE_DAYS: Set<DayOfWeek> = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        )
    }
}

/**
 * String encodings for DataStore values. Null input means "never set" and
 * yields the default; an empty string is a deliberate empty choice.
 */
object SettingsSerialization {

    fun encodeDays(days: Set<DayOfWeek>): String =
        days.map(DayOfWeek::getValue).sorted().joinToString(",")

    fun decodeDays(raw: String?): Set<DayOfWeek> {
        if (raw == null) return BrainBreakSettings.DEFAULT_ACTIVE_DAYS
        return raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .map(DayOfWeek::of)
            .toSet()
    }

    fun decodeDifficulty(raw: String?, default: Difficulty = Difficulty.EASY): Difficulty =
        when (raw) {
            // The pre-rewrite fourth level, folded into HARD.
            "EXPERT" -> Difficulty.HARD
            else -> Difficulty.entries.firstOrNull { it.name == raw } ?: default
        }

    /** Values outside the current picker (incl. the old 1–3 min) mean off. */
    fun decodeTimerMinutes(raw: Int?): Int =
        if (raw != null && raw in BrainBreakSettings.TIMER_OPTIONS) raw else BrainBreakSettings.TIMER_OFF

    /** Before the user ever picks, a Romanian phone starts in Romanian. */
    fun decodeLanguage(
        raw: String?,
        systemLanguage: String = Locale.getDefault().language,
    ): AppLanguage =
        AppLanguage.entries.firstOrNull { it.name == raw }
            ?: if (systemLanguage == "ro") AppLanguage.ROMANIAN else AppLanguage.ENGLISH

    fun decodePalette(raw: String?): AppPalette =
        AppPalette.entries.firstOrNull { it.name == raw } ?: AppPalette.CLAY

    /**
     * Topics are stored as the switched-OFF set: a topic added in a later
     * app version is missing from the stored string and therefore lands
     * switched on. No per-release migration needed anymore.
     */
    fun encodeTopics(enabled: Set<ProblemTopic>): String =
        (ProblemTopic.ALL - enabled).sortedBy(ProblemTopic::ordinal).joinToString(",") { it.name }

    /**
     * Returns the ENABLED set. Anything invalid enough to break the min-2
     * rule falls back to all on. The legacy parameters are the old
     * enabled-set encodings. [legacyV2Raw] from before NUMBERS existed,
     * [legacyV1Raw] from before COMPARE and TARGET split off the CORE
     * switch; the topics they couldn't name carry over switched on.
     */
    fun decodeTopics(
        disabledRaw: String?,
        legacyV2Raw: String? = null,
        legacyV1Raw: String? = null,
    ): Set<ProblemTopic> {
        val enabled = when {
            disabledRaw != null -> ProblemTopic.ALL - parseTopics(disabledRaw)
            legacyV2Raw != null -> legacyEnabled(legacyV2Raw, ProblemTopic.NUMBERS)
            legacyV1Raw != null -> legacyEnabled(
                legacyV1Raw,
                ProblemTopic.COMPARE, ProblemTopic.TARGET, ProblemTopic.NUMBERS,
            )
            else -> return ProblemTopic.ALL
        }
        return if (enabled.size < ProblemTopic.MIN_ENABLED) ProblemTopic.ALL else enabled
    }

    /** A garbage legacy set means all on. Never "only the new topics". */
    private fun legacyEnabled(raw: String, vararg newTopics: ProblemTopic): Set<ProblemTopic> {
        val stored = parseTopics(raw)
        return if (stored.size < ProblemTopic.MIN_ENABLED) ProblemTopic.ALL else stored + newTopics
    }

    private fun parseTopics(raw: String): Set<ProblemTopic> = raw.split(",")
        .mapNotNull { name -> ProblemTopic.entries.firstOrNull { it.name == name.trim() } }
        .toSet()
}

/**
 * Keeps the active window valid (end at least [MIN_LENGTH_MINUTES] after
 * start) by nudging the other endpoint when a pick would break it.
 */
object ActiveWindow {
    const val MIN_LENGTH_MINUTES = 60
    const val MINUTES_IN_DAY = 24 * 60

    /** Returns start to end after the user picked a new start time. */
    fun applyStart(pickedStart: Int, currentEnd: Int): Pair<Int, Int> {
        val start = pickedStart.coerceIn(0, MINUTES_IN_DAY - 1 - MIN_LENGTH_MINUTES)
        val end = maxOf(currentEnd, start + MIN_LENGTH_MINUTES)
        return start to end
    }

    /** Returns start to end after the user picked a new end time. */
    fun applyEnd(pickedEnd: Int, currentStart: Int): Pair<Int, Int> {
        val end = pickedEnd.coerceIn(MIN_LENGTH_MINUTES, MINUTES_IN_DAY - 1)
        val start = minOf(currentStart, end - MIN_LENGTH_MINUTES)
        return start to end
    }
}

/**
 * The three styles offered by the first-run setup guide. Picking one
 * writes these values in a single edit; every one of them can still be
 * changed piece by piece in Settings afterwards.
 */
enum class SetupPreset(
    val problemsPerBreak: Int,
    val startingDifficulty: Difficulty,
    val timerMinutes: Int,
    /** RELAXED never climbs past MEDIUM; the others are uncapped. */
    val maxDifficulty: Difficulty,
) {
    DEFAULT(
        problemsPerBreak = BrainBreakSettings.UNLIMITED_PROBLEMS,
        startingDifficulty = Difficulty.EASY,
        timerMinutes = BrainBreakSettings.TIMER_OFF,
        maxDifficulty = Difficulty.HARD,
    ),
    RELAXED(
        problemsPerBreak = 5,
        startingDifficulty = Difficulty.EASY,
        timerMinutes = BrainBreakSettings.TIMER_OFF,
        maxDifficulty = Difficulty.MEDIUM,
    ),
    CHALLENGE(
        problemsPerBreak = 10,
        startingDifficulty = Difficulty.MEDIUM,
        timerMinutes = 5,
        maxDifficulty = Difficulty.HARD,
    ),
}

class SettingsRepository(private val context: Context) {

    object Keys {
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val INTERVAL_HOURS = intPreferencesKey("interval_hours")
        val ACTIVE_START = intPreferencesKey("active_start_minutes")
        val ACTIVE_END = intPreferencesKey("active_end_minutes")
        val ACTIVE_DAYS = stringPreferencesKey("active_days")
        val STARTING_DIFFICULTY = stringPreferencesKey("starting_difficulty")
        /** The adaptive climb's ceiling; missing means uncapped. */
        val MAX_DIFFICULTY = stringPreferencesKey("max_difficulty")
        val PROBLEMS_PER_BREAK = intPreferencesKey("problems_per_break")
        val TIMER_MINUTES = intPreferencesKey("timer_minutes")
        /** The switched-OFF topics; see [SettingsSerialization.encodeTopics]. */
        val DISABLED_TOPICS = stringPreferencesKey("disabled_problem_topics")
        /** Old enabled-set encodings, kept readable for migration only. */
        val LEGACY_ENABLED_TOPICS_V2 = stringPreferencesKey("enabled_problem_topics_v2")
        val LEGACY_ENABLED_TOPICS_V1 = stringPreferencesKey("enabled_problem_topics")
        val PALETTE = stringPreferencesKey("palette")
        val GUIDE_SHOWN = booleanPreferencesKey("guide_shown")
        val LANGUAGE = stringPreferencesKey("language")
        val SUCCESS_SOUND = booleanPreferencesKey("success_sound")
    }

    val settings: Flow<BrainBreakSettings> = context.brainBreakDataStore.data.map { prefs ->
        BrainBreakSettings(
            remindersEnabled = prefs[Keys.REMINDERS_ENABLED] ?: true,
            intervalHours = (prefs[Keys.INTERVAL_HOURS] ?: BrainBreakSettings.DEFAULT_INTERVAL_HOURS)
                .coerceIn(BrainBreakSettings.MIN_INTERVAL_HOURS, BrainBreakSettings.MAX_INTERVAL_HOURS),
            activeStartMinutes = prefs[Keys.ACTIVE_START] ?: BrainBreakSettings.DEFAULT_ACTIVE_START,
            activeEndMinutes = prefs[Keys.ACTIVE_END] ?: BrainBreakSettings.DEFAULT_ACTIVE_END,
            activeDays = SettingsSerialization.decodeDays(prefs[Keys.ACTIVE_DAYS]),
            startingDifficulty = SettingsSerialization.decodeDifficulty(prefs[Keys.STARTING_DIFFICULTY]),
            problemsPerBreak = (prefs[Keys.PROBLEMS_PER_BREAK] ?: BrainBreakSettings.UNLIMITED_PROBLEMS)
                .coerceIn(BrainBreakSettings.UNLIMITED_PROBLEMS, BrainBreakSettings.MAX_PROBLEMS_PER_BREAK),
            timerMinutes = SettingsSerialization.decodeTimerMinutes(prefs[Keys.TIMER_MINUTES]),
            enabledTopics = SettingsSerialization.decodeTopics(
                prefs[Keys.DISABLED_TOPICS],
                prefs[Keys.LEGACY_ENABLED_TOPICS_V2],
                prefs[Keys.LEGACY_ENABLED_TOPICS_V1],
            ),
            palette = SettingsSerialization.decodePalette(prefs[Keys.PALETTE]),
            guideShown = prefs[Keys.GUIDE_SHOWN] ?: false,
            language = SettingsSerialization.decodeLanguage(prefs[Keys.LANGUAGE]),
            successSound = prefs[Keys.SUCCESS_SOUND] ?: true,
        )
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.brainBreakDataStore.edit { it[Keys.REMINDERS_ENABLED] = enabled }
    }

    suspend fun setIntervalHours(hours: Int) {
        context.brainBreakDataStore.edit {
            it[Keys.INTERVAL_HOURS] =
                hours.coerceIn(BrainBreakSettings.MIN_INTERVAL_HOURS, BrainBreakSettings.MAX_INTERVAL_HOURS)
        }
    }

    suspend fun setActiveWindow(startMinutes: Int, endMinutes: Int) {
        context.brainBreakDataStore.edit {
            it[Keys.ACTIVE_START] = startMinutes
            it[Keys.ACTIVE_END] = endMinutes
        }
    }

    suspend fun setActiveDays(days: Set<DayOfWeek>) {
        context.brainBreakDataStore.edit {
            it[Keys.ACTIVE_DAYS] = SettingsSerialization.encodeDays(days)
        }
    }

    /**
     * Choosing a level by hand also resets the adaptive level to it and
     * lifts any preset's climb cap. A deliberate pick outranks Relaxed.
     */
    suspend fun setStartingDifficulty(difficulty: Difficulty) {
        context.brainBreakDataStore.edit {
            it[Keys.STARTING_DIFFICULTY] = difficulty.name
            it.remove(Keys.MAX_DIFFICULTY)
            it[ProgressRepository.Keys.CURRENT_DIFFICULTY] = difficulty.name
            it[ProgressRepository.Keys.CORRECT_STREAK] = 0
            it[ProgressRepository.Keys.MISS_STREAK] = 0
        }
    }

    /**
     * Applies a setup-guide preset in one edit and marks the guide as
     * answered; the adaptive level restarts from the preset's start.
     */
    suspend fun applyPreset(preset: SetupPreset) {
        context.brainBreakDataStore.edit {
            it[Keys.PROBLEMS_PER_BREAK] = preset.problemsPerBreak
            it[Keys.TIMER_MINUTES] = preset.timerMinutes
            it[Keys.STARTING_DIFFICULTY] = preset.startingDifficulty.name
            it[Keys.MAX_DIFFICULTY] = preset.maxDifficulty.name
            it[ProgressRepository.Keys.CURRENT_DIFFICULTY] = preset.startingDifficulty.name
            it[ProgressRepository.Keys.CORRECT_STREAK] = 0
            it[ProgressRepository.Keys.MISS_STREAK] = 0
            it[Keys.GUIDE_SHOWN] = true
        }
    }

    suspend fun setTimerMinutes(minutes: Int) {
        context.brainBreakDataStore.edit {
            it[Keys.TIMER_MINUTES] =
                if (minutes in BrainBreakSettings.TIMER_OPTIONS) minutes else BrainBreakSettings.TIMER_OFF
        }
    }

    suspend fun setEnabledTopics(topics: Set<ProblemTopic>) {
        context.brainBreakDataStore.edit {
            it[Keys.DISABLED_TOPICS] = SettingsSerialization.encodeTopics(topics)
        }
    }

    suspend fun setPalette(palette: AppPalette) {
        context.brainBreakDataStore.edit { it[Keys.PALETTE] = palette.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.brainBreakDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setProblemsPerBreak(count: Int) {
        context.brainBreakDataStore.edit {
            it[Keys.PROBLEMS_PER_BREAK] = count.coerceIn(
                BrainBreakSettings.UNLIMITED_PROBLEMS,
                BrainBreakSettings.MAX_PROBLEMS_PER_BREAK,
            )
        }
    }

    suspend fun setSuccessSound(enabled: Boolean) {
        context.brainBreakDataStore.edit { it[Keys.SUCCESS_SOUND] = enabled }
    }
}
