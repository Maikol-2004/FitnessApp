package com.maikol.fitnessapp.feature.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.database.BodyWeightEntity
import com.maikol.fitnessapp.core.designsystem.component.FitnessCard
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.component.SecondaryButton
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.feature.tracking.DatePickerSheet
import com.maikol.fitnessapp.feature.tracking.DeleteDialog
import com.maikol.fitnessapp.feature.tracking.FieldKind
import com.maikol.fitnessapp.feature.tracking.FormBackGuard
import com.maikol.fitnessapp.feature.tracking.FormCard
import com.maikol.fitnessapp.feature.tracking.FormField
import com.maikol.fitnessapp.feature.tracking.ModuleScreen
import com.maikol.fitnessapp.feature.tracking.PickerField
import com.maikol.fitnessapp.feature.tracking.RecordRow
import com.maikol.fitnessapp.feature.tracking.SourceLine
import com.maikol.fitnessapp.feature.tracking.dayLabel
import com.maikol.fitnessapp.feature.training.data.formatLoad
import com.maikol.fitnessapp.feature.training.data.formatVolume
import com.maikol.fitnessapp.feature.training.data.kgToDisplay
import com.maikol.fitnessapp.feature.training.data.loadUnit
import com.maikol.fitnessapp.feature.training.ui.InlineMessage
import com.maikol.fitnessapp.feature.training.ui.NumberStyleLarge
import com.maikol.fitnessapp.feature.training.ui.OverlineStyle
import com.maikol.fitnessapp.feature.training.ui.StatTile
import java.time.LocalDate

/** Periodo fijo de la tendencia: últimos 90 días. */
private const val TREND_DAYS = 90L

@Composable
fun ProgressRoute(onOpenHistory: () -> Unit, viewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var toDelete by rememberSaveable { mutableStateOf<String?>(null) }
    FormBackGuard(open = state.form != null, dirty = state.form?.dirty == true, onClose = viewModel::closeForm)

    ModuleScreen(
        title = stringResource(R.string.destination_progress),
        subtitle = stringResource(R.string.progress_subtitle),
        photo = R.drawable.h1_progress,
    ) {
        if (state.loading) return@ModuleScreen
        val form = state.form
        if (form != null) {
            WeightFormCard(state, form, viewModel)
            return@ModuleScreen
        }
        TrainingWeekCard(state, onOpenHistory)
        WeightCard(state, onAdd = viewModel::add)
        if (state.weights.isNotEmpty()) {
            WeightList(state, onEdit = viewModel::edit, onDelete = { toDelete = it.id })
        }
        InlineMessage(visible = state.deleteFailed, text = stringResource(R.string.record_delete_error))
    }
    val deleting = state.weights.firstOrNull { it.id == toDelete }
    if (deleting != null) {
        DeleteDialog(
            stringResource(R.string.progress_weight_entry, dayLabel(LocalDate.parse(deleting.date), state.today)),
            onConfirm = { toDelete = null; viewModel.delete(deleting.id) },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun TrainingWeekCard(state: ProgressUiState, onOpenHistory: () -> Unit) {
    FitnessCard {
        Text(stringResource(R.string.progress_training_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.progress_training_period), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.m))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
            StatTile(stringResource(R.string.progress_sessions), state.week.sessions.toString(), Modifier.weight(1f))
            StatTile(stringResource(R.string.progress_volume), formatVolume(state.week.volumeKg, state.unit), Modifier.weight(1f), highlight = true)
        }
        Spacer(Modifier.height(Spacing.m))
        SecondaryButton(text = stringResource(R.string.progress_open_history), onClick = onOpenHistory, iconRes = R.drawable.ic_history, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun WeightCard(state: ProgressUiState, onAdd: () -> Unit) {
    val latest = state.weights.firstOrNull()
    FitnessCard {
        Text(stringResource(R.string.progress_weight_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Spacing.s))
        if (latest == null) {
            Text(stringResource(R.string.progress_weight_empty), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(stringResource(R.string.progress_weight_latest, dayLabel(LocalDate.parse(latest.date), state.today)).uppercase(), style = OverlineStyle, color = MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(formatLoad(latest.kg, state.unit)?.substringBeforeLast(" ") ?: "—", style = NumberStyleLarge)
                Spacer(Modifier.width(Spacing.s))
                Text(state.unit.loadUnit(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val previous = state.weights.getOrNull(1)
            if (previous != null) {
                val delta = latest.kg - previous.kg
                val sign = if (delta > 0) "+" else if (delta < 0) "−" else "±"
                Text(
                    stringResource(R.string.progress_weight_delta, sign + (formatLoad(kotlin.math.abs(delta), state.unit) ?: "0 ${state.unit.loadUnit()}")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Spacing.m))
            val from = state.today.minusDays(TREND_DAYS)
            val points = state.weights.filter { LocalDate.parse(it.date) >= from }.sortedBy { it.date }
            if (points.size >= 2) {
                WeightTrend(points, state.unit)
                Text(stringResource(R.string.progress_trend_period), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(stringResource(R.string.progress_trend_need_two), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(Spacing.m))
        PrimaryButton(text = stringResource(R.string.progress_weight_add), onClick = onAdd, iconRes = R.drawable.ic_add, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * Tendencia sencilla en Canvas (sin librería): línea champán con puntos y área tenue. Valores en la unidad
 * elegida. La lista inferior es el equivalente textual accesible.
 */
@Composable
private fun WeightTrend(points: List<BodyWeightEntity>, unit: UnitSystem) {
    val line = MaterialTheme.colorScheme.primary
    val grid = FitnessThemeExtras.colors.surfaceRaised
    val values = points.map { kgToDisplay(it.kg, unit) }
    val days = points.map { LocalDate.parse(it.date).toEpochDay() }
    val description = pluralStringResource(
        R.plurals.progress_trend_description,
        points.size,
        points.size,
        formatLoad(points.first().kg, unit).orEmpty(),
        formatLoad(points.last().kg, unit).orEmpty(),
    )
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        val min = values.min()
        val max = values.max()
        val span = (max - min).takeIf { it > 0.0 } ?: 1.0
        val pad = 10.dp.toPx()
        val dayMin = days.first()
        val daySpan = (days.last() - dayMin).takeIf { it > 0 } ?: 1L
        fun pos(i: Int) = Offset(
            pad + (size.width - 2 * pad) * ((days[i] - dayMin).toFloat() / daySpan),
            pad + (size.height - 2 * pad) * (1f - ((values[i] - min) / span).toFloat()),
        )
        listOf(0f, 0.5f, 1f).forEach { f ->
            val y = pad + (size.height - 2 * pad) * f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        val path = Path().apply {
            moveTo(pos(0).x, pos(0).y)
            for (i in 1 until values.size) lineTo(pos(i).x, pos(i).y)
        }
        val area = Path().apply {
            addPath(path)
            lineTo(pos(values.lastIndex).x, size.height)
            lineTo(pos(0).x, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(line.copy(alpha = 0.22f), Color.Transparent)))
        drawPath(path, line, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        values.indices.forEach { drawCircle(line, radius = 4.dp.toPx(), center = pos(it)) }
    }
}

@Composable
private fun WeightList(state: ProgressUiState, onEdit: (BodyWeightEntity) -> Unit, onDelete: (BodyWeightEntity) -> Unit) {
    FitnessCard {
        Text(stringResource(R.string.progress_weight_history), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        SourceLine(stringResource(R.string.progress_weight_source))
        Spacer(Modifier.height(Spacing.xs))
        state.weights.forEachIndexed { index, entry ->
            if (index > 0) HorizontalDivider(color = FitnessThemeExtras.colors.surfaceRaised)
            RecordRow(
                title = formatLoad(entry.kg, state.unit).orEmpty(),
                detail = listOfNotNull(dayLabel(LocalDate.parse(entry.date), state.today), entry.note).joinToString(" · "),
                value = null,
                onEdit = { onEdit(entry) },
                onDelete = { onDelete(entry) },
            )
        }
    }
}

@Composable
private fun WeightFormCard(state: ProgressUiState, form: WeightForm, vm: ProgressViewModel) {
    var pickDate by rememberSaveable { mutableStateOf(false) }
    FormCard(
        title = stringResource(if (form.existing == null) R.string.progress_weight_new else R.string.progress_weight_edit),
        saving = state.saving,
        failed = state.failed,
        onSave = vm::save,
        onCancel = vm::closeForm,
    ) {
        PickerField(stringResource(R.string.field_date), dayLabel(form.date, state.today), onClick = { pickDate = true })
        FormField(
            form.weight.text,
            { v -> vm.update { it.copy(weight = it.weight.edited(v)) } },
            stringResource(R.string.progress_weight_label),
            suffix = state.unit.loadUnit(),
            error = stringResource(R.string.progress_weight_error).takeIf { form.weightError },
        )
        FormField(form.note, { v -> vm.update { it.copy(note = v.take(200)) } }, stringResource(R.string.field_note_optional), kind = FieldKind.TEXT)
    }
    if (pickDate) {
        DatePickerSheet(form.date, state.today, { d -> pickDate = false; vm.update { it.copy(date = d) } }, { pickDate = false })
    }
}
