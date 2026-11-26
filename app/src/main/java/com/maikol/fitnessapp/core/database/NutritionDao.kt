package com.maikol.fitnessapp.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Diario por comidas, alimentos, recetas y objetivos (N1–N3). */
@Dao
interface NutritionDao {

    // ---- Diario ----

    @Transaction
    @Query("SELECT * FROM meal_entries WHERE localDate = :date ORDER BY occurredAt, createdAt")
    fun observeDay(date: String): Flow<List<MealWithItems>>

    @Transaction
    @Query("SELECT * FROM meal_entries WHERE localDate BETWEEN :from AND :to ORDER BY localDate, occurredAt")
    fun observeRange(from: String, to: String): Flow<List<MealWithItems>>

    @Transaction
    @Query("SELECT * FROM meal_entries WHERE id = :id")
    fun observeMeal(id: String): Flow<MealWithItems?>

    @Transaction
    @Query("SELECT * FROM meal_entries WHERE id = :id")
    suspend fun getMeal(id: String): MealWithItems?

    @Query("SELECT * FROM meal_entries WHERE localDate = :date AND mealGroup = :group AND mode = 'FOOD_ITEMS' ORDER BY createdAt LIMIT 1")
    suspend fun findFoodItemsMeal(date: String, group: String): MealEntryEntity?

    @Upsert
    suspend fun upsertMeal(entity: MealEntryEntity)

    @Query("DELETE FROM meal_entries WHERE id = :id")
    suspend fun deleteMeal(id: String): Int

    @Query("UPDATE meal_entries SET mealGroup = :group, updatedAt = :now WHERE id = :id")
    suspend fun setMealGroup(id: String, group: String, now: Long): Int

    @Query("SELECT * FROM meal_items WHERE id = :id")
    suspend fun getItem(id: String): MealItemEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM meal_items WHERE mealId = :mealId")
    suspend fun maxItemPosition(mealId: String): Int

    @Query("SELECT COUNT(*) FROM meal_items WHERE mealId = :mealId")
    suspend fun countItems(mealId: String): Int

    @Upsert
    suspend fun upsertItems(items: List<MealItemEntity>)

    @Query("DELETE FROM meal_items WHERE id = :id")
    suspend fun deleteItem(id: String): Int

    // ---- Alimentos ----

    @Query("SELECT * FROM foods WHERE archived = 0 ORDER BY name COLLATE NOCASE, id")
    fun observeFoods(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE id = :id")
    fun observeFood(id: String): Flow<FoodEntity?>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getFood(id: String): FoodEntity?

    @Query("SELECT * FROM foods WHERE source = :source AND sourceId = :sourceId")
    suspend fun findBySource(source: String, sourceId: String): FoodEntity?

    @Query("SELECT * FROM foods WHERE archived = 0 AND (barcode = :code OR (source = 'OFF' AND sourceId = :code)) ORDER BY updatedAt DESC")
    suspend fun findByBarcode(code: String): List<FoodEntity>

    @Upsert
    suspend fun upsertFood(entity: FoodEntity)

    @Query("UPDATE foods SET favorite = :favorite WHERE id = :id")
    suspend fun setFoodFavorite(id: String, favorite: Boolean): Int

    @Query("UPDATE foods SET archived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setFoodArchived(id: String, archived: Boolean, now: Long): Int

    @Query("UPDATE foods SET lastQuantity = :quantity, lastUnit = :unit, lastUsedAt = :now WHERE id = :id")
    suspend fun markFoodUsed(id: String, quantity: Double, unit: String, now: Long): Int

    // ---- Recetas ----

    @Transaction
    @Query("SELECT * FROM recipes WHERE archived = 0 ORDER BY name COLLATE NOCASE, id")
    fun observeRecipes(): Flow<List<RecipeWithIngredients>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeRecipe(id: String): Flow<RecipeWithIngredients?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipe(id: String): RecipeWithIngredients?

    @Upsert
    suspend fun upsertRecipe(entity: RecipeEntity)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredients(recipeId: String)

    @Upsert
    suspend fun upsertIngredients(items: List<RecipeIngredientEntity>)

    @Query("UPDATE recipes SET archived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setRecipeArchived(id: String, archived: Boolean, now: Long): Int

    @Query("UPDATE recipes SET favorite = :favorite WHERE id = :id")
    suspend fun setRecipeFavorite(id: String, favorite: Boolean): Int

    @Query("UPDATE recipes SET lastUsedAt = :now WHERE id = :id")
    suspend fun markRecipeUsed(id: String, now: Long): Int

    // ---- Objetivos ----

    @Query("SELECT * FROM nutrition_goals ORDER BY effectiveDate, createdAt")
    fun observeGoals(): Flow<List<NutritionGoalEntity>>

    @Query("SELECT COUNT(*) FROM nutrition_goals")
    suspend fun countGoals(): Int

    @Upsert
    suspend fun upsertGoal(entity: NutritionGoalEntity)
}
