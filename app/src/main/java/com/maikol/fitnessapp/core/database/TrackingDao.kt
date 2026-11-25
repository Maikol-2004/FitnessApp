package com.maikol.fitnessapp.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Registros manuales de Salud, Nutrición, Hidratación, Sueño y Peso. */
@Dao
interface TrackingDao {

    // ---- Actividad diaria ----

    @Query("SELECT * FROM daily_activity WHERE date = :date")
    fun observeActivity(date: String): Flow<DailyActivityEntity?>

    @Upsert
    suspend fun upsertActivity(entity: DailyActivityEntity)

    @Query("DELETE FROM daily_activity WHERE date = :date")
    suspend fun deleteActivity(date: String): Int

    // ---- Comidas ----

    @Query("SELECT * FROM meal_entries WHERE localDate = :date ORDER BY occurredAt, createdAt")
    fun observeMeals(date: String): Flow<List<MealEntryEntity>>

    @Upsert
    suspend fun upsertMeal(entity: MealEntryEntity)

    @Query("DELETE FROM meal_entries WHERE id = :id")
    suspend fun deleteMeal(id: String): Int

    // ---- Agua ----

    @Query("SELECT * FROM water_entries WHERE localDate = :date ORDER BY occurredAt DESC, createdAt DESC")
    fun observeWater(date: String): Flow<List<WaterEntryEntity>>

    @Insert
    suspend fun insertWater(entity: WaterEntryEntity)

    @Query("UPDATE water_entries SET ml = :ml WHERE id = :id")
    suspend fun updateWaterMl(id: String, ml: Int): Int

    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun deleteWater(id: String): Int

    // ---- Sueño ----

    @Query("SELECT * FROM sleep_entries ORDER BY endAt DESC LIMIT :limit")
    fun observeRecentSleep(limit: Int): Flow<List<SleepEntryEntity>>

    @Query("SELECT * FROM sleep_entries WHERE id = :id")
    suspend fun getSleep(id: String): SleepEntryEntity?

    /** Intervalos que se solapan con [start, end) (los extremos que solo se tocan no cuentan). */
    @Query("SELECT * FROM sleep_entries WHERE startAt < :end AND endAt > :start AND id != :excludeId")
    suspend fun findOverlappingSleep(start: Long, end: Long, excludeId: String): List<SleepEntryEntity>

    @Upsert
    suspend fun upsertSleep(entity: SleepEntryEntity)

    @Query("DELETE FROM sleep_entries WHERE id = :id")
    suspend fun deleteSleep(id: String): Int

    // ---- Peso corporal ----

    @Query("SELECT * FROM body_weight_entries ORDER BY date DESC, createdAt DESC")
    fun observeWeights(): Flow<List<BodyWeightEntity>>

    @Query("SELECT * FROM body_weight_entries WHERE id = :id")
    suspend fun getWeight(id: String): BodyWeightEntity?

    @Upsert
    suspend fun upsertWeight(entity: BodyWeightEntity)

    @Query("DELETE FROM body_weight_entries WHERE id = :id")
    suspend fun deleteWeight(id: String): Int
}
