package com.maikol.fitnessapp.feature.training.ui

import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.feature.training.data.LoadText
import com.maikol.fitnessapp.feature.training.data.PlannedSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** H2-01 y H2-02: validación completa del editor y agrupado del detalle. */
class RoutineFormTest {

    private fun set(id: String, reps: String = "10", load: String = "", rest: String = "90") =
        SetForm(id, reps, LoadText(load, null, ""), rest, warmup = false)

    @Test
    fun emptyForm_reportsEveryFieldError() {
        val form = RoutineForm("r", " ", "", listOf(ExerciseForm("e", "k", "", "", listOf(set("s", reps = "")))))
        val errors = (form.validate(UnitSystem.METRIC) as FormResult.Invalid).errors
        assertTrue(errors.name)
        assertEquals(setOf("e"), errors.exerciseName)
        assertEquals(setOf("s"), errors.reps)
    }

    @Test
    fun invalidValues_areFlaggedPerSet() {
        val form = RoutineForm(
            "r", "Prueba", "",
            listOf(ExerciseForm("e", "k", "Remo", "", listOf(set("a", load = "-3"), set("b", rest = "9999"), set("c", reps = "abc")))),
        )
        val errors = (form.validate(UnitSystem.METRIC) as FormResult.Invalid).errors
        assertEquals(setOf("a"), errors.load)
        assertEquals(setOf("b"), errors.rest)
        assertEquals(setOf("c"), errors.reps)
    }

    @Test
    fun noExercisesOrNoSets_areInvalid() {
        assertTrue((RoutineForm("r", "Prueba", "", emptyList()).validate(UnitSystem.METRIC) as FormResult.Invalid).errors.noExercises)
        val noSets = RoutineForm("r", "Prueba", "", listOf(ExerciseForm("e", "k", "Remo", "", emptyList())))
        assertEquals(setOf("e"), (noSets.validate(UnitSystem.METRIC) as FormResult.Invalid).errors.noSets)
    }

    @Test
    fun validForm_keepsOrderTrimsAndConvertsRegionalDecimals() {
        val form = RoutineForm(
            "r", "  Prueba  ", "",
            listOf(
                ExerciseForm("e2", "k2", "Remo ", "", listOf(set("a", load = "22,5"), set("b", load = "", rest = ""))),
                ExerciseForm("e1", "k1", "Press", "", listOf(set("c", load = "20"))),
            ),
        )
        val routine = (form.validate(UnitSystem.METRIC) as FormResult.Valid).routine
        assertEquals("Prueba", routine.name)
        assertEquals(listOf("Remo", "Press"), routine.exercises.map { it.name })
        assertEquals(PlannedSet("a", 10, 22.5, 90, false), routine.exercises[0].sets[0])
        assertEquals(PlannedSet("b", 10, null, 0, false), routine.exercises[0].sets[1])
    }

    @Test
    fun grouped_mergesConsecutiveEqualSetsOnly() {
        val sets = listOf(
            PlannedSet("0", 12, 10.0, 60, true),
            PlannedSet("1", 10, 20.0, 90, false),
            PlannedSet("2", 10, 20.0, 90, false),
            PlannedSet("3", 8, 20.0, 90, false),
        )
        assertEquals(
            listOf(SetGroup(1, 12, 10.0, true), SetGroup(2, 10, 20.0, false), SetGroup(1, 8, 20.0, false)),
            sets.grouped(),
        )
    }
}
