package com.maikol.fitnessapp.core.tracking

import com.maikol.fitnessapp.core.database.MealEntryEntity
import com.maikol.fitnessapp.core.database.SleepEntryEntity
import com.maikol.fitnessapp.core.database.WaterEntryEntity
import com.maikol.fitnessapp.core.preferences.Goal
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.feature.health.ActivityField
import com.maikol.fitnessapp.feature.health.ActivityForm
import com.maikol.fitnessapp.feature.health.ActivityValidation
import com.maikol.fitnessapp.feature.health.validateActivity
import com.maikol.fitnessapp.feature.nutrition.MealField
import com.maikol.fitnessapp.feature.nutrition.MealForm
import com.maikol.fitnessapp.feature.nutrition.MealValidation
import com.maikol.fitnessapp.feature.nutrition.validateMeal
import com.maikol.fitnessapp.feature.sleep.SleepError
import com.maikol.fitnessapp.feature.sleep.SleepForm
import com.maikol.fitnessapp.feature.sleep.SleepValidation
import com.maikol.fitnessapp.feature.sleep.validateSleep
import com.maikol.fitnessapp.feature.training.data.HistoryEntry
import com.maikol.fitnessapp.feature.training.data.Parsed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** F1: cálculos y validaciones puras de los registros manuales (datos «Prueba»). */
class TrackingCalcTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    private fun water(id: String, ml: Int) = WaterEntryEntity(id, ml, 0, "UTC", "2026-11-25", 0)

    private fun meal(kcal: Double?, protein: Double?, carbs: Double? = null) =
        MealEntryEntity("m$kcal$protein", "Prueba", 0, "UTC", "2026-11-25", kcal, protein, carbs, null, 0, 0)

    @Test
    fun water_add_edit_delete() {
        val entries = listOf(water("a", 250), water("b", 500))
        assertEquals(750, waterTotalMl(entries))
        val edited = listOf(water("a", 150), water("b", 500))
        assertEquals(650, waterTotalMl(edited))
        assertEquals(150, waterTotalMl(edited.filterNot { it.id == "b" }))
    }

    @Test
    fun nutrition_sumsKnown_andFlagsOnlyPartialFields() {
        val totals = nutritionTotals(listOf(meal(450.0, 30.0), meal(700.0, null)))
        assertEquals(1_150.0, totals.kcal!!, 0.0)
        assertFalse(totals.kcalIncomplete)
        assertEquals(30.0, totals.proteinG!!, 0.0)
        assertTrue(totals.proteinIncomplete)
        assertNull(totals.carbsG)
        assertFalse("sin ningún valor no es «incompleto», es «—»", totals.carbsIncomplete)
        assertTrue(totals.anyIncomplete)
    }

    @Test
    fun goalFraction_clampsAndNeedsGoal() {
        assertEquals(0.5f, goalFraction(4_000.0, 8_000.0)!!, 0f)
        assertEquals(1f, goalFraction(12_000.0, 8_000.0)!!, 0f)
        assertNull(goalFraction(4_000.0, null))
        assertNull(goalFraction(null, 8_000.0))
        assertNull(goalFraction(1.0, 0.0))
    }

    @Test
    fun distance_kmMi_noDrift() {
        val shown = DistanceText.of(4_200.0, UnitSystem.IMPERIAL)
        assertEquals("2,61", shown.text)
        assertEquals(Parsed.Value(4_200.0), shown.resolveMeters(UnitSystem.IMPERIAL))
        val back = DistanceText.of(4_200.0, UnitSystem.METRIC)
        assertEquals("4,2", back.text)
        val edited = shown.copy(text = "1").resolveMeters(UnitSystem.IMPERIAL) as Parsed.Value
        assertEquals(METERS_PER_MILE, edited.value, 1e-9)
        assertEquals(Parsed.Invalid, back.copy(text = "1001").resolveMeters(UnitSystem.METRIC))
    }

    @Test
    fun sleep_acrossMidnight_andDstChange() {
        val normal = SleepForm.new(LocalDate.of(2026, 11, 25))
        val valid = validateSleep(normal, madrid) as SleepValidation.Valid
        assertEquals(8L * 3_600_000, sleepDurationMs(valid.values.startAt, valid.values.endAt))
        // 25-oct-2026: en Madrid el reloj retrocede de 03:00 a 02:00 → 23:00 a 07:00 son 9 h reales.
        val dst = normal.copy(startDate = LocalDate.of(2026, 10, 24), endDate = LocalDate.of(2026, 10, 25))
        val dstValid = validateSleep(dst, madrid) as SleepValidation.Valid
        assertEquals(9L * 3_600_000, sleepDurationMs(dstValid.values.startAt, dstValid.values.endAt))
    }

    @Test
    fun sleep_rejectsReversedAndTooLong() {
        val base = SleepForm.new(LocalDate.of(2026, 11, 25))
        assertEquals(
            SleepValidation.Invalid(SleepError.END_NOT_AFTER_START),
            validateSleep(base.copy(endDate = base.startDate, endTime = LocalTime.of(22, 0)), madrid),
        )
        assertEquals(
            SleepValidation.Invalid(SleepError.TOO_LONG),
            validateSleep(base.copy(endDate = base.endDate.plusDays(1)), madrid),
        )
    }

    @Test
    fun sleepWeekTotal_onlyLastSevenWakeDays() {
        val h8 = 8L * 3_600_000
        fun night(wake: String) = SleepEntryEntity(wake, 0, h8, "UTC", wake, null, null, 0, 0)
        val today = LocalDate.of(2026, 11, 25)
        val entries = listOf(night("2026-11-25"), night("2026-11-19"), night("2026-11-18"))
        assertEquals(2 * h8, sleepWeekTotalMs(entries, today))
    }

    @Test
    fun weekTraining_excludesOlderThanSevenDays() {
        val today = LocalDate.of(2026, 11, 25)
        fun at(date: LocalDate) = date.atTime(12, 0).atZone(madrid).toInstant().toEpochMilli()
        val history = listOf(
            HistoryEntry("a", "Prueba", at(today), 0, 2, 1, 400.0),
            HistoryEntry("b", "Prueba", at(today.minusDays(6)), 0, 2, 1, 100.0),
            HistoryEntry("c", "Prueba", at(today.minusDays(7)), 0, 2, 1, 999.0),
        )
        assertEquals(WeekTraining(2, 500.0), weekTraining(history, today, madrid))
    }

    @Test
    fun weekTraining_boundaries_inZone_excludeFuture() {
        val today = LocalDate.of(2026, 11, 25)
        fun at(date: LocalDate, h: Int, m: Int = 0) = date.atTime(h, m).atZone(madrid).toInstant().toEpochMilli()
        fun entry(id: String, endedAt: Long) = HistoryEntry(id, "Prueba", endedAt, 0, 1, 1, 100.0)
        val history = listOf(
            entry("inicio", at(today.minusDays(6), 0, 0)), // primer instante del periodo: cuenta
            entry("antes", at(today.minusDays(6), 0, 0) - 1), // un ms antes: fuera
            entry("fin", at(today, 23, 59)), // hoy al final del día: cuenta
            entry("manana", at(today.plusDays(1), 0, 0)), // mañana 00:00: fuera
            entry("futuro", at(today.plusDays(3), 12)), // fecha posterior (reloj adelantado): fuera
        )
        assertEquals(WeekTraining(2, 200.0), weekTraining(history, today, madrid))
        // La misma sesión a las 23:30 de Madrid ya es «mañana» en Tokio: la zona decide el día.
        val late = listOf(entry("tarde", at(today, 23, 30)))
        assertEquals(1, weekTraining(late, today, madrid).sessions)
        assertEquals(0, weekTraining(late, today, ZoneId.of("Asia/Tokyo")).sessions)
    }

    @Test
    fun visibleDay_followsToday_keepsChosenPast_andPinnedForm() {
        val today = LocalDate.of(2026, 11, 26)
        assertEquals(today, visibleDay(today, chosen = null, pinned = null))
        assertEquals(today.minusDays(2), visibleDay(today, chosen = today.minusDays(2), pinned = null))
        // Elegir «Hoy» se guarda como null: al cambiar de día sigue al nuevo hoy.
        assertEquals(today, visibleDay(today, chosen = today, pinned = null))
        assertEquals(today.minusDays(1), visibleDay(today, chosen = null, pinned = today.minusDays(1)))
    }

    @Test
    fun activity_emptyIsUnknown_zeroIsValue() {
        val empty = validateActivity(ActivityForm(), UnitSystem.METRIC) as ActivityValidation.Invalid
        assertTrue(empty.form.noValues)
        val zero = validateActivity(ActivityForm(kcal = "0"), UnitSystem.METRIC) as ActivityValidation.Valid
        assertEquals(0.0, zero.values.kcal!!, 0.0)
        assertNull(zero.values.steps)
        val bad = validateActivity(ActivityForm(steps = "-3"), UnitSystem.METRIC) as ActivityValidation.Invalid
        assertEquals(setOf(ActivityField.STEPS), bad.form.errors)
    }

    @Test
    fun meal_needsNameAndOneValue() {
        val noName = validateMeal(MealForm(time = LocalTime.NOON, kcal = "300")) as MealValidation.Invalid
        assertTrue(MealField.NAME in noName.form.errors)
        val noValues = validateMeal(MealForm(name = "Prueba", time = LocalTime.NOON)) as MealValidation.Invalid
        assertTrue(noValues.form.noValues)
        val ok = validateMeal(MealForm(name = " Prueba ", time = LocalTime.NOON, protein = "0")) as MealValidation.Valid
        assertEquals("Prueba", ok.values.name)
        assertNull(ok.values.kcal)
        assertEquals(0.0, ok.values.protein!!, 0.0)
    }

    @Test
    fun goals_ranges() {
        assertTrue(Goal.STEPS.isValid(8_000))
        assertFalse(Goal.WATER.isValid(100))
        assertTrue(Goal.WATER.isValid(2_000))
        assertFalse(Goal.KCAL.isValid(10_001))
    }
}
