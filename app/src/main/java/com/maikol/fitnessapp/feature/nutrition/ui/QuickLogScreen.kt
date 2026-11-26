package com.maikol.fitnessapp.feature.nutrition.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.maikol.fitnessapp.core.database.MealMode
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.feature.nutrition.MealField
import com.maikol.fitnessapp.feature.nutrition.MealForm
import com.maikol.fitnessapp.feature.nutrition.MealValidation
import com.maikol.fitnessapp.feature.nutrition.validateMeal
import com.maikol.fitnessapp.feature.nutrition.data.MealGroup
import com.maikol.fitnessapp.feature.nutrition.data.Nutrients
import com.maikol.fitnessapp.feature.nutrition.data.NutritionRepository
import com.maikol.fitnessapp.feature.tracking.DatePickerSheet
import com.maikol.fitnessapp.feature.tracking.FormBackGuard
import com.maikol.fitnessapp.feature.tracking.PickerField
import com.maikol.fitnessapp.feature.tracking.TimePickerSheet
import com.maikol.fitnessapp.feature.tracking.dayLabel
import com.maikol.fitnessapp.feature.tracking.formatTime
import com.maikol.fitnessapp.feature.training.ui.GlassTextField
import com.maikol.fitnessapp.feature.training.ui.InlineMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class QuickUiState(
    val loading: Boolean = true,
    val form: MealForm = MealForm.new(LocalTime.NOON),
    val date: LocalDate = LocalDate.now(),
    val group: MealGroup = MealGroup.LUNCH,
    val mode: String? = null,
    val errors: Set<MealField> = emptySet(),
    val noValues: Boolean = false,
    val saving: Boolean = false,
    val failed: Boolean = false,
    val dirty: Boolean = false,
)

/**
 * Registro rápido de totales («Totales manuales», sin pedir peso) y edición de registros con totales, también
 * los anteriores a v6. El ID se fija al abrir: un reintento sustituye la misma fila.
 */
class QuickLogViewModel(private val handle: SavedStateHandle, private val repository: NutritionRepository) : ViewModel() {
    private val entryId: String? = handle["entryId"]
    private val id: String = entryId ?: handle.get<String>(KEY_ID) ?: repository.newId().also { handle[KEY_ID] = it }
    private val values = handle.getStateFlow(KEY_VALUES, arrayListOf<String>())
    private val status = MutableStateFlow(QuickUiState(loading = true))
    private val done = MutableStateFlow<String?>(null)
    val savedEvent: StateFlow<String?> = done
    private var initial: List<String> = emptyList()

    init {
        viewModelScope.launch {
            if (handle.get<ArrayList<String>>(KEY_VALUES).isNullOrEmpty()) {
                val e = entryId?.let { repository.getEntry(it) }
                val now = LocalTime.now(repository.zone()).withSecond(0).withNano(0)
                val form = if (e != null && e.meal.mode != MealMode.FOOD_ITEMS) MealForm.of(e.meal) else MealForm.new(now)
                val date = e?.meal?.localDate ?: handle.get<String>("date") ?: LocalDate.now(repository.zone()).toString()
                val group = e?.meal?.mealGroup ?: handle.get<String>("group") ?: suggestedGroup(now.hour).name
                handle[KEY_VALUES] = arrayListOf(form.name, form.time.toString(), form.kcal, form.protein, form.carbs, form.fat, date, group)
                handle[KEY_MODE] = e?.meal?.mode
                handle[KEY_INITIAL] = handle.get<ArrayList<String>>(KEY_VALUES)
            }
            initial = handle.get<ArrayList<String>>(KEY_INITIAL).orEmpty()
            status.value = status.value.copy(loading = false)
        }
    }

    val state: StateFlow<QuickUiState> = combine(values, status) { v, s ->
        if (v.size < 8) return@combine s
        s.copy(
            form = MealForm(name = v[0], time = LocalTime.parse(v[1]), kcal = v[2], protein = v[3], carbs = v[4], fat = v[5]),
            date = LocalDate.parse(v[6]),
            group = MealGroup.of(v[7]),
            mode = handle[KEY_MODE],
            dirty = v != initial,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuickUiState())

    private fun set(index: Int, value: String) {
        val v = ArrayList(values.value)
        if (v.size < 8) return
        v[index] = value
        handle[KEY_VALUES] = v
        status.value = status.value.copy(errors = emptySet(), noValues = false, failed = false)
    }

    fun name(v: String) = set(0, v.take(60))
    fun time(t: LocalTime) = set(1, t.toString())
    fun kcal(v: String) = set(2, v)
    fun protein(v: String) = set(3, v)
    fun carbs(v: String) = set(4, v)
    fun fat(v: String) = set(5, v)
    fun date(d: LocalDate) = set(6, d.toString())
    fun group(g: MealGroup) = set(7, g.name)

    fun save() {
        val s = state.value
        if (s.saving || s.loading) return
        when (val v = validateMeal(s.form)) {
            is MealValidation.Invalid -> status.value = status.value.copy(errors = v.form.errors, noValues = v.form.noValues)
            is MealValidation.Valid -> {
                status.value = status.value.copy(saving = true, failed = false)
                viewModelScope.launch {
                    try {
                        val x = v.values
                        repository.saveTotalsEntry(id, x.name, s.date, s.form.time, s.group, Nutrients(x.kcal, x.protein, x.carbs, x.fat))
                        status.value = status.value.copy(saving = false)
                        done.value = x.name
                    } catch (c: CancellationException) {
                        throw c
                    } catch (_: Exception) {
                        status.value = status.value.copy(saving = false, failed = true)
                    }
                }
            }
        }
    }

    companion object {
        private const val KEY_ID = "quick_id"
        private const val KEY_VALUES = "quick_values"
        private const val KEY_INITIAL = "quick_initial"
        private const val KEY_MODE = "quick_mode"

        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as FitnessApplication).container
                QuickLogViewModel(createSavedStateHandle(), c.nutritionRepository)
            }
        }
    }
}

@Composable
fun QuickLogRoute(onBack: () -> Unit, onSaved: (String) -> Unit, viewModel: QuickLogViewModel = viewModel(factory = QuickLogViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val saved by viewModel.savedEvent.collectAsStateWithLifecycle()
    val savedText = saved?.let { stringResource(R.string.n_saved_message, it) }
    LaunchedEffect(saved) { savedText?.let(onSaved) }
    FormBackGuard(open = true, dirty = state.dirty, onClose = onBack)
    if (state.loading) return
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var pickTime by rememberSaveable { mutableStateOf(false) }
    val f = state.form
    val invalid = stringResource(R.string.field_invalid)
    val today = LocalDate.now()
    val stacked = LocalDensity.current.fontScale > 1.3f
    val legacy = state.mode == MealMode.LEGACY_TOTALS

    NutritionPage(
        title = stringResource(if (state.mode == null) R.string.n_quick_log else R.string.n_edit_totals),
        subtitle = stringResource(if (legacy) R.string.n_legacy_note else R.string.n_quick_note),
        onBack = onBack,
        bottomBar = {
            InlineMessage(visible = state.failed, text = stringResource(R.string.record_save_error))
            PrimaryButton(stringResource(R.string.action_save), viewModel::save, Modifier.fillMaxWidth(), busy = state.saving, iconRes = R.drawable.ic_check)
        },
    ) {
        GlassTextField(f.name, viewModel::name, stringResource(R.string.nutrition_name), error = stringResource(R.string.nutrition_name_error).takeIf { MealField.NAME in state.errors },
            capitalization = KeyboardCapitalization.Sentences)
        Text(stringResource(R.string.n_meal), style = MaterialTheme.typography.titleSmall)
        GlassChoices(MealGroup.choosable, state.group.takeIf { it != MealGroup.UNSPECIFIED }, { stringResource(it.label()) }, viewModel::group)
        if (state.group == MealGroup.UNSPECIFIED) {
            Text(stringResource(R.string.n_unspecified_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            PickerField(stringResource(R.string.n_date), dayLabel(state.date, today), onClick = { pickDate = true }, modifier = Modifier.weight(1f))
            PickerField(stringResource(R.string.nutrition_time), formatTime(f.time), onClick = { pickTime = true }, modifier = Modifier.weight(1f))
        }
        Text(stringResource(R.string.n_totals_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        GlassNumberField(f.kcal, viewModel::kcal, stringResource(R.string.nutrition_energy), suffix = "kcal", error = invalid.takeIf { MealField.KCAL in state.errors })
        val macros = listOf(
            Triple(R.string.n_protein, f.protein, MealField.PROTEIN) to viewModel::protein,
            Triple(R.string.n_carbs, f.carbs, MealField.CARBS) to viewModel::carbs,
            Triple(R.string.n_fat, f.fat, MealField.FAT) to viewModel::fat,
        )
        if (stacked) {
            macros.forEach { (t, set) -> GlassNumberField(t.second, set, stringResource(t.first), suffix = "g", error = invalid.takeIf { t.third in state.errors }) }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                macros.forEach { (t, set) -> GlassNumberField(t.second, set, stringResource(t.first), Modifier.weight(1f), suffix = "g", error = invalid.takeIf { t.third in state.errors }) }
            }
        }
        Text(stringResource(R.string.nutrition_values_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        InlineMessage(visible = state.noValues, text = stringResource(R.string.nutrition_need_one))
    }
    if (pickDate) DatePickerSheet(state.date, today, onPick = { viewModel.date(it); pickDate = false }, onDismiss = { pickDate = false })
    if (pickTime) TimePickerSheet(f.time, onPick = { viewModel.time(it); pickTime = false }, onDismiss = { pickTime = false })
}
