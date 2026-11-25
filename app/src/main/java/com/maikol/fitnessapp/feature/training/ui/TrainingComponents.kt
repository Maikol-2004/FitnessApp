package com.maikol.fitnessapp.feature.training.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.component.PhotoBackground
import com.maikol.fitnessapp.core.designsystem.motion.LocalMotionEnabled
import com.maikol.fitnessapp.core.designsystem.motion.MotionTokens
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing

/** Cifras con dígitos tabulares: no bailan al cambiar (cronómetro, cargas, totales). */
internal val NumberStyleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 44.sp, lineHeight = 50.sp, fontFeatureSettings = "tnum")
internal val NumberStyleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp, fontFeatureSettings = "tnum")
internal val NumberStyleCell = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp, fontFeatureSettings = "tnum", textAlign = TextAlign.Center)
internal val OverlineStyle = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.8.sp)

/** Champán muy tenue para filas completadas y superficies seleccionadas (M06: 8 %). */
@Composable
internal fun champagneTint(alpha: Float = 0.08f): Color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)

/**
 * Cabecera fotográfica (gimnasio por defecto; cada módulo pasa su foto) con foto completa, degradado y título real). [onBack]/[actions] van sobre
 * la foto dentro de los insets de la barra de estado; [below] añade subtítulo o metadatos bajo el título.
 */
@Composable
internal fun TrainingHero(
    title: String,
    modifier: Modifier = Modifier,
    @DrawableRes photo: Int = R.drawable.training_gym_hero,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    minHeight: Dp = 200.dp,
    actions: @Composable RowScope.() -> Unit = {},
    below: @Composable () -> Unit = {},
) {
    PhotoBackground(
        photo = photo,
        photoAlignment = Alignment.Center,
        horizontalScrimEnd = 1f,
        bottomScrimAlpha = 0.92f,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = Spacing.xs, end = Spacing.xs, top = Spacing.xs, bottom = Spacing.l),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                if (onBack != null) BackButton(onBack)
                Spacer(Modifier.weight(1f))
                actions()
            }
            Spacer(Modifier.height(if (onBack != null) Spacing.xxl else Spacing.xxl + Spacing.xl))
            Column(Modifier.padding(horizontal = Spacing.m)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, lineHeight = 36.sp),
                    modifier = Modifier.semantics { heading() },
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f))
                }
                below()
            }
        }
    }
}

/** Barra superior sólida para pantallas de tarea (editor, sesión, resumen guardado). */
@Composable
internal fun TrainingTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        ) {
            BackButton(onBack)
            Column(Modifier.weight(1f).padding(horizontal = Spacing.xs)) {
                Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 2, modifier = Modifier.semantics { heading() })
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
            actions()
        }
    }
}

@Composable
internal fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = stringResource(R.string.back),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Protección fija bajo la barra de estado para pantallas cuyo contenido se desplaza por debajo. */
@Composable
internal fun BoxScope.StatusBarScrim() {
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f)),
    )
}

/** Título de sección 22/28 con acción opcional alineada a la derecha. */
@Composable
internal fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().heightIn(min = MinTouchTarget)) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
        trailing()
    }
}

/** Pequeña etiqueta: champán relleno (estado destacado) o contorno (estado informativo). */
@Composable
internal fun Pill(text: String, modifier: Modifier = Modifier, filled: Boolean = true, @DrawableRes iconRes: Int? = null) {
    val shape = RoundedCornerShape(50)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .then(
                if (filled) Modifier.background(MaterialTheme.colorScheme.primary, shape)
                else Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), shape),
            )
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        val color = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
        if (iconRes != null) {
            Icon(painterResource(iconRes), contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, lineHeight = 16.sp), color = color)
    }
}

/** Cifra destacada con su etiqueta (Duración, Volumen…). */
@Composable
internal fun StatTile(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Column(
        modifier = modifier
            .background(FitnessThemeExtras.colors.card, MaterialTheme.shapes.large)
            .padding(horizontal = Spacing.l, vertical = Spacing.m + 2.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(label.uppercase(), style = OverlineStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xs))
        Text(value, style = NumberStyleMedium, color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * Aro de progreso (M07): pista neutra y relleno champán que interpola desde el valor visual actual al nuevo
 * en 220 ms. Al entrar en la pantalla parte del valor real (sin repetir desde 0). Movimiento reducido: salta.
 */
@Composable
internal fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    stroke: Dp = 7.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val motion = LocalMotionEnabled.current
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = if (motion) tween(220, easing = MotionTokens.EaseOut) else tween(0),
        label = "sessionProgress",
    )
    val track = FitnessThemeExtras.colors.surfaceRaised
    val fill = MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            val w = stroke.toPx()
            val arcSize = Size(this.size.width - w, this.size.height - w)
            val topLeft = Offset(w / 2, w / 2)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(w))
            if (animated > 0f) drawArc(fill, -90f, 360f * animated, false, topLeft, arcSize, style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

/** Mensaje en línea con icono (error o aviso) que aparece con alpha 120 ms (M05) y se anuncia una vez. */
@Composable
internal fun InlineMessage(
    visible: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int = R.drawable.ic_error,
    color: Color = MaterialTheme.colorScheme.error,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(MotionTokens.FEEDBACK_MS)),
        exit = fadeOut(tween(MotionTokens.FEEDBACK_MS)),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            Icon(painterResource(iconRes), contentDescription = null, tint = color, modifier = Modifier.padding(top = 1.dp).size(18.dp))
            Spacer(Modifier.width(Spacing.s))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }
}

/** Barra inferior fija para la acción principal de la pantalla, por encima de la barra de navegación. */
@Composable
internal fun BottomActionBar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider(color = FitnessThemeExtras.colors.surfaceRaised)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.l, vertical = Spacing.m),
                content = content,
            )
        }
    }
}

/** Diálogo con superficie grafito y radio de tarjeta. Cancelar nunca cambia datos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FitnessDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = FitnessThemeExtras.colors.card) {
            Column(Modifier.padding(Spacing.xl)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(Spacing.m))
                Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.xl))
                // Acción principal arriba y a todo el ancho; la destructiva nunca va en champán.
                if (destructive) {
                    TextButton(
                        onClick = onConfirm,
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget + 8.dp),
                    ) {
                        Text(confirmLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    PrimaryButton(text = confirmLabel, onClick = onConfirm, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(Spacing.s))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget)) {
                    Text(dismissLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

/**
 * Celda numérica compacta (carga, reps, descanso): 48 dp de alto, cifra centrada tabular, fondo grafito
 * elevado y borde champán al enfocar o rojo si el valor no es válido. Teclado numérico/decimal.
 */
@Composable
internal fun NumberCell(
    value: String,
    onValueChange: (String) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
    isError: Boolean = false,
    placeholder: String = "",
    imeAction: ImeAction = ImeAction.Next,
    background: Color = FitnessThemeExtras.colors.surfaceRaised,
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        focused -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val shape = MaterialTheme.shapes.medium
    BasicTextField(
        value = value,
        onValueChange = { new -> onValueChange(new.filter { it.isDigit() || (decimal && (it == ',' || it == '.')) }.take(8)) },
        singleLine = true,
        textStyle = NumberStyleCell.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number, imeAction = imeAction),
        keyboardActions = KeyboardActions.Default,
        modifier = modifier
            .heightIn(min = MinTouchTarget)
            .onFocusChanged { focused = it.isFocused }
            .semantics {
                contentDescription = description
                if (isError) error(description)
            },
        decorationBox = { inner ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(background, shape)
                    .border(1.5.dp, borderColor, shape)
                    .heightIn(min = MinTouchTarget)
                    .padding(horizontal = Spacing.s),
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, style = NumberStyleCell, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
                inner()
            }
        },
    )
}
