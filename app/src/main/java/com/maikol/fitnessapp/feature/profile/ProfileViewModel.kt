package com.maikol.fitnessapp.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.core.preferences.UserPreferences
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

enum class NameSaveStatus { IDLE, SAVING, SAVED, FAILED }

/** Ajuste inmediato (no el nombre) cuya última escritura falló. */
enum class FailedSetting { UNITS, MOTION }

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Ready(
        val nameDraft: String,
        val savedName: String?,
        val nameStatus: NameSaveStatus,
        val unitSystem: UnitSystem,
        val reduceMotion: Boolean,
        val failedSetting: FailedSetting?,
    ) : ProfileUiState {
        val canSave: Boolean
            get() = nameStatus != NameSaveStatus.SAVING && nameDraft.trim() != (savedName ?: "")
    }
}

/**
 * Perfil H1. El borrador del nombre vive en SavedStateHandle (sobrevive a rotación y muerte de
 * proceso); solo se confirma «Guardado» tras la escritura correcta en DataStore.
 */
class ProfileViewModel(
    private val repository: UserPreferencesRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val nameStatus = MutableStateFlow(NameSaveStatus.IDLE)
    private val failedSetting = MutableStateFlow<FailedSetting?>(null)
    private val draft: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_DRAFT, null)

    val uiState: StateFlow<ProfileUiState> = combine(
        repository.userPreferences,
        draft,
        nameStatus,
        failedSetting,
    ) { prefs: UserPreferences, draft, status, failed ->
        ProfileUiState.Ready(
            nameDraft = draft ?: prefs.displayName.orEmpty(),
            savedName = prefs.displayName,
            nameStatus = status,
            unitSystem = prefs.unitSystem,
            reduceMotion = prefs.reduceMotion,
            failedSetting = failed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState.Loading)

    fun onNameChange(value: String) {
        savedStateHandle[KEY_DRAFT] = value
        // Un error de guardado sigue visible hasta el siguiente intento; «Guardado» deja de aplicar.
        if (nameStatus.value == NameSaveStatus.SAVED) nameStatus.value = NameSaveStatus.IDLE
    }

    fun saveName() {
        val state = uiState.value as? ProfileUiState.Ready ?: return
        if (!state.canSave) return
        nameStatus.value = NameSaveStatus.SAVING
        viewModelScope.launch {
            nameStatus.value = try {
                repository.setDisplayName(state.nameDraft)
                savedStateHandle[KEY_DRAFT] = state.nameDraft.trim()
                NameSaveStatus.SAVED
            } catch (e: IOException) {
                NameSaveStatus.FAILED
            }
        }
    }

    fun onUnitSystemSelected(unitSystem: UnitSystem) = writeSetting(FailedSetting.UNITS) {
        repository.setUnitSystem(unitSystem)
    }

    fun onReduceMotionChange(enabled: Boolean) = writeSetting(FailedSetting.MOTION) {
        repository.setReduceMotion(enabled)
    }

    private fun writeSetting(setting: FailedSetting, write: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                write()
                if (failedSetting.value == setting) failedSetting.value = null
            } catch (e: IOException) {
                failedSetting.value = setting
            }
        }
    }

    companion object {
        private const val KEY_DRAFT = "name_draft"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FitnessApplication).container
                ProfileViewModel(container.userPreferencesRepository, createSavedStateHandle())
            }
        }
    }
}
