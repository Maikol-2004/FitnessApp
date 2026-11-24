package com.maikol.fitnessapp.feature.training.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.component.LoadingRegion
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import com.maikol.fitnessapp.feature.training.data.HistoryEntry
import com.maikol.fitnessapp.feature.training.data.TrainingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(val loading: Boolean = true, val entries: List<HistoryEntry> = emptyList(), val unit: UnitSystem = UnitSystem.METRIC)

class HistoryViewModel(repository: TrainingRepository, preferences: UserPreferencesRepository) : ViewModel() {
    val state: StateFlow<HistoryUiState> = combine(repository.observeHistory(), preferences.userPreferences) { h, p ->
        HistoryUiState(loading = false, entries = h, unit = p.unitSystem)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FitnessApplication).container
                HistoryViewModel(container.trainingRepository, container.userPreferencesRepository)
            }
        }
    }
}

/** Historial: solo sesiones finalizadas (las descartadas no aparecen), de la más reciente a la más antigua. */
@Composable
fun HistoryRoute(
    onBack: () -> Unit,
    onOpenSummary: (String) -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TrainingTopBar(title = stringResource(R.string.history_title), onBack = onBack)
        LoadingRegion(isLoading = state.loading, message = stringResource(R.string.preparing_workout), modifier = Modifier.weight(1f)) {
            if (state.entries.isEmpty()) {
                Text(
                    stringResource(R.string.training_history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.l),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, top = Spacing.xs, bottom = Spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.entries, key = { it.id }) { entry ->
                        HistoryRow(entry, state.unit, onClick = { onOpenSummary(entry.id) })
                    }
                }
            }
        }
    }
}
