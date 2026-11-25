package com.maikol.fitnessapp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.core.preferences.Goal
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import com.maikol.fitnessapp.core.tracking.TrackingRepository
import com.maikol.fitnessapp.feature.training.data.OpenSession
import com.maikol.fitnessapp.feature.training.data.RoutineSummary
import com.maikol.fitnessapp.feature.training.data.StartResult
import com.maikol.fitnessapp.feature.training.data.TrainingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Contenido de «Tu próximo entrenamiento», siempre desde Room y DataStore. */
sealed interface NextWorkout {
    /** Sin rutinas activas: estado vacío real de H1. */
    data object None : NextWorkout

    /** Hay una sesión abierta: se continúa, nunca se crea otra. */
    data class Active(val session: OpenSession) : NextWorkout

    /** [selected]: elegida como próxima en Entrenar; si no, la primera activa. */
    data class Planned(val routine: RoutineSummary, val selected: Boolean) : NextWorkout
}

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(
        val displayName: String?,
        val next: NextWorkout,
        /** Registro manual de hoy; null si no hay. */
        val activity: ActivityMetrics? = null,
        val starting: Boolean = false,
        val startFailed: Boolean = false,
        /** Evento: sesión que abrir tras iniciar. */
        val sessionToOpen: String? = null,
    ) : HomeUiState
}

private data class StartState(val starting: Boolean = false, val failed: Boolean = false, val sessionToOpen: String? = null)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    preferencesRepository: UserPreferencesRepository,
    private val trainingRepository: TrainingRepository,
    trackingRepository: TrackingRepository,
) : ViewModel() {

    private val start = MutableStateFlow(StartState())

    val uiState: StateFlow<HomeUiState> = combine(
        preferencesRepository.userPreferences,
        trainingRepository.observeRoutines(),
        trainingRepository.observeOpenSession(),
        start,
        // «Hoy» vivo: cambia a medianoche y al volver a la app se lee el registro del día nuevo.
        trackingRepository.observeToday().flatMapLatest { trackingRepository.observeActivity(it) },
    ) { prefs, routines, open, s, today ->
        val active = routines.filterNot { it.archived }
        val selected = active.firstOrNull { it.id == prefs.nextRoutineId }
        val next = when {
            open != null -> NextWorkout.Active(open)
            selected != null -> NextWorkout.Planned(selected, selected = true)
            active.isNotEmpty() -> NextWorkout.Planned(active.first(), selected = false)
            else -> NextWorkout.None
        }
        val activity = today?.let {
            ActivityMetrics(it.steps, prefs.goals[Goal.STEPS], it.distanceMeters, it.activeKcal, it.activeMinutes, prefs.unitSystem)
        }
        HomeUiState.Ready(prefs.displayName, next, activity, s.starting, s.failed, s.sessionToOpen)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    /** Doble toque: ignorado mientras se inicia; el repositorio garantiza además una sola sesión abierta. */
    fun startRoutine(routineId: String) {
        if (start.value.starting) return
        start.value = StartState(starting = true)
        viewModelScope.launch {
            try {
                start.value = when (val result = trainingRepository.startSession(routineId)) {
                    is StartResult.Started -> StartState(sessionToOpen = result.sessionId)
                    is StartResult.AlreadyOpen -> StartState(sessionToOpen = result.sessionId)
                    StartResult.NotRunnable -> StartState(failed = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                start.value = StartState(failed = true)
            }
        }
    }

    fun onSessionOpened() = start.update { it.copy(sessionToOpen = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FitnessApplication).container
                HomeViewModel(container.userPreferencesRepository, container.trainingRepository, container.trackingRepository)
            }
        }
    }
}
