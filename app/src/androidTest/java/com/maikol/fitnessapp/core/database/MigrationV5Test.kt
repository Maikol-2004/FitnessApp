package com.maikol.fitnessapp.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maikol.fitnessapp.feature.training.catalog.AssistMode
import com.maikol.fitnessapp.feature.training.catalog.LoadSemantics
import com.maikol.fitnessapp.feature.training.data.StartResult
import com.maikol.fitnessapp.feature.training.data.SystemTrainingClock
import com.maikol.fitnessapp.feature.training.data.TrainingRepository
import com.maikol.fitnessapp.feature.training.data.exerciseProgress
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * G2.1: una base v4 real (esquema app/schemas/…/4.json con su identity hash) con una dominada asistida ya
 * entrenada migra a v5 sin pérdidas. Lo anterior queda «sin indicar» (assistMode null), no se reclasifica, y la
 * asistencia 0 registrada se conserva como dato. Después, máquina/banda se guardan en la rutina, se copian a la
 * sesión y se pueden indicar durante una sesión abierta, nunca en una terminada.
 * Base propia con nombre de prueba: no toca los datos de la app.
 */
@RunWith(AndroidJUnit4::class)
class MigrationV5Test {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-v5-test.db"
    private val assisted = "back_assisted_pull_up"

    @Before
    @After
    fun clean() {
        context.deleteDatabase(name)
    }

    private fun createV4() {
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            V4_SCHEMA.forEach(db::execSQL)
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '5dc44e125bfe49af1bc403a81db3c25d')")
            db.execSQL("INSERT INTO routines VALUES ('r1', 'Prueba espalda', '', 1, 1, NULL)")
            db.execSQL("INSERT INTO routine_exercises VALUES ('e1', 'r1', 'k-dom', 0, 'Dominada asistida', '', '$assisted')")
            db.execSQL("INSERT INTO planned_sets VALUES ('p1', 'e1', 0, 8, 20.0, 90, 0)")
            db.execSQL(
                "INSERT INTO workout_sessions VALUES ('s1', 'r1', 'Prueba espalda', 'COMPLETED', 1000, ${System.currentTimeMillis()}, 60000, NULL, NULL, NULL, NULL, 0)",
            )
            db.execSQL("INSERT INTO session_exercises VALUES ('se1', 's1', 'k-dom', 0, 'Dominada asistida', '', '$assisted')")
            db.execSQL("INSERT INTO performed_sets VALUES ('ps1', 'se1', 0, 8, 20.0, 90, 0, 8, 20.0, 2000)")
            // Asistencia 0 registrada: sin ayuda, no es un dato ausente.
            db.execSQL("INSERT INTO performed_sets VALUES ('ps2', 'se1', 1, 8, 20.0, 90, 0, 6, 0.0, 3000)")
            db.version = 4
        }
    }

    @Test
    fun v4To5_oldAssistedStaysUnknown_zeroKept_newModesPersist() = runBlocking {
        createV4()
        val db = Room.databaseBuilder(context, FitnessDatabase::class.java, name).build()
        try {
            val training = TrainingRepository(db, SystemTrainingClock)
            val old = training.getRoutine("r1")!!
            assertEquals(5, db.openHelper.readableDatabase.version)
            assertEquals(assisted, old.exercises.single().catalogId)
            assertNull(old.exercises.single().assistMode)

            val rows = training.observeCatalogExerciseSets(assisted).first()
            assertEquals(listOf(20.0, 0.0), rows.map { it.loadKg })
            assertTrue(rows.all { it.assistMode == null })
            val p = exerciseProgress(rows, LoadSemantics.ASSISTANCE)
            assertTrue("sin modalidad no se grafica", p.assistSeries.isEmpty())
            assertEquals(1, p.unknownAssistSessions)
            assertEquals(listOf(20.0, 0.0), p.history.single().sets.map { it.loadKg })

            // La persona indica «banda» en la rutina: se guarda y se copia a la sesión nueva.
            training.saveRoutine(old.copy(exercises = old.exercises.map { it.copy(assistMode = AssistMode.BAND) }))
            assertEquals(AssistMode.BAND, training.getRoutine("r1")!!.exercises.single().assistMode)
            val sessionId = (training.startSession("r1") as StartResult.Started).sessionId
            val exercise = training.observeSession(sessionId).first()!!.exercises.single()
            assertEquals(AssistMode.BAND, exercise.assistMode)

            // Durante la sesión abierta puede cambiarla; terminada ya no se reescribe.
            assertTrue(training.setSessionAssistMode(exercise.id, AssistMode.MACHINE))
            assertEquals(AssistMode.MACHINE, training.observeSession(sessionId).first()!!.exercises.single().assistMode)
            training.completeSet(sessionId, exercise.sets.first().id, 8, 15.0)
            training.finish(sessionId)
            assertFalse(training.setSessionAssistMode(exercise.id, AssistMode.BAND))

            val after = exerciseProgress(training.observeCatalogExerciseSets(assisted).first(), LoadSemantics.ASSISTANCE)
            assertEquals(listOf(AssistMode.MACHINE), after.assistSeries.map { it.mode })
            assertEquals(listOf(15.0), after.assistSeries.single().points.map { it.valueKg })
            // La sesión antigua sigue sin indicar: no se reclasificó.
            assertEquals(1, after.unknownAssistSessions)
        } finally {
            db.close()
        }
    }

    private companion object {
        val V4_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `app_metadata` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))",
            "CREATE TABLE IF NOT EXISTS `routines` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `archivedAt` INTEGER, PRIMARY KEY(`id`))",
            "CREATE INDEX IF NOT EXISTS `index_routines_archivedAt` ON `routines` (`archivedAt`)",
            "CREATE TABLE IF NOT EXISTS `routine_exercises` (`id` TEXT NOT NULL, `routineId` TEXT NOT NULL, `exerciseKey` TEXT NOT NULL, `position` INTEGER NOT NULL, `name` TEXT NOT NULL, `notes` TEXT NOT NULL, `catalogId` TEXT DEFAULT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`routineId`) REFERENCES `routines`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_routine_exercises_routineId_position` ON `routine_exercises` (`routineId`, `position`)",
            "CREATE TABLE IF NOT EXISTS `planned_sets` (`id` TEXT NOT NULL, `exerciseId` TEXT NOT NULL, `position` INTEGER NOT NULL, `reps` INTEGER NOT NULL, `loadKg` REAL, `restSeconds` INTEGER NOT NULL, `warmup` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`exerciseId`) REFERENCES `routine_exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_planned_sets_exerciseId_position` ON `planned_sets` (`exerciseId`, `position`)",
            "CREATE TABLE IF NOT EXISTS `workout_sessions` (`id` TEXT NOT NULL, `routineId` TEXT, `routineNameSnapshot` TEXT NOT NULL, `status` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER, `activeMs` INTEGER NOT NULL, `anchorWallMs` INTEGER, `anchorRealtimeMs` INTEGER, `restTotalMs` INTEGER, `restRemainingMs` INTEGER, `currentExercise` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`routineId`) REFERENCES `routines`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
            "CREATE INDEX IF NOT EXISTS `index_workout_sessions_routineId` ON `workout_sessions` (`routineId`)",
            "CREATE INDEX IF NOT EXISTS `index_workout_sessions_status` ON `workout_sessions` (`status`)",
            "CREATE INDEX IF NOT EXISTS `index_workout_sessions_endedAt` ON `workout_sessions` (`endedAt`)",
            "CREATE TABLE IF NOT EXISTS `session_exercises` (`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `exerciseKey` TEXT NOT NULL, `position` INTEGER NOT NULL, `name` TEXT NOT NULL, `notes` TEXT NOT NULL, `catalogId` TEXT DEFAULT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`sessionId`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
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
