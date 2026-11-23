package com.maikol.fitnessapp.feature.foundation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.core.database.AppMetadataRepository
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

enum class DatabaseStatus { CHECKING, READY, ERROR }

sealed interface FoundationUiState {
    data object Loading : FoundationUiState

    data class Ready(
        val unitSystem: UnitSystem,
        val databaseStatus: DatabaseStatus,
        val saveFailed: Boolean,
    ) : FoundationUiState
}

/** Pantalla técnica de H0: demuestra tema, preferencia persistente (DataStore) y apertura de Room. */
class FoundationViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    appMetadataRepository: AppMetadataRepository,
) : ViewModel() {

    private val saveFailed = MutableStateFlow(false)

    private val databaseStatus = appMetadataRepository.databaseCreatedAtMillis
        .map { DatabaseStatus.READY }
        .onStart { emit(DatabaseStatus.CHECKING) }
        // Fallos al abrir/leer SQLite (p. ej. SQLiteException) se muestran como estado recuperable.
        .catch { emit(DatabaseStatus.ERROR) }

    val uiState: StateFlow<FoundationUiState> = combine(
        preferencesRepository.unitSystem,
        databaseStatus,
        saveFailed,
    ) { unitSystem, dbStatus, failed ->
        FoundationUiState.Ready(unitSystem = unitSystem, databaseStatus = dbStatus, saveFailed = failed)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = FoundationUiState.Loading,
    )

    fun onUnitSystemSelected(unitSystem: UnitSystem) {
        viewModelScope.launch {
            saveFailed.value = try {
                preferencesRepository.setUnitSystem(unitSystem)
                false
            } catch (e: IOException) {
                true
            }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FitnessApplication).container
                FoundationViewModel(container.userPreferencesRepository, container.appMetadataRepository)
            }
        }
    }
}
