package com.ak.momapp.ui.problem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ak.momapp.alarm.BreakCoordinator
import com.ak.momapp.data.ChallengeRepository
import com.ak.momapp.data.ProgressRepository
import com.ak.momapp.data.SettingsRepository
import com.ak.momapp.problem.Difficulty
import com.ak.momapp.problem.PersonalContent
import com.ak.momapp.problem.Problem
import com.ak.momapp.problem.ProblemGenerator
import com.ak.momapp.problem.ProblemKind
import com.ak.momapp.problem.ProblemTopic
import com.ak.momapp.problem.topic
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AnswerPhase {
    /** Waiting for the first attempt. */
    ANSWERING,

    /** Last attempt was wrong; she can try the same problem again. */
    TRY_AGAIN,

    /** Solved it. */
    CORRECT,

    /** Out of tries (three typed, two tapped) or time ran out. The answer is shown. */
    REVEALED,
}

data class ProblemUiState(
    val problem: Problem,
    val input: String = "",
    val phase: AnswerPhase = AnswerPhase.ANSWERING,
    val attempts: Int = 0,
    /**
     * Which encouragement to show, as a random pick resolved against the
     * current language's list at render time. Storing the text itself
     * would freeze it in whatever language was active when she answered.
     */
    val encouragementSeed: Int? = null,
    /** The per-break problem cap is reached; no "One more?" offered. */
    val sessionComplete: Boolean = false,
    /** Seconds left on the problem timer; null when the timer is off. */
    val remainingSeconds: Int? = null,
    /** REVEALED because the timer ran out, not because of three misses. */
    val timedOut: Boolean = false,
    /** Hint-button presses so far; the third press reveals the answer. */
    val hintsUsed: Int = 0,
) {
    val isFinished: Boolean
        get() = phase == AnswerPhase.CORRECT || phase == AnswerPhase.REVEALED

    /** The nudge currently on screen, if any hint has been used. */
    val currentHint: String?
        get() = if (hintsUsed > 0) problem.hints.getOrNull(hintsUsed - 1) else null
}

/** One drill: a single topic at a level she picked herself. */
data class PracticeConfig(val topic: ProblemTopic, val difficulty: Difficulty)

class ProblemViewModel(
    private val settingsRepository: SettingsRepository,
    private val progressRepository: ProgressRepository,
    private val generator: ProblemGenerator = ProblemGenerator(),
    private val random: Random = Random.Default,
    // Clears a pending break notification + re-nudge once she's done with
    // a problem; a no-op default keeps the class testable off-device.
    private val onProblemFinished: suspend () -> Unit = {},
    // Only feeds the trophy's glow; null keeps the class testable off-device.
    challengeRepository: ChallengeRepository? = null,
) : ViewModel() {

    /** Today's challenge is solved. The trophy dims instead of glowing. */
    val challengeDoneToday: StateFlow<Boolean> =
        (challengeRepository?.state ?: emptyFlow())
            .map { it.done && it.day == LocalDate.now().toEpochDay() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // Null until the first problem is loaded from persisted state.
    private val _uiState = MutableStateFlow<ProblemUiState?>(null)
    val uiState: StateFlow<ProblemUiState?> = _uiState.asStateFlow()

    /** Persisted across restarts, unlike the old session-only counter. */
    val solvedToday: StateFlow<Int> = progressRepository.stats
        .map { it.solvedToday }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** The per-break cap, for the sitting's progress dots (0 = no cap). */
    val sessionLimit: StateFlow<Int> = settingsRepository.settings
        .map { it.problemsPerBreak }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** A soft chime on a correct answer, unless switched off in Settings. */
    val successSound: StateFlow<Boolean> = settingsRepository.settings
        .map { it.successSound }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // Problems finished this sitting; drives the cap and the dots.
    private val _sessionDone = MutableStateFlow(0)
    val sessionDone: StateFlow<Int> = _sessionDone.asStateFlow()

    private var problemShownAtMs: Long = 0
    private var timerJob: Job? = null

    /**
     * Nothing is dealt until she presses Start (or a break notification
     * opens the app). Opening the app to check settings or the challenge
     * shouldn't drop her into a problem.
     */
    private val _started = MutableStateFlow(false)
    val started: StateFlow<Boolean> = _started.asStateFlow()

    /**
     * Non-null while she's drilling one topic instead of taking a break.
     * Practice problems still count toward the day's total and the
     * per-topic accuracy, but they leave the adaptive level alone and
     * never fill the per-break cap. Held here rather than in the screen
     * so it survives a rotation.
     */
    private val _practice = MutableStateFlow<PracticeConfig?>(null)
    val practice: StateFlow<PracticeConfig?> = _practice.asStateFlow()

    /** She picked a type and a level. Deal the first drill. */
    fun startPractice(topic: ProblemTopic, difficulty: Difficulty) {
        timerJob?.cancel()
        _practice.value = PracticeConfig(topic, difficulty)
        _sessionDone.value = 0
        _started.value = true
        _uiState.value = null
        nextProblem()
    }

    /** Back out of a drill, to the picker. */
    fun endPractice() {
        timerJob?.cancel()
        _practice.value = null
        _started.value = false
        _uiState.value = null
    }

    fun startSession() {
        if (_started.value) return
        _started.value = true
        nextProblem()
    }

    /**
     * A new break notification arrived. Whatever she did last sitting no
     * longer counts against the per-break cap, and the break itself is the
     * intent to start, so no extra Start press is needed.
     */
    fun onBreakOpened() {
        _sessionDone.value = 0
        _uiState.update { it?.copy(sessionComplete = false) }
        startSession()
    }

    /**
     * The settings button that starts over: the sitting counter restarts,
     * the adaptive level goes back to the chosen starting difficulty, and
     * a brand-new problem at that level is dealt right away.
     */
    fun resetSitting() {
        _sessionDone.value = 0
        _started.value = true
        timerJob?.cancel()
        viewModelScope.launch {
            progressRepository.resetToStartingDifficulty()
            deal()
        }
    }

    /**
     * Deals a new problem immediately. Walking away from an untouched
     * problem counts as a strike (a first-attempt miss for the adaptive
     * ladder); one already missed was recorded then. Skips never count
     * toward the per-break cap.
     */
    fun skipProblem() {
        val state = _uiState.value ?: return
        if (state.isFinished) return
        timerJob?.cancel()
        if (state.attempts == 0) {
            recordMiss(state.problem.kind.topic)
        }
        nextProblem()
    }

    fun onInputChange(value: String) {
        _uiState.update { state ->
            if (state == null || state.isFinished) {
                state
            } else {
                state.copy(input = value.filter(Char::isDigit).take(MAX_INPUT_DIGITS))
            }
        }
    }

    /** Card taps on a target or hunt problem; selection rides in [ProblemUiState.input]. */
    fun toggleCard(index: Int) {
        _uiState.update { state ->
            if (state == null || state.isFinished || state.problem.cards.isEmpty()) {
                state
            } else {
                val mark = index.digitToChar()
                // A target pick stops at the asked-for count; a hunt is
                // open-ended. How many to tap is part of the question.
                val cap = if (state.problem.kind == ProblemKind.TARGET) {
                    state.problem.pickCount
                } else {
                    state.problem.cards.size
                }
                val input = when {
                    mark in state.input -> state.input.filterNot { it == mark }
                    state.input.length >= cap -> state.input
                    else -> state.input + mark
                }
                state.copy(input = input)
            }
        }
    }

    /** One tap answers a comparison: sets the choice and checks it at once. */
    fun submitChoice(choice: Int) {
        _uiState.update { state ->
            if (state == null || state.isFinished) state else state.copy(input = choice.toString())
        }
        submit()
    }

    fun submit() {
        val state = _uiState.value ?: return
        if (state.isFinished) return

        val correct = when (state.problem.kind) {
            // The picked cards (by index) must be the asked-for count and
            // hit the target. Any valid combination counts.
            ProblemKind.TARGET -> {
                val picked = state.input.map { it.digitToInt() }
                picked.size == state.problem.pickCount &&
                    picked.sumOf { state.problem.cards.getOrElse(it) { 0 } } == state.problem.answer
            }
            // Every rule-fitting card and nothing extra; values name the
            // cards because a hunt's spread is distinct.
            ProblemKind.SELECT -> {
                val picked = state.input
                    .map { state.problem.cards.getOrElse(it.digitToInt()) { -1 } }
                    .toSet()
                picked == state.problem.correctCards
            }
            else -> (state.input.toIntOrNull() ?: return) == state.problem.answer
        }

        if (correct) {
            // Adaptivity and the fastest time only count first attempts;
            // a miss on this problem was already recorded.
            val firstAttempt = state.attempts == 0
            val solveTimeMs = System.currentTimeMillis() - problemShownAtMs
            viewModelScope.launch {
                progressRepository.recordCorrect(
                    firstAttempt = firstAttempt,
                    solveTimeMs = solveTimeMs,
                    topic = state.problem.kind.topic,
                    countsTowardLevel = _practice.value == null,
                )
            }
            _uiState.update {
                it?.copy(
                    phase = AnswerPhase.CORRECT,
                    encouragementSeed = maybeEncouragementSeed(),
                )
            }
            notifyProblemFinished()
        } else {
            if (state.attempts == 0) {
                recordMiss(state.problem.kind.topic)
            }
            val attempts = state.attempts + 1
            // One-tap exercises offer fewer choices, so they forgive one
            // miss less than typed answers do.
            val maxAttempts = if (state.problem.submitsOnTap) MAX_TAP_ATTEMPTS else MAX_ATTEMPTS
            _uiState.update {
                it?.copy(
                    phase = if (attempts >= maxAttempts) AnswerPhase.REVEALED else AnswerPhase.TRY_AGAIN,
                    attempts = attempts,
                    input = "",
                )
            }
            if (attempts >= maxAttempts) notifyProblemFinished()
        }
    }

    /**
     * Two nudges, then the third press shows the answer. Hints never cost
     * first-try credit. Only wrong answers and timeouts do. The revealing
     * press counts as a miss only when she hadn't attempted yet, same as a
     * timeout.
     */
    fun useHint() {
        val state = _uiState.value ?: return
        if (state.isFinished) return
        if (state.hintsUsed < MAX_HINTS - 1) {
            _uiState.update { it?.copy(hintsUsed = state.hintsUsed + 1) }
            return
        }
        if (state.attempts == 0) {
            recordMiss(state.problem.kind.topic)
        }
        _uiState.update {
            it?.copy(phase = AnswerPhase.REVEALED, hintsUsed = MAX_HINTS, input = "")
        }
        notifyProblemFinished()
    }

    /** A first-attempt miss. Drills never drag the break level down. */
    private fun recordMiss(topic: ProblemTopic) {
        val drilling = _practice.value != null
        viewModelScope.launch {
            progressRepository.recordIncorrect(topic, countsTowardLevel = !drilling)
        }
    }

    private fun notifyProblemFinished() {
        timerJob?.cancel()
        // A drill isn't a break: there's no notification to clear and no
        // per-break cap to fill, so she can keep going as long as she likes.
        if (_practice.value != null) return
        _sessionDone.value++
        viewModelScope.launch {
            onProblemFinished()
            val limit = settingsRepository.settings.first().problemsPerBreak
            if (limit > 0 && _sessionDone.value >= limit) {
                _uiState.update { it?.copy(sessionComplete = true) }
            }
        }
    }

    fun nextProblem() {
        viewModelScope.launch { deal() }
    }

    private suspend fun deal() {
        val settings = settingsRepository.settings.first()
        val drill = _practice.value
        val difficulty = drill?.difficulty ?: progressRepository.currentDifficulty.first()
        // A drill is one topic at one level she chose herself, so it uses
        // that flat. A break asks each topic for its own level.
        val levels = if (drill != null) null else progressRepository.topicLevels.first()
        val problem: Problem = generator.generate(
            difficulty = difficulty,
            language = settings.language,
            topics = drill?.let { setOf(it.topic) } ?: settings.enabledTopics,
            levelFor = { topic -> levels?.get(topic) ?: difficulty },
        )
        problemShownAtMs = System.currentTimeMillis()
        // Tricky problems get extra time: the single highest factor
        // (hard ×2, logic ×1.5, names ×1.25). Never stacked. A drill is
        // untimed however the timer setting stands.
        val timerSeconds = settings.timerMinutes
            .takeIf { it > 0 && drill == null }
            ?.let { minutes -> (minutes * 60 * problem.timerMultiplier).roundToInt() }
        _uiState.update { ProblemUiState(problem = problem, remainingSeconds = timerSeconds) }
        startTimer(timerSeconds)
    }

    private fun startTimer(totalSeconds: Int?) {
        timerJob?.cancel()
        if (totalSeconds == null) return
        timerJob = viewModelScope.launch {
            // Count against a deadline so ticks can't drift.
            val deadline = System.currentTimeMillis() + totalSeconds * 1_000L
            while (true) {
                val remaining = ((deadline - System.currentTimeMillis()) / 1_000).toInt()
                if (remaining <= 0) break
                _uiState.update { it?.copy(remainingSeconds = remaining) }
                delay(1_000)
            }
            onTimeUp()
        }
    }

    private fun onTimeUp() {
        val state = _uiState.value ?: return
        if (state.isFinished) return
        // Untouched problems count as a miss for the adaptive level; if she
        // already missed an attempt, that was recorded then.
        if (state.attempts == 0) {
            recordMiss(state.problem.kind.topic)
        }
        _uiState.update {
            it?.copy(
                phase = AnswerPhase.REVEALED,
                timedOut = true,
                remainingSeconds = 0,
                input = "",
            )
        }
        notifyProblemFinished()
    }

    private fun maybeEncouragementSeed(): Int? =
        if (random.nextInt(100) < PersonalContent.ENCOURAGEMENT_CHANCE_PERCENT) {
            random.nextInt(Int.MAX_VALUE)
        } else {
            null
        }

    companion object {
        const val MAX_ATTEMPTS = 3

        /** Tap kinds ([Problem.submitsOnTap]) reveal one miss earlier. */
        const val MAX_TAP_ATTEMPTS = 2
        const val MAX_HINTS = 3
        private const val MAX_INPUT_DIGITS = 5

        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                ProblemViewModel(
                    settingsRepository = SettingsRepository(app),
                    progressRepository = ProgressRepository(app),
                    onProblemFinished = { BreakCoordinator.breakCompleted(app) },
                    challengeRepository = ChallengeRepository(app),
                )
            }
        }
    }
}
