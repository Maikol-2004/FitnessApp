package com.maikol.fitnessapp.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private fun token(argb: Long) = Color(argb)

internal val TitaniumChampagneColorScheme: ColorScheme = darkColorScheme(
    primary = token(TitaniumChampagneTokens.PRIMARY),
    onPrimary = token(TitaniumChampagneTokens.ON_PRIMARY),
    primaryContainer = token(TitaniumChampagneTokens.SURFACE_RAISED),
    onPrimaryContainer = token(TitaniumChampagneTokens.PRIMARY),
    // Selección (p. ej. botones segmentados) en champán; la selección también lleva icono.
    secondary = token(TitaniumChampagneTokens.PRIMARY),
    onSecondary = token(TitaniumChampagneTokens.ON_PRIMARY),
    secondaryContainer = token(TitaniumChampagneTokens.PRIMARY),
    onSecondaryContainer = token(TitaniumChampagneTokens.ON_PRIMARY),
    tertiary = token(TitaniumChampagneTokens.SUCCESS),
    onTertiary = token(TitaniumChampagneTokens.ON_PRIMARY),
    background = token(TitaniumChampagneTokens.BACKGROUND),
    onBackground = token(TitaniumChampagneTokens.ON_SURFACE),
    surface = token(TitaniumChampagneTokens.BACKGROUND),
    onSurface = token(TitaniumChampagneTokens.ON_SURFACE),
    surfaceVariant = token(TitaniumChampagneTokens.SURFACE_RAISED),
    onSurfaceVariant = token(TitaniumChampagneTokens.ON_SURFACE_VARIANT),
    surfaceContainerLowest = token(TitaniumChampagneTokens.BACKGROUND),
    surfaceContainerLow = token(TitaniumChampagneTokens.SURFACE),
    surfaceContainer = token(TitaniumChampagneTokens.SURFACE),
    surfaceContainerHigh = token(TitaniumChampagneTokens.SURFACE_RAISED),
    surfaceContainerHighest = token(TitaniumChampagneTokens.SURFACE_RAISED),
    outline = token(TitaniumChampagneTokens.OUTLINE),
    outlineVariant = token(TitaniumChampagneTokens.SURFACE_RAISED),
    error = token(TitaniumChampagneTokens.ERROR),
    onError = token(TitaniumChampagneTokens.ON_PRIMARY),
)

/** Colores sin rol equivalente en Material 3. */
@Immutable
data class FitnessExtendedColors(
    val card: Color,
    val surfaceRaised: Color,
    val success: Color,
)

internal val TitaniumChampagneExtendedColors = FitnessExtendedColors(
    card = token(TitaniumChampagneTokens.SURFACE),
    surfaceRaised = token(TitaniumChampagneTokens.SURFACE_RAISED),
    success = token(TitaniumChampagneTokens.SUCCESS),
)

internal val LocalFitnessExtendedColors = staticCompositionLocalOf { TitaniumChampagneExtendedColors }
