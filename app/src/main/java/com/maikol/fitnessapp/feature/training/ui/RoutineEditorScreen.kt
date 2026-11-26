package com.maikol.fitnessapp.feature.training.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.component.FitnessCard
import com.maikol.fitnessapp.core.designsystem.component.LoadingRegion
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.component.SecondaryButton
import com.maikol.fitnessapp.core.designsystem.theme.FitnessTheme
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.feature.training.catalog.ExerciseCatalog
import com.maikol.fitnessapp.feature.training.catalog.LoadSemantics
import com.maikol.fitnessapp.feature.training.catalog.loadSemantics
import com.maikol.fitnessapp.feature.training.data.LoadText
import com.maikol.fitnessapp.feature.training.data.loadUnit

@Composable
fun RoutineEditorRoute(
    onClose: () -> Unit,
    onSaved: (routineId: String, isNew: Boolean) -> Unit,
    onOpenSession: (sessionId: String) -> Unit = {},
    viewModel: RoutineEditorViewModel = viewModel(factory = RoutineEditorViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val attemptClose = { if (state.hasChanges && !state.saving) confirmDiscard = true else onClose() }

    BackHandler(enabled = state.hasChanges, onBack = attemptClose)
    Box(Modifier.fillMaxSize()) {
    LaunchedEffect(state.savedRoutineId) {
        state.savedRoutineId?.let { id ->
            viewModel.onSavedHandled()
            onSaved(id, state.isNew)
        }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        TrainingTopBar(
            title = stringResource(if (state.isNew) R.string.editor_new_title else R.string.editor_edit_title),
            onBack = attemptClose,
        )
        LoadingRegion(isLoading = state.loading, message = stringResource(R.string.loading_data), modifier = Modifier.weight(1f)) {
            val form = state.form
            if (form == null) {
                Text(
                    stringResource(R.string.detail_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(Spacing.l),
                )
                return@LoadingRegion
            }
            RoutineEditorContent(state, form, viewModel)
        }
        if (state.form != null) {
            BottomActionBar {
                Column(Modifier.weight(1f)) {
                    InlineMessage(visible = state.saveFailed, text = stringResource(R.string.editor_save_failed), modifier = Modifier.padding(bottom = Spacing.s))
                    InlineMessage(
                        visible = !state.saveFailed && state.showErrors && !state.errors.isEmpty,
                        text = stringResource(R.string.editor_fix_errors),
                        modifier = Modifier.padding(bottom = Spacing.s),
                    )
                    PrimaryButton(
                        text = stringResource(R.string.editor_save),
                        onClick = {
                            // El teclado no debe seguir abierto en la pantalla siguiente (tapaba las series).
                            focus.clearFocus()
                            viewModel.save()
                        },
                        busy = state.saving,
                        iconRes = R.drawable.ic_check,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (confirmDiscard) {
        FitnessDialog(
            title = stringResource(R.string.editor_discard_title),
            body = stringResource(R.string.editor_discard_body),
            confirmLabel = stringResource(R.string.editor_discard_confirm),
            onConfirm = {
                confirmDiscard = false
                onClose()
            },
            dismissLabel = stringResource(R.string.editor_keep_editing),
            onDismiss = { confirmDiscard = false },
            destructive = true,
        )
    }

    // Capas G1: el editor sigue compuesto debajo (conserva scroll y foco); su «atrás» tiene prioridad.
    state.picker?.let { picker ->
        ExercisePickerScreen(
            picker = picker,
            inRoutine = state.catalogIdsInForm,
            onQuery = viewModel::onPickerQuery,
            onMuscle = viewModel::onPickerMuscle,
            onEquipment = viewModel::onPickerEquipment,
            onClearFilters = viewModel::clearPickerFilters,
            onToggle = viewModel::togglePicked,
            onInfo = viewModel::openGuide,
            onCancel = viewModel::closePicker,
            onConfirm = viewModel::confirmPicker,
        )
    }
    // Ficha: catálogo por catalogId; personalizado por su exerciseKey (G2.1), con el nombre y las notas del borrador.
    val subject = DetailSubject.resolve(state.guideId) { key ->
        state.form?.exercises?.firstOrNull { it.key == key && it.catalogId == null }?.let { it.name to it.notes }
    }
    subject?.let { detail ->
        val picker = state.picker
        val catalogId = (detail as? DetailSubject.Catalog)?.exercise?.id
        ExerciseDetailScreen(
            subject = detail,
            onClose = viewModel::closeGuide,
            onOpenSession = onOpenSession,
            selection = if (picker == null || catalogId == null) null else GuideSelection(
                selected = catalogId in picker.selected,
                inRoutine = catalogId in state.catalogIdsInForm,
                onToggle = { viewModel.togglePicked(catalogId) },
            ),
        )
    }
    }
}

@Composable
private fun RoutineEditorContent(state: EditorUiState, form: RoutineForm, vm: RoutineEditorViewModel) {
    val errors = state.visibleErrors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.l)
            .padding(top = Spacing.s, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
    ) {
        FitnessCard {
            EditorTextField(
                value = form.name,
                onValueChange = vm::onNameChange,
                label = stringResource(R.string.editor_name_label),
                error = if (errors.name) stringResource(R.string.editor_error_name) else null,
                capitalization = KeyboardCapitalization.Sentences,
            )
            Spacer(Modifier.height(Spacing.m))
            EditorTextField(
                value = form.notes,
                onValueChange = vm::onNotesChange,
                label = stringResource(R.string.editor_notes_label),
                singleLine = false,
                capitalization = KeyboardCapitalization.Sentences,
            )
        }

        SectionHeader(title = stringResource(R.string.editor_exercises_title), modifier = Modifier.padding(top = Spacing.m))
        InlineMessage(visible = errors.noExercises, text = stringResource(R.string.editor_error_no_exercises))

        form.exercises.forEachIndexed { index, exercise ->
            ExerciseEditorCard(
                index = index,
                count = form.exercises.size,
                exercise = exercise,
                unit = state.unit,
                errors = errors,
                vm = vm,
            )
        }

        if (form.exercises.isEmpty()) {
            Text(
                stringResource(R.string.editor_empty_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SecondaryButton(
            text = stringResource(R.string.editor_add_exercise),
            onClick = vm::openPicker,
            iconRes = R.drawable.ic_add,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = vm::addExercise, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget)) {
            Icon(painterResource(R.drawable.ic_edit), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text(stringResource(R.string.editor_create_custom), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ExerciseEditorCard(
    index: Int,
    count: Int,
    exercise: ExerciseForm,
    unit: UnitSystem,
    errors: FormErrors,
    vm: RoutineEditorViewModel,
) {
    val label = exercise.name.ifBlank { stringResource(R.string.editor_exercise_label, index + 1) }
    FitnessCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .background(champagneTint(0.14f), MaterialTheme.shapes.small),
            ) {
                Text("${index + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(Spacing.m))
            Text(
                stringResource(R.string.editor_exercise_label, index + 1).uppercase(),
                style = OverlineStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { vm.moveExercise(exercise.id, -1) }, enabled = index > 0) {
                Icon(painterResource(R.drawable.ic_arrow_up), contentDescription = stringResource(R.string.editor_move_up, label))
            }
            IconButton(onClick = { vm.moveExercise(exercise.id, 1) }, enabled = index < count - 1) {
                Icon(painterResource(R.drawable.ic_arrow_down), contentDescription = stringResource(R.string.editor_move_down, label))
            }
            OverflowMenu(
                description = stringResource(R.string.training_routine_actions, label),
                actions = listOf(
                    MenuAction(R.string.action_duplicate, R.drawable.ic_copy) { vm.duplicateExercise(exercise.id) },
                    MenuAction(R.string.editor_delete_exercise_short, R.drawable.ic_delete) { vm.removeExercise(exercise.id) },
                ),
            )
        }
        Spacer(Modifier.height(Spacing.m))
        val catalog = ExerciseCatalog.byId(exercise.catalogId)
        if (catalog != null) {
            // Del catálogo: el nombre viene de la definición; no se vuelve a pedir.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                    Text(exercise.name, style = MaterialTheme.typography.titleLarge)
                    Text(catalog.metaLine(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { vm.openGuide(catalog.id) }) {
                    Icon(
                        painterResource(R.drawable.ic_info),
                        contentDescription = stringResource(R.string.picker_info, exercise.name),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (catalog.loadSemantics == LoadSemantics.ASSISTANCE) {
                Spacer(Modifier.height(Spacing.s))
                AssistModeSelector(exercise.assistMode, onChange = { vm.onAssistModeChange(exercise.id, it) })
            }
        } else {
            // Personalizado: nombre libre y, con nombre, acceso a su ficha (progreso e historial por exerciseKey).
            Row(verticalAlignment = Alignment.CenterVertically) {
                EditorTextField(
                    value = exercise.name,
                    onValueChange = { vm.onExerciseNameChange(exercise.id, it) },
                    label = stringResource(R.string.editor_exercise_name_label),
                    error = if (exercise.id in errors.exerciseName) stringResource(R.string.editor_error_exercise_name) else null,
                    capitalization = KeyboardCapitalization.Sentences,
                    modifier = Modifier.weight(1f),
                )
                if (exercise.name.isNotBlank()) {
                    IconButton(onClick = { vm.openCustomGuide(exercise.key) }) {
                        Icon(
                            painterResource(R.drawable.ic_info),
                            contentDescription = stringResource(R.string.custom_open_detail, exercise.name),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.s))
        EditorTextField(
            value = exercise.notes,
            onValueChange = { vm.onExerciseNotesChange(exercise.id, it) },
            label = stringResource(R.string.editor_exercise_notes_label),
            singleLine = false,
            capitalization = KeyboardCapitalization.Sentences,
        )
        Spacer(Modifier.height(Spacing.l))

        BoxWithConstraints {
            // Cuatro columnas si caben cómodamente; con fuente grande o pantalla estrecha, cada serie en bloque.
            val stacked = maxWidth / LocalDensity.current.fontScale < 300.dp
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                if (!stacked) SetHeaderRow(unit)
                exercise.sets.forEachIndexed { setIndex, set ->
                    SetEditorRow(
                        number = setIndex + 1,
                        set = set,
                        unit = unit,
                        stacked = stacked,
                        canDelete = exercise.sets.size > 1,
                        repsError = set.id in errors.reps,
                        loadError = set.id in errors.load,
                        restError = set.id in errors.rest,
                        onReps = { vm.onRepsChange(exercise.id, set.id, it) },
                        onLoad = { vm.onLoadChange(exercise.id, set.id, it) },
                        onRest = { vm.onRestChange(exercise.id, set.id, it) },
                        onWarmup = { vm.onWarmupToggle(exercise.id, set.id) },
                        onDelete = { vm.removeSet(exercise.id, set.id) },
                    )
                }
            }
        }
        Text(
            stringResource(R.string.editor_warmup_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.s),
        )
        val setIds = exercise.sets.map { it.id }.toSet()
        InlineMessage(visible = exercise.id in errors.noSets, text = stringResource(R.string.editor_error_no_sets), modifier = Modifier.padding(top = Spacing.s))
        InlineMessage(visible = errors.reps.any { it in setIds }, text = stringResource(R.string.error_reps), modifier = Modifier.padding(top = Spacing.s))
        InlineMessage(visible = errors.load.any { it in setIds }, text = stringResource(R.string.error_load), modifier = Modifier.padding(top = Spacing.s))
        InlineMessage(visible = errors.rest.any { it in setIds }, text = stringResource(R.string.error_rest), modifier = Modifier.padding(top = Spacing.s))
        TextButton(onClick = { vm.addSet(exercise.id) }, modifier = Modifier.heightIn(min = MinTouchTarget)) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text(stringResource(R.string.editor_add_set), style = MaterialTheme.typography.labelLarge)
        }
    }
}

private val SetNumberWidth = 44.dp

@Composable
private fun SetHeaderRow(unit: UnitSystem) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.semantics(mergeDescendants = true) {}) {
        HeaderLabel(stringResource(R.string.editor_col_set), Modifier.width(SetNumberWidth))
        HeaderLabel(unit.loadUnit(), Modifier.weight(1f))
        HeaderLabel(stringResource(R.string.editor_col_reps), Modifier.weight(1f))
        HeaderLabel(stringResource(R.string.editor_col_rest), Modifier.weight(1f))
        Spacer(Modifier.width(MinTouchTarget))
    }
}

@Composable
internal fun HeaderLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = OverlineStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
private fun SetEditorRow(
    number: Int,
    set: SetForm,
    unit: UnitSystem,
    stacked: Boolean,
    canDelete: Boolean,
    repsError: Boolean,
    loadError: Boolean,
    restError: Boolean,
    onReps: (String) -> Unit,
    onLoad: (String) -> Unit,
    onRest: (String) -> Unit,
    onWarmup: () -> Unit,
    onDelete: () -> Unit,
) {
    val loadDesc = stringResource(R.string.editor_field_description, number, unit.loadUnit())
    val repsDesc = stringResource(R.string.editor_field_description, number, stringResource(R.string.editor_col_reps))
    val restDesc = stringResource(R.string.editor_field_description, number, stringResource(R.string.editor_col_rest))
    val numberSlot: @Composable () -> Unit = { WarmupToggle(number, set.warmup, onWarmup) }
    val delete: @Composable () -> Unit = {
        IconButton(onClick = onDelete, enabled = canDelete) {
            Icon(
                painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.editor_delete_set, number),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (!stacked) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
            numberSlot()
            NumberCell(set.load.text, onLoad, loadDesc, Modifier.weight(1f), decimal = true, isError = loadError, placeholder = "—")
            NumberCell(set.reps, onReps, repsDesc, Modifier.weight(1f), isError = repsError)
            NumberCell(set.rest, onRest, restDesc, Modifier.weight(1f), isError = restError, placeholder = "0")
            delete()
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, FitnessThemeExtras.colors.surfaceRaised, MaterialTheme.shapes.medium)
                .padding(Spacing.s),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                numberSlot()
                Spacer(Modifier.weight(1f))
                delete()
            }
            LabeledCell(unit.loadUnit()) { NumberCell(set.load.text, onLoad, loadDesc, Modifier.fillMaxWidth(), decimal = true, isError = loadError, placeholder = "—") }
            LabeledCell(stringResource(R.string.editor_col_reps)) { NumberCell(set.reps, onReps, repsDesc, Modifier.fillMaxWidth(), isError = repsError) }
            LabeledCell(stringResource(R.string.editor_col_rest)) { NumberCell(set.rest, onRest, restDesc, Modifier.fillMaxWidth(), isError = restError, placeholder = "0") }
        }
    }
}

@Composable
private fun LabeledCell(label: String, content: @Composable () -> Unit) {
    Column {
        Text(label.uppercase(), style = OverlineStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xs))
        content()
    }
}

/** Número de serie que alterna calentamiento («C»). Casilla accesible con su estado. */
@Composable
private fun WarmupToggle(number: Int, warmup: Boolean, onToggle: () -> Unit) {
    val description = stringResource(R.string.editor_warmup_toggle, number)
    val state = stringResource(if (warmup) R.string.editor_warmup else R.string.editor_working_set)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(SetNumberWidth, MinTouchTarget)
            .background(if (warmup) champagneTint(0.16f) else FitnessThemeExtras.colors.surfaceRaised, MaterialTheme.shapes.medium)
            .toggleable(value = warmup, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics {
                contentDescription = description
                stateDescription = state
            },
    ) {
        Text(
            if (warmup) stringResource(R.string.editor_warmup_short) else "$number",
            style = MaterialTheme.typography.titleMedium,
            color = if (warmup) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun EditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    singleLine: Boolean = true,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(capitalization = capitalization),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FitnessThemeExtras.colors.surfaceRaised,
            unfocusedContainerColor = FitnessThemeExtras.colors.surfaceRaised,
            errorContainerColor = FitnessThemeExtras.colors.surfaceRaised,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview(name = "Fila de serie", showBackground = true, backgroundColor = 0xFF1D2023, widthDp = 360)
@Composable
private fun SetRowPreview() {
    FitnessTheme {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            SetHeaderRow(UnitSystem.METRIC)
            SetEditorRow(1, SetForm("a", "10", LoadText("20", 20.0, "20"), "90", true), UnitSystem.METRIC, false, true, false, false, false, {}, {}, {}, {}, {})
            SetEditorRow(2, SetForm("b", "", LoadText("x", null, ""), "90", false), UnitSystem.METRIC, false, true, true, true, false, {}, {}, {}, {}, {})
        }
    }
}
