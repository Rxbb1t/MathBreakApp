package com.ak.momapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ak.momapp.alarm.BreakCoordinator
import com.ak.momapp.data.ActiveWindow
import com.ak.momapp.data.BrainBreakSettings
import com.ak.momapp.data.BreakStateRepository
import com.ak.momapp.data.ProgressRepository
import com.ak.momapp.data.SettingsRepository
import com.ak.momapp.data.SetupPreset
import com.ak.momapp.i18n.AppLanguage
import com.ak.momapp.problem.Difficulty
import com.ak.momapp.problem.Level
import com.ak.momapp.problem.ProblemTopic
import com.ak.momapp.ui.theme.AppPalette
import com.ak.momapp.ui.theme.UiSkin
import java.time.DayOfWeek
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    breakStateRepository: BreakStateRepository,
    // Alarm side effect as a lambda so the ViewModel stays context-free.
    private val reschedule: suspend () -> Unit = {},
    // Null keeps the class testable off-device; only the Exercises screen
    // needs the per-topic levels.
    private val progressRepository: ProgressRepository? = null,
) : ViewModel() {

    /**
     * The NAME each topic is at, for the Exercises rows. The fine points
     * underneath are deliberately not exposed: she sees Easy, Normal or
     * Hard, and there is no number to chase.
     */
    val topicLevels: StateFlow<Map<ProblemTopic, Difficulty>> =
        (progressRepository?.topicBands ?: emptyFlow())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /**
     * The fine position behind each of those names, for the long-press
     * readout only. Nothing on the ordinary screen reads this.
     */
    val topicPoints: StateFlow<Map<ProblemTopic, Level>> =
        (progressRepository?.topicLevels ?: emptyFlow())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())


    // Null while the first DataStore read is in flight.
    val settings: StateFlow<BrainBreakSettings?> = repository.settings
        .map { it as BrainBreakSettings? }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Epoch millis of the next scheduled break, for display. */
    val nextBreakAt: StateFlow<Long?> = breakStateRepository.nextBreakAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Applies one of the setup presets from Settings, the same way the
     * first-run guide does. It restarts the adaptive level and clears the
     * per-topic ladders, which is the point: she is saying the app has the
     * wrong idea of her, so keeping what it worked out would defeat it.
     */
    fun applyPreset(preset: SetupPreset) {
        viewModelScope.launch { repository.applyPreset(preset) }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setRemindersEnabled(enabled)
            reschedule()
        }
    }

    fun setIntervalHours(hours: Int) {
        viewModelScope.launch {
            repository.setIntervalHours(hours)
            reschedule()
        }
    }

    fun setActiveStart(minutes: Int) {
        val current = settings.value ?: return
        val (start, end) = ActiveWindow.applyStart(minutes, current.activeEndMinutes)
        viewModelScope.launch {
            repository.setActiveWindow(start, end)
            reschedule()
        }
    }

    fun setActiveEnd(minutes: Int) {
        val current = settings.value ?: return
        val (start, end) = ActiveWindow.applyEnd(minutes, current.activeStartMinutes)
        viewModelScope.launch {
            repository.setActiveWindow(start, end)
            reschedule()
        }
    }

    fun toggleDay(day: DayOfWeek) {
        val current = settings.value ?: return
        val days = if (day in current.activeDays) current.activeDays - day else current.activeDays + day
        viewModelScope.launch {
            repository.setActiveDays(days)
            reschedule()
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { repository.setLanguage(language) }
    }

    fun setProblemsPerBreak(count: Int) {
        viewModelScope.launch { repository.setProblemsPerBreak(count) }
    }

    /** Flips one problem-type switch; never drops below [ProblemTopic.MIN_ENABLED]. */
    fun toggleTopic(topic: ProblemTopic) {
        val current = settings.value ?: return
        val updated =
            if (topic in current.enabledTopics) current.enabledTopics - topic
            else current.enabledTopics + topic
        if (updated.size < ProblemTopic.MIN_ENABLED) return
        viewModelScope.launch { repository.setEnabledTopics(updated) }
    }

    fun setPalette(palette: AppPalette) {
        viewModelScope.launch { repository.setPalette(palette) }
    }

    fun setStartingDifficulty(difficulty: Difficulty) {
        viewModelScope.launch { repository.setStartingDifficulty(difficulty) }
    }

    fun setSuccessSound(enabled: Boolean) {
        viewModelScope.launch { repository.setSuccessSound(enabled) }
    }

    fun setSkin(skin: UiSkin) {
        viewModelScope.launch { repository.setSkin(skin) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                SettingsViewModel(
                    repository = SettingsRepository(app),
                    breakStateRepository = BreakStateRepository(app),
                    reschedule = { BreakCoordinator.rescheduleFromNow(app) },
                    progressRepository = ProgressRepository(app),
                )
            }
        }
    }
}
