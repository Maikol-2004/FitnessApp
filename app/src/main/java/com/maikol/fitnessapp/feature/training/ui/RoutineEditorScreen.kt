package com.maikol.fitnessapp.feature.training.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.component.LoadingRegion
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.theme.FitnessTheme
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.feature.training.catalog.ExerciseCatalog
import com.maikol.fitnessapp.feature.training.catalog.LoadSemantics
import com.maikol.fitnessapp.feature.training.catalog.loadSemantics
import com.maikol.fitnessapp.feature.training.catalog.mediaAssets
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
            // V1: «Guardar rutina» flota sobre el fondo con su inset inferior, sin barra ni tarjeta detrás. Queda
            // fuera del scroll (encima del teclado con imePadding), así que nunca tapa campos ni errores.
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.l)
                    .padding(top = Spacing.s, bottom = Spacing.m),
            ) {
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
    // V1: todo al aire sobre el fondo; solo los campos y botones llevan cristal.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.l)
            .padding(top = Spacing.s, bottom = Spacing.xl),
    ) {
        GlassTextField(
            value = form.name,
            onValueChange = vm::onNameChange,
            label = stringResource(R.string.editor_name_label),
            error = if (errors.name) stringResource(R.string.editor_error_name) else null,
            capitalization = KeyboardCapitalization.Sentences,
        )
        RoutineNotes(form.notes, vm::onNotesChange)

        OpenSectionTitle(
            title = stringResource(R.string.editor_exercises_title),
            count = form.exercises.size.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.exercise_count, it, it) },
            modifier = Modifier.padding(top = Spacing.xl),
        )
        InlineMessage(visible = errors.noExercises, text = stringResource(R.string.editor_error_no_exercises), modifier = Modifier.padding(top = Spacing.s))

        form.exercises.forEachIndexed { index, exercise ->
            // Clave estable: notas abiertas y diálogos siguen al ejercicio al reordenar.
            key(exercise.id) {
                if (index > 0) {
                    HorizontalDivider(Modifier.padding(vertical = Spacing.xl), color = FitnessThemeExtras.colors.surfaceRaised)
                } else {
                    Spacer(Modifier.height(Spacing.l))
                }
                OpenExerciseEditor(index = index, count = form.exercises.size, exercise = exercise, unit = state.unit, errors = errors, vm = vm)
            }
        }

        if (form.exercises.isEmpty()) {
            Text(
                stringResource(R.string.editor_empty_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.m),
            )
        }
        GlassButton(
            text = stringResource(R.string.editor_add_exercise),
            iconRes = R.drawable.ic_add,
            onClick = vm::openPicker,
            modifier = Modifier.padding(top = Spacing.xl),
        )
        PlainAction(
            text = stringResource(R.string.editor_create_custom),
            iconRes = R.drawable.ic_edit,
            onClick = vm::addExercise,
            modifier = Modifier.padding(top = Spacing.s),
        )
    }
}

/** «Añadir notas» de la rutina: plegable; con notas empieza abierto y plegar nunca borra el texto. */
@Composable
private fun RoutineNotes(notes: String, onChange: (String) -> Unit) {
    var open by rememberSaveable { mutableStateOf(notes.isNotBlank()) }
    val focus = remember { FocusRequester() }
    var focusOnOpen by remember { mutableStateOf(false) }
    PlainAction(
        text = stringResource(notesToggleLabel(open, notes)),
        iconRes = if (open) null else R.drawable.ic_add,
        onClick = {
            open = !open
            focusOnOpen = open && notes.isBlank()
        },
        modifier = Modifier.padding(top = Spacing.xs),
    )
    if (open) {
        GlassTextField(
            value = notes,
            onValueChange = onChange,
            label = stringResource(R.string.editor_notes_label),
            singleLine = false,
            capitalization = KeyboardCapitalization.Sentences,
            focusRequester = focus,
        )
        LaunchedEffect(focusOnOpen) {
            if (focusOnOpen) {
                focus.requestFocus()
                focusOnOpen = false
            }
        }
    }
}

private fun notesToggleLabel(open: Boolean, notes: String): Int = when {
    open -> R.string.v1_hide_notes
    notes.isBlank() -> R.string.v1_add_notes
    else -> R.string.v1_show_notes
}

private val ThumbnailSize = 72.dp
private val ThumbnailShape = RoundedCornerShape(16.dp)

@Composable
private fun OpenExerciseEditor(
    index: Int,
    count: Int,
    exercise: ExerciseForm,
    unit: UnitSystem,
    errors: FormErrors,
    vm: RoutineEditorViewModel,
) {
    val label = exercise.name.ifBlank { stringResource(R.string.editor_exercise_label, index + 1) }
    val catalog = ExerciseCatalog.byId(exercise.catalogId)
    var notesOpen by rememberSaveable { mutableStateOf(exercise.notes.isNotBlank()) }
    val notesFocus = remember { FocusRequester() }
    var focusNotes by remember { mutableStateOf(false) }
    val largeFont = LocalDensity.current.fontScale >= 1.3f

    // Subir, bajar, notas y menú, sin barra contenedora.
    val actions: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            GlassIconButton(
                R.drawable.ic_arrow_up,
                stringResource(R.string.editor_move_up, label),
                onClick = { vm.moveExercise(exercise.id, -1) },
                enabled = index > 0,
            )
            GlassIconButton(
                R.drawable.ic_arrow_down,
                stringResource(R.string.editor_move_down, label),
                onClick = { vm.moveExercise(exercise.id, 1) },
                enabled = index < count - 1,
            )
            PlainAction(
                text = stringResource(if (notesOpen) R.string.v1_hide_notes else R.string.v1_exercise_notes),
                iconRes = if (notesOpen) null else R.drawable.ic_add,
                onClick = {
                    notesOpen = !notesOpen
                    focusNotes = notesOpen && exercise.notes.isBlank()
                },
            )
            Spacer(Modifier.weight(1f))
            OverflowMenu(
                description = stringResource(R.string.training_routine_actions, label),
                actions = listOf(
                    MenuAction(R.string.action_duplicate, R.drawable.ic_copy) { vm.duplicateExercise(exercise.id) },
                    MenuAction(R.string.editor_delete_exercise_short, R.drawable.ic_delete) { vm.removeExercise(exercise.id) },
                ),
            )
        }
    }

    Row(verticalAlignment = Alignment.Top) {
        if (catalog != null) {
            ExerciseAssetImage(
                path = catalog.mediaAssets.thumbnail,
                contentDescription = null,
                opaque = true,
                modifier = Modifier.size(ThumbnailSize).clip(ThumbnailShape),
            )
        } else {
            // Personalizado: sin foto de otro ejercicio.
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ThumbnailSize).editorGlass(ThumbnailShape)) {
                Icon(
                    painterResource(R.drawable.ic_edit),
                    contentDescription = stringResource(R.string.v1_custom_no_image),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(Modifier.width(Spacing.m))
        Column(Modifier.weight(1f)) {
            if (catalog != null) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f).padding(top = Spacing.xs).semantics(mergeDescendants = true) {}) {
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
            } else {
                // Personalizado: nombre libre y, con nombre, acceso a su ficha (progreso e historial por exerciseKey).
                Row(verticalAlignment = Alignment.Top) {
                    GlassTextField(
                        value = exercise.name,
                        onValueChange = { vm.onExerciseNameChange(exercise.id, it) },
                        label = stringResource(R.string.editor_exercise_name_label),
                        error = if (exercise.id in errors.exerciseName) stringResource(R.string.editor_error_exercise_name) else null,
                        capitalization = KeyboardCapitalization.Sentences,
                        modifier = Modifier.weight(1f),
                    )
                    if (exercise.name.isNotBlank()) {
                        IconButton(onClick = { vm.openCustomGuide(exercise.key) }, modifier = Modifier.padding(top = Spacing.xs)) {
                            Icon(
                                painterResource(R.drawable.ic_info),
                                contentDescription = stringResource(R.string.custom_open_detail, exercise.name),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            if (!largeFont) {
                Spacer(Modifier.height(Spacing.s))
                actions()
            }
        }
    }
    if (largeFont) {
        // Fuente grande: las acciones bajan a todo el ancho para no comprimirse junto a la miniatura.
        Spacer(Modifier.height(Spacing.s))
        actions()
    }
    if (catalog?.loadSemantics == LoadSemantics.ASSISTANCE) {
        Spacer(Modifier.height(Spacing.m))
        AssistModeSelector(exercise.assistMode, onChange = { vm.onAssistModeChange(exercise.id, it) })
    }
    if (notesOpen) {
        GlassTextField(
            value = exercise.notes,
            onValueChange = { vm.onExerciseNotesChange(exercise.id, it) },
            label = stringResource(R.string.editor_exercise_notes_label),
            singleLine = false,
            capitalization = KeyboardCapitalization.Sentences,
            focusRequester = notesFocus,
            modifier = Modifier.padding(top = Spacing.m),
        )
        LaunchedEffect(focusNotes) {
            if (focusNotes) {
                notesFocus.requestFocus()
                focusNotes = false
            }
        }
    }

    val working = exercise.sets.count { !it.warmup }
    OpenSectionTitle(
        title = stringResource(R.string.v1_planned_sets),
        count = pluralStringResource(R.plurals.v1_working_sets, working, working),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = Spacing.xl),
    )
    HorizontalDivider(Modifier.padding(top = Spacing.s, bottom = Spacing.m), color = FitnessThemeExtras.colors.surfaceRaised)

    BoxWithConstraints {
        // Una fila por serie si caben los controles a 48 dp; con fuente grande o pantalla estrecha, dos líneas con etiquetas.
        val stacked = maxWidth / LocalDensity.current.fontScale < 300.dp
        Column(verticalArrangement = Arrangement.spacedBy(if (stacked) Spacing.l else Spacing.s)) {
            if (!stacked) SetHeaderRow(unit)
            exercise.sets.forEachIndexed { setIndex, set ->
                key(set.id) {
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
    }
    Text(
        stringResource(R.string.v1_type_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.m),
    )
    val setIds = exercise.sets.map { it.id }.toSet()
    InlineMessage(visible = exercise.id in errors.noSets, text = stringResource(R.string.editor_error_no_sets), modifier = Modifier.padding(top = Spacing.s))
    InlineMessage(visible = errors.reps.any { it in setIds }, text = stringResource(R.string.error_reps), modifier = Modifier.padding(top = Spacing.s))
    InlineMessage(visible = errors.load.any { it in setIds }, text = stringResource(R.string.error_load), modifier = Modifier.padding(top = Spacing.s))
    InlineMessage(visible = errors.rest.any { it in setIds }, text = stringResource(R.string.error_rest), modifier = Modifier.padding(top = Spacing.s))
    GlassButton(
        text = stringResource(R.string.editor_add_set),
        iconRes = R.drawable.ic_add,
        onClick = { vm.addSet(exercise.id) },
        modifier = Modifier.padding(top = Spacing.m),
    )
}

private val SetTypeWidth = 72.dp

@Composable
private fun SetHeaderRow(unit: UnitSystem) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.semantics(mergeDescendants = true) {}) {
        HeaderLabel(stringResource(R.string.v1_col_type), Modifier.width(SetTypeWidth))
        HeaderLabel(unit.loadUnit(), Modifier.weight(1f))
        HeaderLabel(stringResource(R.string.editor_col_reps), Modifier.weight(1f))
        HeaderLabel(stringResource(R.string.v1_col_rest), Modifier.weight(1f))
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

/** Fila de serie: campos de cristal independientes, sin nada detrás que englobe la fila. */
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
    val restDesc = stringResource(R.string.editor_field_description, number, stringResource(R.string.v1_col_rest))
    val type: @Composable (Modifier) -> Unit = { m ->
        SetTypeSelector(number, set.warmup, onSelect = { if (it != set.warmup) onWarmup() }, modifier = m)
    }
    val load: @Composable (Modifier) -> Unit = { m ->
        NumberCell(
            set.load.text, onLoad, loadDesc, m.editorGlass(),
            decimal = true, isError = loadError, placeholder = "—", background = Color.Transparent, shape = GlassShape,
        )
    }
    val reps: @Composable (Modifier) -> Unit = { m ->
        NumberCell(set.reps, onReps, repsDesc, m.editorGlass(), isError = repsError, background = Color.Transparent, shape = GlassShape)
    }
    val rest: @Composable (Modifier) -> Unit = { m -> RestCell(set.rest, restDesc, restError, onRest, m) }
    val menu: @Composable () -> Unit = {
        // Eliminar queda dentro de un menú (menos borrados accidentales) y respeta el mínimo de una serie.
        OverflowMenu(
            description = stringResource(R.string.v1_set_actions, number),
            actions = listOf(MenuAction(R.string.v1_delete_set, R.drawable.ic_delete, onDelete)),
            enabled = canDelete,
        )
    }
    if (!stacked) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
            type(Modifier.width(SetTypeWidth))
            load(Modifier.weight(1f))
            reps(Modifier.weight(1f))
            rest(Modifier.weight(1f))
            menu()
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.Bottom) {
                LabeledCell(stringResource(R.string.v1_col_type), Modifier.width(SetTypeWidth)) { type(Modifier.fillMaxWidth()) }
                LabeledCell(unit.loadUnit(), Modifier.weight(1f)) { load(Modifier.fillMaxWidth()) }
                menu()
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.Bottom) {
                LabeledCell(stringResource(R.string.editor_col_reps), Modifier.weight(1f)) { reps(Modifier.fillMaxWidth()) }
                LabeledCell(stringResource(R.string.v1_col_rest), Modifier.weight(1f)) { rest(Modifier.fillMaxWidth()) }
                Spacer(Modifier.width(MinTouchTarget))
            }
        }
    }
}

@Composable
private fun LabeledCell(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier) {
        Text(label.uppercase(), style = OverlineStyle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(Modifier.height(Spacing.xs))
        content()
    }
}

@Preview(name = "Fila de serie", showBackground = true, backgroundColor = 0xFF121416, widthDp = 360)
@Composable
private fun SetRowPreview() {
    FitnessTheme {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            SetHeaderRow(UnitSystem.METRIC)
            SetEditorRow(
                1, SetForm("a", "10", LoadText("20", 20.0, "20"), "90", true), UnitSystem.METRIC,
                false, true, false, false, false, {}, {}, {}, {}, {},
            )
            SetEditorRow(
                2, SetForm("b", "", LoadText("x", null, ""), "120", false), UnitSystem.METRIC,
                false, true, true, true, false, {}, {}, {}, {}, {},
            )
        }
    }
}
