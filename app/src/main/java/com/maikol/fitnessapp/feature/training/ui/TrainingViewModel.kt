package com.maikol.fitnessapp.feature.training.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import com.maikol.fitnessapp.feature.training.data.HistoryEntry
import com.maikol.fitnessapp.feature.training.data.OpenSession
import com.maikol.fitnessapp.feature.training.data.RoutineSummary
import com.maikol.fitnessapp.feature.training.data.TrainingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrainingUiState(
    val loading: Boolean = true,
    val routines: List<RoutineSummary> = emptyList(),
    val archived: List<RoutineSummary> = emptyList(),
    val nextRoutineId: String? = null,
    val openSession: OpenSession? = null,
    val recentHistory: List<HistoryEntry> = emptyList(),
    val historyCount: Int = 0,
    val unit: UnitSystem = UnitSystem.METRIC,
    val actionFailed: Boolean = false,
)

class TrainingViewModel(
    private val repository: TrainingRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val actionFailed = MutableStateFlow(false)

    val uiState: StateFlow<TrainingUiState> = combine(
        repository.observeRoutines(),
        repository.observeOpenSession(),
        repository.observeHistory(),
        preferences.userPreferences,
        actionFailed,
    ) { routines, open, history, prefs, failed ->
        TrainingUiState(
            loading = false,
            routines = routines.filterNot { it.archived },
            archived = routines.filter { it.archived },
            nextRoutineId = prefs.nextRoutineId?.takeIf { id -> routines.any { it.id == id && !it.archived } },
            openSession = open,
            recentHistory = history.take(RECENT_HISTORY),
            historyCount = history.size,
            unit = prefs.unitSystem,
            actionFailed = failed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrainingUiState())

    fun duplicate(id: String, suffix: String) = act { repository.duplicateRoutine(id, suffix) }

    /** Archivar la rutina elegida como próxima también limpia esa selección (no queda un ID colgando). */
    fun archive(id: String) = act {
        repository.setArchived(id, true)
        if (uiState.value.nextRoutineId == id) preferences.setNextRoutineId(null)
    }

    fun restore(id: String) = act { repository.setArchived(id, false) }

    fun toggleNext(id: String) = act {
        preferences.setNextRoutineId(if (uiState.value.nextRoutineId == id) null else id)
    }

    fun dismissError() = actionFailed.update { false }

    private fun act(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                actionFailed.value = true
            }
        }
    }

    companion object {
        const val RECENT_HISTORY = 3

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FitnessApplication).container
                TrainingViewModel(container.trainingRepository, container.userPreferencesRepository)
            }
        }
    }
}
