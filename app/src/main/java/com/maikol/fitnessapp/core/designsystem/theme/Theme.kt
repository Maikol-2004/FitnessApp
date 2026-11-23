package com.maikol.fitnessapp.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Tema único funcional: Titanium Champagne oscuro, sin color dinámico.
 * Las siete paletas futuras siguen pendientes y no se definen aquí.
 */
@Composable
fun FitnessTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalFitnessExtendedColors provides TitaniumChampagneExtendedColors) {
        MaterialTheme(
            colorScheme = TitaniumChampagneColorScheme,
            typography = FitnessTypography,
            shapes = FitnessShapes,
            content = content,
        )
    }
}

object FitnessThemeExtras {
    val colors: FitnessExtendedColors
        @Composable @ReadOnlyComposable get() = LocalFitnessExtendedColors.current
}
