package com.maikol.fitnessapp.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maikol.fitnessapp.core.tracking.SleepSaveResult
import com.maikol.fitnessapp.core.tracking.TrackingRepository
import com.maikol.fitnessapp.feature.training.data.SystemTrainingClock
import com.maikol.fitnessapp.feature.training.data.TrainingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * F1: una base de datos v2 real (esquema app/schemas/…/2.json con su identity hash) con datos H2 «Prueba»
 * migra a v3 sin perder rutinas, sesiones ni metadatos; las tablas F1 quedan operativas.
 */
@RunWith(AndroidJUnit4::class)
class MigrationV3Test {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-v3-test.db"

    @Before
    @After
    fun clean() {
        context.deleteDatabase(name)
    }

    private fun createV2(endedAt: Long) {
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            V2_SCHEMA.forEach(db::execSQL)
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '496e95fbec8151d075e2690c20b9049f')")
            db.execSQL("INSERT INTO app_metadata(`key`, value) VALUES ('prueba', 'conservado')")
            db.execSQL("INSERT INTO routines VALUES ('r1', 'Prueba torso', '', 1, 1, NULL)")
            db.execSQL("INSERT INTO routine_exercises VALUES ('e1', 'r1', 'k', 0, 'Remo', '')")
            db.execSQL("INSERT INTO planned_sets VALUES ('p1', 'e1', 0, 10, 20.0, 90, 0)")
            db.execSQL(
                "INSERT INTO workout_sessions VALUES ('s1', 'r1', 'Prueba torso', 'COMPLETED', 1000, $endedAt, 60000, NULL, NULL, NULL, NULL, 0)",
            )
            db.execSQL("INSERT INTO session_exercises VALUES ('se1', 's1', 'k', 0, 'Remo', '')")
            db.execSQL("INSERT INTO performed_sets VALUES ('ps1', 'se1', 0, 10, 20.0, 90, 0, 10, 20.0, 2000)")
            db.execSQL("INSERT INTO performed_sets VALUES ('ps2', 'se1', 1, 10, 20.0, 90, 0, 10, 20.0, 3000)")
            db.version = 2
        }
    }

    @Test
    fun v2To3_keepsTraining_andTrackingTablesWork() = runBlocking {
        createV2(System.currentTimeMillis())
        val db = Room.databaseBuilder(context, FitnessDatabase::class.java, name).build()
        try {
            assertEquals("conservado", db.appMetadataDao().observeValue("prueba").first())
            val training = TrainingRepository(db, SystemTrainingClock)
            assertEquals("Prueba torso", training.getRoutine("r1")?.name)
            val history = training.observeHistory().first()
            assertEquals(1, history.size)
            assertEquals(400.0, history.single().volumeKg, 0.0)
            assertEquals(6, db.openHelper.readableDatabase.version)

            val zone = ZoneId.of("Europe/Madrid")
            val tracking = TrackingRepository(db, zone = { zone })
            val day = LocalDate.of(2026, 11, 25)
            val first = tracking.addWater(day, 250)
            tracking.addWater(day, 500)
            assertEquals(750, tracking.observeWater(day).first().sumOf { it.ml })
            tracking.deleteWater(first)
            assertEquals(500, tracking.observeWater(day).first().sumOf { it.ml })

            val start = TrackingRepository.epochOf(day.minusDays(1), LocalTime.of(23, 0), zone)
            val end = TrackingRepository.epochOf(day, LocalTime.of(7, 0), zone)
            assertEquals(SleepSaveResult.Saved, tracking.saveSleep(null, start, end, 3, null))
            val overlap = tracking.saveSleep(null, start + 3_600_000, end + 3_600_000, null, null)
            assertTrue(overlap is SleepSaveResult.Overlap)
            assertEquals(1, tracking.observeRecentSleep().first().size)
        } finally {
            db.close()
        }
    }

    private companion object {
        val V2_SCHEMA = listOf(
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
        )
    }
}
