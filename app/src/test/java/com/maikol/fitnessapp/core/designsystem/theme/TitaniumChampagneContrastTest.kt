package com.maikol.fitnessapp.core.designsystem.theme

import com.maikol.fitnessapp.core.designsystem.theme.TitaniumChampagneTokens as T
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Verifica contraste WCAG 2.x de las combinaciones de tokens usadas realmente
 * (docs/02_DESIGN_UX.md: texto normal >= 4.5:1, límites funcionales/iconos >= 3:1).
 */
class TitaniumChampagneContrastTest {

    @Test
    fun textOnBackgroundAndCards_meetsNormalTextContrast() {
        for (bg in listOf(T.BACKGROUND, T.SURFACE, T.SURFACE_RAISED)) {
            assertAtLeast(4.5, T.ON_SURFACE, bg, "onSurface")
            assertAtLeast(4.5, T.ON_SURFACE_VARIANT, bg, "onSurfaceVariant")
        }
    }

    @Test
    fun onPrimaryOnChampagne_meetsNormalTextContrast() {
        assertAtLeast(4.5, T.ON_PRIMARY, T.PRIMARY, "onPrimary/primary")
    }

    @Test
    fun statusAndAccentColors_meetNormalTextContrastOnCards() {
        for (bg in listOf(T.BACKGROUND, T.SURFACE)) {
            assertAtLeast(4.5, T.PRIMARY, bg, "primary")
            assertAtLeast(4.5, T.ERROR, bg, "error")
            assertAtLeast(4.5, T.SUCCESS, bg, "success")
        }
    }

    @Test
    fun outline_meetsNonTextContrastOnBackgroundAndCards() {
        assertAtLeast(3.0, T.OUTLINE, T.BACKGROUND, "outline/background")
        assertAtLeast(3.0, T.OUTLINE, T.SURFACE, "outline/surface")
    }

    @Test
    fun contrastFormula_matchesKnownReference() {
        // Negro sobre blanco = 21:1 (referencia WCAG).
        assertTrue(kotlin.math.abs(contrast(0xFF000000, 0xFFFFFFFF) - 21.0) < 0.01)
    }

    private fun assertAtLeast(minimum: Double, fg: Long, bg: Long, label: String) {
        val ratio = contrast(fg, bg)
        assertTrue(
            "$label sobre ${bg.toString(16)}: %.2f < %.1f".format(ratio, minimum),
            ratio >= minimum,
        )
    }

    private fun contrast(a: Long, b: Long): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun luminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
