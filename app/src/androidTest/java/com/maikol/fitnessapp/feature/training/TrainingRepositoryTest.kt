package com.maikol.fitnessapp.feature.training

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maikol.fitnessapp.core.database.FitnessDatabase
import com.maikol.fitnessapp.core.database.SessionStatus
import com.maikol.fitnessapp.feature.training.data.ClockReading
import com.maikol.fitnessapp.feature.training.data.FinishResult
import com.maikol.fitnessapp.feature.training.data.PlannedSet
import com.maikol.fitnessapp.feature.training.data.Routine
import com.maikol.fitnessapp.feature.training.data.RoutineExercise
import com.maikol.fitnessapp.feature.training.data.SessionTiming
import com.maikol.fitnessapp.feature.training.data.StartResult
import com.maikol.fitnessapp.feature.training.data.TrainingClock
import com.maikol.fitnessapp.feature.training.data.TrainingRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** H2-01, H2-03, H2-04, H2-05, H2-07, H2-09, H2-10 y H2-11 sobre Room real (en memoria) con reloj controlado. */
@RunWith(AndroidJUnit4::class)
class TrainingRepositoryTest {

    private class FakeClock : TrainingClock {
        var wall = 1_700_000_000_000L
        var realtime = 10_000L
        fun advance(ms: Long) { wall += ms; realtime += ms }
        override fun now() = ClockReading(wall, realtime)
    }

    private lateinit var db: FitnessDatabase
    private lateinit var repo: TrainingRepository
    private val clock = FakeClock()

    private val routine = Routine(
        "r1", "Prueba torso", "", archived = false,
        exercises = listOf(
            RoutineExercise("e1", "k1", "Remo con barra", "", listOf(PlannedSet("p1", 10, 20.0, 90, false), PlannedSet("p2", 10, 20.0, 90, false))),
            RoutineExercise("e2", "k2", "Press banca", "Agarre medio", listOf(PlannedSet("p3", 8, 22.5, 120, false))),
        ),
    )

    @Before
    fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), FitnessDatabase::class.java).build()
        repo = TrainingRepository(db, clock)
    }

    @After
    fun close() = db.close()

    private suspend fun started(): String {
        repo.saveRoutine(routine)
        return (repo.startSession(routine.id) as StartResult.Started).sessionId
    }

    private suspend fun session(id: String) = repo.observeSession(id).first()!!

    @Test
    fun saveAndReopen_returnsIdenticalRoutine_andReorderIsKept() = runBlocking {
        repo.saveRoutine(routine)
        assertEquals(routine, repo.getRoutine("r1"))
        val reordered = routine.copy(exercises = routine.exercises.reversed())
        repo.saveRoutine(reordered)
        assertEquals(reordered, repo.getRoutine("r1"))
    }

    @Test
    fun duplicate_createsNewIds_andArchiveKeepsHistory() = runBlocking {
        val id = started()
        repo.finish(id)
        val copyId = repo.duplicateRoutine("r1", " (copia)")!!
        val copy = repo.getRoutine(copyId)!!
        assertNotEquals("r1", copyId)
        assertEquals("Prueba torso (copia)", copy.name)
        val originalIds = routine.exercises.flatMap { e -> listOf(e.id) + e.sets.map { it.id } }.toSet()
        copy.exercises.flatMap { e -> listOf(e.id) + e.sets.map { it.id } }.forEach { assertFalse(it in originalIds) }
        assertEquals(routine.exercises.map { it.name }, copy.exercises.map { it.name })

        repo.setArchived("r1", true)
        assertTrue(repo.getRoutine("r1")!!.archived)
        assertEquals(1, repo.observeHistory().first().size)
    }

    @Test
    fun concurrentStarts_createOnlyOneOpenSession() = runBlocking {
        repo.saveRoutine(routine)
        val results = List(5) { async { repo.startSession("r1") } }.awaitAll()
        assertEquals(1, results.count { it is StartResult.Started })
        val ids = results.map { (it as? StartResult.Started)?.sessionId ?: (it as StartResult.AlreadyOpen).sessionId }.toSet()
        assertEquals(1, ids.size)
        // El plan se copia: series y valores prellenados.
        val s = session(ids.single())
        assertEquals(3, s.totalSets)
        assertEquals(listOf(10, 10), s.exercises[0].sets.map { it.reps })
    }

    @Test
    fun completeTwice_isIdempotent_andRestStartsOnce() = runBlocking {
        val id = started()
        val set = session(id).exercises[0].sets[0]
        assertTrue(repo.completeSet(id, set.id, 10, 20.0))
        clock.advance(30_000)
        assertFalse(repo.completeSet(id, set.id, 10, 20.0))
        val s = session(id)
        assertEquals(1, s.completedSets)
        // El descanso no se reinició con el segundo toque: quedan 60 s, no 90.
        assertEquals(60_000L, SessionTiming.restRemainingMs(s.timer, clock.now()))
    }

    @Test
    fun volume_400_thenEditToEightReps_360() = runBlocking {
        val id = started()
        val sets = session(id).exercises[0].sets
        sets.forEach { repo.completeSet(id, it.id, 10, 20.0) }
        assertEquals(400.0, session(id).volumeKg, 0.0)
        repo.updateSetValues(sets[1].id, 8, 20.0)
        assertEquals(360.0, session(id).volumeKg, 0.0)
    }

    @Test
    fun pauseFreezesTime_resumeContinues() = runBlocking {
        val id = started()
        clock.advance(40_000)
        repo.pause(id)
        clock.advance(600_000)
        assertEquals(SessionStatus.PAUSED, session(id).status)
        assertEquals(40_000, SessionTiming.elapsedMs(session(id).timer, clock.now()))
        repo.resume(id)
        clock.advance(5_000)
        assertEquals(45_000, SessionTiming.elapsedMs(session(id).timer, clock.now()))
    }

    @Test
    fun finishTwice_givesOneHistoryEntry() = runBlocking {
        val id = started()
        val set = session(id).exercises[0].sets[0]
        repo.completeSet(id, set.id, 10, 20.0)
        clock.advance(60_000)
        val results = listOf(async { repo.finish(id) }, async { repo.finish(id) }).awaitAll()
        assertEquals(setOf(FinishResult.Completed, FinishResult.AlreadyCompleted), results.toSet())
        val history = repo.observeHistory().first()
        assertEquals(1, history.size)
        assertEquals(200.0, history.single().volumeKg, 0.0)
        assertEquals(60_000, history.single().durationMs)
        assertEquals(null, repo.observeOpenSession().first())
    }

    @Test
    fun discarded_isExcludedFromHistory_andFreesStart() = runBlocking {
        val id = started()
        repo.discard(id)
        assertEquals(SessionStatus.DISCARDED, session(id).status)
        assertTrue(repo.observeHistory().first().isEmpty())
        assertTrue(repo.startSession("r1") is StartResult.Started)
    }

    @Test
    fun editingRoutineAfterwards_leavesHistoryUnchanged() = runBlocking {
        val id = started()
        repo.completeSet(id, session(id).exercises[0].sets[0].id, 10, 20.0)
        repo.finish(id)
        val before = session(id)
        repo.saveRoutine(routine.copy(name = "Otra", exercises = listOf(routine.exercises[1].copy(name = "Cambiado"))))
        val after = session(id)
        assertEquals(before, after)
        assertEquals("Prueba torso", repo.observeHistory().first().single().routineName)
        assertEquals("Remo con barra", after.exercises[0].name)
    }
}
