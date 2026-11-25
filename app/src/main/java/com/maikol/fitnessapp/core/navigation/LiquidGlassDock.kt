package com.maikol.fitnessapp.core.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.maikol.fitnessapp.core.designsystem.motion.LocalMotionEnabled
import com.maikol.fitnessapp.core.designsystem.motion.MotionTokens
import com.maikol.fitnessapp.core.designsystem.theme.TitaniumChampagneTokens

/*
 * Menú Liquid Glass elegido (docs/11_LIQUID_GLASS.md, design/liquid-glass/tokens.json): cápsula de cristal
 * ahumado flotante con lente champán que sigue al destino activo. Material de bajo coste sin blur del fondo
 * (fallback documentado): grafito translúcido, reflejo superior y borde champán. Iconos y etiquetas se dibujan
 * siempre nítidos encima.
 */

private val DockShape = RoundedCornerShape(32.dp)
private val LensShape = RoundedCornerShape(24.dp)
private val DockBodyHeight = 84.dp
private val DockSideMargin = 16.dp
private val DockBottomMargin = 10.dp
private val LensInsetVertical = 6.dp
private val LensInsetHorizontal = 5.dp
private const val SELECTION_MS = 240
private const val COLOR_MS = 150
private const val LIFT_MS = 160

private val Graphite = Color(TitaniumChampagneTokens.SURFACE)
private val Champagne = Color(TitaniumChampagneTokens.PRIMARY)
private val OffWhite = Color(TitaniumChampagneTokens.ON_SURFACE)
private val WarmGrey = Color(TitaniumChampagneTokens.ON_SURFACE_VARIANT)

private const val MaxLabelScale = 1.15f

private val LabelStyle = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp)

/**
 * Espacio que el dock ocupa sobre el contenido (cuerpo + márgenes, sin la barra de gestos). Las raíces lo
 * suman al final de su scroll con [dockBottomPadding]: el contenido pasa por detrás del cristal y la última
 * tarjeta o acción siempre queda por encima.
 */
val LocalDockInset = compositionLocalOf { 0.dp }

/** Relleno inferior para el final del contenido desplazable de una pantalla con dock. */
@Composable
fun dockBottomPadding(): Dp {
    val dock = LocalDockInset.current
    return if (dock == 0.dp) 0.dp else dock + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
}

internal val DockTotalInset: Dp = DockBodyHeight + DockBottomMargin + 12.dp

@Composable
fun LiquidGlassDock(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotionEnabled.current
    val destinations = TopLevelDestination.entries
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = DockSideMargin, end = DockSideMargin, bottom = DockBottomMargin)
            .shadow(28.dp, DockShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(DockShape)
            // Cristal ahumado: grafito al 74 % sobre lo que se desplaza detrás, con reflejo superior.
            .background(Graphite.copy(alpha = 0.74f))
            .background(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.10f),
                    0.35f to Color.White.copy(alpha = 0.02f),
                    1f to Color.Black.copy(alpha = 0.18f),
                ),
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    0f to OffWhite.copy(alpha = 0.30f),
                    0.5f to Champagne.copy(alpha = 0.16f),
                    1f to Champagne.copy(alpha = 0.08f),
                ),
                DockShape,
            ),
    ) {
        val index = destinations.indexOf(selected)
        // La lente parte de su posición visual actual (retarget) y no bloquea el cambio de destino.
        val lensPosition by animateFloatAsState(
            targetValue = index.toFloat(),
            animationSpec = if (motion) tween(SELECTION_MS, easing = MotionTokens.EaseOut) else snap(),
            label = "dockLens",
        )
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
        ) {
            // La lente ocupa una celda y toma la altura de la fila de destinos.
            Box(
                Modifier
                    .matchParentSize()
                    .layout { measurable, constraints ->
                        val cell = constraints.maxWidth / destinations.size
                        val placeable = measurable.measure(Constraints.fixed(cell, constraints.maxHeight))
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.place((cell * lensPosition).roundToInt(), 0)
                        }
                    },
            ) {
                Lens(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = LensInsetHorizontal, vertical = LensInsetVertical),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = DockBodyHeight)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                destinations.forEach { destination ->
                    DockCell(
                        destination = destination,
                        selected = destination == selected,
                        motion = motion,
                        onClick = { onSelect(destination) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Lens(modifier: Modifier) {
    Box(
        modifier
            .clip(LensShape)
            .background(
                Brush.verticalGradient(
                    0f to Champagne.copy(alpha = 0.26f),
                    0.6f to Champagne.copy(alpha = 0.12f),
                    1f to Champagne.copy(alpha = 0.20f),
                ),
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    0f to Champagne.copy(alpha = 0.75f),
                    0.5f to Champagne.copy(alpha = 0.30f),
                    1f to Champagne.copy(alpha = 0.65f),
                ),
                LensShape,
            ),
    )
}

@Composable
private fun DockCell(
    destination: TopLevelDestination,
    selected: Boolean,
    motion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorSpec = if (motion) tween<Color>(COLOR_MS) else snap()
    val tint by animateColorAsState(if (selected) Champagne else WarmGrey, colorSpec, label = "dockTint")
    val labelColor by animateColorAsState(if (selected) Champagne else OffWhite.copy(alpha = 0.86f), colorSpec, label = "dockLabel")
    val lift by animateDpAsState(
        targetValue = if (selected && motion) (-2).dp else 0.dp,
        animationSpec = if (motion) tween(LIFT_MS, easing = MotionTokens.EaseOut) else snap(),
        label = "dockLift",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = DockBodyHeight)
            .clip(LensShape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                indication = ripple(bounded = true, color = Champagne),
                interactionSource = null,
            )
            .padding(vertical = 10.dp),
    ) {
        Icon(
            painterResource(destination.iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .offset { IntOffset(0, lift.roundToPx()) }
                .size(24.dp),
        )
        Spacer(Modifier.height(5.dp))
        // Etiqueta siempre visible y del mismo tamaño en las cinco celdas: 13 sp que crecen con la fuente
        // del sistema hasta 1,15× (como NavigationBar); por encima se mantiene para no cortar «Progreso».
        val fontScale = LocalDensity.current.fontScale
        val labelSize = (LabelStyle.fontSize.value * minOf(1f, MaxLabelScale / fontScale)).sp
        Text(
            stringResource(destination.labelRes),
            style = LabelStyle.copy(fontSize = labelSize, lineHeight = labelSize * 1.25f),
            color = labelColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.offset { IntOffset(0, lift.roundToPx()) },
        )
    }
}
