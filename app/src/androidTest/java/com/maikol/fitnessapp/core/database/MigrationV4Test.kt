package com.maikol.fitnessapp.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maikol.fitnessapp.feature.training.data.PlannedSet
import com.maikol.fitnessapp.feature.training.data.Routine
import com.maikol.fitnessapp.feature.training.data.RoutineExercise
import com.maikol.fitnessapp.feature.training.data.StartResult
import com.maikol.fitnessapp.feature.training.data.SystemTrainingClock
import com.maikol.fitnessapp.feature.training.data.TrainingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * G1: una base v3 real (esquema app/schemas/…/3.json con su identity hash) con rutina, sesión y agua «Prueba»
 * migra a v4 sin pérdidas. Los ejercicios antiguos quedan sin catalogId (personalizados); una rutina del
 * catálogo guarda la referencia, la sesión la copia en su snapshot y editar la rutina no reescribe la sesión.
 * Base propia con nombre de prueba: no toca los datos de la app.
 */
@RunWith(AndroidJUnit4::class)
class MigrationV4Test {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-v4-test.db"

    @Before
    @After
    fun clean() {
        context.deleteDatabase(name)
    }

    private fun createV3() {
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            V3_SCHEMA.forEach(db::execSQL)
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '9c07d4ce903c7591c114a4ac4ad30836')")
            db.execSQL("INSERT INTO routines VALUES ('r1', 'Prueba torso', 'nota', 1, 1, NULL)")
            db.execSQL("INSERT INTO routine_exercises VALUES ('e1', 'r1', 'k-remo', 0, 'Remo Prueba', 'agarre')")
            db.execSQL("INSERT INTO planned_sets VALUES ('p1', 'e1', 0, 10, 20.0, 90, 0)")
            db.execSQL(
                "INSERT INTO workout_sessions VALUES ('s1', 'r1', 'Prueba torso', 'COMPLETED', 1000, ${System.currentTimeMillis()}, 60000, NULL, NULL, NULL, NULL, 0)",
            )
            db.execSQL("INSERT INTO session_exercises VALUES ('se1', 's1', 'k-remo', 0, 'Remo Prueba', 'agarre')")
            db.execSQL("INSERT INTO performed_sets VALUES ('ps1', 'se1', 0, 10, 20.0, 90, 0, 10, 20.0, 2000)")
            db.execSQL("INSERT INTO water_entries VALUES ('w1', 250, 0, 'UTC', '2026-11-25', 0)")
            db.version = 3
        }
    }

    @Test
    fun v3To4_keepsEverything_andCatalogIdIsOptional() = runBlocking {
        createV3()
        val db = Room.databaseBuilder(context, FitnessDatabase::class.java, name).build()
        try {
            val training = TrainingRepository(db, SystemTrainingClock)
            val old = training.getRoutine("r1")!!
            assertEquals(4, db.openHelper.readableDatabase.version)
            assertEquals("Prueba torso", old.name)
            val oldExercise = old.exercises.single()
            assertEquals("Remo Prueba", oldExercise.name)
            assertEquals("k-remo", oldExercise.key)
            assertEquals("agarre", oldExercise.notes)
            assertNull(oldExercise.catalogId)
            val history = training.observeHistory().first()
            assertEquals(1, history.size)
            assertEquals(200.0, history.single().volumeKg, 0.0)
            assertEquals(250, db.trackingDao().observeWater("2026-11-25").first().sumOf { it.ml })

            // Rutina nueva con un ejercicio del catálogo y otro personalizado.
            val routine = Routine(
                id = "r2", name = "Prueba hombros", notes = "", archived = false,
                exercises = listOf(
                    RoutineExercise("e2", "k-lat", "Elevaciones laterales", "", listOf(PlannedSet("p2", 12, null, 60, false)), "shoulders_dumbbell_lateral_raise"),
                    RoutineExercise("e3", "k-mio", "Mi ejercicio Prueba", "", listOf(PlannedSet("p3", 10, null, 90, false))),
                ),
            )
            training.saveRoutine(routine)
            val saved = training.getRoutine("r2")!!
            assertEquals(listOf("shoulders_dumbbell_lateral_raise", null), saved.exercises.map { it.catalogId })

            val started = training.startSession("r2") as StartResult.Started
            val session = training.observeSession(started.sessionId).first()!!
            assertEquals(listOf("shoulders_dumbbell_lateral_raise", null), session.exercises.map { it.catalogId })
            assertEquals(listOf("k-lat", "k-mio"), session.exercises.map { it.key })

            // Editar la rutina después no reescribe el snapshot de la sesión.
            training.saveRoutine(routine.copy(exercises = routine.exercises.take(1).map { it.copy(name = "Otro nombre") }))
            val after = training.observeSession(started.sessionId).first()!!
            assertEquals(listOf("Elevaciones laterales", "Mi ejercicio Prueba"), after.exercises.map { it.name })
        } finally {
            db.close()
        }
    }

    private companion object {
        val V3_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `app_metadata` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))",
            "CREATE TABLE IF NOT EXISTS `routines` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `archivedAt` INTEGER, PRIMARY KEY(`id`))",
            "CREATE INDEX IF NOT EXISTS `index_routines_archivedAt` ON `routines` (`archivedAt`)",
            "CREATE TABLE IF NOT EXISTS `routine_exercises` (`id` TEXT NOT NULL, `routineId` TEXT NOT NULL, `exerciseKey` TEXT NOT NULL, `position` INTEGER NOT NULL, `name` TEXT NOT NULL, `notes` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`routineId`) REFERENCES `routines`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_routine_exercises_routineId_position` ON `routine_exercises` (`routineId`, `position`)",
            "CREATE TABLE IF NOT EXISTS `planned_sets` (`id` TEXT NOT NULL, `exerciseId` TEXT NOT NULL, `position` INTEGER NOT NULL, `reps` INTEGER NOT NULL, `loadKg` REAL, `restSeconds` INTEGER NOT NULL, `warmup` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`exerciseId`) REFERENCES `routine_exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_planned_sets_exerciseId_position` ON `planned_sets` (`exerciseId`, `position`)",
            "CREATE TABLE IF NOT EXISTS `workout_sessions` (`id` TEXT NOT NULL, `routineId` TEXT, `routineNameSnapshot` TEXT NOT NULL, `status` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER, `activeMs` INTEGER NOT NULL, `anchorWallMs` INTEGER, `anchorRealtimeMs` INTEGER, `restTotalMs` INTEGER, `restRemainingMs` INTEGER, `currentExercise` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`routineId`) REFERENCES `routines`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
            "CREATE INDEX IF NOT EXISTS `index_workout_sessions_routineId` ON `workout_sessions` (`routineId`)",
            "CREATE INDEX IF NOT EXISTS `index_workout_sessions_status` ON `workout_sessions` (`status`)",
            "CREATE INDEX IF NOT EXISTS `index_workout_sessions_endedAt` ON `workout_sessions` (`endedAt`)",
            "CREATE TABLE IF NOT EXISTS `session_exercises` (`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `exerciseKey` TEXT NOT NULL, `position` INTEGER NOT NULL, `name` TEXT NOT NULL, `notes` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`sessionId`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
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
