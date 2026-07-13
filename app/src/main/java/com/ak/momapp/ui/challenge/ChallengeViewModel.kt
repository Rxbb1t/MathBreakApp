package com.ak.momapp.ui.challenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ak.momapp.data.ChallengeRepository
import com.ak.momapp.data.SettingsRepository
import com.ak.momapp.problem.DailyChallenge
import com.ak.momapp.problem.DailyChallengeGenerator
import com.ak.momapp.problem.Problem
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ChallengePhase {
    /** Typing an answer for the current stage. */
    ANSWERING,

    /** A wrong answer landed; same stage, try again. */
    TRY_AGAIN,

    /** Stage solved or revealed; the Continue button moves the story on. */
    STAGE_DONE,

    /** All five stages behind her — or she opens an already-done day. */
    COMPLETE,
}

data class ChallengeUiState(
    val intro: String,
    val stages: List<Problem>,
    val stageIndex: Int = 0,
    val input: String = "",
    val phase: ChallengePhase = ChallengePhase.ANSWERING,
    val hintsUsed: Int = 0,
    val currentHint: String? = null,
    val totalCompleted: Int = 0,
    /** Bumped on every solve so the confetti knows when to fire. */
    val celebrations: Int = 0,
) {
    val stage: Problem get() = stages[stageIndex]
}

/**
 * Runs the daily challenge. Deliberately separate from the break flow:
 * nothing here touches the adaptive difficulty, the solved-today count,
 * or the per-break cap — a rough challenge day can't demote her level.
 */
class ChallengeViewModel(
    private val settingsRepository: SettingsRepository,
    private val challengeRepository: ChallengeRepository,
    private val generator: DailyChallengeGenerator = DailyChallengeGenerator(),
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChallengeUiState?>(null)
    val uiState: StateFlow<ChallengeUiState?> = _uiState.asStateFlow()

    private var day: Long = 0

    init {
        viewModelScope.launch {
            val language = settingsRepository.settings.first().language
            val saved = challengeRepository.state.first()
            val date = today()
            day = date.toEpochDay()
            val challenge: DailyChallenge = generator.generate(date, language)
            val sameDay = saved.day == day
            _uiState.value = ChallengeUiState(
                intro = challenge.intro,
                stages = challenge.stages,
                stageIndex = if (sameDay) saved.stage.coerceIn(0, challenge.stages.lastIndex) else 0,
                phase = if (sameDay && saved.done) ChallengePhase.COMPLETE else ChallengePhase.ANSWERING,
                totalCompleted = saved.totalCompleted,
            )
            if (!sameDay) challengeRepository.saveProgress(day, 0)
        }
    }

    fun onInputChange(value: String) {
        _uiState.update { it?.copy(input = value.filter(Char::isDigit).take(5)) }
    }

    fun submit() {
        val state = _uiState.value ?: return
        if (state.phase != ChallengePhase.ANSWERING && state.phase != ChallengePhase.TRY_AGAIN) return
        val guess = state.input.toIntOrNull() ?: return
        if (guess == state.stage.answer) {
            finishStage()
        } else {
            _uiState.update { it?.copy(phase = ChallengePhase.TRY_AGAIN) }
        }
    }

    /**
     * Two nudges and that's all — the challenge never reveals an answer,
     * so a stage can only be passed by solving it.
     */
    fun useHint() {
        val state = _uiState.value ?: return
        if (state.phase != ChallengePhase.ANSWERING && state.phase != ChallengePhase.TRY_AGAIN) return
        if (state.hintsUsed >= MAX_HINTS) return
        _uiState.update {
            it?.copy(
                hintsUsed = state.hintsUsed + 1,
                currentHint = state.stage.hints.getOrNull(state.hintsUsed),
            )
        }
    }

    private fun finishStage() {
        val state = _uiState.value ?: return
        val last = state.stageIndex == state.stages.lastIndex
        if (last) {
            _uiState.update {
                it?.copy(
                    phase = ChallengePhase.COMPLETE,
                    totalCompleted = state.totalCompleted + 1,
                    celebrations = state.celebrations + 1,
                )
            }
            viewModelScope.launch { challengeRepository.markCompleted(day) }
        } else {
            _uiState.update {
                it?.copy(
                    phase = ChallengePhase.STAGE_DONE,
                    celebrations = state.celebrations + 1,
                )
            }
        }
    }

    fun nextStage() {
        val state = _uiState.value ?: return
        if (state.phase != ChallengePhase.STAGE_DONE) return
        val next = state.stageIndex + 1
        _uiState.update {
            it?.copy(
                stageIndex = next,
                input = "",
                phase = ChallengePhase.ANSWERING,
                hintsUsed = 0,
                currentHint = null,
            )
        }
        viewModelScope.launch { challengeRepository.saveProgress(day, next) }
    }

    companion object {
        const val MAX_HINTS = 2

        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                ChallengeViewModel(
                    settingsRepository = SettingsRepository(app),
                    challengeRepository = ChallengeRepository(app),
                )
            }
        }
    }
}
