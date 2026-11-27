package com.maikol.fitnessapp.feature.nutrition

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maikol.fitnessapp.core.database.FitnessDatabase
import com.maikol.fitnessapp.core.database.MealMode
import com.maikol.fitnessapp.core.preferences.Goal
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import com.maikol.fitnessapp.feature.nutrition.data.Basis
import com.maikol.fitnessapp.feature.nutrition.data.CopyRequest
import com.maikol.fitnessapp.feature.nutrition.data.DeletedItem
import com.maikol.fitnessapp.feature.nutrition.ui.UndoSlot
import com.maikol.fitnessapp.feature.nutrition.data.FoodDraft
import com.maikol.fitnessapp.feature.nutrition.data.GoalValues
import com.maikol.fitnessapp.feature.nutrition.data.IngredientDraft
import com.maikol.fitnessapp.feature.nutrition.data.MealGroup
import com.maikol.fitnessapp.feature.nutrition.data.Nutrients
import com.maikol.fitnessapp.feature.nutrition.data.NutritionRepository
import com.maikol.fitnessapp.feature.nutrition.data.Preparation
import com.maikol.fitnessapp.feature.nutrition.data.QtyUnit
import com.maikol.fitnessapp.feature.nutrition.data.RecipeDraft
import com.maikol.fitnessapp.feature.nutrition.data.RemoteProduct
import com.maikol.fitnessapp.feature.nutrition.data.goalFor
import com.maikol.fitnessapp.feature.nutrition.data.portionable
import com.maikol.fitnessapp.feature.nutrition.data.totals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * N1–N3 sobre Room en memoria con datos «Prueba» sintéticos (no son alimentos reales): idempotencia, snapshots,
 * deshacer, copiar, mover, recetas, caché del proveedor y objetivos con historial. No toca la base de la app.
 */
@RunWith(AndroidJUnit4::class)
class NutritionRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: FitnessDatabase
    private lateinit var repo: NutritionRepository
    private var clock = 1_000L
    private val day = LocalDate.of(2026, 11, 26)
    private val noon = LocalTime.of(12, 0)
    private val a = FoodDraft("Prueba A", null, Preparation.UNKNOWN, Basis(100.0, QtyUnit.G), null, Nutrients(165.0, 10.0, 20.0, 5.0), null)
    private val b = FoodDraft("Prueba B", "Marca Prueba", Preparation.RAW, Basis(100.0, QtyUnit.G, 30.0, QtyUnit.G), "1 ración", Nutrients(250.0, 5.0, 35.0, 10.0), "0012345678905")

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, FitnessDatabase::class.java).build()
        repo = NutritionRepository(db, now = { clock++ }, zone = { ZoneOffset.UTC })
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun logPortion_isIdempotent_snapshotSurvivesFoodEdit() = runBlocking {
        val foodId = repo.saveFood("fa", a)
        val food = repo.getFood(foodId)!!
        repo.logPortion("i1", day, MealGroup.LUNCH, noon, food.portionable(), 150.0, QtyUnit.G)
        repo.logPortion("i1", day, MealGroup.LUNCH, noon, food.portionable(), 150.0, QtyUnit.G) // doble toque
        val entries = repo.observeDay(day).first()
        assertEquals(1, entries.size)
        assertEquals(MealMode.FOOD_ITEMS, entries.single().meal.mode)
        assertEquals(1, entries.single().items.size)
        assertEquals(247.5, entries.totals().kcal.value!!, 1e-9)
        // Último uso solo tras guardar.
        val used = repo.getFood(foodId)!!
        assertEquals(150.0, used.lastQuantity!!, 0.0)
        assertNotNull(used.lastUsedAt)

        // Editar el alimento crea versión nueva; el consumo pasado conserva su snapshot.
        repo.saveFood(foodId, a.copy(nutrients = Nutrients(200.0, 10.0, 20.0, 5.0)))
        assertEquals(2, repo.getFood(foodId)!!.version)
        val item = repo.observeDay(day).first().single().items.single().entity
        assertEquals(1, item.foodVersion)
        assertEquals(247.5, item.kcal!!, 1e-9)
        // Cambiar la cantidad recalcula desde el snapshot, no desde la definición actual.
        repo.updateItemQuantity("i1", 100.0, QtyUnit.G)
        assertEquals(165.0, repo.observeDay(day).first().totals().kcal.value!!, 1e-9)
    }

    @Test
    fun deleteUndo_restoresSameRows_withoutDuplicates() = runBlocking {
        val food = repo.getFood(repo.saveFood("fa", a))!!.portionable()
        repo.logPortion("i1", day, MealGroup.DINNER, noon, food, 100.0, QtyUnit.G)
        val mealId = repo.observeDay(day).first().single().id
        val deleted = repo.deleteItem("i1")!!
        assertTrue("el contenedor vacío desaparece", repo.observeDay(day).first().isEmpty())
        repo.restore(deleted)
        repo.restore(deleted)
        val after = repo.observeDay(day).first()
        assertEquals(listOf(mealId), after.map { it.id })
        assertEquals(listOf("i1"), after.single().items.map { it.entity.id })
    }

    @Test
    fun copy_toOtherDay_derivedIds_retryDoesNotDuplicate_sourceUnchanged() = runBlocking {
        val food = repo.getFood(repo.saveFood("fb", b))!!.portionable()
        repo.logPortion("i1", day, MealGroup.BREAKFAST, LocalTime.of(8, 0), food, 1.5, QtyUnit.SERVING)
        repo.saveTotalsEntry("q1", "Prueba rápida", day, LocalTime.of(10, 0), MealGroup.SNACK, Nutrients(120.0, null, null, null))
        val ids = repo.observeDay(day).first().map { it.id }
        val target = day.plusDays(1)
        val req = CopyRequest("op-1", ids, emptyList(), target, MealGroup.LUNCH, noon)
        repo.copy(req)
        repo.copy(req) // reintento
        val copied = repo.observeDay(target).first()
        assertEquals(2, copied.size)
        assertTrue(copied.all { it.group == MealGroup.LUNCH })
        assertEquals(setOf(MealMode.FOOD_ITEMS, MealMode.QUICK_TOTALS), copied.map { it.meal.mode }.toSet())
        assertEquals(1, copied.single { it.isFoodItems }.items.size)
        // 1,5 raciones de 30 g = 45 g → 112,5 kcal; más 120 de la rápida.
        assertEquals(232.5, copied.totals().kcal.value!!, 1e-9)
        assertTrue(copied.flatMap { it.items }.none { it.entity.id == "i1" })
        // Origen intacto.
        val source = repo.observeDay(day).first()
        assertEquals(ids, source.map { it.id })
        assertEquals(setOf(MealGroup.BREAKFAST, MealGroup.SNACK), source.map { it.group }.toSet())
    }

    @Test
    fun moveItem_andMergeGroups() = runBlocking {
        val food = repo.getFood(repo.saveFood("fa", a))!!.portionable()
        repo.logPortion("i1", day, MealGroup.SNACK, noon, food, 100.0, QtyUnit.G)
        repo.logPortion("i2", day, MealGroup.DINNER, noon, food, 50.0, QtyUnit.G)
        repo.moveItem("i1", MealGroup.DINNER)
        val entries = repo.observeDay(day).first()
        assertEquals(1, entries.size)
        assertEquals(MealGroup.DINNER, entries.single().group)
        assertEquals(listOf("i2", "i1"), entries.single().items.sortedBy { it.entity.position }.map { it.entity.id })
    }

    @Test
    fun quickEntry_cannotBecomeFoodItems_andUnknownStaysUnknown() = runBlocking {
        repo.saveTotalsEntry("q1", "Prueba", day, noon, MealGroup.UNSPECIFIED, Nutrients(300.0, null, null, null))
        val t = repo.observeDay(day).first().totals()
        assertEquals(300.0, t.kcal.value!!, 0.0)
        assertNull(t.protein.value)
        val food = repo.getFood(repo.saveFood("fa", a))!!.portionable()
        repo.logPortion("i1", day, MealGroup.UNSPECIFIED, noon, food, 100.0, QtyUnit.G)
        val entries = repo.observeDay(day).first()
        assertEquals(2, entries.size)
        assertEquals("q1", entries.first { !it.isFoodItems }.id)
        assertEquals(465.0, entries.totals().kcal.value!!, 1e-9)
    }

    @Test
    fun recipe_versionedSnapshot_loggedAsServings() = runBlocking {
        val fa = repo.getFood(repo.saveFood("fa", a))!!.portionable()
        val fb = repo.getFood(repo.saveFood("fb", b))!!.portionable()
        val draft = RecipeDraft("Prueba receta", 2.0, 200.0, "", listOf(IngredientDraft("g1", fa, 100.0, QtyUnit.G), IngredientDraft("g2", fb, 80.0, QtyUnit.G)))
        repo.saveRecipe("r1", draft)
        val recipe = repo.getRecipe("r1")!!
        assertEquals(1, recipe.recipe.version)
        val p = recipe.portionable()
        assertEquals(Basis(1.0, QtyUnit.SERVING, 100.0, QtyUnit.G), p.basis)
        assertEquals(182.5, p.perBasis.kcal!!, 1e-9)
        repo.logPortion("i1", day, MealGroup.DINNER, noon, p, 1.0, QtyUnit.SERVING)
        assertEquals(182.5, repo.observeDay(day).first().totals().kcal.value!!, 1e-9)
        // Editar el ingrediente o la receta no reescribe el diario.
        repo.saveFood("fa", a.copy(nutrients = Nutrients(999.0, 1.0, 1.0, 1.0)))
        repo.saveRecipe("r1", draft.copy(servings = 4.0))
        assertEquals(2, repo.getRecipe("r1")!!.recipe.version)
        assertEquals(165.0, repo.getRecipe("r1")!!.ingredients.first { it.id == "g1" }.basisKcal!!, 0.0)
        val item = repo.observeDay(day).first().single().items.single().entity
        assertEquals(1, item.recipeVersion)
        assertEquals(182.5, item.kcal!!, 1e-9)
    }

    @Test
    fun providerCache_keepsUserFields_editCreatesPrivateCopy() = runBlocking {
        val p = RemoteProduct(
            "0012345678905", "Prueba producto", "Marca Prueba", 100.0, QtyUnit.ML, true, Nutrients(42.0, 0.0, 10.5, 0.0), false,
            250.0, QtyUnit.ML, "250 ml", "https://world.openfoodfacts.org/product/0012345678905",
        )
        val id = repo.cacheProduct(p)
        assertTrue(repo.getFood(id)!!.basisNeedsReview)
        repo.confirmBasis(id, QtyUnit.ML)
        repo.setFoodFavorite(id, true)
        val again = repo.cacheProduct(p)
        assertEquals(id, again)
        val cached = repo.getFood(id)!!
        assertTrue(cached.favorite)
        assertFalse("la base confirmada se mantiene si el proveedor no cambió", cached.basisNeedsReview)
        assertEquals(1, cached.version)
        // Un cambio del proveedor sube la versión.
        repo.cacheProduct(p.copy(nutrients = Nutrients(45.0, 0.0, 11.0, 0.0)))
        assertEquals(2, repo.getFood(id)!!.version)
        // Editar el externo crea una copia propia; el producto de origen no cambia.
        val copy = repo.saveFood(id, a.copy(name = "Prueba producto propio"))
        assertNotEquals(id, copy)
        assertEquals("Prueba producto", repo.getFood(id)!!.name)
        assertEquals(id, repo.getFood(copy)!!.derivedFromId)
        assertEquals("0012345678905", repo.getFood(copy)!!.barcode)
    }

    @Test
    fun goals_seedFromProfileOnce_changesDoNotRewritePast() = runBlocking {
        val file = File(context.filesDir, "datastore/nutrition-test.preferences_pb").apply { delete() }
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        try {
            val prefs = UserPreferencesRepository(PreferenceDataStoreFactory.create(scope = scope) { file })
            prefs.setGoals(mapOf(Goal.KCAL to 2000, Goal.PROTEIN to 120))
            val today = LocalDate.now(ZoneOffset.UTC)
            val r = NutritionRepository(db, prefs, now = { clock++ }, zone = { ZoneOffset.UTC })
            val seeded = r.observeGoalHistory().first()
            assertEquals(1, seeded.size)
            assertEquals(today, seeded.single().effectiveDate)
            assertEquals(GoalValues(2000, 120, null, null), seeded.single().values)
            assertNull("sin efecto retroactivo", goalFor(today.minusDays(1), seeded))
            r.observeGoalHistory().first()
            r.setGoals(GoalValues(2200, 120, null, null), today.plusDays(1))
            val h = r.observeGoalHistory().first()
            assertEquals(2, h.size)
            assertEquals(2000, goalFor(today, h)!!.values.kcal)
            assertEquals(2200, goalFor(today.plusDays(1), h)!!.values.kcal)
        } finally {
            scope.cancel()
            file.delete()
        }
    }

    /** N1.1-2: si mover falla, la cantidad tampoco cambia (una sola transacción); al reintentar se aplica todo. */
    @Test
    fun updateItem_moveFails_rollsBackQuantity_thenRetrySucceeds() = runBlocking {
        val failing = object : NutritionRepository(db, now = { clock++ }, zone = { ZoneOffset.UTC }) {
            override suspend fun moveItem(itemId: String, group: MealGroup) {
                super.moveItem(itemId, group)
                error("fallo simulado al mover")
            }
        }
        val food = failing.getFood(failing.saveFood("fa", a))!!.portionable()
        failing.logPortion("i1", day, MealGroup.SNACK, noon, food, 100.0, QtyUnit.G)

        assertTrue(runCatching { failing.updateItem("i1", 200.0, QtyUnit.G, MealGroup.DINNER) }.isFailure)
        val unchanged = repo.observeDay(day).first()
        assertEquals(listOf(MealGroup.SNACK), unchanged.map { it.group })
        val item = unchanged.single().items.single().entity
        assertEquals(100.0, item.quantity, 0.0)
        assertEquals(165.0, item.kcal!!, 1e-9)

        repo.updateItem("i1", 200.0, QtyUnit.G, MealGroup.DINNER)
        val moved = repo.observeDay(day).first()
        assertEquals(listOf(MealGroup.DINNER), moved.map { it.group })
        assertEquals(listOf("i1"), moved.single().items.map { it.entity.id })
        assertEquals(330.0, moved.totals().kcal.value!!, 1e-9)
    }

    /** N1.1-3: si restaurar falla, lo borrado sigue disponible; el reintento lo recupera una sola vez. */
    @Test
    fun undo_restoreFails_keepsReference_retryRestoresOnce() = runBlocking {
        var failNext = true
        val flaky = object : NutritionRepository(db, now = { clock++ }, zone = { ZoneOffset.UTC }) {
            override suspend fun restore(deleted: DeletedItem) {
                if (failNext) {
                    failNext = false
                    error("fallo simulado al restaurar")
                }
                super.restore(deleted)
            }
        }
        val food = flaky.getFood(flaky.saveFood("fa", a))!!.portionable()
        flaky.logPortion("i1", day, MealGroup.LUNCH, noon, food, 150.0, QtyUnit.G)
        val deleted = flaky.deleteItem("i1")!!
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val slot = UndoSlot<DeletedItem>(scope) { flaky.restore(it) }
            slot.offer(deleted)
            slot.undo()
            val failed = withTimeout(5_000) { slot.state.first { !it.restoring && (it.failed || it.deleted == null) } }
            assertTrue(failed.failed)
            assertSame(deleted, failed.deleted)
            assertTrue("nada restaurado a medias", repo.observeDay(day).first().isEmpty())

            slot.undo()
            withTimeout(5_000) { slot.state.first { it.deleted == null } }
            val after = repo.observeDay(day).first()
            assertEquals(listOf("i1"), after.single().items.map { it.entity.id })
            assertEquals(247.5, after.totals().kcal.value!!, 1e-9)
        } finally {
            scope.cancel()
        }
    }
}
