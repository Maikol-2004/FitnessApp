package com.maikol.fitnessapp.feature.training

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maikol.fitnessapp.core.database.FitnessDatabase
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import com.maikol.fitnessapp.feature.training.data.PlannedSet
import com.maikol.fitnessapp.feature.training.data.Routine
import com.maikol.fitnessapp.feature.training.data.RoutineExercise
import com.maikol.fitnessapp.feature.training.data.StartResult
import com.maikol.fitnessapp.feature.training.data.SystemTrainingClock
import com.maikol.fitnessapp.feature.training.data.TrainingRepository
import com.maikol.fitnessapp.feature.training.ui.SessionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

/** H2-06: si Room no puede guardar la serie, no aparece el check y se muestra el error junto a la fila. */
@RunWith(AndroidJUnit4::class)
class SessionWriteFailureTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, FitnessDatabase::class.java).build()
    private val storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val storeFile = File(context.cacheDir, "write-failure-test.preferences_pb").apply { delete() }

    private class FailingRepository(db: FitnessDatabase) : TrainingRepository(db, SystemTrainingClock) {
        override suspend fun completeSet(sessionId: String, setId: String, reps: Int, loadKg: Double?): Boolean =
            throw IOException("disco lleno (simulado)")
    }

    @After
    fun close() {
        storeScope.cancel()
        db.close()
        storeFile.delete()
    }

    @Test
    fun failedWrite_showsErrorAndNoCheck() = runBlocking {
        val repo = FailingRepository(db)
        repo.saveRoutine(Routine("r", "Prueba", "", false, listOf(RoutineExercise("e", "k", "Remo", "", listOf(PlannedSet("s", 10, 20.0, 90, false))))))
        val sessionId = (repo.startSession("r") as StartResult.Started).sessionId
        val prefs = UserPreferencesRepository(PreferenceDataStoreFactory.create(scope = storeScope) { storeFile })

        withContext(Dispatchers.Main) {
            val vm = SessionViewModel(SavedStateHandle(mapOf(SessionViewModel.ARG_SESSION_ID to sessionId)), repo, prefs)
            val collector = launch { vm.state.collect {} }
            val set = withTimeout(5_000) { vm.state.first { it.session != null } }.session!!.exercises[0].sets[0]

            vm.complete(set)
            val after = withTimeout(5_000) { vm.state.first { set.id in it.failed } }

            assertFalse(after.session!!.exercises[0].sets[0].completed)
            assertEquals(0, after.session!!.completedSets)
            assertTrue(set.id !in after.pending)
            collector.cancel()
        }
    }
}
