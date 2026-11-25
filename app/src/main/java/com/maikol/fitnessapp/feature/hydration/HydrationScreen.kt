package com.maikol.fitnessapp.feature.hydration

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.database.WaterEntryEntity
import com.maikol.fitnessapp.core.designsystem.component.FitnessCard
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.component.SecondaryButton
import com.maikol.fitnessapp.core.designsystem.motion.LocalMotionEnabled
import com.maikol.fitnessapp.core.designsystem.motion.MotionTokens
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.tracking.TrackingRepository
import com.maikol.fitnessapp.core.tracking.formatInteger
import com.maikol.fitnessapp.core.tracking.formatMl
import com.maikol.fitnessapp.core.tracking.goalFraction
import com.maikol.fitnessapp.feature.tracking.DaySelector
import com.maikol.fitnessapp.feature.tracking.DeleteDialog
import com.maikol.fitnessapp.feature.tracking.FieldKind
import com.maikol.fitnessapp.feature.tracking.FormBackGuard
import com.maikol.fitnessapp.feature.tracking.FormCard
import com.maikol.fitnessapp.feature.tracking.FormField
import com.maikol.fitnessapp.feature.tracking.GoalHint
import com.maikol.fitnessapp.feature.tracking.ModuleScreen
import com.maikol.fitnessapp.feature.tracking.RecordRow
import com.maikol.fitnessapp.feature.tracking.SourceLine
import com.maikol.fitnessapp.feature.tracking.dayLabel
import com.maikol.fitnessapp.feature.tracking.formatTime
import com.maikol.fitnessapp.feature.training.ui.InlineMessage
import com.maikol.fitnessapp.feature.training.ui.NumberStyleLarge
import com.maikol.fitnessapp.feature.training.ui.OverlineStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.sin

private val Shortcuts = listOf(150, 250, 500)
private const val UNDO_VISIBLE_MS = 8_000L

@Composable
fun HydrationRoute(onBack: () -> Unit, onOpenGoals: () -> Unit, viewModel: HydrationViewModel = viewModel(factory = HydrationViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var toDelete by rememberSaveable { mutableStateOf<String?>(null) }
    FormBackGuard(open = state.edit != null, dirty = state.edit?.dirty == true, onClose = viewModel::closeEdit)
    LaunchedEffect(state.lastAddedId) {
        if (state.lastAddedId != null) {
            delay(UNDO_VISIBLE_MS)
            viewModel.dismissUndo()
        }
    }

    ModuleScreen(
        title = stringResource(R.string.category_hydration),
        subtitle = stringResource(R.string.hydration_subtitle),
        photo = R.drawable.h1_hydration,
        onBack = onBack,
    ) {
        if (state.loading) return@ModuleScreen
        val edit = state.edit
        if (edit != null) {
            FormCard(
                title = stringResource(R.string.hydration_edit_title),
                note = formatTime(TrackingRepository.timeOf(edit.entry.occurredAt, ZoneId.of(edit.entry.zoneId))),
                saving = state.saving,
                failed = state.failed,
                onSave = viewModel::saveEdit,
                onCancel = viewModel::closeEdit,
            ) {
                FormField(edit.text, viewModel::onEditText, stringResource(R.string.hydration_amount), kind = FieldKind.INTEGER, suffix = "ml",
                    error = stringResource(R.string.hydration_amount_error).takeIf { edit.error })
            }
            return@ModuleScreen
        }
        DaySelector(state.date, state.today, viewModel::selectDate)
        WaterCard(state, viewModel, onOpenGoals)
        UndoBar(state, viewModel::undo)
        InlineMessage(visible = state.failed, text = stringResource(R.string.record_save_error))
        if (state.entries.isNotEmpty()) {
            EntryList(state.entries, onEdit = viewModel::startEdit, onDelete = { toDelete = it.id })
        }
    }
    val deleting = state.entries.firstOrNull { it.id == toDelete }
    if (deleting != null) {
        DeleteDialog(
            stringResource(R.string.hydration_entry_name, formatMl(deleting.ml)),
            onConfirm = { toDelete = null; viewModel.remove(deleting.id) },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun WaterCard(state: HydrationUiState, vm: HydrationViewModel, onOpenGoals: () -> Unit) {
    FitnessCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val fraction = goalFraction(state.totalMl.toDouble(), state.goalMl?.toDouble())
            val description = stringResource(R.string.hydration_total_description, formatMl(state.totalMl)) +
                (state.goalMl?.let { ", " + stringResource(R.string.hydration_goal_of, formatMl(it)) } ?: "")
            WaterGlass(
                fraction = fraction ?: 0f,
                modifier = Modifier
                    .size(width = 92.dp, height = 132.dp)
                    .clearAndSetSemantics { contentDescription = description },
            )
            Spacer(Modifier.width(Spacing.xl))
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
                Text(stringResource(R.string.hydration_total, dayLabel(state.date, state.today)).uppercase(), style = OverlineStyle, color = MaterialTheme.colorScheme.primary)
                // El total exacto cambia de inmediato tras guardar; el nivel le sigue en 250 ms.
                Text(formatInteger(state.totalMl), style = NumberStyleLarge)
                Text(
                    "ml" + (state.goalMl?.let { " / " + formatMl(it) } ?: ""),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.goalMl != null && state.totalMl >= state.goalMl) {
                    Text(stringResource(R.string.hydration_goal_reached), style = MaterialTheme.typography.bodyMedium, color = FitnessThemeExtras.colors.success)
                }
            }
        }
        if (state.goalMl == null) {
            GoalHint(stringResource(R.string.hydration_goal_missing), onOpenGoals)
        }
        Spacer(Modifier.height(Spacing.l))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Shortcuts.forEach { ml ->
                SecondaryButton(
                    text = "+$ml",
                    onClick = { vm.add(ml) },
                    enabled = !state.adding,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(Spacing.m))
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            FormField(
                state.custom,
                vm::onCustom,
                stringResource(R.string.hydration_custom),
                kind = FieldKind.INTEGER,
                suffix = "ml",
                error = stringResource(R.string.hydration_amount_error).takeIf { state.customError },
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = stringResource(R.string.hydration_add),
                onClick = vm::addCustom,
                busy = state.adding,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Spacer(Modifier.height(Spacing.s))
        SourceLine(stringResource(R.string.hydration_source_detail))
    }
}

@Composable
private fun UndoBar(state: HydrationUiState, onUndo: () -> Unit) {
    val ml = state.lastAddedMl
    if (state.lastAddedId == null || ml == null) return
    Surface(color = FitnessThemeExtras.colors.surfaceRaised, shape = MaterialTheme.shapes.large) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            Text(
                stringResource(R.string.hydration_added, formatMl(ml)),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(start = Spacing.l),
            )
            TextButton(onClick = onUndo, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.action_undo), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun EntryList(entries: List<WaterEntryEntity>, onEdit: (WaterEntryEntity) -> Unit, onDelete: (WaterEntryEntity) -> Unit) {
    FitnessCard {
        Text(stringResource(R.string.hydration_entries), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Spacing.xs))
        entries.forEachIndexed { index, entry ->
            if (index > 0) HorizontalDivider(color = FitnessThemeExtras.colors.surfaceRaised)
            RecordRow(
                title = formatMl(entry.ml),
                detail = formatTime(TrackingRepository.timeOf(entry.occurredAt, ZoneId.of(entry.zoneId))),
                value = null,
                onEdit = { onEdit(entry) },
                onDelete = { onDelete(entry) },
            )
        }
    }
}

/**
 * M11: vaso con relleno recortado. El nivel sube/baja desde su valor visual actual al real en 250 ms con
 * salida suave y una sola onda de 2 dp que se apaga en 250 ms; sin bucle. Movimiento reducido: nivel estático.
 */
@Composable
private fun WaterGlass(fraction: Float, modifier: Modifier = Modifier) {
    val motion = LocalMotionEnabled.current
    val level = remember { Animatable(fraction) }
    val wave = remember { Animatable(0f) }
    LaunchedEffect(fraction, motion) {
        if (!motion) {
            level.snapTo(fraction)
            wave.snapTo(0f)
            return@LaunchedEffect
        }
        if (level.value == fraction) return@LaunchedEffect
        launch {
            wave.snapTo(1f)
            wave.animateTo(0f, tween(250))
        }
        level.animateTo(fraction, tween(250, easing = MotionTokens.EaseOut))
    }
    val glass = FitnessThemeExtras.colors.surfaceRaised
    val rim = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val water = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val corner = CornerRadius(18.dp.toPx())
        val outline = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, corner)) }
        drawPath(outline, glass)
        clipPath(outline) {
            val surfaceY = size.height * (1f - level.value.coerceIn(0f, 1f))
            if (level.value > 0f) {
                val amp = 2.dp.toPx() * wave.value
                val path = Path().apply {
                    moveTo(0f, surfaceY)
                    val steps = 24
                    for (i in 0..steps) {
                        val x = size.width * i / steps
                        lineTo(x, surfaceY + amp * sin(2 * PI * i / steps).toFloat())
                    }
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, Brush.verticalGradient(listOf(water.copy(alpha = 0.95f), water.copy(alpha = 0.55f)), startY = surfaceY, endY = size.height))
            }
            // Reflejo vertical del vidrio.
            drawRect(
                Brush.horizontalGradient(listOf(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.10f), androidx.compose.ui.graphics.Color.Transparent)),
                topLeft = Offset(size.width * 0.12f, 0f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.16f, size.height),
            )
        }
        drawPath(outline, rim, style = Stroke(1.5.dp.toPx()))
    }
}
