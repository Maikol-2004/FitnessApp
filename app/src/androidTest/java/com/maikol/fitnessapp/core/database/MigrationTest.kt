package com.maikol.fitnessapp.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maikol.fitnessapp.feature.training.data.PlannedSet
import com.maikol.fitnessapp.feature.training.data.Routine
import com.maikol.fitnessapp.feature.training.data.RoutineExercise
import com.maikol.fitnessapp.feature.training.data.SystemTrainingClock
import com.maikol.fitnessapp.feature.training.data.TrainingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * H2-12: una base de datos v1 real (esquema de app/schemas/…/1.json, con su identity hash) se abre con la
 * app v2 sin perder `app_metadata` y con las tablas nuevas operativas. Sin fallback destructivo.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-test.db"

    @Before
    @After
    fun clean() {
        context.deleteDatabase(name)
    }

    private fun createV1() {
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `app_metadata` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'eb0bbe18501ebcc5d9f62aa645a0589c')")
            db.execSQL("INSERT INTO app_metadata(`key`, value) VALUES ('database_created_at', '1700000000000')")
            db.execSQL("INSERT INTO app_metadata(`key`, value) VALUES ('prueba', 'conservado')")
            db.version = 1
        }
    }

    @Test
    fun v1ToCurrent_keepsMetadata_andTrainingTablesWork() = runBlocking {
        createV1()
        val db = Room.databaseBuilder(context, FitnessDatabase::class.java, name).build()
        try {
            assertEquals("1700000000000", db.appMetadataDao().observeValue(FitnessDatabase.KEY_CREATED_AT).first())
            assertEquals("conservado", db.appMetadataDao().observeValue("prueba").first())
            assertEquals(3, db.openHelper.readableDatabase.version)

            val repo = TrainingRepository(db, SystemTrainingClock)
            val routine = Routine("r", "Prueba", "", false, listOf(RoutineExercise("e", "k", "Remo", "", listOf(PlannedSet("s", 10, 20.0, 90, false)))))
            repo.saveRoutine(routine)
            assertEquals(routine, repo.getRoutine("r"))
        } finally {
            db.close()
        }
    }
}
