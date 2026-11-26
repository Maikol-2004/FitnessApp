package com.maikol.fitnessapp.feature.nutrition.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.tracking.formatInteger
import com.maikol.fitnessapp.feature.nutrition.data.GoalChange
import com.maikol.fitnessapp.feature.nutrition.data.GoalValues
import com.maikol.fitnessapp.feature.nutrition.data.Nutrient
import com.maikol.fitnessapp.feature.nutrition.data.NutritionRepository
import com.maikol.fitnessapp.feature.nutrition.data.goalFor
import com.maikol.fitnessapp.feature.tracking.formatDay
import com.maikol.fitnessapp.feature.training.ui.InlineMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

data class GoalsUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val history: List<GoalChange> = emptyList(),
    val drafts: Map<Nutrient, String> = emptyMap(),
    val errors: Set<Nutrient> = emptySet(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val failed: Boolean = false,
) {
    val current get() = goalFor(today, history)
}

/**
 * Objetivos manuales opcionales (kcal y gramos). Cada guardado es un cambio con fecha efectiva de hoy: los días
 * anteriores conservan su objetivo o quedan sin él. Mismo escritor que Perfil.
 */
class GoalsViewModel(private val handle: SavedStateHandle, private val repository: NutritionRepository) : ViewModel() {
    private val drafts = handle.getStateFlow<ArrayList<String>?>(KEY_DRAFTS, null)
    private val status = MutableStateFlow(GoalsUiState())

    val state: StateFlow<GoalsUiState> = combine(repository.observeGoalHistory(), drafts, status) { h, d, s ->
        val today = LocalDate.now(repository.zone())
        val current = goalFor(today, h)?.values
        val texts = d?.let { l -> Nutrient.entries.associateWith { l[it.ordinal] } }
            ?: Nutrient.entries.associateWith { current?.get(it)?.toString().orEmpty() }
        s.copy(loading = false, today = today, history = h, drafts = texts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState())

    fun onChange(n: Nutrient, text: String) {
        val base = state.value.drafts
        handle[KEY_DRAFTS] = ArrayList(Nutrient.entries.map { if (it == n) text.filter(Char::isDigit).take(5) else base[it].orEmpty() })
        status.update { it.copy(errors = emptySet(), saved = false, failed = false) }
    }

    fun save() {
        val s = state.value
        if (s.saving) return
        val parsed = Nutrient.entries.associateWith { s.drafts[it].orEmpty().trim().takeIf(String::isNotEmpty)?.toIntOrNull() }
        val errors = Nutrient.entries.filter { n ->
            val text = s.drafts[n].orEmpty().trim()
            text.isNotEmpty() && parsed[n]?.let { NutritionRepository.goalRange(n).contains(it) } != true
        }.toSet()
        if (errors.isNotEmpty()) {
            status.update { it.copy(errors = errors) }
            return
        }
        val values = GoalValues(parsed[Nutrient.KCAL], parsed[Nutrient.PROTEIN], parsed[Nutrient.CARBS], parsed[Nutrient.FAT])
        if (values == s.current?.values) {
            status.update { it.copy(saved = true) }
            return
        }
        status.update { it.copy(saving = true, failed = false) }
        viewModelScope.launch {
            try {
                repository.setGoals(values)
                handle[KEY_DRAFTS] = null
                status.update { it.copy(saving = false, saved = true) }
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                status.update { it.copy(saving = false, failed = true) }
            }
        }
    }

    companion object {
        private const val KEY_DRAFTS = "goal_drafts"

        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as FitnessApplication).container
                GoalsViewModel(createSavedStateHandle(), c.nutritionRepository)
            }
        }
    }
}

@Composable
fun GoalsRoute(onBack: () -> Unit, viewModel: GoalsViewModel = viewModel(factory = GoalsViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.loading) return
    NutritionPage(
        title = stringResource(R.string.n_goals_title),
        subtitle = stringResource(R.string.n_goals_subtitle),
        onBack = onBack,
        bottomBar = {
            InlineMessage(visible = state.failed, text = stringResource(R.string.record_save_error))
            InlineMessage(visible = state.saved, text = stringResource(R.string.n_goals_saved), iconRes = R.drawable.ic_check_circle, color = MaterialTheme.colorScheme.primary)
            PrimaryButton(stringResource(R.string.action_save), viewModel::save, Modifier.fillMaxWidth(), busy = state.saving, iconRes = R.drawable.ic_check)
        },
    ) {
        Text(stringResource(R.string.n_goals_effective, formatDay(state.today, state.today)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Nutrient.entries.forEach { n ->
            val range = NutritionRepository.goalRange(n)
            GlassNumberField(
                state.drafts[n].orEmpty(), { viewModel.onChange(n, it) }, stringResource(n.label()),
                suffix = if (n == Nutrient.KCAL) "kcal" else "g", integer = true,
                imeAction = if (n == Nutrient.FAT) ImeAction.Done else ImeAction.Next,
                error = stringResource(R.string.goal_range, range.first, range.last).takeIf { n in state.errors },
            )
        }
        Text(stringResource(R.string.n_goals_optional), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MacroEnergyNote(state.drafts)
        Text(stringResource(R.string.n_goals_history), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        if (state.history.isEmpty()) {
            Text(stringResource(R.string.n_goals_history_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column {
            state.history.reversed().forEachIndexed { i, g ->
                if (i > 0) HorizontalDivider(color = FitnessThemeExtras.colors.surfaceRaised)
                Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
                    Text(stringResource(R.string.n_goals_from, formatDay(g.effectiveDate, state.today)), style = MaterialTheme.typography.bodyLarge)
                    Text(goalSummary(g.values), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(stringResource(R.string.n_goals_no_backfill), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun goalSummary(v: GoalValues): String {
    if (v.isEmpty) return stringResource(R.string.n_goals_cleared)
    return listOfNotNull(
        v.kcal?.let { "${formatInteger(it)} kcal" },
        v.proteinG?.let { stringResource(R.string.n_protein) + " $it g" },
        v.carbsG?.let { stringResource(R.string.n_carbs) + " $it g" },
        v.fatG?.let { stringResource(R.string.n_fat) + " $it g" },
    ).joinToString(" · ")
}

/** Aviso informativo, no bloqueo: 4/4/9 frente a la energía indicada. */
@Composable
private fun MacroEnergyNote(drafts: Map<Nutrient, String>) {
    val v = GoalValues(drafts[Nutrient.KCAL]?.toIntOrNull(), drafts[Nutrient.PROTEIN]?.toIntOrNull(), drafts[Nutrient.CARBS]?.toIntOrNull(), drafts[Nutrient.FAT]?.toIntOrNull())
    val macroKcal = v.macroKcal() ?: return
    val kcal = v.kcal ?: return
    if (abs(macroKcal - kcal) / kcal <= 0.10) return
    Text(stringResource(R.string.n_goals_macro_mismatch, kcalText(macroKcal), kcalText(kcal.toDouble())), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
