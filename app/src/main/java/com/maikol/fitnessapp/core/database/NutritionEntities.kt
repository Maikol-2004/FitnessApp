package com.maikol.fitnessapp.core.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/*
 * Nutrición N1–N3 (v6, docs/nutrition/02_DATA_AND_INTEGRATION.md).
 * - meal_entries conserva sus columnas; v6 añade grupo (UNSPECIFIED) y modo (LEGACY_TOTALS) a lo anterior.
 * - Un registro cuenta por UNA sola vía: totales propios (LEGACY_TOTALS/QUICK_TOTALS) o sus alimentos (FOOD_ITEMS).
 * - meal_items guarda un snapshot completo: base, nutrientes por base y para la cantidad. Editar o archivar un
 *   alimento o una receta no reescribe el diario (sin FK hacia foods/recipes).
 * - Nutrientes: null = desconocido, 0 = dato. kcal y gramos; las cantidades en g, ml o raciones.
 * - partialMask: bits de nutrientes cuyo valor es una suma parcial (receta con ingredientes sin ese dato).
 */

object MealMode {
    const val LEGACY_TOTALS = "LEGACY_TOTALS"
    const val QUICK_TOTALS = "QUICK_TOTALS"
    const val FOOD_ITEMS = "FOOD_ITEMS"
}

object FoodSource {
    const val MANUAL = "MANUAL"
    const val OFF = "OFF"
}

@Entity(tableName = "meal_items", indices = [Index("mealId")],
    foreignKeys = [ForeignKey(entity = MealEntryEntity::class, parentColumns = ["id"], childColumns = ["mealId"], onDelete = ForeignKey.CASCADE)])
data class MealItemEntity(
    @PrimaryKey val id: String,
    val mealId: String,
    val position: Int,
    val foodId: String?,
    val foodVersion: Int?,
    val recipeId: String?,
    val recipeVersion: Int?,
    val name: String,
    val brand: String?,
    val preparation: String,
    val source: String,
    val sourceId: String?,
    val basisAmount: Double,
    val basisUnit: String,
    val servingName: String?,
    val servingAmount: Double?,
    val servingUnit: String?,
    val basisKcal: Double?,
    val basisProteinG: Double?,
    val basisCarbsG: Double?,
    val basisFatG: Double?,
    val partialMask: Int,
    val quantity: Double,
    val quantityUnit: String,
    val kcal: Double?,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class MealWithItems(
    @Embedded val meal: MealEntryEntity,
    @Relation(parentColumn = "id", entityColumn = "mealId") val items: List<MealItemEntity>,
)

/**
 * Definición reutilizable. Contenido del proveedor (nombre, base, nutrientes) separado de los datos del usuario
 * (favorito, archivado, última cantidad, último uso). Identidad externa = source + sourceId, nunca el nombre.
 */
@Entity(tableName = "foods", indices = [Index(value = ["source", "sourceId"], unique = true), Index("lastUsedAt")])
data class FoodEntity(
    @PrimaryKey val id: String,
    val version: Int,
    val name: String,
    val brand: String?,
    val preparation: String,
    val source: String,
    val sourceId: String?,
    val barcode: String?,
    val sourceUrl: String?,
    val fetchedAt: Long?,
    val basisAmount: Double,
    val basisUnit: String,
    /** Base sin confirmar (producto de volumen publicado «por 100 g»): exige revisión antes de registrar. */
    val basisNeedsReview: Boolean,
    val kcal: Double?,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    val kcalFromKj: Boolean,
    val servingName: String?,
    val servingAmount: Double?,
    val servingUnit: String?,
    val derivedFromId: String?,
    val favorite: Boolean,
    val archived: Boolean,
    val lastQuantity: Double?,
    val lastUnit: String?,
    val lastUsedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Receta propia. Editarla incrementa [version]; los consumos guardan su propio snapshot. */
@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val version: Int,
    val name: String,
    val servings: Double,
    val finalWeightG: Double?,
    val notes: String,
    val favorite: Boolean,
    val archived: Boolean,
    val lastUsedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Ingrediente con snapshot del alimento (base y nutrientes por base) y cantidad elegida. */
@Entity(tableName = "recipe_ingredients", indices = [Index("recipeId")],
    foreignKeys = [ForeignKey(entity = RecipeEntity::class, parentColumns = ["id"], childColumns = ["recipeId"], onDelete = ForeignKey.CASCADE)])
data class RecipeIngredientEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val position: Int,
    val foodId: String?,
    val foodVersion: Int?,
    val name: String,
    val brand: String?,
    val preparation: String,
    val source: String,
    val sourceId: String?,
    val basisAmount: Double,
    val basisUnit: String,
    val servingName: String?,
    val servingAmount: Double?,
    val servingUnit: String?,
    val basisKcal: Double?,
    val basisProteinG: Double?,
    val basisCarbsG: Double?,
    val basisFatG: Double?,
    val quantity: Double,
    val quantityUnit: String,
)

data class RecipeWithIngredients(
    @Embedded val recipe: RecipeEntity,
    @Relation(parentColumn = "id", entityColumn = "recipeId") val ingredients: List<RecipeIngredientEntity>,
)

/** Historial de objetivos: cada cambio guarda su fecha efectiva. No se fabrican objetivos anteriores. */
@Entity(tableName = "nutrition_goals", indices = [Index("effectiveDate")])
data class NutritionGoalEntity(
    @PrimaryKey val id: String,
    val effectiveDate: String,
    val kcal: Int?,
    val proteinG: Int?,
    val carbsG: Int?,
    val fatG: Int?,
    val source: String,
    val createdAt: Long,
)
