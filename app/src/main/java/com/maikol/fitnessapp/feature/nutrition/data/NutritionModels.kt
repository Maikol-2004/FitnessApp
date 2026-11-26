package com.maikol.fitnessapp.feature.nutrition.data

import com.maikol.fitnessapp.core.database.FoodEntity
import com.maikol.fitnessapp.core.database.MealEntryEntity
import com.maikol.fitnessapp.core.database.MealItemEntity
import com.maikol.fitnessapp.core.database.MealMode
import com.maikol.fitnessapp.core.database.MealWithItems
import com.maikol.fitnessapp.core.database.RecipeIngredientEntity
import com.maikol.fitnessapp.core.database.RecipeWithIngredients
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Grupos del diario. UNSPECIFIED = «Sin clasificar»: lo anterior a v6, sin deducirlo de la hora. */
enum class MealGroup { BREAKFAST, LUNCH, SNACK, DINNER, UNSPECIFIED;

    companion object {
        fun of(code: String?): MealGroup = entries.firstOrNull { it.name == code } ?: UNSPECIFIED

        /** Grupos que el usuario elige al registrar (Sin clasificar solo aparece si tiene registros). */
        val choosable = listOf(BREAKFAST, LUNCH, SNACK, DINNER)
    }
}

/** Estado de preparación tal como lo indica la fuente o el usuario. UNKNOWN = sin indicar. */
enum class Preparation { UNKNOWN, RAW, COOKED, READY;

    companion object {
        fun of(code: String?): Preparation = entries.firstOrNull { it.name == code } ?: UNKNOWN
    }
}

/** Lo que se puede registrar: un alimento o una ración de receta, con su base y nutrientes por base. */
data class Portionable(
    val foodId: String?,
    val foodVersion: Int?,
    val recipeId: String?,
    val recipeVersion: Int?,
    val name: String,
    val brand: String?,
    val preparation: Preparation,
    val source: String,
    val sourceId: String?,
    val basis: Basis,
    val servingName: String?,
    val perBasis: Nutrients,
    val partialMask: Int = 0,
) {
    val basisTotals get() = Totals.of(perBasis, partialMask)
}

fun FoodEntity.basis() = Basis(basisAmount, QtyUnit.of(basisUnit) ?: QtyUnit.G, servingAmount, QtyUnit.of(servingUnit))

fun FoodEntity.nutrients() = Nutrients(kcal, proteinG, carbsG, fatG)

fun FoodEntity.portionable() = Portionable(
    foodId = id, foodVersion = version, recipeId = null, recipeVersion = null, name = name, brand = brand,
    preparation = Preparation.of(preparation), source = source, sourceId = sourceId ?: barcode, basis = basis(),
    servingName = servingName, perBasis = nutrients(),
)

fun MealItemEntity.portionable() = Portionable(
    foodId = foodId, foodVersion = foodVersion, recipeId = recipeId, recipeVersion = recipeVersion, name = name, brand = brand,
    preparation = Preparation.of(preparation), source = source, sourceId = sourceId,
    basis = Basis(basisAmount, QtyUnit.of(basisUnit) ?: QtyUnit.G, servingAmount, QtyUnit.of(servingUnit)),
    servingName = servingName, perBasis = Nutrients(basisKcal, basisProteinG, basisCarbsG, basisFatG), partialMask = partialMask,
)

fun RecipeIngredientEntity.portionable() = Portionable(
    foodId = foodId, foodVersion = foodVersion, recipeId = null, recipeVersion = null, name = name, brand = brand,
    preparation = Preparation.of(preparation), source = source, sourceId = sourceId,
    basis = Basis(basisAmount, QtyUnit.of(basisUnit) ?: QtyUnit.G, servingAmount, QtyUnit.of(servingUnit)),
    servingName = servingName, perBasis = Nutrients(basisKcal, basisProteinG, basisCarbsG, basisFatG),
)

fun RecipeIngredientEntity.ingredient() = Ingredient(portionable().perBasis, portionable().basis, quantity, QtyUnit.of(quantityUnit) ?: QtyUnit.G)

fun RecipeWithIngredients.totals(): RecipeTotals =
    recipeTotals(ingredients.sortedBy { it.position }.map { it.ingredient() }, recipe.servings, recipe.finalWeightG)

/** Una ración de receta como base registrable: gramos solo si hay peso final. */
fun RecipeWithIngredients.portionable(): Portionable {
    val t = totals()
    return Portionable(
        foodId = null, foodVersion = null, recipeId = recipe.id, recipeVersion = recipe.version, name = recipe.name, brand = null,
        preparation = Preparation.READY, source = SOURCE_RECIPE, sourceId = null,
        basis = Basis(1.0, QtyUnit.SERVING, t.servingMassG, t.servingMassG?.let { QtyUnit.G }),
        servingName = null, perBasis = t.perServing.toNutrients(), partialMask = t.perServing.partialMask(),
    )
}

const val SOURCE_RECIPE = "RECIPE"

data class DiaryItem(val entity: MealItemEntity) {
    val totals get() = Totals.of(Nutrients(entity.kcal, entity.proteinG, entity.carbsG, entity.fatG), entity.partialMask)
    val unit get() = QtyUnit.of(entity.quantityUnit) ?: QtyUnit.G
}

/** Registro del diario con su única fuente contable (totales propios o alimentos). */
data class DiaryEntry(val meal: MealEntryEntity, val items: List<DiaryItem>) {
    val id get() = meal.id
    val group get() = MealGroup.of(meal.mealGroup)
    val isFoodItems get() = meal.mode == MealMode.FOOD_ITEMS
    val time: LocalTime get() = Instant.ofEpochMilli(meal.occurredAt).atZone(ZoneId.of(meal.zoneId)).toLocalTime()

    val totals: Totals get() = if (isFoodItems) Totals.sum(items.map { it.totals }) else Totals.of(Nutrients(meal.kcal, meal.proteinG, meal.carbsG, meal.fatG))
}

fun MealWithItems.entry() = DiaryEntry(meal, items.sortedWith(compareBy({ it.position }, { it.createdAt })).map(::DiaryItem))

fun List<DiaryEntry>.totals(): Totals = Totals.sum(map { it.totals })

fun dayPoints(from: LocalDate, to: LocalDate, entries: List<DiaryEntry>): List<DayPoint> {
    val byDay = entries.groupBy { LocalDate.parse(it.meal.localDate) }
    return generateSequence(from) { it.plusDays(1) }.takeWhile { it <= to }
        .map { d -> DayPoint(d, byDay[d]?.totals() ?: Totals.Empty) }.toList()
}
