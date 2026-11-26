package com.maikol.fitnessapp.feature.training.data

import com.maikol.fitnessapp.core.preferences.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** H2-02 (decimales regionales y no válidos), H2-05 (volumen) y H2-14 (kg/lb sin deriva). */
class TrainingFormatTest {

    @Test
    fun parseDecimal_acceptsCommaAndDot_rejectsGarbage() {
        assertEquals(Parsed.Value(22.5), parseDecimal("22,5"))
        assertEquals(Parsed.Value(22.5), parseDecimal("22.5"))
        assertEquals(Parsed.Value(20.0), parseDecimal("20,"))
        assertEquals(Parsed.Empty, parseDecimal("  "))
        listOf("-5", "1e3", "NaN", "abc", "2,5,5", "1.2345").forEach {
            assertEquals("«$it»", Parsed.Invalid, parseDecimal(it))
        }
    }

    @Test
    fun repsAndRest_ranges() {
        assertEquals(10, parseReps("10"))
        assertNull(parseReps("0"))
        assertNull(parseReps("1000"))
        assertNull(parseReps(""))
        assertEquals(0, parseRestSeconds(""))
        assertEquals(3_600, parseRestSeconds("3600"))
        assertNull(parseRestSeconds("3601"))
    }

    @Test
    fun loadAboveMaximum_isInvalid() {
        assertEquals(Parsed.Invalid, LoadText.of(null, UnitSystem.METRIC).edited("2010").resolveKg(UnitSystem.METRIC))
    }

    @Test
    fun unchangedText_keepsExactKilograms_inPounds() {
        val shown = LoadText.of(20.0, UnitSystem.IMPERIAL)
        assertEquals("44,1", shown.text)
        assertEquals(Parsed.Value(20.0), shown.resolveKg(UnitSystem.IMPERIAL))
        // Ida y vuelta kg → lb → kg sin tocar el campo: sigue siendo exactamente 20.
        val back = LoadText.of((shown.resolveKg(UnitSystem.IMPERIAL) as Parsed.Value).value, UnitSystem.METRIC)
        assertEquals("20", back.text)
    }

    /** G2.1: 0 registrado (p. ej. asistencia nula en la dominada asistida) se muestra y se conserva; null sigue vacío. */
    @Test
    fun zeroLoad_isShownAndKept_nullStaysEmpty() {
        val zero = LoadText.of(0.0, UnitSystem.METRIC)
        assertEquals("0", zero.text)
        assertEquals(Parsed.Value(0.0), zero.resolveKg(UnitSystem.METRIC))
        assertEquals(Parsed.Empty, LoadText.of(null, UnitSystem.METRIC).resolveKg(UnitSystem.METRIC))
    }

    @Test
    fun editedPounds_convertToKilograms() {
        val kg = (LoadText.of(null, UnitSystem.IMPERIAL).edited("100").resolveKg(UnitSystem.IMPERIAL) as Parsed.Value).value
        assertEquals(45.359237, kg, 1e-9)
    }

    @Test
    fun volume_400_then_360_whenOneSetDropsTo8Reps() {
        val sets = listOf(SetValues(10, 20.0, false, true), SetValues(10, 20.0, false, true))
        assertEquals(400.0, volumeKg(sets), 0.0)
        assertEquals(360.0, volumeKg(listOf(sets[0], SetValues(8, 20.0, false, true))), 0.0)
    }

    @Test
    fun volume_excludesWarmupPendingAndBodyweight() {
        val sets = listOf(
            SetValues(10, 20.0, warmup = true, completed = true),
            SetValues(10, 20.0, warmup = false, completed = false),
            SetValues(10, null, warmup = false, completed = true),
            SetValues(5, 50.0, warmup = false, completed = true),
        )
        assertEquals(250.0, volumeKg(sets), 0.0)
    }

    /** Ejemplo de aceptación: press 40 × 10 + dominada asistida 30 × 8 = 400 kg; la asistencia (también 0) no suma. */
    @Test
    fun volume_mixedSession_assistanceIsNotExternalLoad() {
        val sets = listOf(
            SetValues(10, 40.0, warmup = false, completed = true),
            SetValues(10, 20.0, warmup = true, completed = true),
            SetValues(10, 40.0, warmup = false, completed = false),
            SetValues(8, 30.0, warmup = false, completed = true, external = false),
            SetValues(8, 0.0, warmup = false, completed = true, external = false),
        )
        assertEquals(400.0, volumeKg(sets), 0.0)
    }

    @Test
    fun formats_useSpanishConventions() {
        assertEquals("22,5 kg", formatLoad(22.5, UnitSystem.METRIC))
        assertEquals("2.400 kg", formatVolume(2_400.0, UnitSystem.METRIC))
        assertEquals("794 lb", formatVolume(360.0, UnitSystem.IMPERIAL))
        assertEquals("1:30", formatCountdown(89_001))
        assertEquals("1 h 05 min", formatDuration(3_900_000))
    }
}
