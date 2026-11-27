package com.maikol.fitnessapp.feature.nutrition.data

import androidx.room.withTransaction
import com.maikol.fitnessapp.core.database.FitnessDatabase
import com.maikol.fitnessapp.core.database.FoodEntity
import com.maikol.fitnessapp.core.database.FoodSource
import com.maikol.fitnessapp.core.database.MealEntryEntity
import com.maikol.fitnessapp.core.database.MealItemEntity
import com.maikol.fitnessapp.core.database.MealMode
import com.maikol.fitnessapp.core.database.MealWithItems
import com.maikol.fitnessapp.core.database.NutritionGoalEntity
import com.maikol.fitnessapp.core.database.RecipeEntity
import com.maikol.fitnessapp.core.database.RecipeIngredientEntity
import com.maikol.fitnessapp.core.database.RecipeWithIngredients
import com.maikol.fitnessapp.core.preferences.Goal
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/** Alimento propio tal como lo escribe el usuario (desde una etiqueta). */
data class FoodDraft(
    val name: String,
    val brand: String?,
    val preparation: Preparation,
    val basis: Basis,
    val servingName: String?,
    val nutrients: Nutrients,
    val barcode: String?,
)

data class IngredientDraft(val id: String, val source: Portionable, val quantity: Double, val unit: QtyUnit)

data class RecipeDraft(val name: String, val servings: Double, val finalWeightG: Double?, val notes: String, val ingredients: List<IngredientDraft>)

/** Lo borrado para deshacer exactamente esa operación (mismas filas e IDs: restaurar no duplica). */
data class DeletedItem(val item: MealItemEntity, val header: MealEntryEntity?)

/** Elementos a copiar: registros completos y/o alimentos sueltos de un registro FOOD_ITEMS. */
data class CopyRequest(val opId: String, val entryIds: List<String>, val itemIds: List<String>, val date: LocalDate, val group: MealGroup, val time: LocalTime)

/**
 * Nutrición local sobre Room. Las escrituras lanzan si fallan: la UI conserva el borrador y muestra el error.
 * Las operaciones con ID fijado por el borrador (registro, copia) son idempotentes: reintentar no duplica.
 */
open class NutritionRepository(
    private val db: FitnessDatabase,
    private val preferences: UserPreferencesRepository? = null,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private val dao = db.nutritionDao()

    fun newId(): String = newId.invoke()

    fun zone(): ZoneId = zone.invoke()

    // ---- Diario ----

    fun observeDay(date: LocalDate): Flow<List<DiaryEntry>> = dao.observeDay(date.toString()).map { l -> l.map(MealWithItems::entry) }

    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DiaryEntry>> =
        dao.observeRange(from.toString(), to.toString()).map { l -> l.map(MealWithItems::entry) }

    fun observeEntry(id: String): Flow<DiaryEntry?> = dao.observeMeal(id).map { it?.entry() }

    private fun epoch(date: LocalDate, time: LocalTime, z: ZoneId) = date.atTime(time).atZone(z).toInstant().toEpochMilli()

    /** Contenedor FOOD_ITEMS de ese día y grupo; se crea con la hora indicada si no existe. */
    private suspend fun foodItemsMeal(date: LocalDate, group: MealGroup, time: LocalTime): MealEntryEntity {
        dao.findFoodItemsMeal(date.toString(), group.name)?.let { return it }
        val z = zone()
        val t = now()
        return MealEntryEntity(
            id = newId(), name = "", occurredAt = epoch(date, time, z), zoneId = z.id, localDate = date.toString(),
            kcal = null, proteinG = null, carbsG = null, fatG = null, createdAt = t, updatedAt = t,
            mealGroup = group.name, mode = MealMode.FOOD_ITEMS,
        ).also { dao.upsertMeal(it) }
    }

    private fun item(id: String, mealId: String, position: Int, p: Portionable, quantity: Double, unit: QtyUnit, createdAt: Long): MealItemEntity {
        val n = requireNotNull(nutrientsFor(p.perBasis, p.basis, quantity, unit)) { "Cantidad sin equivalencia" }
        return MealItemEntity(
            id = id, mealId = mealId, position = position, foodId = p.foodId, foodVersion = p.foodVersion, recipeId = p.recipeId,
            recipeVersion = p.recipeVersion, name = p.name, brand = p.brand, preparation = p.preparation.name, source = p.source,
            sourceId = p.sourceId, basisAmount = p.basis.amount, basisUnit = p.basis.unit.code, servingName = p.servingName,
            servingAmount = p.basis.servingAmount, servingUnit = p.basis.servingUnit?.code, basisKcal = p.perBasis.kcal,
            basisProteinG = p.perBasis.proteinG, basisCarbsG = p.perBasis.carbsG, basisFatG = p.perBasis.fatG,
            partialMask = p.partialMask, quantity = quantity, quantityUnit = unit.code, kcal = n.kcal, proteinG = n.proteinG,
            carbsG = n.carbsG, fatG = n.fatG, createdAt = createdAt, updatedAt = now(),
        )
    }

    /**
     * Registra una cantidad en (fecha, grupo). [itemId] lo fija el borrador: un doble toque o un reintento
     * sustituye la misma fila. Último uso/cantidad se actualizan solo si el guardado tiene éxito.
     */
    open suspend fun logPortion(itemId: String, date: LocalDate, group: MealGroup, time: LocalTime, p: Portionable, quantity: Double, unit: QtyUnit) {
        require(validQuantity(quantity, unit))
        db.withTransaction {
            val existing = dao.getItem(itemId)
            val meal = existing?.let { dao.getMeal(it.mealId)?.meal } ?: foodItemsMeal(date, group, time)
            val position = existing?.position ?: (dao.maxItemPosition(meal.id) + 1)
            dao.upsertItems(listOf(item(itemId, meal.id, position, p, quantity, unit, existing?.createdAt ?: now())))
            val t = now()
            p.foodId?.let { dao.markFoodUsed(it, quantity, unit.code, t) }
            p.recipeId?.let { dao.markRecipeUsed(it, t) }
        }
    }

    /** Cambia la cantidad desde el snapshot guardado (no desde la definición actual del alimento). */
    open suspend fun updateItemQuantity(itemId: String, quantity: Double, unit: QtyUnit) {
        require(validQuantity(quantity, unit))
        db.withTransaction {
            val old = requireNotNull(dao.getItem(itemId))
            dao.upsertItems(listOf(item(old.id, old.mealId, old.position, old.portionable(), quantity, unit, old.createdAt)))
        }
    }

    /** Mueve un alimento a otro grupo del mismo día. Un contenedor que queda vacío se elimina. */
    open suspend fun moveItem(itemId: String, group: MealGroup) {
        db.withTransaction {
            val old = requireNotNull(dao.getItem(itemId))
            val from = requireNotNull(dao.getMeal(old.mealId)).meal
            if (MealGroup.of(from.mealGroup) == group) return@withTransaction
            val target = foodItemsMeal(LocalDate.parse(from.localDate), group, LocalTime.now(zone()))
            dao.upsertItems(listOf(old.copy(mealId = target.id, position = dao.maxItemPosition(target.id) + 1, updatedAt = now())))
            if (dao.countItems(from.id) == 0) dao.deleteMeal(from.id)
        }
    }

    /**
     * Edición de un alimento registrado: cantidad y comida en una única transacción. Si cualquiera de los dos pasos
     * falla, no queda nada a medias (ni la cantidad cambiada en la comida de antes).
     */
    open suspend fun updateItem(itemId: String, quantity: Double, unit: QtyUnit, group: MealGroup) {
        require(validQuantity(quantity, unit))
        db.withTransaction {
            updateItemQuantity(itemId, quantity, unit)
            moveItem(itemId, group)
        }
    }

    suspend fun deleteItem(itemId: String): DeletedItem? = db.withTransaction {
        val old = dao.getItem(itemId) ?: return@withTransaction null
        val header = dao.getMeal(old.mealId)?.meal
        dao.deleteItem(itemId)
        val removedHeader = if (header != null && dao.countItems(header.id) == 0) header.also { dao.deleteMeal(it.id) } else null
        DeletedItem(old, removedHeader)
    }

    open suspend fun restore(deleted: DeletedItem) {
        db.withTransaction {
            deleted.header?.let { if (dao.getMeal(it.id) == null) dao.upsertMeal(it) }
            dao.upsertItems(listOf(deleted.item))
        }
    }

    /** Registro rápido o edición de un registro con totales (antiguo o rápido): conserva su modo e ID. */
    open suspend fun saveTotalsEntry(id: String, name: String, date: LocalDate, time: LocalTime, group: MealGroup, n: Nutrients) {
        require(name.isNotBlank() && n.anyKnown)
        db.withTransaction {
            val existing = dao.getMeal(id)?.meal
            require(existing == null || existing.mode != MealMode.FOOD_ITEMS)
            // Una edición conserva la zona de origen; la fecha y hora elegidas se interpretan en ella.
            val z = existing?.let { ZoneId.of(it.zoneId) } ?: zone()
            val t = now()
            dao.upsertMeal(
                MealEntryEntity(
                    id = id, name = name.trim(), occurredAt = epoch(date, time, z), zoneId = z.id, localDate = date.toString(),
                    kcal = n.kcal, proteinG = n.proteinG, carbsG = n.carbsG, fatG = n.fatG,
                    createdAt = existing?.createdAt ?: t, updatedAt = t, mealGroup = group.name,
                    mode = existing?.mode ?: MealMode.QUICK_TOTALS,
                ),
            )
        }
    }

    suspend fun getEntry(id: String): DiaryEntry? = dao.getMeal(id)?.entry()

    suspend fun getItemWithEntry(itemId: String): Pair<MealItemEntity, MealEntryEntity>? {
        val item = dao.getItem(itemId) ?: return null
        val meal = dao.getMeal(item.mealId)?.meal ?: return null
        return item to meal
    }

    suspend fun setEntryGroup(id: String, group: MealGroup) {
        db.withTransaction {
            val meal = requireNotNull(dao.getMeal(id)).meal
            if (meal.mode != MealMode.FOOD_ITEMS) {
                dao.setMealGroup(id, group.name, now())
            } else {
                // Un contenedor de alimentos se une al del grupo destino si ya existe.
                val target = dao.findFoodItemsMeal(meal.localDate, group.name)
                if (target == null) {
                    dao.setMealGroup(id, group.name, now())
                } else if (target.id != id) {
                    val items = dao.getMeal(id)!!.items.sortedBy { it.position }
                    var pos = dao.maxItemPosition(target.id)
                    dao.upsertItems(items.map { it.copy(mealId = target.id, position = ++pos, updatedAt = now()) })
                    dao.deleteMeal(id)
                }
            }
        }
    }

    /** Borra el registro y solo sus hijos. */
    suspend fun deleteEntry(id: String) {
        dao.deleteMeal(id)
    }

    /**
     * Copia a destino explícito con IDs nuevos derivados de la operación: el mismo reintento no duplica y el
     * origen no cambia. Los snapshots se conservan tal cual.
     */
    open suspend fun copy(req: CopyRequest) {
        fun derived(id: String) = UUID.nameUUIDFromBytes("${req.opId}:$id".toByteArray()).toString()
        db.withTransaction {
            val z = zone()
            val t = now()
            for (entryId in req.entryIds) {
                val src = dao.getMeal(entryId) ?: continue
                if (src.meal.mode == MealMode.FOOD_ITEMS) {
                    copyItems(src.items.sortedBy { it.position }, req, ::derived)
                } else {
                    dao.upsertMeal(
                        src.meal.copy(
                            id = derived(src.meal.id), occurredAt = epoch(req.date, req.time, z), zoneId = z.id,
                            localDate = req.date.toString(), mealGroup = req.group.name, createdAt = t, updatedAt = t,
                        ),
                    )
                }
            }
            val loose = req.itemIds.mapNotNull { dao.getItem(it) }
            if (loose.isNotEmpty()) copyItems(loose, req, ::derived)
        }
    }

    private suspend fun copyItems(items: List<MealItemEntity>, req: CopyRequest, derived: (String) -> String) {
        if (items.isEmpty()) return
        val target = foodItemsMeal(req.date, req.group, req.time)
        var pos = dao.maxItemPosition(target.id)
        val t = now()
        dao.upsertItems(items.map { src ->
            val id = derived(src.id)
            val existing = dao.getItem(id)
            src.copy(id = id, mealId = target.id, position = existing?.position ?: ++pos, createdAt = existing?.createdAt ?: t, updatedAt = t)
        })
    }

    // ---- Alimentos ----

    fun observeFoods(): Flow<List<FoodEntity>> = dao.observeFoods()

    fun observeFood(id: String): Flow<FoodEntity?> = dao.observeFood(id)

    suspend fun getFood(id: String): FoodEntity? = dao.getFood(id)

    suspend fun findByBarcode(code: String): List<FoodEntity> = dao.findByBarcode(code)

    /**
     * Crea o edita un alimento propio (no registra consumo). Editar incrementa la versión; los consumos
     * pasados conservan su snapshot. Editar un producto externo crea una copia privada con su origen.
     */
    open suspend fun saveFood(id: String, draft: FoodDraft): String = db.withTransaction {
        require(draft.name.isNotBlank() && draft.nutrients.anyKnown && draft.basis.amount > 0)
        val existing = dao.getFood(id)
        val t = now()
        val copyOfExternal = existing != null && existing.source != FoodSource.MANUAL
        val targetId = if (copyOfExternal) newId() else id
        dao.upsertFood(
            FoodEntity(
                id = targetId,
                version = if (existing != null && !copyOfExternal) existing.version + 1 else 1,
                name = draft.name.trim(), brand = draft.brand?.trim()?.takeIf { it.isNotEmpty() }, preparation = draft.preparation.name,
                source = FoodSource.MANUAL, sourceId = null,
                barcode = draft.barcode?.trim()?.takeIf { it.isNotEmpty() } ?: existing?.let { it.barcode ?: it.sourceId.takeIf { _ -> copyOfExternal } },
                sourceUrl = null, fetchedAt = null, basisAmount = draft.basis.amount, basisUnit = draft.basis.unit.code, basisNeedsReview = false,
                kcal = draft.nutrients.kcal, proteinG = draft.nutrients.proteinG, carbsG = draft.nutrients.carbsG, fatG = draft.nutrients.fatG,
                kcalFromKj = false, servingName = draft.servingName?.trim()?.takeIf { it.isNotEmpty() },
                servingAmount = draft.basis.servingAmount, servingUnit = draft.basis.servingUnit?.code,
                derivedFromId = if (copyOfExternal) existing!!.id else existing?.derivedFromId,
                favorite = if (copyOfExternal) false else existing?.favorite ?: false, archived = false,
                lastQuantity = if (copyOfExternal) null else existing?.lastQuantity, lastUnit = if (copyOfExternal) null else existing?.lastUnit,
                lastUsedAt = if (copyOfExternal) null else existing?.lastUsedAt, createdAt = if (copyOfExternal) t else existing?.createdAt ?: t, updatedAt = t,
            ),
        )
        targetId
    }

    /**
     * Guarda en caché el producto externo elegido. Una nueva lectura actualiza solo el contenido del proveedor;
     * favorito, último uso y archivado son del usuario y se conservan. Devuelve el ID local.
     */
    open suspend fun cacheProduct(p: RemoteProduct): String = db.withTransaction {
        val existing = dao.findBySource(FoodSource.OFF, p.code)
        val t = now()
        val id = existing?.id ?: newId()
        val changed = existing == null || existing.name != p.name || existing.basisAmount != p.basisAmount || existing.basisUnit != p.basisUnit.code ||
            existing.kcal != p.nutrients.kcal || existing.proteinG != p.nutrients.proteinG || existing.carbsG != p.nutrients.carbsG ||
            existing.fatG != p.nutrients.fatG || existing.servingAmount != p.servingAmount
        dao.upsertFood(
            FoodEntity(
                id = id, version = if (existing == null) 1 else if (changed) existing.version + 1 else existing.version,
                name = p.name, brand = p.brand, preparation = Preparation.UNKNOWN.name, source = FoodSource.OFF, sourceId = p.code,
                barcode = p.code, sourceUrl = p.url, fetchedAt = t, basisAmount = p.basisAmount, basisUnit = p.basisUnit.code,
                // Una base ya confirmada por el usuario se mantiene si el proveedor no cambió.
                basisNeedsReview = p.basisNeedsReview && !(existing != null && !existing.basisNeedsReview && !changed),
                kcal = p.nutrients.kcal, proteinG = p.nutrients.proteinG, carbsG = p.nutrients.carbsG, fatG = p.nutrients.fatG,
                kcalFromKj = p.kcalFromKj, servingName = p.servingLabel, servingAmount = p.servingAmount, servingUnit = p.servingUnit?.code,
                derivedFromId = null, favorite = existing?.favorite ?: false, archived = false, lastQuantity = existing?.lastQuantity,
                lastUnit = existing?.lastUnit, lastUsedAt = existing?.lastUsedAt, createdAt = existing?.createdAt ?: t, updatedAt = t,
            ),
        )
        id
    }

    /** El usuario confirma la base de un producto (100 g o 100 ml) antes de registrarlo. */
    suspend fun confirmBasis(foodId: String, unit: QtyUnit) {
        db.withTransaction {
            val f = requireNotNull(dao.getFood(foodId))
            // La ración conserva su propia unidad: si no coincide con la base, no se ofrece como equivalencia.
            dao.upsertFood(f.copy(basisUnit = unit.code, basisNeedsReview = false, updatedAt = now()))
        }
    }

    suspend fun setFoodFavorite(id: String, favorite: Boolean) {
        dao.setFoodFavorite(id, favorite)
    }

    /** Archivar oculta la definición; los consumos y recetas conservan su snapshot. */
    suspend fun setFoodArchived(id: String, archived: Boolean) {
        dao.setFoodArchived(id, archived, now())
    }

    // ---- Recetas ----

    fun observeRecipes(): Flow<List<RecipeWithIngredients>> = dao.observeRecipes()

    fun observeRecipe(id: String): Flow<RecipeWithIngredients?> = dao.observeRecipe(id)

    suspend fun getRecipe(id: String): RecipeWithIngredients? = dao.getRecipe(id)

    /** Guardar no registra consumo. Editar crea una versión nueva; el diario conserva sus snapshots. */
    open suspend fun saveRecipe(id: String, draft: RecipeDraft) {
        require(draft.name.isNotBlank() && draft.ingredients.isNotEmpty() && draft.servings > 0 && draft.servings <= MAX_SERVINGS)
        require(draft.finalWeightG == null || draft.finalWeightG > 0)
        require(draft.ingredients.all { validQuantity(it.quantity, it.unit) && it.source.basis.factor(it.quantity, it.unit) != null && it.source.recipeId == null })
        db.withTransaction {
            val existing = dao.getRecipe(id)?.recipe
            val t = now()
            dao.upsertRecipe(
                RecipeEntity(
                    id = id, version = (existing?.version ?: 0) + 1, name = draft.name.trim(), servings = draft.servings,
                    finalWeightG = draft.finalWeightG, notes = draft.notes.trim(), favorite = existing?.favorite ?: false, archived = false,
                    lastUsedAt = existing?.lastUsedAt, createdAt = existing?.createdAt ?: t, updatedAt = t,
                ),
            )
            dao.deleteIngredients(id)
            dao.upsertIngredients(draft.ingredients.mapIndexed { i, d ->
                val p = d.source
                RecipeIngredientEntity(
                    id = d.id, recipeId = id, position = i, foodId = p.foodId, foodVersion = p.foodVersion, name = p.name, brand = p.brand,
                    preparation = p.preparation.name, source = p.source, sourceId = p.sourceId, basisAmount = p.basis.amount,
                    basisUnit = p.basis.unit.code, servingName = p.servingName, servingAmount = p.basis.servingAmount,
                    servingUnit = p.basis.servingUnit?.code, basisKcal = p.perBasis.kcal, basisProteinG = p.perBasis.proteinG,
                    basisCarbsG = p.perBasis.carbsG, basisFatG = p.perBasis.fatG, quantity = d.quantity, quantityUnit = d.unit.code,
                )
            })
        }
    }

    suspend fun setRecipeArchived(id: String, archived: Boolean) {
        dao.setRecipeArchived(id, archived, now())
    }

    suspend fun setRecipeFavorite(id: String, favorite: Boolean) {
        dao.setRecipeFavorite(id, favorite)
    }

    // ---- Objetivos ----

    /**
     * Historial de objetivos (Room es la autoridad). La primera vez se adoptan las metas ya guardadas en Perfil
     * con fecha efectiva de hoy (la de la migración), nunca hacia atrás.
     */
    fun observeGoalHistory(): Flow<List<GoalChange>> = dao.observeGoals()
        .onStart { seedFromPreferences() }
        .map { l -> l.map { GoalChange(LocalDate.parse(it.effectiveDate), it.createdAt, GoalValues(it.kcal, it.proteinG, it.carbsG, it.fatG)) } }

    private suspend fun seedFromPreferences() {
        val prefs = preferences ?: return
        if (dao.countGoals() > 0) return
        val g = runCatching { prefs.userPreferences.first().goals }.getOrNull() ?: return
        val v = GoalValues(g[Goal.KCAL], g[Goal.PROTEIN], g[Goal.CARBS], g[Goal.FAT])
        if (v.isEmpty) return
        db.withTransaction {
            if (dao.countGoals() == 0) insertGoal(v, LocalDate.now(zone()), SOURCE_PROFILE_PREFS)
        }
    }

    private suspend fun insertGoal(v: GoalValues, effective: LocalDate, source: String) {
        dao.upsertGoal(NutritionGoalEntity(newId(), effective.toString(), v.kcal, v.proteinG, v.carbsG, v.fatG, source, now()))
    }

    /** Un cambio de objetivo con su fecha efectiva (hoy por defecto). No cambia días anteriores. */
    open suspend fun setGoals(values: GoalValues, effective: LocalDate = LocalDate.now(zone()), source: String = SOURCE_MANUAL) {
        require(Nutrient.entries.all { n -> values[n]?.let { goalRange(n).contains(it) } ?: true })
        seedFromPreferences()
        insertGoal(values, effective, source)
    }

    companion object {
        const val SOURCE_MANUAL = "MANUAL"
        const val SOURCE_PROFILE = "PROFILE"
        const val SOURCE_PROFILE_PREFS = "PROFILE_PREFS"

        fun goalRange(n: Nutrient): IntRange = when (n) {
            Nutrient.KCAL -> Goal.KCAL.min..Goal.KCAL.max
            Nutrient.PROTEIN -> Goal.PROTEIN.min..Goal.PROTEIN.max
            Nutrient.CARBS -> Goal.CARBS.min..Goal.CARBS.max
            Nutrient.FAT -> Goal.FAT.min..Goal.FAT.max
        }
    }
}
