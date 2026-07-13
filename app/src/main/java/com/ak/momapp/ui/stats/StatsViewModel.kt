package com.ak.momapp.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ak.momapp.data.BrainBreakStats
import com.ak.momapp.data.ProgressRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(progressRepository: ProgressRepository) : ViewModel() {

    // Null while the first DataStore read is in flight.
    val stats: StateFlow<BrainBreakStats?> = progressRepository.stats
        .map { it as BrainBreakStats? }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                StatsViewModel(ProgressRepository(app))
            }
        }
    }
}
