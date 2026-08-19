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
import com.ak.momapp.problem.Level
import com.ak.momapp.problem.Outcome
import com.ak.momapp.problem.PersonalContent
import com.ak.momapp.problem.Problem
import com.ak.momapp.problem.ProblemGenerator
import com.ak.momapp.problem.ProblemKind
import com.ak.momapp.problem.ProblemShape
import com.ak.momapp.problem.ProblemTopic
import com.ak.momapp.problem.Warmup
import com.ak.momapp.problem.toLevel
import com.ak.momapp.problem.topic
import java.time.LocalDate
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
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

    /** Out of tries (three typed, two tapped), or she asked. Answer shown. */
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
    /** Hint-button presses so far; the third press reveals the answer. */
    val hintsUsed: Int = 0,
    /**
     * Solved by landing near the answer rather than on it, which only an
     * estimate allows. Changes the wording of the praise so she is handed
     * the exact number she was close to.
     */
    val closeEnough: Boolean = false,
) {
    val isFinished: Boolean
        get() = phase == AnswerPhase.CORRECT || phase == AnswerPhase.REVEALED

    /** The nudge currently on screen, if any hint has been used. */
    val currentHint: String?
        get() = if (hintsUsed > 0) problem.hints.getOrNull(hintsUsed - 1) else null
}

/** One drill: a single topic at a level she picked herself. */
data class PracticeConfig(val topic: ProblemTopic, val difficulty: Difficulty)

/**
 * Whether an answer arriving at [now] counts, for a problem dealt at
 * [dealtAt].
 *
 * A negative gap means the wall clock moved backwards under us, which an
 * NTP correction can do at any moment. That is allowed through rather
 * than blocked: the worst a wrongly accepted tap does is answer one
 * problem, while a wrongly rejected one would leave her tapping a dead
 * screen with no way forward.
 */
fun acceptsInputAt(dealtAt: Long, now: Long): Boolean {
    val elapsed = now - dealtAt
    return elapsed >= ProblemViewModel.INPUT_LOCKOUT_MS || elapsed < 0
}

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

    // The day's tally used to be exposed here for the Start and problem
    // screens to print. Neither shows it any more -- it is one of the
    // figures on the stats screen, which reads the same repository -- so
    // the flow is gone rather than left running with nobody collecting it.

    /** The per-break cap, for the sitting's progress dots (0 = no cap). */
    val sessionLimit: StateFlow<Int> = settingsRepository.settings
        .map { it.problemsPerBreak }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** A soft chime on a correct answer, unless switched off in Settings. */
    val successSound: StateFlow<Boolean> = settingsRepository.settings
        .map { it.successSound }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Where she sits on the fine scale. Shown ONLY behind the long-press on
     * the level chip, never as part of the ordinary screen: a number on
     * display is a number to chase, and this app deliberately has no score.
     */
    val currentLevel: StateFlow<Level?> = progressRepository.currentLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Problems finished this sitting; drives the cap and the dots.
    private val _sessionDone = MutableStateFlow(0)
    val sessionDone: StateFlow<Int> = _sessionDone.asStateFlow()

    /**
     * Problems DEALT this sitting, which the warm-up counts. Kept separate
     * from [_sessionDone] on purpose: that one counts finished problems for
     * the per-break cap and deliberately ignores skips, but a skipped
     * warm-up problem has still been seen, so easing the next one off the
     * same counter would re-ease forever for anyone who skips the openers.
     */
    private var sittingDealt = 0

    private var problemShownAtMs: Long = 0

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

    init {
        // The words are baked into a problem when it is generated, so a
        // language change has to rebuild the one she is looking at. Her
        // typed input, attempt count and phase all survive: only the
        // wording is replaced.
        viewModelScope.launch {
            settingsRepository.settings
                .map { it.language }
                .distinctUntilChanged()
                .drop(1)
                .collect { language ->
                    _uiState.update { state ->
                        val spec = state?.problem?.spec ?: return@update state
                        state.copy(problem = ProblemGenerator.replay(spec, language))
                    }
                }
        }
    }

    /** She picked a type and a level. Deal the first drill. */
    fun startPractice(topic: ProblemTopic, difficulty: Difficulty) {
        _practice.value = PracticeConfig(topic, difficulty)
        _sessionDone.value = 0
        sittingDealt = 0
        _started.value = true
        _uiState.value = null
        nextProblem()
    }

    /** Back out of a drill, to the picker. */
    fun endPractice() {
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
        clearSitting()
        startSession()
    }

    /**
     * Keep going past the per-break cap, offered on the problem screen the
     * moment she reaches it.
     *
     * Her level is deliberately untouched. Finishing the number of problems
     * she asked for and wanting more is the app working, not a reason to
     * hand back the ground it took to get there. That is the difference
     * between this and [resetSitting].
     */
    fun startNewRound() {
        clearSitting()
        _started.value = true
        nextProblem()
    }

    /**
     * The settings button that starts over properly: a fresh sitting AND
     * the adaptive level back to the chosen starting difficulty, with a
     * new problem at that level dealt right away.
     */
    fun resetSitting() {
        clearSitting()
        _started.value = true
        viewModelScope.launch {
            progressRepository.resetToStartingDifficulty()
            deal()
        }
    }

    /** Forgets what this sitting has done, so the cap starts over. */
    private fun clearSitting() {
        _sessionDone.value = 0
        sittingDealt = 0
        _uiState.update { it?.copy(sessionComplete = false) }
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
        // ONE call, not a stats call followed by a level call: anything
        // that reaches the ladder also ends her run of clean answers, so
        // recording the accuracy vote separately would end it twice.
        if (state.attempts == 0) {
            recordMiss(state.problem.kind.topic, Outcome.SKIPPED)
        } else {
            recordProblemLost(state.problem.kind.topic, Outcome.SKIPPED)
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
        // Guarded before the choice is even recorded, so a swallowed tap
        // leaves no trace of itself on screen.
        if (!acceptsInputAt(problemShownAtMs, System.currentTimeMillis())) return
        _uiState.update { state ->
            if (state == null || state.isFinished) state else state.copy(input = choice.toString())
        }
        submit()
    }

    fun submit() {
        val state = _uiState.value ?: return
        if (state.isFinished) return
        if (!acceptsInputAt(problemShownAtMs, System.currentTimeMillis())) return

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
            // Everything typed. Exact for almost every kind, but an
            // estimate carries a tolerance and accepts anything near
            // enough; see Problem.accepts.
            else -> state.problem.accepts(state.input.toIntOrNull() ?: return)
        }

        // Close but not exact, which only an estimate can be. She is told
        // the true number afterwards: landing near it is the skill, and
        // never learning what it was near would waste the moment.
        val approximate = state.input.toIntOrNull()
            ?.let { state.problem.isApproximate(it) } == true

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
                    shape = ProblemShape.of(state.problem),
                    effort = state.problem.effort,
                )
            }
            _uiState.update {
                it?.copy(
                    phase = AnswerPhase.CORRECT,
                    encouragementSeed = maybeEncouragementSeed(),
                    closeEnough = approximate,
                )
            }
            notifyProblemFinished()
        } else {
            // The first wrong attempt votes in the accuracy tally, but it
            // costs nothing on the ladder. Being wrong on the way to the
            // right answer is how the exercise works.
            if (state.attempts == 0) {
                recordStumble(state.problem.kind.topic)
            }
            val attempts = state.attempts + 1
            // How many tries a problem gets depends on how many answers it
            // offers, which the problem itself knows.
            val lost = attempts >= state.problem.maxAttempts
            _uiState.update {
                it?.copy(
                    phase = if (lost) AnswerPhase.REVEALED else AnswerPhase.TRY_AGAIN,
                    attempts = attempts,
                    input = "",
                )
            }
            if (lost) {
                // Out of tries: this is the one moment a wrong answer costs
                // anything, and it is charged once for the whole problem.
                recordProblemLost(state.problem.kind.topic, Outcome.LOST)
                notifyProblemFinished()
            }
        }
    }

    /**
     * Two nudges, then the third press shows the answer. Hints never cost
     * first-try credit; only wrong answers do. The revealing press counts
     * as a miss only when she hadn't attempted yet.
     */
    fun useHint() {
        val state = _uiState.value ?: return
        if (state.isFinished) return
        if (state.hintsUsed < MAX_HINTS - 1) {
            _uiState.update { it?.copy(hintsUsed = state.hintsUsed + 1) }
            return
        }
        // Asking to be shown the answer is giving up, not getting it wrong.
        // It costs the same as losing the problem: if it cost less, giving
        // up early would be the cheapest way out of anything hard.
        if (state.attempts == 0) {
            recordMiss(state.problem.kind.topic, Outcome.GAVE_UP)
        } else {
            recordProblemLost(state.problem.kind.topic, Outcome.GAVE_UP)
        }
        _uiState.update {
            it?.copy(phase = AnswerPhase.REVEALED, hintsUsed = MAX_HINTS, input = "")
        }
        notifyProblemFinished()
    }

    /**
     * The first stumble on a problem: casts its one vote in the accuracy
     * tally and files the shape for review.
     *
     * It does NOT move the level, because a wrong attempt she recovers from
     * is not evidence that the level is wrong. Only [recordProblemLost]
     * moves it.
     */
    private fun recordStumble(topic: ProblemTopic) = recordMiss(topic, Outcome.WRONG)

    /**
     * Casts a problem's one vote in the accuracy tally, files its shape for
     * review, and applies whatever [outcome] costs on the ladder. The two
     * halves stay in ONE call because they share the skip-run counter.
     */
    private fun recordMiss(topic: ProblemTopic, outcome: Outcome) {
        val drilling = _practice.value != null
        val problem = _uiState.value?.problem
        viewModelScope.launch {
            progressRepository.recordIncorrect(
                topic = topic,
                countsTowardLevel = !drilling,
                shape = problem?.let(ProblemShape::of),
                outcome = outcome,
                effort = problem?.effort ?: 1.0,
            )
        }
    }

    /**
     * The problem is gone: out of tries, out of time, or she asked to see
     * it. This is the only thing that lowers the level, and it happens once
     * per problem however many attempts went into it.
     */
    private fun recordProblemLost(topic: ProblemTopic, outcome: Outcome) {
        val drilling = _practice.value != null
        val effort = _uiState.value?.problem?.effort ?: 1.0
        viewModelScope.launch {
            progressRepository.recordLevelOnly(
                topic = topic,
                outcome = outcome,
                effort = effort,
                countsTowardLevel = !drilling,
            )
        }
    }

    private fun notifyProblemFinished() {
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
        val level = drill?.difficulty?.toLevel() ?: progressRepository.currentLevel.first()
        // A drill is one topic at one level she chose herself, so it uses
        // that flat. A break asks each topic for its own level.
        val levels = if (drill != null) null else progressRepository.topicLevels.first()
        // A drill is already aimed at one topic she picked, so nothing needs
        // steering toward anything; the queue only shapes ordinary breaks.
        val review = if (drill != null) {
            null
        } else {
            progressRepository.dueReview(settings.enabledTopics)
        }
        // The first couple of problems of a break are dealt a little
        // easier, to ease her in. Never during a drill, where she has
        // already chosen the level herself. Only what is dealt changes;
        // her stored level and everything the answer does to it are
        // untouched.
        fun eased(base: Level) = if (drill == null) Warmup.ease(base, sittingDealt) else base
        val problem: Problem = generator.generate(
            level = eased(level),
            language = settings.language,
            topics = drill?.let { setOf(it.topic) } ?: settings.enabledTopics,
            levelFor = { topic -> eased(levels?.get(topic) ?: level) },
            review = review,
        )
        // Recorded whatever else happens: how long an answer took is the
        // pace signal, and that is independent of any countdown being shown.
        problemShownAtMs = System.currentTimeMillis()
        _uiState.update { ProblemUiState(problem = problem) }
        sittingDealt++
    }

    private fun maybeEncouragementSeed(): Int? =
        if (random.nextInt(100) < PersonalContent.ENCOURAGEMENT_CHANCE_PERCENT) {
            random.nextInt(Int.MAX_VALUE)
        } else {
            null
        }

    companion object {
        const val MAX_HINTS = 3
        private const val MAX_INPUT_DIGITS = 5

        /**
         * How long a freshly dealt problem ignores input.
         *
         * Long enough to swallow the second half of a double-tap, short
         * enough that a deliberate answer never bounces. It matters most
         * on TRUE_FALSE, which allows a single attempt: there a stray
         * second tap loses the whole problem and costs seven points.
         */
        const val INPUT_LOCKOUT_MS = 350L

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
