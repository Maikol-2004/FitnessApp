package com.maikol.fitnessapp.feature.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.database.SleepEntryEntity
import com.maikol.fitnessapp.core.designsystem.component.FitnessCard
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.tracking.TrackingRepository
import com.maikol.fitnessapp.core.tracking.sleepDurationMs
import com.maikol.fitnessapp.feature.tracking.DatePickerSheet
import com.maikol.fitnessapp.feature.tracking.DeleteDialog
import com.maikol.fitnessapp.feature.tracking.EmptyCard
import com.maikol.fitnessapp.feature.tracking.FieldKind
import com.maikol.fitnessapp.feature.tracking.FormBackGuard
import com.maikol.fitnessapp.feature.tracking.FormCard
import com.maikol.fitnessapp.feature.tracking.FormField
import com.maikol.fitnessapp.feature.tracking.ModuleScreen
import com.maikol.fitnessapp.feature.tracking.PickerField
import com.maikol.fitnessapp.feature.tracking.RecordRow
import com.maikol.fitnessapp.feature.tracking.SourceLine
import com.maikol.fitnessapp.feature.tracking.TimePickerSheet
import com.maikol.fitnessapp.feature.tracking.dayLabel
import com.maikol.fitnessapp.feature.tracking.formatTime
import com.maikol.fitnessapp.feature.training.data.formatDuration
import com.maikol.fitnessapp.feature.training.ui.InlineMessage
import com.maikol.fitnessapp.feature.training.ui.NumberStyleLarge
import com.maikol.fitnessapp.feature.training.ui.OverlineStyle
import com.maikol.fitnessapp.feature.training.ui.StatTile
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun SleepRoute(onBack: () -> Unit, viewModel: SleepViewModel = viewModel(factory = SleepViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var toDelete by rememberSaveable { mutableStateOf<String?>(null) }
    FormBackGuard(open = state.form != null, dirty = state.form?.dirty == true, onClose = viewModel::closeForm)

    ModuleScreen(
        title = stringResource(R.string.category_sleep),
        subtitle = stringResource(R.string.sleep_subtitle),
        photo = R.drawable.h1_sleep,
        onBack = onBack,
    ) {
        if (state.loading) return@ModuleScreen
        val form = state.form
        if (form != null) {
            SleepFormCard(state, form, viewModel)
            return@ModuleScreen
        }
        val last = state.entries.firstOrNull()
        if (last == null) {
            EmptyCard(
                iconRes = R.drawable.ic_moon,
                title = stringResource(R.string.sleep_empty_title),
                body = stringResource(R.string.sleep_empty_body),
                actionLabel = stringResource(R.string.sleep_add),
                onAction = viewModel::add,
            )
        } else {
            SummaryCard(state, last)
            PrimaryButton(text = stringResource(R.string.sleep_add), onClick = viewModel::add, iconRes = R.drawable.ic_add, modifier = Modifier.fillMaxWidth())
            NightList(state, onEdit = viewModel::edit, onDelete = { toDelete = it.id })
        }
        InlineMessage(visible = state.deleteFailed, text = stringResource(R.string.record_delete_error))
    }
    val deleting = state.entries.firstOrNull { it.id == toDelete }
    if (deleting != null) {
        DeleteDialog(
            stringResource(R.string.sleep_entry_name, dayLabel(LocalDate.parse(deleting.wakeDate), state.today)),
            onConfirm = { toDelete = null; viewModel.delete(deleting.id) },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun SummaryCard(state: SleepUiState, last: SleepEntryEntity) {
    FitnessCard {
        Text(stringResource(R.string.sleep_last_night).uppercase(), style = OverlineStyle, color = MaterialTheme.colorScheme.primary)
        Text(formatDuration(sleepDurationMs(last.startAt, last.endAt)), style = NumberStyleLarge)
        Text(nightDetail(last, state.today), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.l))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
            StatTile(stringResource(R.string.sleep_week_total), if (state.weekNights == 0) "—" else formatDuration(state.weekTotalMs), Modifier.weight(1f))
            StatTile(
                stringResource(R.string.sleep_week_average),
                if (state.weekNights == 0) "—" else formatDuration(state.weekTotalMs / state.weekNights),
                Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(Spacing.s))
        Text(pluralStringResource(R.plurals.sleep_week_note, state.weekNights, state.weekNights), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.s))
        SourceLine(stringResource(R.string.sleep_source_detail))
    }
}

@Composable
private fun nightDetail(entry: SleepEntryEntity, today: LocalDate): String {
    val zone = ZoneId.of(entry.zoneId)
    val qualities = stringArrayResource(R.array.sleep_quality_levels)
    val parts = listOfNotNull(
        "${formatTime(TrackingRepository.timeOf(entry.startAt, zone))} → ${formatTime(TrackingRepository.timeOf(entry.endAt, zone))}",
        dayLabel(LocalDate.parse(entry.wakeDate), today),
        entry.quality?.let { qualities[it - 1] },
    )
    return parts.joinToString(" · ")
}

@Composable
private fun NightList(state: SleepUiState, onEdit: (SleepEntryEntity) -> Unit, onDelete: (SleepEntryEntity) -> Unit) {
    FitnessCard {
        Text(stringResource(R.string.sleep_recent), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Spacing.xs))
        state.entries.forEachIndexed { index, entry ->
            if (index > 0) HorizontalDivider(color = FitnessThemeExtras.colors.surfaceRaised)
            RecordRow(
                title = formatDuration(sleepDurationMs(entry.startAt, entry.endAt)),
                detail = nightDetail(entry, state.today) + (entry.note?.let { "\n$it" } ?: ""),
                value = null,
                onEdit = { onEdit(entry) },
                onDelete = { onDelete(entry) },
            )
        }
    }
}

private enum class Picker { START_DATE, START_TIME, END_DATE, END_TIME }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SleepFormCard(state: SleepUiState, form: SleepForm, vm: SleepViewModel) {
    var picker by rememberSaveable { mutableStateOf<Picker?>(null) }
    val errorText = when (form.error) {
        SleepError.END_NOT_AFTER_START -> stringResource(R.string.sleep_error_order)
        SleepError.TOO_LONG -> stringResource(R.string.sleep_error_long)
        SleepError.NOTE_TOO_LONG -> stringResource(R.string.sleep_error_note)
        null -> null
    }
    FormCard(
        title = stringResource(if (form.existing == null) R.string.sleep_form_new else R.string.sleep_form_edit),
        note = stringResource(R.string.sleep_form_note),
        saving = state.saving,
        failed = state.failed,
        onSave = vm::save,
        onCancel = vm::closeForm,
    ) {
        Text(stringResource(R.string.sleep_start), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            PickerField(stringResource(R.string.field_date), dayLabel(form.startDate, state.today), { picker = Picker.START_DATE }, Modifier.weight(1f))
            PickerField(stringResource(R.string.field_time), formatTime(form.startTime), { picker = Picker.START_TIME }, Modifier.weight(1f))
        }
        Text(stringResource(R.string.sleep_end), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            PickerField(stringResource(R.string.field_date), dayLabel(form.endDate, state.today), { picker = Picker.END_DATE }, Modifier.weight(1f), error = errorText != null)
            PickerField(stringResource(R.string.field_time), formatTime(form.endTime), { picker = Picker.END_TIME }, Modifier.weight(1f), error = errorText != null)
        }
        InlineMessage(visible = errorText != null, text = errorText.orEmpty())
        val overlap = state.overlap
        InlineMessage(
            visible = overlap != null,
            text = overlap?.let { stringResource(R.string.sleep_error_overlap, nightDetail(it, state.today)) }.orEmpty(),
        )
        Text(stringResource(R.string.sleep_quality), style = MaterialTheme.typography.titleMedium)
        val qualities = stringArrayResource(R.array.sleep_quality_levels)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            qualities.forEachIndexed { i, label ->
                val value = i + 1
                FilterChip(
                    selected = form.quality == value,
                    onClick = { vm.update { it.copy(quality = if (it.quality == value) null else value) } },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.heightIn(min = MinTouchTarget),
                )
            }
        }
        FormField(form.note, { v -> vm.update { it.copy(note = v.take(200)) } }, stringResource(R.string.sleep_note), kind = FieldKind.TEXT, singleLine = false)
    }
    when (picker) {
        Picker.START_DATE -> DatePickerSheet(form.startDate, state.today, { d -> picker = null; vm.update { it.copy(startDate = d) } }, { picker = null })
        Picker.END_DATE -> DatePickerSheet(form.endDate, state.today, { d -> picker = null; vm.update { it.copy(endDate = d) } }, { picker = null })
        Picker.START_TIME -> TimePickerSheet(form.startTime, { t -> picker = null; vm.update { it.copy(startTime = t) } }, { picker = null })
        Picker.END_TIME -> TimePickerSheet(form.endTime, { t -> picker = null; vm.update { it.copy(endTime = t) } }, { picker = null })
        null -> Unit
    }
}
