package com.maikol.fitnessapp.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

private val Scrim = Color.Black

/**
 * Fotografía local que ocupa todo el contenedor (recorte Crop) con protección de legibilidad:
 * degradado horizontal negro 85 % → transparente desde la izquierda y protección inferior opcional
 * (hasta 80 %). La foto se ajusta al tamaño del contenido (matchParentSize): no define la altura.
 * Es decorativa: TalkBack solo lee el texto real que va encima.
 */
@Composable
fun PhotoBackground(
    @DrawableRes photo: Int,
    modifier: Modifier = Modifier,
    photoAlignment: Alignment = Alignment.CenterEnd,
    /** Fracción del ancho donde el degradado horizontal llega a transparente. */
    horizontalScrimEnd: Float = 0.85f,
    /** Opacidad máxima de la protección inferior (0 = sin protección). */
    bottomScrimAlpha: Float = 0f,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = photoAlignment,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Scrim.copy(alpha = 0.85f),
                        horizontalScrimEnd * 0.55f to Scrim.copy(alpha = 0.7f),
                        horizontalScrimEnd to Color.Transparent,
                    ),
                ),
        )
        if (bottomScrimAlpha > 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.4f to Color.Transparent,
                            1f to Scrim.copy(alpha = bottomScrimAlpha),
                        ),
                    ),
            )
        }
        content()
    }
}
