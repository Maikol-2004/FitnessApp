package com.maikol.fitnessapp.feature.training.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.designsystem.theme.TitaniumChampagneTokens
import com.maikol.fitnessapp.feature.training.data.parseRestSeconds

/*
 * V1 (docs/15_OPEN_GLASS_ROUTINE_EDITOR.md): material de cristal ahumado SOLO para campos, selectores y botones
 * del editor de rutinas. Mismo lenguaje que LiquidGlassDock (grafito, reflejo superior fino, contorno champán y
 * lente champán), pero sin desenfoque del fondo por celda: degradados y bordes ligeros.
 */

internal val GlassShape = RoundedCornerShape(16.dp)
private val Graphite = Color(TitaniumChampagneTokens.SURFACE)
private val GraphiteRaised = Color(TitaniumChampagneTokens.SURFACE_RAISED)
private val OffWhite = Color(TitaniumChampagneTokens.ON_SURFACE)

/** Cristal ahumado; con [lens], la lente champán del dock (selector de tipo). [outline] sustituye el contorno (foco/error). */
@Composable
internal fun Modifier.editorGlass(shape: Shape = GlassShape, lens: Boolean = false, outline: Color? = null): Modifier {
    val champagne = MaterialTheme.colorScheme.primary
    val fill = if (lens) {
        Brush.verticalGradient(0f to champagne.copy(alpha = 0.30f), 0.6f to champagne.copy(alpha = 0.15f), 1f to champagne.copy(alpha = 0.22f))
    } else {
        Brush.verticalGradient(0f to GraphiteRaised, 1f to Graphite)
    }
    val rim = if (outline != null) {
        SolidColor(outline)
    } else if (lens) {
        Brush.verticalGradient(0f to champagne.copy(alpha = 0.75f), 0.5f to champagne.copy(alpha = 0.30f), 1f to champagne.copy(alpha = 0.55f))
    } else {
        Brush.verticalGradient(0f to OffWhite.copy(alpha = 0.22f), 0.5f to champagne.copy(alpha = 0.12f), 1f to champagne.copy(alpha = 0.06f))
    }
    return this
        .clip(shape)
        .background(fill)
        .background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.07f), 0.4f to Color.White.copy(alpha = 0.01f), 1f to Color.Black.copy(alpha = 0.14f)))
        .border(if (outline != null) 1.5.dp else 1.dp, rim, shape)
}

/** Contorno de foco/error de los campos de cristal. */
@Composable
private fun fieldOutline(focused: Boolean, isError: Boolean): Color? = when {
    isError -> MaterialTheme.colorScheme.error
    focused -> MaterialTheme.colorScheme.primary
    else -> null
}

/**
 * Campo de texto de cristal. La etiqueta se ve como texto guía cuando está vacío y se anuncia siempre como
 * descripción del campo (también con valor). El error aparece debajo y marca el contorno.
 */
@Composable
internal fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    singleLine: Boolean = true,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    focusRequester: FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 2,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(capitalization = capitalization),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { focused = it.isFocused }
                .semantics {
                    contentDescription = label
                    if (error != null) error(error)
                },
            decorationBox = { inner ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .editorGlass(outline = fieldOutline(focused, error != null))
                        .heightIn(min = 56.dp)
                        .padding(horizontal = Spacing.l, vertical = Spacing.m),
                ) {
                    if (value.isEmpty()) {
                        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    inner()
                }
            },
        )
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = Spacing.l, top = Spacing.xs))
        }
    }
}

/** Botón secundario de cristal ahumado (añadir serie/ejercicio). */
@Composable
internal fun GlassButton(text: String, @DrawableRes iconRes: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = MinTouchTarget)
            .editorGlass()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.l, vertical = Spacing.s),
    ) {
        if (iconRes != null) {
            Icon(painterResource(iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Spacing.s))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Botón de icono de cristal, 48 dp tocables. Deshabilitado: atenuado y sin acción. */
@Composable
internal fun GlassIconButton(@DrawableRes iconRes: Int, description: String, onClick: () -> Unit, enabled: Boolean = true, size: Dp = MinTouchTarget) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.38f)
            .editorGlass()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
    }
}

/** Texto de acción sin contenedor («+ Añadir notas»): 48 dp tocables sobre el fondo. */
@Composable
internal fun PlainAction(text: String, @DrawableRes iconRes: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = MinTouchTarget)) {
        if (iconRes != null) {
            Icon(painterResource(iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.xs))
        }
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Título de sección sobre el fondo con su recuento a la derecha («Ejercicios · 2 ejercicios»). */
@Composable
internal fun OpenSectionTitle(
    title: String,
    count: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Text(title, style = style, modifier = Modifier.weight(1f).semantics { heading() })
        if (count != null) {
            Text(count, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Selector de tipo de serie: lente champán con el número (o «C» en calentamiento) y chevron. Abre «Trabajo» y
 * «Calentamiento (C)»; elegir el tipo actual no cambia nada. Solo alterna warmup: no toca orden ni valores.
 */
@Composable
internal fun SetTypeSelector(number: Int, warmup: Boolean, onSelect: (warmup: Boolean) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val type = stringResource(if (warmup) R.string.editor_warmup else R.string.editor_working_set)
    val description = stringResource(R.string.v1_set_type_description, number)
    Box(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .heightIn(min = MinTouchTarget)
                .fillMaxWidth()
                .editorGlass(lens = true)
                .clickable(onClick = { expanded = true })
                .semantics {
                    contentDescription = description
                    stateDescription = type
                    role = Role.DropdownList
                }
                .padding(horizontal = Spacing.s),
        ) {
            Text(
                if (warmup) stringResource(R.string.editor_warmup_short) else "$number",
                style = NumberStyleCell,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(Spacing.xs))
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp).rotate(90f),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = FitnessThemeExtras.colors.surfaceRaised,
            shape = MaterialTheme.shapes.medium,
        ) {
            listOf(false to R.string.v1_set_type_work, true to R.string.v1_set_type_warmup).forEach { (option, label) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label), style = MaterialTheme.typography.bodyLarge) },
                    trailingIcon = if (option == warmup) {
                        { Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        if (option != warmup) onSelect(option)
                    },
                    modifier = Modifier.heightIn(min = MinTouchTarget),
                )
            }
        }
    }
}

/** Descanso guardado en segundos y mostrado «m:ss» (120 → 2:00). Un borrador no válido se muestra tal cual, en error. */
internal fun restClock(text: String): String? = parseRestSeconds(text)?.let { restClockOf(it) }

/** m:ss sin horas (3600 → 60:00), para que quepa en la celda. */
internal fun restClockOf(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

/**
 * Celda de descanso: muestra m:ss y, al tocarla, abre el editor en segundos. El texto «2:00» nunca llega al
 * parser de segundos: el formulario sigue guardando los segundos tal cual.
 */
@Composable
internal fun RestCell(rest: String, description: String, isError: Boolean, onRest: (String) -> Unit, modifier: Modifier = Modifier) {
    var editing by rememberSaveable { mutableStateOf(false) }
    val shown = restClock(rest) ?: rest
    val action = stringResource(R.string.v1_rest_edit)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = MinTouchTarget)
            .editorGlass(outline = fieldOutline(focused = false, isError = isError))
            .clickable(role = Role.Button, onClickLabel = action, onClick = { editing = true })
            .semantics {
                contentDescription = "$description, $shown"
                if (isError) error(description)
            }
            .padding(horizontal = Spacing.s),
    ) {
        Text(shown, style = NumberStyleCell, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
    if (editing) {
        RestDialog(
            initial = rest,
            onDismiss = { editing = false },
            onConfirm = {
                editing = false
                onRest(it)
            },
        )
    }
}

/** Editor «Descanso en segundos» (0–3600, cero válido). Cancelar no cambia nada; el borrador sobrevive a la rotación. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    // El valor actual llega seleccionado: escribir lo sustituye.
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val draft = field.text
    val seconds = parseRestSeconds(draft)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = FitnessThemeExtras.colors.card) {
            Column(Modifier.padding(Spacing.xl)) {
                val title = stringResource(R.string.v1_rest_dialog_title)
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(Spacing.l))
                BasicTextField(
                    value = field,
                    onValueChange = { new -> field = new.copy(text = new.text.filter { it.isDigit() }.take(5)) },
                    singleLine = true,
                    textStyle = NumberStyleCell.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus)
                        .semantics {
                            contentDescription = title
                            if (seconds == null) error(title)
                        },
                    decorationBox = { inner ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .editorGlass(outline = if (seconds == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                .heightIn(min = MinTouchTarget)
                                .padding(horizontal = Spacing.s),
                        ) { inner() }
                    },
                )
                Spacer(Modifier.height(Spacing.s))
                Text(
                    if (seconds == null) stringResource(R.string.error_rest) else stringResource(R.string.v1_rest_preview, restClockOf(seconds)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (seconds == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.xl))
                PrimaryButton(
                    text = stringResource(R.string.v1_rest_confirm),
                    onClick = { onConfirm(draft) },
                    enabled = seconds != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.s))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget)) {
                    Text(stringResource(R.string.v1_cancel), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
