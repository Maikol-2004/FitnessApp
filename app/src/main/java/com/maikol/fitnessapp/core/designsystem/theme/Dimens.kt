package com.maikol.fitnessapp.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Escala de espaciado 4/8/12/16/24/32 dp (especificación propuesta). */
object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    val screenHorizontal = l
    val cardGap = m
}

object FitnessRadius {
    val card = 20.dp
    val control = 12.dp
}

val MinTouchTarget = 48.dp

internal val FitnessShapes = Shapes(
    small = RoundedCornerShape(FitnessRadius.control),
    medium = RoundedCornerShape(FitnessRadius.control),
    large = RoundedCornerShape(FitnessRadius.card),
)
