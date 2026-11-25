package com.maikol.fitnessapp.feature.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.database.MealEntryEntity
import com.maikol.fitnessapp.core.designsystem.component.FitnessCard
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.preferences.Goal
import com.maikol.fitnessapp.core.tracking.TrackingRepository
import com.maikol.fitnessapp.core.tracking.formatGrams
import com.maikol.fitnessapp.core.tracking.formatInteger
import com.maikol.fitnessapp.core.tracking.formatKcal
import com.maikol.fitnessapp.core.tracking.goalFraction
import com.maikol.fitnessapp.feature.tracking.DaySelector
import com.maikol.fitnessapp.feature.tracking.DeleteDialog
import com.maikol.fitnessapp.feature.tracking.EmptyCard
import com.maikol.fitnessapp.feature.tracking.FieldKind
import com.maikol.fitnessapp.feature.tracking.FormBackGuard
import com.maikol.fitnessapp.feature.tracking.FormCard
import com.maikol.fitnessapp.feature.tracking.FormField
import com.maikol.fitnessapp.feature.tracking.GoalBar
import com.maikol.fitnessapp.feature.tracking.GoalHint
import com.maikol.fitnessapp.feature.tracking.ModuleScreen
import com.maikol.fitnessapp.feature.tracking.PickerField
import com.maikol.fitnessapp.feature.tracking.RecordRow
import com.maikol.fitnessapp.feature.tracking.SourceLine
import com.maikol.fitnessapp.feature.tracking.TimePickerSheet
import com.maikol.fitnessapp.feature.tracking.dayLabel
import com.maikol.fitnessapp.feature.tracking.formatTime
import com.maikol.fitnessapp.feature.training.ui.InlineMessage
import com.maikol.fitnessapp.feature.training.ui.NumberStyleLarge
import com.maikol.fitnessapp.feature.training.ui.OverlineStyle
import java.time.ZoneId

@Composable
fun NutritionRoute(onBack: () -> Unit, onOpenGoals: () -> Unit, viewModel: NutritionViewModel = viewModel(factory = NutritionViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var toDelete by rememberSaveable { mutableStateOf<String?>(null) }
    FormBackGuard(open = state.form != null, dirty = state.form?.dirty == true, onClose = viewModel::closeForm)

    ModuleScreen(
        title = stringResource(R.string.category_nutrition),
        subtitle = stringResource(R.string.nutrition_subtitle),
        photo = R.drawable.h1_nutrition,
        onBack = onBack,
    ) {
        if (state.loading) return@ModuleScreen
        val form = state.form
        if (form != null) {
            MealFormCard(state, form, viewModel)
            return@ModuleScreen
        }
        DaySelector(state.date, state.today, viewModel::selectDate)
        TotalsCard(state, onOpenGoals)
        if (state.meals.isEmpty()) {
            EmptyCard(
                iconRes = R.drawable.ic_restaurant,
                title = stringResource(R.string.nutrition_empty_title),
                body = stringResource(R.string.nutrition_empty_body),
                actionLabel = stringResource(R.string.nutrition_add),
                onAction = viewModel::add,
            )
        } else {
            MealList(state.meals, onEdit = viewModel::edit, onDelete = { toDelete = it.id })
            PrimaryButton(text = stringResource(R.string.nutrition_add), onClick = viewModel::add, iconRes = R.drawable.ic_add, modifier = Modifier.fillMaxWidth())
        }
        InlineMessage(visible = state.deleteFailed, text = stringResource(R.string.record_delete_error))
    }
    val deleting = state.meals.firstOrNull { it.id == toDelete }
    if (deleting != null) {
        DeleteDialog(deleting.name, onConfirm = { toDelete = null; viewModel.delete(deleting.id) }, onDismiss = { toDelete = null })
    }
}

@Composable
private fun TotalsCard(state: NutritionUiState, onOpenGoals: () -> Unit) {
    val t = state.totals
    val goals = state.goals
    FitnessCard {
        Text(stringResource(R.string.nutrition_totals, dayLabel(state.date, state.today)).uppercase(), style = OverlineStyle, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(Spacing.xs))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(t.kcal?.let { formatInteger(Math.round(it)) } ?: "—", style = NumberStyleLarge)
            Spacer(Modifier.width(Spacing.s))
            Text(
                "kcal" + (goals[Goal.KCAL]?.let { " / " + formatInteger(it) } ?: ""),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        goalFraction(t.kcal, goals[Goal.KCAL]?.toDouble())?.let { GoalBar(label = null, value = "", fraction = it) }
        Spacer(Modifier.height(Spacing.m))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Macro(stringResource(R.string.nutrition_protein), t.proteinG, goals[Goal.PROTEIN], t.proteinIncomplete)
            Macro(stringResource(R.string.nutrition_carbs), t.carbsG, goals[Goal.CARBS], t.carbsIncomplete)
            Macro(stringResource(R.string.nutrition_fat), t.fatG, goals[Goal.FAT], t.fatIncomplete)
        }
        if (t.kcalIncomplete) {
            Spacer(Modifier.height(Spacing.s))
            Text(stringResource(R.string.nutrition_kcal_incomplete), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (goals[Goal.KCAL] == null) {
            GoalHint(stringResource(R.string.nutrition_goal_missing), onOpenGoals)
        }
    }
}

@Composable
private fun Macro(label: String, grams: Double?, goal: Int?, incomplete: Boolean) {
    GoalBar(
        label = label,
        value = grams?.let(::formatGrams) ?: "—",
        fraction = goalFraction(grams, goal?.toDouble()),
        goalText = goal?.let { "$it g" },
        incomplete = incomplete,
    )
}

@Composable
private fun MealList(meals: List<MealEntryEntity>, onEdit: (MealEntryEntity) -> Unit, onDelete: (MealEntryEntity) -> Unit) {
    FitnessCard {
        Text(stringResource(R.string.nutrition_meals), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        SourceLine(stringResource(R.string.nutrition_source_detail))
        Spacer(Modifier.height(Spacing.s))
        meals.forEachIndexed { index, meal ->
            if (index > 0) HorizontalDivider(color = FitnessThemeExtras.colors.surfaceRaised)
            val macros = listOfNotNull(
                meal.proteinG?.let { "P " + formatGrams(it) },
                meal.carbsG?.let { "H " + formatGrams(it) },
                meal.fatG?.let { "G " + formatGrams(it) },
            ).joinToString(" · ")
            val time = formatTime(TrackingRepository.timeOf(meal.occurredAt, ZoneId.of(meal.zoneId)))
            RecordRow(
                title = meal.name,
                detail = listOf(time, macros).filter { it.isNotEmpty() }.joinToString(" · "),
                value = meal.kcal?.let(::formatKcal) ?: stringResource(R.string.nutrition_kcal_unknown),
                onEdit = { onEdit(meal) },
                onDelete = { onDelete(meal) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MealFormCard(state: NutritionUiState, form: MealForm, vm: NutritionViewModel) {
    var pickTime by rememberSaveable { mutableStateOf(false) }
    val invalid = stringResource(R.string.field_invalid)
    FormCard(
        title = stringResource(if (form.existing == null) R.string.nutrition_form_new else R.string.nutrition_form_edit),
        note = stringResource(R.string.nutrition_form_note, dayLabel(state.date, state.today)),
        saving = state.saving,
        failed = state.failed,
        onSave = vm::save,
        onCancel = vm::closeForm,
    ) {
        FormField(form.name, { v -> vm.update { it.copy(name = v.take(60)) } }, stringResource(R.string.nutrition_name), kind = FieldKind.TEXT,
            error = stringResource(R.string.nutrition_name_error).takeIf { MealField.NAME in form.errors })
        PickerField(stringResource(R.string.nutrition_time), formatTime(form.time), onClick = { pickTime = true })
        FormField(form.kcal, { v -> vm.update { it.copy(kcal = v) } }, stringResource(R.string.nutrition_energy), suffix = "kcal",
            error = invalid.takeIf { MealField.KCAL in form.errors })
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            FormField(form.protein, { v -> vm.update { it.copy(protein = v) } }, stringResource(R.string.nutrition_protein), suffix = "g",
                error = invalid.takeIf { MealField.PROTEIN in form.errors }, modifier = Modifier.weight(1f))
            FormField(form.carbs, { v -> vm.update { it.copy(carbs = v) } }, stringResource(R.string.nutrition_carbs_short), suffix = "g",
                error = invalid.takeIf { MealField.CARBS in form.errors }, modifier = Modifier.weight(1f))
        }
        FormField(form.fat, { v -> vm.update { it.copy(fat = v) } }, stringResource(R.string.nutrition_fat), suffix = "g",
            error = invalid.takeIf { MealField.FAT in form.errors })
        Text(stringResource(R.string.nutrition_values_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        InlineMessage(visible = form.noValues, text = stringResource(R.string.nutrition_need_one))
    }
    if (pickTime) {
        TimePickerSheet(form.time, onPick = { t -> pickTime = false; vm.update { it.copy(time = t) } }, onDismiss = { pickTime = false })
    }
}
