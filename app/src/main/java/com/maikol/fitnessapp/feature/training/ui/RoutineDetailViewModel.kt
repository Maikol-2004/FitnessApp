package com.maikol.fitnessapp.feature.training.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import com.maikol.fitnessapp.feature.training.data.OpenSession
import com.maikol.fitnessapp.feature.training.data.PlannedSet
import com.maikol.fitnessapp.feature.training.data.Routine
import com.maikol.fitnessapp.feature.training.data.StartResult
import com.maikol.fitnessapp.feature.training.data.TrainingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val loading: Boolean = true,
    val routine: Routine? = null,
    val isNext: Boolean = false,
    val openSession: OpenSession? = null,
    val unit: UnitSystem = UnitSystem.METRIC,
    val starting: Boolean = false,
    val actionFailed: Boolean = false,
    /** Evento: sesión a abrir (nueva o la ya abierta). */
    val sessionToOpen: String? = null,
) {
    val openIsThisRoutine: Boolean get() = openSession != null && openSession.routineId == routine?.id
    val blockedByOtherSession: Boolean get() = openSession != null && !openIsThisRoutine
}

/** Grupo de series consecutivas iguales para leer el plan de un vistazo («3 × 10 · 20 kg»). */
data class SetGroup(val count: Int, val reps: Int, val loadKg: Double?, val warmup: Boolean)

fun List<PlannedSet>.grouped(): List<SetGroup> = fold(mutableListOf()) { acc, s ->
    val last = acc.lastOrNull()
    if (last != null && last.reps == s.reps && last.loadKg == s.loadKg && last.warmup == s.warmup) {
        acc[acc.lastIndex] = last.copy(count = last.count + 1)
    } else {
        acc += SetGroup(1, s.reps, s.loadKg, s.warmup)
    }
    acc
}

class RoutineDetailViewModel(
    savedState: SavedStateHandle,
    private val repository: TrainingRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val routineId: String = checkNotNull(savedState[ARG_ROUTINE_ID])
    private val local = MutableStateFlow(DetailUiState())

    val state: StateFlow<DetailUiState> = combine(
        repository.observeRoutine(routineId),
        repository.observeOpenSession(),
        preferences.userPreferences,
        local,
    ) { routine, open, prefs, l ->
        l.copy(
            loading = false,
            routine = routine,
            isNext = prefs.nextRoutineId == routineId && routine?.archived == false,
            openSession = open,
            unit = prefs.unitSystem,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    /** Doble toque o segunda ruta: el repositorio crea como mucho una sesión abierta. */
    fun start() {
        if (local.value.starting) return
        local.update { it.copy(starting = true, actionFailed = false) }
        viewModelScope.launch {
            try {
                when (val result = repository.startSession(routineId)) {
                    is StartResult.Started -> local.update { it.copy(starting = false, sessionToOpen = result.sessionId) }
                    is StartResult.AlreadyOpen -> local.update { it.copy(starting = false, sessionToOpen = result.sessionId) }
                    StartResult.NotRunnable -> local.update { it.copy(starting = false, actionFailed = true) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                local.update { it.copy(starting = false, actionFailed = true) }
            }
        }
    }

    fun onSessionOpened() = local.update { it.copy(sessionToOpen = null) }

    fun toggleNext() = act { preferences.setNextRoutineId(if (state.value.isNext) null else routineId) }

    fun setArchived(archived: Boolean) = act {
        repository.setArchived(routineId, archived)
        if (archived && state.value.isNext) preferences.setNextRoutineId(null)
    }

    fun duplicate(suffix: String, onCreated: (String) -> Unit) = act {
        repository.duplicateRoutine(routineId, suffix)?.let(onCreated)
    }

    private fun act(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                local.update { it.copy(actionFailed = true) }
            }
        }
    }

    companion object {
        const val ARG_ROUTINE_ID = "routineId"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FitnessApplication).container
                RoutineDetailViewModel(createSavedStateHandle(), container.trainingRepository, container.userPreferencesRepository)
            }
        }
    }
}
