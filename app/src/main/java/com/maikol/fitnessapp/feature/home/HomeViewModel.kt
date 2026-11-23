package com.maikol.fitnessapp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface HomeUiState {
    data object Loading : HomeUiState

    /**
     * H1 no tiene fuentes de actividad ni rutinas: el resumen y el próximo entrenamiento se muestran
     * en su estado vacío real. H2/H3 añaden aquí sus datos sin rehacer la pantalla.
     */
    data class Ready(val displayName: String?) : HomeUiState
}

class HomeViewModel(preferencesRepository: UserPreferencesRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = preferencesRepository.userPreferences
        .map<_, HomeUiState> { HomeUiState.Ready(displayName = it.displayName) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FitnessApplication).container
                HomeViewModel(container.userPreferencesRepository)
            }
        }
    }
}
