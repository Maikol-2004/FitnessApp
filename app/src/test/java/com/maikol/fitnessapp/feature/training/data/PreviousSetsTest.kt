package com.maikol.fitnessapp.feature.training.data

import com.maikol.fitnessapp.core.database.ExerciseSetRow
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.feature.training.catalog.AssistMode
import com.maikol.fitnessapp.feature.training.catalog.LoadSemantics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** V2: referencia «Anterior» de la sesión activa y carga explícita. */
class PreviousSetsTest {

    private fun row(
        session: String,
        endedAt: Long,
        set: Int,
        reps: Int?,
        kg: Double?,
        warmup: Boolean = false,
        exercisePos: Int = 0,
        mode: String? = null,
    ) = ExerciseSetRow(session, "Prueba", endedAt, exercisePos, set, reps, kg, warmup, mode)

    @Test
    fun noRows_meansNoPrevious() {
        assertNull(previousSession(emptyList(), LoadSemantics.EXTERNAL, null))
    }

    @Test
    fun takesLatestFinishedSession_inSetOrder() {
        val rows = listOf(
            row("old", 100, 0, 10, 30.0),
            row("new", 200, 1, 9, 40.0),
            row("new", 200, 0, 10, 40.0),
        )
        val prev = previousSession(rows, LoadSemantics.EXTERNAL, null)!!
        assertEquals("new", prev.sessionId)
        assertEquals(listOf(10, 9), prev.sets.map { it.reps })
    }

    @Test
    fun warmupAndWork_areMatchedSeparately() {
        val rows = listOf(
            row("s", 1, 0, 12, 20.0, warmup = true),
            row("s", 1, 1, 10, 40.0),
            row("s", 1, 2, 8, 45.0),
        )
        val prev = previousSession(rows, LoadSemantics.EXTERNAL, null)!!
        assertEquals(PreviousSet(12, 20.0, true), prev.counterpart(warmup = true, ordinal = 0))
        assertEquals(PreviousSet(10, 40.0, false), prev.counterpart(warmup = false, ordinal = 0))
        assertEquals(PreviousSet(8, 45.0, false), prev.counterpart(warmup = false, ordinal = 1))
        assertNull(prev.counterpart(warmup = true, ordinal = 1))
        assertNull(prev.counterpart(warmup = false, ordinal = 2))
    }

    @Test
    fun repeatedExerciseInSession_usesFirstAppearance() {
        val rows = listOf(row("s", 1, 0, 5, 50.0, exercisePos = 3), row("s", 1, 0, 10, 40.0, exercisePos = 1))
        assertEquals(listOf(10), previousSession(rows, LoadSemantics.EXTERNAL, null)!!.sets.map { it.reps })
    }

    @Test
    fun assistance_onlyComparesSameMode_neverMixes() {
        val rows = listOf(
            row("maquina", 100, 0, 8, 30.0, mode = AssistMode.MACHINE.name),
            row("banda", 200, 0, 6, 20.0, mode = AssistMode.BAND.name),
            row("sin", 300, 0, 5, 10.0, mode = null),
        )
        assertNull(previousSession(rows, LoadSemantics.ASSISTANCE, null))
        assertEquals("maquina", previousSession(rows, LoadSemantics.ASSISTANCE, AssistMode.MACHINE)!!.sessionId)
        assertEquals("banda", previousSession(rows, LoadSemantics.ASSISTANCE, AssistMode.BAND)!!.sessionId)
        assertNull(previousSession(rows.take(1), LoadSemantics.ASSISTANCE, AssistMode.BAND))
    }

    @Test
    fun explicitLoad_keepsZero_absentIsNull() {
        assertEquals("0 kg", formatExplicitLoad(0.0, UnitSystem.METRIC))
        assertEquals("22,5 kg", formatExplicitLoad(22.5, UnitSystem.METRIC))
        assertNull(formatExplicitLoad(null, UnitSystem.METRIC))
        // La lectura general no cambia: 0 sigue siendo «Sin carga» donde ya se usaba.
        assertNull(formatLoad(0.0, UnitSystem.METRIC))
    }
}
