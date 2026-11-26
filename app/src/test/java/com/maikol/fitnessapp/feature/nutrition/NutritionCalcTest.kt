package com.maikol.fitnessapp.feature.nutrition

import com.maikol.fitnessapp.core.database.FoodEntity
import com.maikol.fitnessapp.core.database.FoodSource
import com.maikol.fitnessapp.feature.nutrition.data.Basis
import com.maikol.fitnessapp.feature.nutrition.data.Coverage
import com.maikol.fitnessapp.feature.nutrition.data.DayPoint
import com.maikol.fitnessapp.feature.nutrition.data.GoalChange
import com.maikol.fitnessapp.feature.nutrition.data.GoalValues
import com.maikol.fitnessapp.feature.nutrition.data.Ingredient
import com.maikol.fitnessapp.feature.nutrition.data.MinuteLimiter
import com.maikol.fitnessapp.feature.nutrition.data.Nutrient
import com.maikol.fitnessapp.feature.nutrition.data.Nutrients
import com.maikol.fitnessapp.feature.nutrition.data.QtyUnit
import com.maikol.fitnessapp.feature.nutrition.data.Totals
import com.maikol.fitnessapp.feature.nutrition.data.goalDifference
import com.maikol.fitnessapp.feature.nutrition.data.goalFor
import com.maikol.fitnessapp.feature.nutrition.data.kjToKcal
import com.maikol.fitnessapp.feature.nutrition.data.matchesQuery
import com.maikol.fitnessapp.feature.nutrition.data.normalizeBarcode
import com.maikol.fitnessapp.feature.nutrition.data.nutrientsFor
import com.maikol.fitnessapp.feature.nutrition.data.recipeTotals
import com.maikol.fitnessapp.feature.nutrition.data.sumValues
import com.maikol.fitnessapp.feature.nutrition.data.trendSummary
import com.maikol.fitnessapp.feature.nutrition.data.validQuantity
import com.maikol.fitnessapp.feature.nutrition.ui.FoodField
import com.maikol.fitnessapp.feature.nutrition.ui.FoodForm
import com.maikol.fitnessapp.feature.nutrition.ui.FoodSearchViewModel
import com.maikol.fitnessapp.feature.nutrition.ui.FoodValidation
import com.maikol.fitnessapp.feature.nutrition.ui.LocalFilter
import com.maikol.fitnessapp.feature.nutrition.ui.SearchRow
import com.maikol.fitnessapp.feature.nutrition.ui.validateFood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * N1–N3: cálculos puros con los fixtures SINTÉTICOS de docs/nutrition/calculation-fixtures.json (solo
 * matemáticas: no son alimentos y no se distribuyen como datos).
 */
class NutritionCalcTest {
    private val g100 = Basis(100.0, QtyUnit.G)
    private val fixtureA = Nutrients(165.0, 10.0, 20.0, 5.0)
    private val fixtureB = Nutrients(250.0, 5.0, 35.0, 10.0)

    @Test
    fun scaling_fixtureA_150g() {
        val n = nutrientsFor(fixtureA, g100, 150.0, QtyUnit.G)!!
        assertEquals(247.5, n.kcal!!, 1e-9)
        assertEquals(15.0, n.proteinG!!, 1e-9)
        assertEquals(30.0, n.carbsG!!, 1e-9)
        assertEquals(7.5, n.fatG!!, 1e-9)
    }

    @Test
    fun serving_30g_roundTrip_andNoMassVolumeConversion() {
        val b = Basis(100.0, QtyUnit.G, servingAmount = 30.0, servingUnit = QtyUnit.G)
        assertEquals(listOf(QtyUnit.G, QtyUnit.SERVING), b.units())
        assertEquals(45.0, b.convert(1.5, QtyUnit.SERVING, QtyUnit.G)!!, 1e-9)
        assertEquals(1.5, b.convert(45.0, QtyUnit.G, QtyUnit.SERVING)!!, 1e-9)
        assertEquals(45.0 to QtyUnit.G, b.equivalence(1.5, QtyUnit.SERVING))
        // ml sin densidad no se convierte a g (ni al revés).
        val ml = Basis(100.0, QtyUnit.ML)
        assertNull(ml.factor(100.0, QtyUnit.G))
        assertEquals(listOf(QtyUnit.ML), ml.units())
        assertNull(Basis(100.0, QtyUnit.G, 250.0, QtyUnit.ML).factor(1.0, QtyUnit.SERVING))
        // Base «ración» con su masa: se puede registrar en gramos.
        val serving = Basis(1.0, QtyUnit.SERVING, 40.0, QtyUnit.G)
        assertEquals(2.0, serving.factor(80.0, QtyUnit.G)!!, 1e-9)
        // Una ración sin equivalencia nunca ofrece gramos.
        assertEquals(listOf(QtyUnit.SERVING), Basis(1.0, QtyUnit.SERVING).units())
    }

    @Test
    fun quantity_rejectsZeroNegativeNonFinite() {
        assertFalse(validQuantity(0.0, QtyUnit.G))
        assertFalse(validQuantity(-5.0, QtyUnit.G))
        assertFalse(validQuantity(Double.NaN, QtyUnit.G))
        assertFalse(validQuantity(Double.POSITIVE_INFINITY, QtyUnit.G))
        assertFalse(validQuantity(101.0, QtyUnit.SERVING))
        assertTrue(validQuantity(0.5, QtyUnit.SERVING))
    }

    @Test
    fun coverage_fixtures() {
        assertEquals(Coverage.UNKNOWN, sumValues(listOf(null, null)).coverage)
        assertNull(sumValues(listOf(null, null)).value)
        assertEquals(0.0, sumValues(listOf(0.0, 0.0)).value!!, 0.0)
        assertEquals(Coverage.KNOWN, sumValues(listOf(0.0, 0.0)).coverage)
        assertEquals(0.0, sumValues(listOf(0.0, null)).value!!, 0.0)
        assertEquals(Coverage.PARTIAL, sumValues(listOf(0.0, null)).coverage)
        assertEquals(15.0, sumValues(listOf(10.0, null, 5.0)).value!!, 0.0)
        assertEquals(Coverage.PARTIAL, sumValues(listOf(10.0, null, 5.0)).coverage)
        assertEquals(Coverage.EMPTY, sumValues(emptyList()).coverage)
        assertNull(sumValues(emptyList()).value)
    }

    @Test
    fun kcalWithoutMacros_isPartialPerNutrient_notZero() {
        val day = Totals.sum(listOf(Totals.of(Nutrients(300.0, null, null, null)), Totals.of(Nutrients(200.0, 10.0, null, null))))
        assertEquals(500.0, day.kcal.value!!, 0.0)
        assertEquals(Coverage.KNOWN, day.kcal.coverage)
        assertEquals(Coverage.PARTIAL, day.protein.coverage)
        assertEquals(Coverage.UNKNOWN, day.carbs.coverage)
        // Un parcial heredado (receta) se propaga al día.
        val withRecipe = Totals.sum(listOf(day, Totals.of(Nutrients(100.0, 1.0, 1.0, 1.0), partialMask = Nutrient.KCAL.bit)))
        assertEquals(Coverage.PARTIAL, withRecipe.kcal.coverage)
    }

    @Test
    fun recipe_fixture_total_perServing_per100g() {
        val t = recipeTotals(listOf(Ingredient(fixtureA, g100, 100.0, QtyUnit.G), Ingredient(fixtureB, g100, 80.0, QtyUnit.G)), 2.0, 200.0)
        assertEquals(365.0, t.total.kcal.value!!, 1e-9)
        assertEquals(14.0, t.total.protein.value!!, 1e-9)
        assertEquals(48.0, t.total.carbs.value!!, 1e-9)
        assertEquals(13.0, t.total.fat.value!!, 1e-9)
        assertEquals(182.5, t.perServing.kcal.value!!, 1e-9)
        assertEquals(7.0, t.perServing.protein.value!!, 1e-9)
        assertEquals(24.0, t.perServing.carbs.value!!, 1e-9)
        assertEquals(6.5, t.perServing.fat.value!!, 1e-9)
        assertEquals(182.5, t.per100g!!.kcal.value!!, 1e-9)
        assertEquals(100.0, t.servingMassG!!, 1e-9)
    }

    @Test
    fun recipe_withoutFinalWeight_hasNoGrams_andUnknownPropagates() {
        val t = recipeTotals(listOf(Ingredient(fixtureA, g100, 100.0, QtyUnit.G), Ingredient(Nutrients(50.0, null, 2.0, 1.0), g100, 100.0, QtyUnit.G)), 2.0, null)
        assertNull(t.per100g)
        assertNull(t.servingMassG)
        assertEquals(Coverage.PARTIAL, t.total.protein.coverage)
        assertEquals(10.0, t.total.protein.value!!, 1e-9)
        assertEquals(Coverage.KNOWN, t.total.kcal.coverage)
        // Como base registrable: solo raciones.
        assertEquals(listOf(QtyUnit.SERVING), Basis(1.0, QtyUnit.SERVING, t.servingMassG, t.servingMassG?.let { QtyUnit.G }).units())
    }

    @Test
    fun trend_fixture_twoOfSeven_mean1500() {
        val start = LocalDate.of(2026, 11, 16)
        val kcal = listOf(1000.0, null, 2000.0, null, null, null, null)
        val days = kcal.mapIndexed { i, v -> DayPoint(start.plusDays(i.toLong()), if (v == null) Totals.Empty else Totals.of(Nutrients(v, null, null, null))) }
        val s = trendSummary(days)
        assertEquals(7, s.periodDays)
        assertEquals(2, s.daysWithRecords)
        assertEquals(2, s.recordedDays[Nutrient.KCAL])
        assertEquals(1500.0, s.meanOfRecordedDays[Nutrient.KCAL]!!, 1e-9)
        // Proteínas desconocidas: sin media (no 0).
        assertNull(s.meanOfRecordedDays[Nutrient.PROTEIN])
        assertEquals(5, days.count { !it.hasRecords })
    }

    @Test
    fun goals_noBackfill_latestChangeApplies() {
        val h = listOf(
            GoalChange(LocalDate.of(2026, 11, 20), 1, GoalValues(2000, null, null, null)),
            GoalChange(LocalDate.of(2026, 11, 25), 2, GoalValues(2200, 120, null, null)),
            GoalChange(LocalDate.of(2026, 11, 25), 3, GoalValues(2100, 120, null, null)),
        )
        assertNull(goalFor(LocalDate.of(2026, 11, 19), h))
        assertEquals(2000, goalFor(LocalDate.of(2026, 11, 24), h)!!.values.kcal)
        assertEquals(2100, goalFor(LocalDate.of(2026, 11, 27), h)!!.values.kcal)
        assertEquals(300.0, goalDifference(1800.0, 2100)!!, 0.0)
        assertEquals(-150.0, goalDifference(2250.0, 2100)!!, 0.0)
        assertNull(goalDifference(null, 2100))
        assertNull(goalDifference(1800.0, null))
        assertEquals(1598.0, GoalValues(2100, 150, 200, 22).macroKcal()!!, 1e-9)
    }

    @Test
    fun energy_kj_conversion() {
        assertEquals(100.0, kjToKcal(418.4), 1e-9)
    }

    @Test
    fun barcode_keepsLeadingZeros_rejectsOthers() {
        assertEquals("0012345678905", normalizeBarcode("0012345678905"))
        assertEquals("04963406", normalizeBarcode(" 0496 3406 "))
        assertNull(normalizeBarcode("12345"))
        assertNull(normalizeBarcode("12345678901a"))
    }

    @Test
    fun minuteLimiter_respectsWindow() {
        var now = 0L
        val l = MinuteLimiter(2) { now }
        assertNull(l.tryAcquire())
        assertNull(l.tryAcquire())
        val wait = l.tryAcquire()
        assertNotNull(wait)
        assertEquals(60, wait)
        now = 60_000
        assertNull(l.tryAcquire())
    }

    @Test
    fun localSearch_accentInsensitive_reproducibleOrder_filters() {
        fun food(id: String, name: String, brand: String? = null, fav: Boolean = false, used: Long? = null, source: String = FoodSource.MANUAL) = FoodEntity(
            id, 1, name, brand, "UNKNOWN", source, if (source == FoodSource.MANUAL) null else id, null, null, null, 100.0, "g", false,
            100.0, null, null, null, false, null, null, null, null, fav, false, null, null, used, 0, 0,
        )
        val foods = listOf(
            food("1", "Plátano prueba", used = 5),
            food("2", "Pan de prueba", brand = "Marca Prueba", fav = true),
            food("3", "Platano frito prueba", source = FoodSource.OFF, used = 9),
        )
        val found = FoodSearchViewModel.localRows(foods, emptyList(), "platano", LocalFilter.RECENT).map { (it as SearchRow.Food).food.id }
        assertEquals(listOf("3", "1"), found)
        assertEquals(setOf("1", "3"), found.toSet())
        assertTrue(matchesQuery("marca", "Pan", "Marca Prueba"))
        assertEquals(listOf("3", "1"), FoodSearchViewModel.localRows(foods, emptyList(), "", LocalFilter.RECENT).map { (it as SearchRow.Food).food.id })
        assertEquals(listOf("2"), FoodSearchViewModel.localRows(foods, emptyList(), "", LocalFilter.FAVORITES).map { (it as SearchRow.Food).food.id })
        assertEquals(listOf("2", "1"), FoodSearchViewModel.localRows(foods, emptyList(), "", LocalFilter.MINE).map { (it as SearchRow.Food).food.id })
    }

    @Test
    fun foodForm_needsNameAndOneNutrient_zeroIsData_nullIsUnknown() {
        val noName = validateFood(FoodForm(kcal = "100")) as FoodValidation.Invalid
        assertTrue(FoodField.NAME in noName.errors)
        val noValues = validateFood(FoodForm(name = "Prueba")) as FoodValidation.Invalid
        assertTrue(noValues.noValues)
        val ok = validateFood(FoodForm(name = " Prueba ", kcal = "0", protein = "12,5")) as FoodValidation.Valid
        assertEquals(0.0, ok.draft.nutrients.kcal!!, 0.0)
        assertEquals(12.5, ok.draft.nutrients.proteinG!!, 0.0)
        assertNull(ok.draft.nutrients.carbsG)
        assertEquals(Basis(100.0, QtyUnit.G), ok.draft.basis)
        val kj = validateFood(FoodForm(name = "Prueba", energyKj = true, kcal = "418,4")) as FoodValidation.Valid
        assertEquals(100.0, kj.draft.nutrients.kcal!!, 1e-9)
        val serving = validateFood(FoodForm(name = "Prueba", kcal = "50", servingAmount = "30")) as FoodValidation.Valid
        assertEquals(Basis(100.0, QtyUnit.G, 30.0, QtyUnit.G), serving.draft.basis)
        val bad = validateFood(FoodForm(name = "Prueba", kcal = "-1", basisAmount = "0", barcode = "123")) as FoodValidation.Invalid
        assertEquals(setOf(FoodField.KCAL, FoodField.BASIS, FoodField.CODE), bad.errors)
    }
}
