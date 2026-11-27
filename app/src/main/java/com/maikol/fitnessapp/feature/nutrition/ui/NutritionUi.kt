package com.maikol.fitnessapp.feature.nutrition.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.motion.LocalMotionEnabled
import com.maikol.fitnessapp.core.designsystem.motion.MotionTokens
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.navigation.dockBottomPadding
import com.maikol.fitnessapp.core.tracking.formatInput
import com.maikol.fitnessapp.core.tracking.formatInteger
import com.maikol.fitnessapp.feature.nutrition.data.Coverage
import com.maikol.fitnessapp.feature.nutrition.data.MealGroup
import com.maikol.fitnessapp.feature.nutrition.data.Nutrient
import com.maikol.fitnessapp.feature.nutrition.data.NutrientSum
import com.maikol.fitnessapp.feature.nutrition.data.Preparation
import com.maikol.fitnessapp.feature.nutrition.data.QtyUnit
import com.maikol.fitnessapp.feature.nutrition.data.SOURCE_RECIPE
import com.maikol.fitnessapp.core.database.FoodSource
import com.maikol.fitnessapp.feature.training.data.AppLocale
import com.maikol.fitnessapp.feature.training.ui.editorGlass
import com.maikol.fitnessapp.feature.nutrition.data.roundKcal
import java.text.NumberFormat

/*
 * Nutrición abierta (docs/nutrition/01_UX_AND_DESIGN.md): contenido sobre el fondo, cristal solo en controles.
 * Reutiliza el material editorGlass del editor V1; sin tarjetas por comida ni desenfoque por fila.
 */

/**
 * Oferta de deshacer un borrado. Mientras se restaura no se muestra nada; si restaurar falla, se ofrece reintentar
 * con lo borrado aún guardado. Descartar el aviso renuncia a deshacer.
 */
@Composable
fun UndoSnackbar(host: SnackbarHostState, pending: Boolean, restoring: Boolean, failed: Boolean, onUndo: () -> Unit, onDismiss: () -> Unit) {
    val text = stringResource(if (failed) R.string.n_undo_failed else R.string.n_deleted)
    val action = stringResource(if (failed) R.string.n_retry else R.string.n_undo)
    LaunchedEffect(pending, restoring, failed) {
        if (pending && !restoring) {
            val r = host.showSnackbar(text, actionLabel = action, duration = SnackbarDuration.Long)
            if (r == SnackbarResult.ActionPerformed) onUndo() else onDismiss()
        }
    }
}

// ---- Formatos ----

private val Kcal0 = NumberFormat.getIntegerInstance(AppLocale)
private val G1 = NumberFormat.getNumberInstance(AppLocale).apply { maximumFractionDigits = 1; minimumFractionDigits = 0 }

fun kcalText(v: Double): String = "${Kcal0.format(roundKcal(v))} kcal"
fun gramsText(v: Double): String = "${G1.format(v)} g"
fun kcalNumber(v: Double): String = Kcal0.format(roundKcal(v))
fun gramsNumber(v: Double): String = G1.format(v)

/** Valor de un nutriente con unidad o «—» si es desconocido. */
fun nutrientText(n: Nutrient, v: Double?): String = when {
    v == null -> "—"
    n == Nutrient.KCAL -> kcalText(v)
    else -> gramsText(v)
}

@Composable
fun quantityText(q: Double, unit: QtyUnit): String = when (unit) {
    QtyUnit.G -> "${formatInput(q)} g"
    QtyUnit.ML -> "${formatInput(q)} ml"
    QtyUnit.SERVING -> pluralStringResource(R.plurals.n_servings, if (q == 1.0) 1 else 2, formatInput(q))
}

@Composable
fun unitLabel(unit: QtyUnit): String = when (unit) {
    QtyUnit.G -> "g"
    QtyUnit.ML -> "ml"
    QtyUnit.SERVING -> stringResource(R.string.n_unit_serving)
}

@StringRes
fun MealGroup.label(): Int = when (this) {
    MealGroup.BREAKFAST -> R.string.n_group_breakfast
    MealGroup.LUNCH -> R.string.n_group_lunch
    MealGroup.SNACK -> R.string.n_group_snack
    MealGroup.DINNER -> R.string.n_group_dinner
    MealGroup.UNSPECIFIED -> R.string.n_group_unspecified
}

@DrawableRes
fun MealGroup.icon(): Int = when (this) {
    MealGroup.BREAKFAST -> R.drawable.ic_meal_breakfast
    MealGroup.LUNCH -> R.drawable.ic_meal_lunch
    MealGroup.SNACK -> R.drawable.ic_meal_snack
    MealGroup.DINNER -> R.drawable.ic_restaurant
    MealGroup.UNSPECIFIED -> R.drawable.ic_inbox
}

@StringRes
fun Preparation.label(): Int = when (this) {
    Preparation.UNKNOWN -> R.string.n_prep_unknown
    Preparation.RAW -> R.string.n_prep_raw
    Preparation.COOKED -> R.string.n_prep_cooked
    Preparation.READY -> R.string.n_prep_ready
}

@Composable
fun sourceLabel(source: String): String = stringResource(
    when (source) {
        FoodSource.OFF -> R.string.n_source_off
        SOURCE_RECIPE -> R.string.n_source_recipe
        else -> R.string.n_source_manual
    },
)

@Composable
fun basisLabel(amount: Double, unit: QtyUnit): String = when (unit) {
    QtyUnit.SERVING -> stringResource(R.string.n_per_serving)
    else -> stringResource(R.string.n_per_amount, "${formatInput(amount)} ${unit.code}")
}

/** Grupo propuesto para un registro NUEVO según la hora (editable y visible; nunca se aplica a lo antiguo). */
fun suggestedGroup(hour: Int): MealGroup = when (hour) {
    in 5..10 -> MealGroup.BREAKFAST
    in 11..15 -> MealGroup.LUNCH
    in 16..18 -> MealGroup.SNACK
    else -> MealGroup.DINNER
}

// ---- Estructura ----

/**
 * Pantalla de Nutrición: cabecera compacta (atrás, título y acción), contenido desplazable y, si hay,
 * una barra de acción fija que sube con el teclado sin tapar la última fila.
 */
@Composable
fun NutritionPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: (@Composable ColumnScope.() -> Unit)? = null,
    withDock: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().imePadding()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(bottom = Spacing.xl + if (withDock && bottomBar == null) dockBottomPadding() else 0.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = Spacing.s)) {
                IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back)) }
                Spacer(Modifier.weight(1f))
                actions()
            }
            Column {
                Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
        if (bottomBar != null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.m)
                    .padding(bottom = if (withDock) dockBottomPadding() else 0.dp)
                    .navigationBarsPadding(),
                content = bottomBar,
            )
        }
    }
}

/** Pestañas/segmentos de cristal con lente champán en el seleccionado; rol de pestaña y estado anunciados. */
@Composable
fun GlassSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().editorGlass(RoundedCornerShape(24.dp)).padding(4.dp)) {
        options.forEachIndexed { i, label ->
            val isSelected = i == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MinTouchTarget - 4.dp)
                    .then(if (isSelected) Modifier.editorGlass(RoundedCornerShape(20.dp), lens = true) else Modifier)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(i) })
                    .padding(horizontal = Spacing.s, vertical = Spacing.xs),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    // Una línea que se reduce solo si no cabe: con fuente grande no se parte una palabra («Tendenci/as»).
                    maxLines = 1,
                    softWrap = false,
                    autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = MaterialTheme.typography.labelLarge.fontSize),
                )
            }
        }
    }
}

/** Chips de elección de cristal (grupo, unidad, base). La seleccionada lleva lente champán y check. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> GlassChoices(options: List<T>, selected: T?, label: @Composable (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s), modifier = modifier) {
        options.forEach { option ->
            val isSelected = option == selected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .heightIn(min = MinTouchTarget)
                    .editorGlass(RoundedCornerShape(24.dp), lens = isSelected)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option) })
                    .padding(horizontal = Spacing.l),
            ) {
                if (isSelected) {
                    Icon(painterResource(R.drawable.ic_check), null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(Spacing.xs))
                }
                Text(label(option), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/**
 * Campo decimal de cristal: etiqueta persistente encima, coma o punto, todo el valor seleccionado al enfocar,
 * unidad pronunciable y error junto al campo.
 */
@Composable
fun GlassNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    error: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    integer: Boolean = false,
    /** Cifras máximas: 9 para cantidades; 14 para códigos de barras (GTIN). */
    maxLength: Int = 9,
) {
    var focused by remember { mutableStateOf(false) }
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    val shown = if (field.text == value) field else TextFieldValue(value, TextRange(value.length))
    LaunchedEffect(focused) {
        if (focused) {
            withFrameNanos { }
            field = TextFieldValue(value, TextRange(0, value.length))
        }
    }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = shown,
            onValueChange = { new ->
                val text = new.text.filter { it.isDigit() || (!integer && (it == ',' || it == '.')) }.take(maxLength)
                field = if (text == new.text) new else TextFieldValue(text, TextRange(text.length))
                if (text != value) onValueChange(text)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal, imeAction = imeAction),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .semantics {
                    contentDescription = if (suffix != null) "$label, $suffix" else label
                    if (error != null) error(error)
                },
            decorationBox = { inner ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .editorGlass(outline = when {
                            error != null -> MaterialTheme.colorScheme.error
                            focused -> MaterialTheme.colorScheme.primary
                            else -> null
                        })
                        .heightIn(min = 56.dp)
                        .padding(horizontal = Spacing.l),
                ) {
                    Box(Modifier.weight(1f)) { inner() }
                    if (suffix != null) {
                        Text(suffix, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
        )
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = Spacing.s, top = 2.dp))
        }
    }
}

/** Aro de energía: solo con objetivo y valor; limitado visualmente a 100 %, el texto conserva el exceso. */
@Composable
fun KcalRing(fraction: Float, center: String, modifier: Modifier = Modifier) {
    val motion = LocalMotionEnabled.current
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = if (motion) tween(MotionTokens.FEEDBACK_MS * 2, easing = MotionTokens.EaseOut) else snap(),
        label = "kcalRing",
    )
    val track = FitnessThemeExtras.colors.surfaceRaised
    val fill = MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(112.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, topLeft = Offset(inset, inset), size = arc, style = Stroke(stroke))
            if (animated > 0f) drawArc(fill, -90f, 360f * animated, false, topLeft = Offset(inset, inset), size = arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(center, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Barra fina de progreso de un macro (sin barra si no hay objetivo). */
@Composable
fun ThinBar(fraction: Float?, modifier: Modifier = Modifier) {
    if (fraction == null) return
    val track = FitnessThemeExtras.colors.surfaceRaised
    val fill = MaterialTheme.colorScheme.primary
    Canvas(modifier.fillMaxWidth().height(6.dp)) {
        val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = r)
        val f = fraction.coerceIn(0f, 1f)
        if (f > 0f) drawRoundRect(fill, size = size.copy(width = size.width * f), cornerRadius = r)
    }
}

@StringRes
fun Nutrient.label(): Int = when (this) {
    Nutrient.KCAL -> R.string.n_energy
    Nutrient.PROTEIN -> R.string.n_protein
    Nutrient.CARBS -> R.string.n_carbs
    Nutrient.FAT -> R.string.n_fat
}

/** Valor con su cobertura: «Parcial» rotulado en texto, nunca solo color. */
@Composable
fun coverageNote(sum: NutrientSum): String? = when (sum.coverage) {
    Coverage.PARTIAL -> stringResource(R.string.n_partial)
    else -> null
}

/** Tres macros en columnas (o apiladas con fuente grande): nombre, gramos conocidos, objetivo y barra. */
@Composable
fun MacroRow(values: Map<Nutrient, NutrientSum>, goals: Map<Nutrient, Int?>, modifier: Modifier = Modifier, stacked: Boolean = false) {
    val macros = listOf(Nutrient.PROTEIN, Nutrient.CARBS, Nutrient.FAT)
    @Composable
    fun Cell(n: Nutrient, m: Modifier) {
        val sum = values[n] ?: NutrientSum.Empty
        val goal = goals[n]
        val value = sum.value?.let(::gramsNumber) ?: "—"
        val text = if (goal != null) "$value / ${formatInteger(goal)} g" else if (sum.value != null) "$value g" else value
        Column(m.semantics(mergeDescendants = true) {}) {
            Text(stringResource(n.label()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.titleMedium)
            coverageNote(sum)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.height(4.dp))
            ThinBar(if (goal != null && goal > 0 && sum.value != null) (sum.value / goal).toFloat() else null)
        }
    }
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.m), modifier = modifier.fillMaxWidth()) { macros.forEach { Cell(it, Modifier.fillMaxWidth()) } }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.l), modifier = modifier.fillMaxWidth()) { macros.forEach { Cell(it, Modifier.weight(1f)) } }
    }
}

/** Línea de acción sin contenedor con icono (Ver diario, Configurar objetivo…). */
@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, @DrawableRes iconRes: Int? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.xs),
    ) {
        if (iconRes != null) {
            Icon(painterResource(iconRes), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.xs))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}
