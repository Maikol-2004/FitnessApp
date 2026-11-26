package com.maikol.fitnessapp.feature.training.data

import com.maikol.fitnessapp.core.database.ExerciseSetRow
import com.maikol.fitnessapp.feature.training.catalog.AssistMode
import com.maikol.fitnessapp.feature.training.catalog.LoadSemantics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** G2-04/05: definiciones de docs/14 con fixtures (nunca cargados en datos reales). */
class ExerciseProgressTest {
    private val day = 86_400_000L
    private val now = 100 * day

    private fun set(
        session: String,
        endedAt: Long,
        reps: Int?,
        load: Double?,
        warmup: Boolean = false,
        pos: Int = 0,
        exercisePos: Int = 0,
        routine: String = "Prueba A",
        mode: AssistMode? = null,
    ) = ExerciseSetRow(session, routine, endedAt, exercisePos, pos, reps, load, warmup, mode?.name)

    @Test
    fun docFixture_40x10_50x8_gives50_800_45_andWarmupDoesNotChangeIt() {
        val rows = listOf(
            set("s1", now - day, 10, 20.0, warmup = true, pos = 0),
            set("s1", now - day, 10, 40.0, pos = 1),
            set("s1", now - day, 8, 50.0, pos = 2),
        )
        val p = exerciseProgress(rows, LoadSemantics.EXTERNAL, nowMs = now)
        assertEquals(50.0, p.maxLoad!!.loadKg, 0.0)
        assertEquals(8, p.maxLoad!!.reps)
        assertEquals(800.0, p.bestVolume!!.volumeKg, 0.0)
        assertEquals(2, p.bestVolume!!.sets)
        assertEquals(45.0, p.averageLoad!!.loadKg, 0.0)
        assertEquals(2, p.averageLoad!!.sets)
        assertEquals(2, p.workSetsInPeriod)
        // La serie de calentamiento aparece en el historial, marcada, pero no en las marcas.
        assertEquals(3, p.history.single().sets.size)
    }

    @Test
    fun twoRoutines_sameCatalogId_andRepeatedInOneSession() {
        val rows = listOf(
            set("s1", now - 10 * day, 10, 40.0, routine = "Prueba torso"),
            set("s2", now - 2 * day, 5, 60.0, routine = "Prueba pecho"),
            // Mismo ejercicio dos veces en la sesión s2: el volumen de la sesión se acumula.
            set("s2", now - 2 * day, 10, 30.0, exercisePos = 3),
        )
        val p = exerciseProgress(rows, LoadSemantics.EXTERNAL, nowMs = now)
        assertEquals(2, p.sessionsInPeriod)
        assertEquals(listOf("s2", "s1"), p.history.map { it.sessionId })
        assertEquals(600.0, p.history.first().volumeKg!!, 0.0)
        assertEquals(600.0, p.bestVolume!!.volumeKg, 0.0)
        assertEquals(listOf(40.0, 60.0), p.chart.map { it.valueKg })
        assertEquals("s1", p.firstPractice!!.sessionId)
        assertEquals("s2", p.lastPractice!!.sessionId)
    }

    @Test
    fun nullLoad_isNotZero_bodyweightHasNoRecord() {
        val rows = listOf(set("s1", now - day, 15, null), set("s1", now - day, 12, 0.0, pos = 1))
        val p = exerciseProgress(rows, LoadSemantics.EXTERNAL, nowMs = now)
        assertTrue(p.hasPractice)
        assertEquals(2, p.workSetsInPeriod)
        assertNull(p.maxLoad)
        assertNull(p.bestVolume)
        assertNull(p.averageLoad)
        assertTrue(p.chart.isEmpty())
        assertNull(p.history.single().volumeKg)
    }

    @Test
    fun noRows_isEmpty_notZero() {
        val p = exerciseProgress(emptyList(), LoadSemantics.EXTERNAL, nowMs = now)
        assertFalse(p.hasPractice)
        assertNull(p.maxLoad)
        assertTrue(p.history.isEmpty())
    }

    @Test
    fun warmupOnlySession_isNotAPractice() {
        val p = exerciseProgress(listOf(set("s1", now - day, 10, 20.0, warmup = true)), LoadSemantics.EXTERNAL, nowMs = now)
        assertFalse(p.hasPractice)
        assertEquals(1, p.history.size)
        assertEquals(0, p.history.single().workSets)
    }

    @Test
    fun assistance_hasNoRecordOrVolume_butShowsEvolution() {
        val rows = listOf(
            set("s1", now - 5 * day, 8, 30.0, mode = AssistMode.MACHINE),
            set("s1", now - 5 * day, 8, 25.0, pos = 1, mode = AssistMode.MACHINE),
            set("s2", now - day, 8, 20.0, mode = AssistMode.MACHINE),
        )
        val p = exerciseProgress(rows, LoadSemantics.ASSISTANCE, nowMs = now)
        assertNull(p.maxLoad)
        assertNull(p.bestVolume)
        assertNull(p.averageLoad)
        assertTrue(p.history.all { it.volumeKg == null })
        assertTrue(p.chart.isEmpty())
        assertEquals(listOf(AssistMode.MACHINE), p.assistSeries.map { it.mode })
        assertEquals(listOf(25.0, 20.0), p.assistSeries.single().points.map { it.valueKg })
        assertEquals(3, p.workSetsInPeriod)
    }

    @Test
    fun assistance_machineAndBand_areSeparateSeries_unknownIsNotCharted() {
        val rows = listOf(
            set("old", now - 9 * day, 8, 40.0), // anterior a G2.1: sin indicar
            set("m", now - 5 * day, 8, 30.0, mode = AssistMode.MACHINE),
            set("b", now - 3 * day, 8, 15.0, mode = AssistMode.BAND),
            set("m2", now - day, 8, 25.0, mode = AssistMode.MACHINE),
        )
        val p = exerciseProgress(rows, LoadSemantics.ASSISTANCE, nowMs = now)
        assertEquals(listOf(AssistMode.MACHINE, AssistMode.BAND), p.assistSeries.map { it.mode })
        assertEquals(listOf(30.0, 25.0), p.assistSeries[0].points.map { it.valueKg })
        assertEquals(listOf(15.0), p.assistSeries[1].points.map { it.valueKg })
        assertEquals(1, p.unknownAssistSessions)
        // El registro sin modalidad se conserva tal cual en el historial: no se reclasifica.
        assertEquals(listOf<AssistMode?>(null), p.history.last().sets.map { it.assistMode })
        assertEquals(4, p.sessionsInPeriod)
    }

    @Test
    fun assistance_zeroIsAValue_nullIsNot() {
        val rows = listOf(
            set("a", now - 3 * day, 8, 10.0, mode = AssistMode.BAND),
            set("b", now - day, 6, 0.0, mode = AssistMode.BAND),
            set("b", now - day, 6, null, pos = 1, mode = AssistMode.BAND),
        )
        val p = exerciseProgress(rows, LoadSemantics.ASSISTANCE, nowMs = now)
        assertEquals(listOf(10.0, 0.0), p.assistSeries.single().points.map { it.valueKg })
        assertEquals(listOf(0.0, null), p.history.first().sets.map { it.loadKg })
        assertEquals(2, p.history.first().workSets)
    }

    @Test
    fun period_filtersMetricsAndChart_firstPracticeStaysGlobal() {
        val rows = listOf(
            set("old", now - 200 * day, 5, 80.0),
            set("new", now - 10 * day, 5, 60.0),
        )
        val p = exerciseProgress(rows, LoadSemantics.EXTERNAL, ProgressPeriod.DAYS_30, nowMs = now)
        assertEquals(60.0, p.maxLoad!!.loadKg, 0.0)
        assertEquals(1, p.sessionsInPeriod)
        assertEquals(1, p.chart.size)
        assertEquals("old", p.firstPractice!!.sessionId)
        assertEquals(2, p.history.size)
        assertEquals(80.0, exerciseProgress(rows, LoadSemantics.EXTERNAL, ProgressPeriod.ALL, now).maxLoad!!.loadKg, 0.0)
    }

    @Test
    fun maxLoadTie_keepsFirstTimeReached() {
        val rows = listOf(set("a", now - 5 * day, 6, 50.0), set("b", now - day, 9, 50.0))
        val p = exerciseProgress(rows, LoadSemantics.EXTERNAL, nowMs = now)
        assertEquals(now - 5 * day, p.maxLoad!!.endedAt)
        assertEquals(6, p.maxLoad!!.reps)
    }
}
