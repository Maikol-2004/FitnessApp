package com.maikol.fitnessapp.feature.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.database.DailyActivityEntity
import com.maikol.fitnessapp.core.designsystem.component.FitnessCard
import com.maikol.fitnessapp.core.designsystem.component.SecondaryButton
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.tracking.distanceUnit
import com.maikol.fitnessapp.core.tracking.formatDistance
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
import com.maikol.fitnessapp.feature.tracking.GoalHint
import com.maikol.fitnessapp.feature.tracking.ModuleScreen
import com.maikol.fitnessapp.feature.tracking.SourceLine
import com.maikol.fitnessapp.feature.tracking.dayLabel
import com.maikol.fitnessapp.feature.training.ui.InlineMessage
import com.maikol.fitnessapp.feature.training.ui.NumberStyleMedium
import com.maikol.fitnessapp.feature.training.ui.OverlineStyle
import com.maikol.fitnessapp.feature.training.ui.ProgressRing
import com.maikol.fitnessapp.feature.training.ui.StatTile

@Composable
fun HealthRoute(onOpenGoals: () -> Unit, viewModel: HealthViewModel = viewModel(factory = HealthViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    FormBackGuard(open = state.form != null, dirty = state.form?.dirty == true, onClose = viewModel::closeForm)

    ModuleScreen(
        title = stringResource(R.string.health_title),
        subtitle = stringResource(R.string.health_subtitle),
        photo = R.drawable.h1_health,
    ) {
        if (state.loading) return@ModuleScreen
        val form = state.form
        if (form != null) {
            ActivityFormCard(state, form, viewModel)
            return@ModuleScreen
        }
        DaySelector(state.date, state.today, viewModel::selectDate)
        val activity = state.activity
        if (activity == null) {
            EmptyCard(
                iconRes = R.drawable.ic_heart,
                title = stringResource(R.string.health_empty_title),
                body = stringResource(R.string.health_empty_body),
                actionLabel = stringResource(R.string.health_add),
                onAction = viewModel::openForm,
            )
        } else {
            ActivityCard(activity, state, onEdit = viewModel::openForm, onDelete = { confirmDelete = true })
        }
        InlineMessage(visible = state.deleteFailed, text = stringResource(R.string.record_delete_error))
        if (state.stepGoal == null) {
            GoalHint(stringResource(R.string.health_goal_missing), onOpenGoals)
        }
    }
    if (confirmDelete) {
        DeleteDialog(
            name = stringResource(R.string.health_record_name, dayLabel(state.date, state.today)),
            onConfirm = { confirmDelete = false; viewModel.delete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun ActivityCard(activity: DailyActivityEntity, state: HealthUiState, onEdit: () -> Unit, onDelete: () -> Unit) {
    FitnessCard {
        val steps = activity.steps
        Row(verticalAlignment = Alignment.CenterVertically) {
            val fraction = goalFraction(steps?.toDouble(), state.stepGoal?.toDouble())
            val stepsText = steps?.let { formatInteger(it) } ?: "—"
            val description = if (steps == null) stringResource(R.string.health_steps_missing) else
                stringResource(R.string.health_steps_description, stepsText) +
                    (state.stepGoal?.let { ", " + stringResource(R.string.health_goal_of, formatInteger(it)) } ?: "")
            ProgressRing(
                progress = fraction ?: 0f,
                size = 128.dp,
                stroke = 10.dp,
                modifier = Modifier.clearAndSetSemantics { contentDescription = description },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 18.dp)) {
                    Text(
                        stepsText,
                        style = NumberStyleMedium,
                        maxLines = 1,
                        softWrap = false,
                        autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = NumberStyleMedium.fontSize),
                    )
                    Text(
                        stringResource(R.string.health_steps),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = MaterialTheme.typography.bodyMedium.fontSize),
                    )
                }
            }
            Spacer(Modifier.width(Spacing.l))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.health_day_title).uppercase(), style = OverlineStyle, color = MaterialTheme.colorScheme.primary)
                Text(dayLabel(state.date, state.today), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(Spacing.xs))
                // Cifra real aparte del aro (que se limita a 100 %); sin meta no hay porcentaje.
                Text(
                    state.stepGoal?.let { stringResource(R.string.health_goal_of, formatInteger(it)) }
                        ?: stringResource(R.string.health_no_goal_short),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(Spacing.l))
        // Solo los valores introducidos; un campo vacío no se muestra como 0.
        val tiles = buildList {
            activity.distanceMeters?.let { add(stringResource(R.string.health_distance) to formatDistance(it, state.unit)) }
            activity.activeMinutes?.let { add(stringResource(R.string.health_minutes) to "${formatInteger(it)} min") }
            activity.activeKcal?.let { add(stringResource(R.string.health_kcal) to formatKcal(it)) }
        }
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.cardGap), modifier = Modifier.padding(bottom = Spacing.cardGap)) {
                row.forEach { (label, value) -> StatTile(label, value, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        SourceLine(stringResource(R.string.health_source_detail))
        Spacer(Modifier.height(Spacing.m))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            SecondaryButton(text = stringResource(R.string.action_edit), onClick = onEdit, iconRes = R.drawable.ic_edit, modifier = Modifier.weight(1f))
            TextButton(onClick = onDelete, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ActivityFormCard(state: HealthUiState, form: ActivityForm, vm: HealthViewModel) {
    val invalid = stringResource(R.string.field_invalid)
    FormCard(
        title = stringResource(if (state.activity == null) R.string.health_form_new else R.string.health_form_edit),
        note = stringResource(R.string.health_form_note, dayLabel(state.date, state.today)),
        saving = state.saving,
        failed = state.failed,
        onSave = vm::save,
        onCancel = vm::closeForm,
    ) {
        FormField(form.steps, { v -> vm.update { it.copy(steps = v) } }, stringResource(R.string.health_steps_label), kind = FieldKind.INTEGER,
            error = invalid.takeIf { ActivityField.STEPS in form.errors })
        FormField(form.distance.text, vm::onDistance, stringResource(R.string.health_distance), suffix = state.unit.distanceUnit(),
            error = invalid.takeIf { ActivityField.DISTANCE in form.errors })
        FormField(form.minutes, { v -> vm.update { it.copy(minutes = v) } }, stringResource(R.string.health_minutes), kind = FieldKind.INTEGER, suffix = "min",
            error = invalid.takeIf { ActivityField.MINUTES in form.errors })
        FormField(form.kcal, { v -> vm.update { it.copy(kcal = v) } }, stringResource(R.string.health_kcal), suffix = "kcal",
            error = invalid.takeIf { ActivityField.KCAL in form.errors })
        InlineMessage(visible = form.noValues, text = stringResource(R.string.health_form_need_one))
    }
}
