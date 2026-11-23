package com.maikol.fitnessapp.core.designsystem.motion

import android.animation.ValueAnimator
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** Curvas y duraciones de docs/08_MOTION.md. Las duraciones las escala el sistema (incluido 0). */
object MotionTokens {
    /** Salida suave: entradas, pulsación, apariciones. */
    val EaseOut: Easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

    /** Desplazamiento entre estados. */
    val EaseInOut: Easing = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)

    const val PRESS_MS = 120
    const val RELEASE_MS = 150
    const val PRESSED_SCALE = 0.98f

    const val SCREEN_ENTER_MS = 220
    const val SCREEN_EXIT_MS = 140
    const val HOME_FIRST_ENTER_MS = 180
    const val ROOT_CROSSFADE_MS = 150

    /** Errores y confirmaciones (M05): 100–150 ms. */
    const val FEEDBACK_MS = 120

    /** Alpha reducida permitida con movimiento reducido: <= 100 ms. */
    const val REDUCED_FADE_MS = 90
}

/**
 * true cuando se permiten traslación, escala y bucles: el usuario no ha activado «Reducir animaciones»
 * en Perfil y la escala de animación del sistema no es 0.
 */
val LocalMotionEnabled = staticCompositionLocalOf { true }

/**
 * Estado de las animaciones del sistema (escala de duración 0 = desactivadas).
 * Se vuelve a leer al reanudar porque el usuario puede cambiarla en Ajustes con la app en segundo plano.
 */
@Composable
fun rememberSystemAnimationsEnabled(): Boolean {
    var enabled by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { enabled = ValueAnimator.areAnimatorsEnabled() }
    return enabled
}

/**
 * M01: escala visual 1 → 0.98 (120 ms) al pulsar y vuelta a 1 (150 ms), sin rebote.
 * Solo graphicsLayer: el área táctil y el layout no cambian. Con movimiento reducido no hace nada
 * (queda la indicación nativa de pulsación).
 */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource, motionEnabled: Boolean): Modifier {
    if (!motionEnabled) return this
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) MotionTokens.PRESSED_SCALE else 1f,
        animationSpec = tween(
            durationMillis = if (pressed) MotionTokens.PRESS_MS else MotionTokens.RELEASE_MS,
            easing = MotionTokens.EaseOut,
        ),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
