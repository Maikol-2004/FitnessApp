package com.maikol.fitnessapp.feature.training

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maikol.fitnessapp.core.database.FitnessDatabase
import com.maikol.fitnessapp.core.tracking.weekTraining
import com.maikol.fitnessapp.feature.training.catalog.ASSISTED_PULL_UP_ID
import com.maikol.fitnessapp.feature.training.catalog.AssistMode
import com.maikol.fitnessapp.feature.training.catalog.LoadSemantics
import com.maikol.fitnessapp.feature.training.data.PlannedSet
import com.maikol.fitnessapp.feature.training.data.Routine
import com.maikol.fitnessapp.feature.training.data.RoutineExercise
import com.maikol.fitnessapp.feature.training.data.StartResult
import com.maikol.fitnessapp.feature.training.data.SystemTrainingClock
import com.maikol.fitnessapp.feature.training.data.TrainingRepository
import com.maikol.fitnessapp.feature.training.data.exerciseProgress
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/**
 * G2-04/05 con la consulta real de Room: dos rutinas con el mismo catalogId, un personalizado con el mismo
 * nombre (sin catalogId), una sesión descartada, otra abierta, calentamientos y series pendientes.
 * Base en memoria con datos «Prueba»: no toca los datos reales de la app.
 */
@RunWith(AndroidJUnit4::class)
class ExerciseProgressDbTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, FitnessDatabase::class.java).build()
    private val repo = TrainingRepository(db, SystemTrainingClock)
    private val bench = "chest_barbell_bench_press"

    @After
    fun close() = db.close()

    private suspend fun routine(id: String, name: String, catalogId: String?, sets: List<PlannedSet>) {
        repo.saveRoutine(
            Routine(id, name, "", archived = false, exercises = listOf(RoutineExercise("$id-e", "$id-k", "Press banca con barra", "", sets, catalogId))),
        )
    }

    private fun planned(id: String, reps: Int, kg: Double?, warmup: Boolean = false) = PlannedSet(id, reps, kg, 0, warmup)

    /** Inicia, completa las series indicadas (índice → reps, carga) y termina o descarta. */
    private suspend fun train(routineId: String, complete: Map<Int, Pair<Int, Double?>>, finish: Boolean = true): String {
        val id = (repo.startSession(routineId) as StartResult.Started).sessionId
        val session = repo.observeSession(id).first()!!
        val sets = session.exercises.single().sets
        complete.forEach { (i, v) -> assertTrue(repo.completeSet(id, sets[i].id, v.first, v.second)) }
        if (finish) repo.finish(id) else repo.discard(id)
        return id
    }

    @Test
    fun progressByCatalogId_realQuery() = runBlocking {
        routine("r1", "Prueba torso", bench, listOf(planned("a", 10, 20.0, warmup = true), planned("b", 10, 40.0), planned("c", 8, 50.0)))
        routine("r2", "Prueba pecho", bench, listOf(planned("d", 6, 55.0), planned("e", 6, 55.0)))
        routine("r3", "Prueba personal", null, listOf(planned("f", 5, 200.0)))

        // Doc fixture en una sesión: calentamiento + 40×10 + 50×8.
        train("r1", mapOf(0 to (10 to 20.0), 1 to (10 to 40.0), 2 to (8 to 50.0)))
        var p = exerciseProgress(repo.observeCatalogExerciseSets(bench).first(), LoadSemantics.EXTERNAL)
        assertEquals(50.0, p.maxLoad!!.loadKg, 0.0)
        assertEquals(800.0, p.bestVolume!!.volumeKg, 0.0)
        assertEquals(45.0, p.averageLoad!!.loadKg, 0.0)

        // Descartada con 100 kg y personalizado homónimo de 200 kg: no cuentan.
        train("r1", mapOf(1 to (10 to 100.0)), finish = false)
        train("r3", mapOf(0 to (5 to 200.0)))
        // Segunda rutina con el mismo catalogId: una serie completada y otra pendiente (objetivo 55 kg).
        train("r2", mapOf(0 to (6 to 55.0)))

        val rows = withTimeout(5_000) { repo.observeCatalogExerciseSets(bench).first { r -> r.map { it.sessionId }.distinct().size == 2 } }
        p = exerciseProgress(rows, LoadSemantics.EXTERNAL)
        assertEquals(55.0, p.maxLoad!!.loadKg, 0.0)
        assertEquals(6, p.maxLoad!!.reps)
        assertEquals(800.0, p.bestVolume!!.volumeKg, 0.0)
        assertEquals((40.0 + 50.0 + 55.0) / 3, p.averageLoad!!.loadKg, 1e-9)
        assertEquals(listOf("Prueba pecho", "Prueba torso"), p.history.map { it.routineName })
        assertEquals(1, p.history.first().sets.size) // la serie pendiente no aparece

        // El personalizado sigue consultándose por su exerciseKey, sin mezclarse.
        val custom = exerciseProgress(repo.observeCustomExerciseSets("r3-k").first(), LoadSemantics.EXTERNAL)
        assertEquals(200.0, custom.maxLoad!!.loadKg, 0.0)
        assertEquals(1, custom.history.size)
    }

    /**
     * Volumen tras G2.1: la asistencia no es carga levantada. Sesión mixta (press 40 × 10 + dominada asistida
     * 30 × 8 en máquina y 0 × 8) con calentamiento y serie pendiente, y otra con un registro asistido antiguo
     * (sin modalidad). Sesión activa, resumen, historial, total semanal y progreso por ejercicio deben coincidir.
     */
    @Test
    fun volume_excludesAssistance_consistentEverywhere() = runBlocking {
        val pullUp = ASSISTED_PULL_UP_ID
        repo.saveRoutine(
            Routine(
                "mix", "Prueba mixta", "", archived = false,
                exercises = listOf(
                    RoutineExercise("mix-b", "mix-bk", "Press banca con barra", "", listOf(planned("w", 10, 20.0, warmup = true), planned("b1", 10, 40.0), planned("b2", 10, 40.0)), bench),
                    RoutineExercise("mix-p", "mix-pk", "Dominada asistida", "", listOf(planned("p1", 8, 30.0), planned("p2", 8, 0.0)), pullUp, AssistMode.MACHINE),
                ),
            ),
        )
        repo.saveRoutine(
            Routine("old", "Prueba antigua", "", archived = false, exercises = listOf(RoutineExercise("old-p", "old-pk", "Dominada asistida", "", listOf(planned("o1", 8, 20.0)), pullUp))),
        )

        val id = (repo.startSession("mix") as StartResult.Started).sessionId
        val (press, assisted) = repo.observeSession(id).first()!!.exercises
        assertTrue(repo.completeSet(id, press.sets[0].id, 10, 20.0)) // calentamiento
        assertTrue(repo.completeSet(id, press.sets[1].id, 10, 40.0)) // press.sets[2] queda pendiente
        assertTrue(repo.completeSet(id, assisted.sets[0].id, 8, 30.0))
        assertTrue(repo.completeSet(id, assisted.sets[1].id, 8, 0.0))
        val active = withTimeout(5_000) { repo.observeSession(id).first { it!!.completedSets == 4 } }!!
        assertEquals("sesión activa", 400.0, active.volumeKg, 0.0)
        repo.finish(id)
        val summary = withTimeout(5_000) { repo.observeSession(id).first { it?.isOpen == false } }!!
        assertEquals("resumen", 400.0, summary.volumeKg, 0.0)
        assertEquals(4, summary.completedSets)

        // Registro asistido antiguo, sin modalidad: no se reclasifica ni suma volumen.
        val oldId = (repo.startSession("old") as StartResult.Started).sessionId
        assertTrue(repo.completeSet(oldId, repo.observeSession(oldId).first()!!.exercises.single().sets.single().id, 8, 20.0))
        repo.finish(oldId)

        val history = withTimeout(5_000) { repo.observeHistory().first { it.size == 2 } }
        val byId = history.associateBy { it.id }
        assertEquals("historial", 400.0, byId.getValue(id).volumeKg, 0.0)
        assertEquals(4, byId.getValue(id).completedSets)
        assertEquals(2, byId.getValue(id).exerciseCount)
        assertEquals(0.0, byId.getValue(oldId).volumeKg, 0.0)
        assertEquals(1, byId.getValue(oldId).completedSets)
        val zone = ZoneId.systemDefault()
        assertEquals("total semanal (Progreso)", 400.0, weekTraining(history, LocalDate.now(zone), zone).volumeKg, 0.0)

        // Progreso por ejercicio: mismo volumen del press; la asistencia conserva series, 0 y evolución.
        val pressProgress = exerciseProgress(repo.observeCatalogExerciseSets(bench).first(), LoadSemantics.EXTERNAL)
        assertEquals(400.0, pressProgress.bestVolume!!.volumeKg, 0.0)
        val assist = exerciseProgress(repo.observeCatalogExerciseSets(pullUp).first(), LoadSemantics.ASSISTANCE)
        assertNull(assist.bestVolume)
        assertEquals(listOf(AssistMode.MACHINE), assist.assistSeries.map { it.mode })
        assertEquals(listOf(0.0), assist.assistSeries.single().points.map { it.valueKg })
        assertEquals(1, assist.unknownAssistSessions)
        assertEquals(listOf(30.0, 0.0), assist.history.first { it.sessionId == id }.sets.map { it.loadKg })
        assertEquals(listOf(20.0), assist.history.first { it.sessionId == oldId }.sets.map { it.loadKg })
    }
}
