package com.maikol.fitnessapp.feature.nutrition.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.feature.nutrition.data.CopyRequest
import com.maikol.fitnessapp.feature.nutrition.data.MealGroup
import com.maikol.fitnessapp.feature.tracking.DatePickerSheet
import com.maikol.fitnessapp.feature.tracking.PickerField
import com.maikol.fitnessapp.feature.tracking.TimePickerSheet
import com.maikol.fitnessapp.feature.tracking.dayLabel
import com.maikol.fitnessapp.feature.tracking.formatTime
import com.maikol.fitnessapp.feature.training.ui.InlineMessage
import java.time.LocalDate
import java.time.LocalTime

/** Una fila de la vista previa de copia: texto y si se incluye (selección). */
data class CopyCandidate(val id: String, val isEntry: Boolean, val title: String, val detail: String)

/**
 * Copiar a una fecha/hora y comida de destino explícitas, con vista previa y confirmación. El ID de operación
 * se fija al abrir: un doble toque o un reintento no crea otra copia. El origen no cambia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyDialog(
    candidates: List<CopyCandidate>,
    today: LocalDate,
    initialDate: LocalDate,
    initialGroup: MealGroup,
    opId: String,
    onCopy: (CopyRequest, (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    var date by rememberSaveable { mutableStateOf(initialDate.toString()) }
    var time by rememberSaveable { mutableStateOf(LocalTime.now().withSecond(0).withNano(0).toString()) }
    var group by rememberSaveable { mutableStateOf(initialGroup.takeIf { it != MealGroup.UNSPECIFIED }?.name ?: MealGroup.LUNCH.name) }
    var selected by rememberSaveable { mutableStateOf(candidates.map { it.id }) }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var pickTime by rememberSaveable { mutableStateOf(false) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var failed by rememberSaveable { mutableStateOf(false) }
    val d = LocalDate.parse(date)
    BasicAlertDialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(shape = MaterialTheme.shapes.large, color = FitnessThemeExtras.colors.card) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                Text(stringResource(R.string.n_copy_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    PickerField(stringResource(R.string.n_date), dayLabel(d, today), onClick = { pickDate = true }, modifier = Modifier.weight(1f))
                    PickerField(stringResource(R.string.n_time), formatTime(LocalTime.parse(time)), onClick = { pickTime = true }, modifier = Modifier.weight(1f))
                }
                Text(stringResource(R.string.n_meal), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                GlassChoices(MealGroup.choosable, MealGroup.valueOf(group), { stringResource(it.label()) }, { group = it.name })
                Text(stringResource(R.string.n_copy_preview), style = MaterialTheme.typography.titleSmall)
                candidates.forEach { c ->
                    val on = c.id in selected
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = MinTouchTarget)
                            .toggleable(on, role = Role.Checkbox, enabled = candidates.size > 1) { selected = if (it) selected + c.id else selected - c.id },
                    ) {
                        if (candidates.size > 1) Checkbox(checked = on, onCheckedChange = null)
                        Column(Modifier.weight(1f).padding(start = Spacing.s)) {
                            Text(c.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(c.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Text(stringResource(R.string.n_copy_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                InlineMessage(visible = failed, text = stringResource(R.string.record_save_error))
                Spacer(Modifier.height(Spacing.xs))
                PrimaryButton(
                    text = stringResource(R.string.n_copy_confirm, selected.size),
                    busy = busy,
                    enabled = selected.isNotEmpty(),
                    onClick = {
                        if (busy) return@PrimaryButton
                        busy = true
                        failed = false
                        val chosen = candidates.filter { it.id in selected }
                        onCopy(
                            CopyRequest(opId, chosen.filter { it.isEntry }.map { it.id }, chosen.filterNot { it.isEntry }.map { it.id }, d, MealGroup.valueOf(group), LocalTime.parse(time)),
                        ) { ok -> busy = false; if (ok) onDismiss() else failed = true }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget)) {
                    Text(stringResource(R.string.action_cancel), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
    if (pickDate) DatePickerSheet(d, today, onPick = { date = it.toString(); pickDate = false }, onDismiss = { pickDate = false })
    if (pickTime) TimePickerSheet(LocalTime.parse(time), onPick = { time = it.toString(); pickTime = false }, onDismiss = { pickTime = false })
}

/** Mover a otra comida del mismo día: elección explícita, sin deducir nada. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveDialog(title: String, current: MealGroup, onMove: (MealGroup) -> Unit, onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = FitnessThemeExtras.colors.card) {
            Column(Modifier.padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                Text(stringResource(R.string.n_move_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                GlassChoices(MealGroup.choosable, current.takeIf { it != MealGroup.UNSPECIFIED }, { stringResource(it.label()) }, { if (it != current) onMove(it) else onDismiss() })
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget)) {
                    Text(stringResource(R.string.action_cancel), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
