package com.maikol.fitnessapp.feature.training.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.component.FitnessCard
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.component.SecondaryButton
import com.maikol.fitnessapp.core.designsystem.motion.LocalMotionEnabled
import com.maikol.fitnessapp.core.designsystem.theme.FitnessTheme
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.feature.training.catalog.CatalogExercise
import com.maikol.fitnessapp.feature.training.catalog.Equipment
import com.maikol.fitnessapp.feature.training.catalog.ExerciseCatalog
import com.maikol.fitnessapp.feature.training.catalog.MuscleGroup
import com.maikol.fitnessapp.feature.training.catalog.search

/*
 * G1: selector del catálogo y ficha de ejercicio. Son capas a pantalla completa dentro del editor (y la ficha
 * también en la sesión): no añaden rutas, conservan el estado de la pantalla de debajo y su «atrás» tiene
 * prioridad. Sin medios de demostración: la ficha lo declara con «Guía en texto».
 */

/** «Músculo · equipo principal» para filas y tarjetas. */
internal fun CatalogExercise.metaLine(): String = "${primary.label} · ${equipment.joinToString(" / ") { it.label }}"

@Composable
internal fun ExercisePickerScreen(
    picker: PickerState,
    inRoutine: Set<String>,
    onQuery: (String) -> Unit,
    onMuscle: (MuscleGroup?) -> Unit,
    onEquipment: (Equipment?) -> Unit,
    onClearFilters: () -> Unit,
    onToggle: (String) -> Unit,
    onInfo: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    BackHandler(onBack = onCancel)
    val results = remember(picker.query, picker.muscle, picker.equipment) {
        ExerciseCatalog.all.search(picker.query, picker.muscle, picker.equipment)
    }
    // Saveable: la posición se conserva al abrir/cerrar la ficha (que se dibuja encima) y al rotar.
    val listState = rememberLazyListState()
    val focus = LocalFocusManager.current
    val count = picker.selected.size
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().imePadding()) {
            TrainingTopBar(
                title = stringResource(R.string.picker_title),
                subtitle = pluralStringResource(R.plurals.picker_results, results.size, results.size),
                onBack = onCancel,
            )
            SearchField(picker.query, onQuery, onDone = { focus.clearFocus() }, modifier = Modifier.padding(horizontal = Spacing.l))
            ChipRow(
                label = stringResource(R.string.picker_filter_muscle),
                allLabel = stringResource(R.string.picker_all_muscles),
                options = MuscleGroup.entries,
                selected = picker.muscle,
                optionLabel = { it.label },
                onSelect = onMuscle,
            )
            ChipRow(
                label = stringResource(R.string.picker_filter_equipment),
                allLabel = stringResource(R.string.picker_all_equipment),
                options = Equipment.entries,
                selected = picker.equipment,
                optionLabel = { it.label },
                onSelect = onEquipment,
            )
            if (results.isEmpty()) {
                EmptyResults(onClearFilters, Modifier.weight(1f))
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, top = Spacing.s, bottom = Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s),
                    modifier = Modifier.weight(1f),
                ) {
                    items(results, key = { it.id }) { exercise ->
                        PickerRow(
                            exercise = exercise,
                            selectedOrder = picker.selected.indexOf(exercise.id).takeIf { it >= 0 }?.plus(1),
                            inRoutine = exercise.id in inRoutine,
                            onToggle = {
                                focus.clearFocus()
                                onToggle(exercise.id)
                            },
                            onInfo = { onInfo(exercise.id) },
                        )
                    }
                }
            }
            BottomActionBar {
                val counter = if (count == 0) stringResource(R.string.picker_none_selected)
                else pluralStringResource(R.plurals.picker_selected_count, count, count)
                val addText = if (count == 0) stringResource(R.string.picker_add_none) else pluralStringResource(R.plurals.picker_add, count, count)
                // Fuente grande: contador encima y botón a lo ancho, sin partir palabras.
                if (LocalDensity.current.fontScale >= 1.5f) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(counter, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        PrimaryButton(text = addText, onClick = onConfirm, enabled = count > 0, iconRes = R.drawable.ic_add, modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    Text(counter, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    PrimaryButton(text = addText, onClick = onConfirm, enabled = count > 0, iconRes = R.drawable.ic_add)
                }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(stringResource(R.string.picker_search)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = if (value.isEmpty()) null else {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.picker_clear_search))
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Search),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FitnessThemeExtras.colors.surfaceRaised,
            unfocusedContainerColor = FitnessThemeExtras.colors.surfaceRaised,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
    )
}

@Composable
private fun <T> ChipRow(
    label: String,
    allLabel: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelect: (T?) -> Unit,
) {
    Column(Modifier.padding(top = Spacing.s)) {
        Text(
            label.uppercase(),
            style = OverlineStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.l),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.l),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            item { FilterOption(allLabel, selected == null) { onSelect(null) } }
            items(options) { option ->
                // Tocar el filtro activo lo quita (vuelve a «Todos»).
                FilterOption(optionLabel(option), selected == option) { onSelect(if (selected == option) null else option) }
            }
        }
    }
}

@Composable
private fun FilterOption(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier.heightIn(min = MinTouchTarget),
    )
}

@Composable
private fun EmptyResults(onClear: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = modifier.fillMaxWidth().padding(Spacing.xl),
    ) {
        Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        Text(stringResource(R.string.picker_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.picker_empty_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SecondaryButton(text = stringResource(R.string.picker_clear_filters), onClick = onClear, iconRes = R.drawable.ic_close)
    }
}

/** Fila: zona de selección (casilla) y botón de ficha separados; tocar «i» nunca añade. */
@Composable
private fun PickerRow(
    exercise: CatalogExercise,
    selectedOrder: Int?,
    inRoutine: Boolean,
    onToggle: () -> Unit,
    onInfo: () -> Unit,
) {
    val selected = selectedOrder != null
    val motion = LocalMotionEnabled.current
    val spec = if (motion) tween<Color>(160) else snap()
    val primary = MaterialTheme.colorScheme.primary
    val border by animateColorAsState(if (selected) primary else Color.Transparent, spec, label = "pickerBorder")
    val checkFill by animateColorAsState(if (selected) primary else Color.Transparent, spec, label = "pickerCheck")
    val stateText = stringResource(
        when {
            inRoutine -> R.string.picker_in_routine
            selected -> R.string.picker_select_state
            else -> R.string.picker_unselected_state
        },
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(FitnessThemeExtras.colors.card)
            .border(1.dp, border, MaterialTheme.shapes.large),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 64.dp)
                .toggleable(value = selected || inRoutine, enabled = !inRoutine, role = Role.Checkbox, onValueChange = { onToggle() })
                .semantics(mergeDescendants = true) { stateDescription = stateText }
                .padding(start = Spacing.m, top = Spacing.s, bottom = Spacing.s),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (inRoutine) FitnessThemeExtras.colors.surfaceRaised else checkFill)
                    .border(2.dp, if (selected) primary else MaterialTheme.colorScheme.outline, CircleShape),
            ) {
                when {
                    selected -> Text("$selectedOrder", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
                    inRoutine -> Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (inRoutine) "${exercise.metaLine()} · ${stringResource(R.string.picker_in_routine)}" else exercise.metaLine(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (inRoutine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onInfo, modifier = Modifier.size(MinTouchTarget + Spacing.s)) {
            Icon(
                painterResource(R.drawable.ic_info),
                contentDescription = stringResource(R.string.picker_info, exercise.name),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Acción de la ficha cuando se abre desde el selector. */
internal data class GuideSelection(val selected: Boolean, val inRoutine: Boolean, val onToggle: () -> Unit)

/** Ficha completa del ejercicio. Sin [selection] (sesión, editor) solo informa. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExerciseGuideScreen(exercise: CatalogExercise, onClose: () -> Unit, selection: GuideSelection? = null) {
    BackHandler(onBack = onClose)
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TrainingTopBar(title = exercise.name, subtitle = exercise.metaLine(), onBack = onClose)
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.l)
                    .padding(top = Spacing.s, bottom = Spacing.xl),
            ) {
                FitnessCard {
                    Fact(stringResource(R.string.guide_primary), exercise.primary.label)
                    if (exercise.secondary.isNotEmpty()) {
                        Fact(stringResource(R.string.guide_secondary), exercise.secondary.joinToString(", ") { it.label })
                    }
                    val main = exercise.equipment.joinToString(" / ") { it.label }
                    val accessory = stringResource(R.string.guide_accessory, exercise.accessories.joinToString(", ") { it.label.lowercase() })
                    Fact(stringResource(R.string.guide_equipment), if (exercise.accessories.isEmpty()) main else "$main · $accessory")
                    Fact(stringResource(R.string.guide_difficulty), "${exercise.difficulty.label}. ${exercise.difficulty.description}", last = true)
                }
                // Ausencia explícita de medios: nada de imágenes rotas ni vídeos prometidos.
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(champagneTint(0.10f))
                        .padding(Spacing.m)
                        .semantics(mergeDescendants = true) {},
                ) {
                    Icon(painterResource(R.drawable.ic_article), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Spacing.m))
                    Column {
                        Text(stringResource(R.string.guide_text_only), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.guide_text_only_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                GuideSection(stringResource(R.string.guide_preparation)) {
                    Text(exercise.preparation, style = MaterialTheme.typography.bodyLarge)
                }
                GuideSection(stringResource(R.string.guide_steps)) {
                    exercise.steps.forEachIndexed { i, step ->
                        Row(Modifier.semantics(mergeDescendants = true) {}.padding(vertical = Spacing.xs)) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(28.dp).background(champagneTint(0.14f), CircleShape),
                            ) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(Spacing.m))
                            Text(step, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        }
                    }
                }
                GuideSection(stringResource(R.string.guide_breathing)) {
                    Text(exercise.breathing, style = MaterialTheme.typography.bodyLarge)
                }
                GuideSection(stringResource(R.string.guide_mistakes)) {
                    exercise.mistakes.forEach { mistake ->
                        Row(Modifier.padding(vertical = Spacing.xs).semantics(mergeDescendants = true) {}) {
                            Icon(painterResource(R.drawable.ic_error), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp).size(18.dp))
                            Spacer(Modifier.width(Spacing.m))
                            Text(mistake, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            if (selection != null) {
                BottomActionBar {
                    when {
                        selection.inRoutine -> SecondaryButton(
                            text = stringResource(R.string.picker_in_routine),
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.weight(1f),
                        )
                        selection.selected -> SecondaryButton(
                            text = stringResource(R.string.guide_selected),
                            onClick = selection.onToggle,
                            iconRes = R.drawable.ic_check,
                            modifier = Modifier.weight(1f),
                        )
                        else -> PrimaryButton(
                            text = stringResource(R.string.guide_add),
                            onClick = selection.onToggle,
                            iconRes = R.drawable.ic_add,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Fact(label: String, value: String, last: Boolean = false) {
    Column(Modifier.padding(bottom = if (last) 0.dp else Spacing.m).semantics(mergeDescendants = true) {}) {
        Text(label.uppercase(), style = OverlineStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xs))
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun GuideSection(title: String, content: @Composable () -> Unit) {
    FitnessCard {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() }.padding(bottom = Spacing.s))
        content()
    }
}

@Preview(name = "Selector", showBackground = true, backgroundColor = 0xFF1D2023, widthDp = 400, heightDp = 860)
@Composable
private fun PickerPreview() {
    FitnessTheme {
        ExercisePickerScreen(
            picker = PickerState(muscle = MuscleGroup.SHOULDERS, selected = listOf("shoulders_dumbbell_lateral_raise")),
            inRoutine = setOf("shoulders_seated_dumbbell_press"),
            onQuery = {}, onMuscle = {}, onEquipment = {}, onClearFilters = {}, onToggle = {}, onInfo = {}, onCancel = {}, onConfirm = {},
        )
    }
}

@Preview(name = "Ficha", showBackground = true, backgroundColor = 0xFF1D2023, widthDp = 400, heightDp = 860)
@Composable
private fun GuidePreview() {
    FitnessTheme {
        ExerciseGuideScreen(ExerciseCatalog.all.first(), onClose = {}, selection = GuideSelection(selected = false, inRoutine = false, onToggle = {}))
    }
}
