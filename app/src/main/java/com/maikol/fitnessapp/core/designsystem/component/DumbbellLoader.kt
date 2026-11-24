package com.maikol.fitnessapp.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.motion.LocalMotionEnabled
import com.maikol.fitnessapp.core.designsystem.motion.MotionTokens
import com.maikol.fitnessapp.core.designsystem.theme.FitnessTheme
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformLatest

/** Fase visual de una espera real (M04). No representa porcentaje de progreso. */
enum class LoadingPhase { HIDDEN, VISIBLE, SLOW }

const val LOADER_SHOW_AFTER_MS = 200L
const val LOADER_SLOW_AFTER_MS = 10_000L

/**
 * Convierte un flujo «cargando sí/no» en la fase del indicador: nada si la operación termina antes de
 * [showAfterMs]; visible después; aviso de lentitud a [slowAfterMs]. Al dejar de cargar vuelve a HIDDEN
 * de inmediato (sin esperar a terminar ningún ciclo de animación).
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<Boolean>.toLoadingPhase(
    showAfterMs: Long = LOADER_SHOW_AFTER_MS,
    slowAfterMs: Long = LOADER_SLOW_AFTER_MS,
): Flow<LoadingPhase> = transformLatest { loading ->
    emit(LoadingPhase.HIDDEN)
    if (loading) {
        delay(showAfterMs)
        emit(LoadingPhase.VISIBLE)
        delay(slowAfterMs - showAfterMs)
        emit(LoadingPhase.SLOW)
    }
}.distinctUntilChanged()

/**
 * Región de contenido con carga real: muestra [content] cuando [isLoading] es false; durante la espera
 * no muestra nada los primeros 200 ms y después la mancuerna con [message].
 */
@Composable
fun LoadingRegion(
    isLoading: Boolean,
    message: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val phase by remember { snapshotFlow { isLoading }.toLoadingPhase() }
        .collectAsState(initial = LoadingPhase.HIDDEN)
    if (!isLoading) {
        // El contenido ocupa el mismo espacio que el indicador (p. ej. weight en una Column).
        Box(modifier = modifier) { content() }
    } else {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (phase != LoadingPhase.HIDDEN) {
                DumbbellLoadingIndicator(message = message, slow = phase == LoadingPhase.SLOW)
            }
        }
    }
}

/** Mancuerna + texto contextual. Se anuncia una vez como región dinámica, no por cada disco o ciclo. */
@Composable
fun DumbbellLoadingIndicator(
    message: String,
    modifier: Modifier = Modifier,
    slow: Boolean = false,
) {
    val slowText = stringResource(R.string.loading_slow)
    val description = if (slow) "$message. $slowText" else message
    Column(
        modifier = modifier
            .padding(Spacing.xl)
            .clearAndSetSemantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        DumbbellAssembly(animate = LocalMotionEnabled.current)
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (slow) {
            Text(
                slowText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val CYCLE_MS = 1_200

/**
 * Canvas 96×64 dp: barra 60×5 dp y cuatro discos champán. Ciclo de ~1.200 ms (M04). Con [animate] false
 * se dibuja montada y estática, sin bucle. El tiempo solo se lee dentro del dibujo.
 */
@Composable
fun DumbbellAssembly(animate: Boolean, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val time: State<Float>? = if (animate) {
        rememberInfiniteTransition(label = "dumbbell").animateFloat(
            initialValue = 0f,
            targetValue = CYCLE_MS.toFloat(),
            animationSpec = infiniteRepeatable(tween(CYCLE_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "dumbbellTime",
        )
    } else {
        null
    }
    Canvas(modifier = modifier.size(96.dp, 64.dp)) {
        drawDumbbell(t = time?.value, color = color)
    }
}

private fun DrawScope.drawDumbbell(t: Float?, color: Color) {
    // Fundido final común a todo el conjunto (950–1.100 ms).
    val groupAlpha = if (t == null) 1f else 1f - segment(t, 950f, 150f)
    if (groupAlpha <= 0f) return
    val cx = size.width / 2f
    val cy = size.height / 2f

    val barAlpha = if (t == null) 1f else segment(t, 0f, 150f)
    val barW = 60.dp.toPx()
    val barH = 5.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - barW / 2f, cy - barH / 2f),
        size = Size(barW, barH),
        cornerRadius = CornerRadius(barH / 2f),
        alpha = barAlpha * groupAlpha,
    )

    // Discos interiores (más grandes) y exteriores; entran desde fuera con salida suave.
    disc(t, startMs = 150f, side = -1, centerOffset = 18.dp, travel = 12.dp, w = 8.dp, h = 34.dp, color, groupAlpha, cx, cy)
    disc(t, startMs = 250f, side = 1, centerOffset = 18.dp, travel = 12.dp, w = 8.dp, h = 34.dp, color, groupAlpha, cx, cy)
    disc(t, startMs = 350f, side = -1, centerOffset = 27.dp, travel = 16.dp, w = 6.dp, h = 24.dp, color, groupAlpha, cx, cy)
    disc(t, startMs = 450f, side = 1, centerOffset = 27.dp, travel = 16.dp, w = 6.dp, h = 24.dp, color, groupAlpha, cx, cy)
}

@Suppress("LongParameterList")
private fun DrawScope.disc(
    t: Float?,
    startMs: Float,
    side: Int,
    centerOffset: Dp,
    travel: Dp,
    w: Dp,
    h: Dp,
    color: Color,
    groupAlpha: Float,
    cx: Float,
    cy: Float,
) {
    val p = if (t == null) 1f else MotionTokens.EaseOut.transform(segment(t, startMs, 200f))
    if (p <= 0f) return
    val x = cx + side * (centerOffset.toPx() + (1f - p) * travel.toPx())
    val wPx = w.toPx()
    val hPx = h.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(x - wPx / 2f, cy - hPx / 2f),
        size = Size(wPx, hPx),
        cornerRadius = CornerRadius(wPx / 2f),
        alpha = p * groupAlpha,
    )
}

/** Progreso 0..1 de un tramo [start, start+duration] del ciclo. */
private fun segment(t: Float, start: Float, duration: Float): Float =
    ((t - start) / duration).coerceIn(0f, 1f)

@Preview(name = "Mancuerna · carga", showBackground = true, backgroundColor = 0xFF121416)
@Composable
private fun DumbbellLoadingPreview() {
    FitnessTheme { DumbbellLoadingIndicator(message = "Cargando tus datos") }
}

@Preview(name = "Mancuerna · lenta y estática", showBackground = true, backgroundColor = 0xFF121416)
@Composable
private fun DumbbellSlowStaticPreview() {
    FitnessTheme {
        androidx.compose.runtime.CompositionLocalProvider(LocalMotionEnabled provides false) {
            DumbbellLoadingIndicator(message = "Preparando tu entrenamiento", slow = true)
        }
    }
}
