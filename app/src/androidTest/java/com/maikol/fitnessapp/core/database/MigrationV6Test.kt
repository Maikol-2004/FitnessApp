package com.maikol.fitnessapp.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maikol.fitnessapp.feature.nutrition.data.Coverage
import com.maikol.fitnessapp.feature.nutrition.data.MealGroup
import com.maikol.fitnessapp.feature.nutrition.data.Nutrients
import com.maikol.fitnessapp.feature.nutrition.data.NutritionRepository
import com.maikol.fitnessapp.feature.nutrition.data.totals
import com.maikol.fitnessapp.feature.training.data.SystemTrainingClock
import com.maikol.fitnessapp.feature.training.data.TrainingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

/**
 * N1-01: una base v5 real (esquema 5.json con su identity hash) con comidas antiguas —incluidas 0 y valores
 * ausentes— migra a v6 sin pérdidas: quedan «Sin asignar» y LEGACY_TOTALS, sin ingredientes inventados, y
 * cuentan una sola vez. Entrenamiento y agua siguen intactos. Base propia con nombre de prueba.
 */
@RunWith(AndroidJUnit4::class)
class MigrationV6Test {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-v6-test.db"

    @Before
    @After
    fun clean() {
        context.deleteDatabase(name)
    }

    private fun createV5() {
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            V5_SCHEMA.forEach(db::execSQL)
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '88f7864537833222f0dabfd36599f964')")
            db.execSQL("INSERT INTO routines VALUES ('r1', 'Prueba pierna', '', 1, 1, NULL)")
            db.execSQL("INSERT INTO meal_entries VALUES ('m1', 'Prueba comida', 1000, 'UTC', '2026-11-25', 650.0, 30.0, NULL, 20.0, 1, 1)")
            db.execSQL("INSERT INTO meal_entries VALUES ('m2', 'Prueba café', 2000, 'UTC', '2026-11-25', 0.0, 0.0, 0.0, 0.0, 2, 2)")
            db.execSQL("INSERT INTO meal_entries VALUES ('m3', 'Prueba sin kcal', 3000, 'UTC', '2026-11-25', NULL, 12.0, NULL, NULL, 3, 3)")
            db.execSQL("INSERT INTO water_entries VALUES ('w1', 250, 0, 'UTC', '2026-11-25', 0)")
            db.version = 5
        }
    }

    @Test
    fun v5To6_legacyMealsKeptExactly_unassigned_countedOnce() = runBlocking {
        createV5()
        val db = Room.databaseBuilder(context, FitnessDatabase::class.java, name).build()
        try {
            assertEquals(6, db.openHelper.readableDatabase.version)
            val repo = NutritionRepository(db)
            val date = LocalDate.parse("2026-11-25")
            val day = repo.observeDay(date).first()
            assertEquals(listOf("m1", "m2", "m3"), day.map { it.id })
            assertTrue(day.all { it.meal.mode == MealMode.LEGACY_TOTALS && it.group == MealGroup.UNSPECIFIED && it.items.isEmpty() })
            val m1 = day[0].meal
            assertEquals(listOf(650.0, 30.0, null, 20.0), listOf(m1.kcal, m1.proteinG, m1.carbsG, m1.fatG))
            assertEquals(1000L, m1.occurredAt)
            assertEquals("UTC", m1.zoneId)
            // 0 es dato; null es desconocido. Total del día: una sola cuenta.
            val m2 = day[1].meal
            assertEquals(listOf(0.0, 0.0, 0.0, 0.0), listOf(m2.kcal, m2.proteinG, m2.carbsG, m2.fatG))
            val t = day.totals()
            assertEquals(650.0, t.kcal.value!!, 0.0)
            assertEquals(Coverage.PARTIAL, t.kcal.coverage)
            assertEquals(42.0, t.protein.value!!, 0.0)
            assertEquals(Coverage.KNOWN, t.protein.coverage)
            assertEquals(0.0, t.carbs.value!!, 0.0)
            assertEquals(Coverage.PARTIAL, t.carbs.coverage)

            // Editar un antiguo conserva su modo, zona y alta; no se convierte en ingredientes.
            repo.saveTotalsEntry("m1", "Prueba comida", date, LocalTime.of(0, 0, 1), MealGroup.LUNCH, Nutrients(700.0, 30.0, null, 20.0))
            val edited = repo.getEntry("m1")!!
            assertEquals(MealMode.LEGACY_TOTALS, edited.meal.mode)
            assertEquals(MealGroup.LUNCH, edited.group)
            assertEquals("UTC", edited.meal.zoneId)
            assertEquals(1L, edited.meal.createdAt)
            assertNull(edited.meal.carbsG)
            assertTrue(edited.items.isEmpty())

            // Entrenamiento y agua, intactos.
            assertEquals("Prueba pierna", TrainingRepository(db, SystemTrainingClock).getRoutine("r1")!!.name)
            assertEquals(listOf(250), db.trackingDao().observeWater("2026-11-25").first().map { it.ml })
        } finally {
            db.close()
        }
    }

    private companion object {
        val V5_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `app_metadata` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))",
            "CREATE TABLE IF NOT EXISTS `routines` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `archivedAt` INTEGER, PRIMARY KEY(`id`))",
            "CREATE INDEX IF NOT EXISTS `index_routines_archivedAt` ON `routines` (`archivedAt`)",
            "CREATE TABLE IF NOT EXISTS `routine_exercises` (`id` TEXT NOT NULL, `routineId` TEXT NOT NULL, `exerciseKey` TEXT NOT NULL, `position` INTEGER NOT NULL, `name` TEXT NOT NULL, `notes` TEXT NOT NULL, `catalogId` TEXT DEFAULT NULL, `assistMode` TEXT DEFAULT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`routineId`) REFERENCES `routines`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_routine_exercises_routineId_position` ON `routine_exercises` (`routineId`, `position`)",
            "CREATE TABLE IF NOT EXISTS `planned_sets` (`id` TEXT NOT NULL, `exerciseId` TEXT NOT NULL, `position` INTEGER NOT NULL, `reps` INTEGER NOT NULL, `loadKg` REAL, `restSeconds` INTEGER NOT NULL, `warmup` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`exerciseId`) REFERENCES `routine_exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_planned_sets_exerciseId_position` ON `planned_sets` (`exerciseId`, `position`)",
            "CREATE TABLE IF NOT EXISTS `workout_sessions` (`id` TEXT NOT NULL, `routineId` TEXT, `routineNameSnapshot` TEXT NOT NULL, `status` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER, `activeMs` INTEGER NOT NULL, `anchorWallMs` INTEGER, `anchorRealtimeMs` INTEGER, `restTotalMs` INTEGER, `restRemainingMs` INTEGER, `currentExercise` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`routineId`) REFERENCES `routines`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
            "CREATE INDEX IF NOT EXISTS `index_workout_sessions_routineId` ON `workout_sessions` (`routineId`)",
            "CREATE INDEX IF NOT EXISTS `index_workout_sessions_status` ON `workout_sessions` (`status`)",
            "CREATE INDEX IF NOT EXISTS `index_workout_sessions_endedAt` ON `workout_sessions` (`endedAt`)",
            "CREATE TABLE IF NOT EXISTS `session_exercises` (`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `exerciseKey` TEXT NOT NULL, `position` INTEGER NOT NULL, `name` TEXT NOT NULL, `notes` TEXT NOT NULL, `catalogId` TEXT DEFAULT NULL, `assistMode` TEXT DEFAULT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`sessionId`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_session_exercises_sessionId_position` ON `session_exercises` (`sessionId`, `position`)",
            "CREATE TABLE IF NOT EXISTS `performed_sets` (`id` TEXT NOT NULL, `sessionExerciseId` TEXT NOT NULL, `position` INTEGER NOT NULL, `targetReps` INTEGER, `targetLoadKg` REAL, `restSeconds` INTEGER NOT NULL, `warmup` INTEGER NOT NULL, `reps` INTEGER, `loadKg` REAL, `completedAt` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`sessionExerciseId`) REFERENCES `session_exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_performed_sets_sessionExerciseId_position` ON `performed_sets` (`sessionExerciseId`, `position`)",
            "CREATE TABLE IF NOT EXISTS `daily_activity` (`date` TEXT NOT NULL, `steps` INTEGER, `distanceMeters` REAL, `activeMinutes` INTEGER, `activeKcal` REAL, `source` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`date`))",
            "CREATE TABLE IF NOT EXISTS `meal_entries` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `occurredAt` INTEGER NOT NULL, `zoneId` TEXT NOT NULL, `localDate` TEXT NOT NULL, `kcal` REAL, `proteinG` REAL, `carbsG` REAL, `fatG` REAL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE INDEX IF NOT EXISTS `index_meal_entries_localDate` ON `meal_entries` (`localDate`)",
            "CREATE TABLE IF NOT EXISTS `water_entries` (`id` TEXT NOT NULL, `ml` INTEGER NOT NULL, `occurredAt` INTEGER NOT NULL, `zoneId` TEXT NOT NULL, `localDate` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE INDEX IF NOT EXISTS `index_water_entries_localDate` ON `water_entries` (`localDate`)",
            "CREATE TABLE IF NOT EXISTS `sleep_entries` (`id` TEXT NOT NULL, `startAt` INTEGER NOT NULL, `endAt` INTEGER NOT NULL, `zoneId` TEXT NOT NULL, `wakeDate` TEXT NOT NULL, `quality` INTEGER, `note` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE INDEX IF NOT EXISTS `index_sleep_entries_wakeDate` ON `sleep_entries` (`wakeDate`)",
            "CREATE INDEX IF NOT EXISTS `index_sleep_entries_startAt` ON `sleep_entries` (`startAt`)",
            "CREATE INDEX IF NOT EXISTS `index_sleep_entries_endAt` ON `sleep_entries` (`endAt`)",
            "CREATE TABLE IF NOT EXISTS `body_weight_entries` (`id` TEXT NOT NULL, `date` TEXT NOT NULL, `kg` REAL NOT NULL, `note` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE INDEX IF NOT EXISTS `index_body_weight_entries_date` ON `body_weight_entries` (`date`)",
        )
    }
}
