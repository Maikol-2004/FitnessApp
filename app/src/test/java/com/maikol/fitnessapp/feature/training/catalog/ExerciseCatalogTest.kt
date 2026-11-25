package com.maikol.fitnessapp.feature.training.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** G1-02: integridad del catálogo, búsqueda sin tildes con alias y filtros combinados. */
class ExerciseCatalogTest {
    private val all = ExerciseCatalog.all

    @Test
    fun catalog_hasFiftyComplete_uniqueIds_allGroups() {
        assertTrue("≥50 ejercicios", all.size >= 50)
        assertEquals("IDs únicos", all.size, all.map { it.id }.toSet().size)
        assertEquals("nombres únicos", all.size, all.map { normalizeForSearch(it.name) }.toSet().size)
        MuscleGroup.entries.forEach { g -> assertTrue("grupo $g", all.count { it.primary == g } >= 5) }
        all.forEach { e ->
            assertTrue(e.id, e.id.matches(Regex("[a-z0-9_]+")))
            assertTrue(e.id, e.name.isNotBlank() && e.preparation.isNotBlank() && e.breathing.isNotBlank())
            assertTrue("${e.id}: 3–5 pasos", e.steps.size in 3..5)
            assertTrue("${e.id}: 2–3 errores", e.mistakes.size in 2..3)
            assertTrue("${e.id}: equipo", e.equipment.isNotEmpty())
            assertTrue("${e.id}: principal fuera de secundarios", e.primary !in e.secondary)
        }
        // Errores redactados por ejercicio: ninguno se repite literalmente en todo el catálogo.
        val mistakes = all.flatMap { it.mistakes }
        assertEquals(mistakes.size, mistakes.toSet().size)
        assertTrue("no todos principiantes", Difficulty.entries.all { d -> all.any { it.difficulty == d } })
    }

    @Test
    fun byId_isStable() {
        assertEquals("Elevaciones laterales", ExerciseCatalog.byId("shoulders_dumbbell_lateral_raise")?.name)
        assertEquals(null, ExerciseCatalog.byId("no_existe"))
        assertEquals(null, ExerciseCatalog.byId(null))
    }

    @Test
    fun search_ignoresCaseAndAccents_andMatchesAliases() {
        assertEquals("biceps", normalizeForSearch("  BÍCEPS "))
        val hombro = all.search("hombro", null, null)
        assertTrue(hombro.any { it.id == "shoulders_seated_dumbbell_press" })
        assertTrue("por músculo principal", hombro.any { it.id == "shoulders_dumbbell_lateral_raise" })
        assertEquals(all.search("biceps", null, null), all.search("Bíceps", null, null))
        assertTrue("alias", all.search("pec deck", null, null).any { it.id == "chest_machine_fly" })
        assertTrue("alias con tilde", all.search("rompecraneos", null, null).any { it.id == "triceps_ez_skull_crusher" })
        assertTrue("varias palabras en cualquier orden", all.search("polea lateral", null, null).any { it.id == "shoulders_cable_lateral_raise" })
    }

    @Test
    fun filters_combineWithAnd_secondaryIncluded_primaryFirst() {
        val shoulders = all.search("", MuscleGroup.SHOULDERS, null)
        assertTrue(shoulders.take(5).all { it.primary == MuscleGroup.SHOULDERS })
        assertTrue("compuesto en varios filtros", shoulders.any { it.id == "chest_barbell_bench_press" })
        val shouldersCable = all.search("", MuscleGroup.SHOULDERS, Equipment.CABLE)
        assertTrue(shouldersCable.isNotEmpty())
        assertTrue(shouldersCable.all { MuscleGroup.SHOULDERS in it.muscles && Equipment.CABLE in it.allEquipment })
        assertTrue("equipo de apoyo cuenta", all.search("", null, Equipment.BENCH).any { it.id == "chest_dumbbell_bench_press" })
        assertEquals(emptyList<CatalogExercise>(), all.search("hombro", MuscleGroup.CALVES, Equipment.BAND))
        assertEquals(emptyList<CatalogExercise>(), all.search("zzz", null, null))
        assertEquals(all, all.search("  ", null, null))
    }

    @Test
    fun template_isRepsOnly_withEditableRest() {
        assertEquals(SetTemplate(3, 8, 120), ExerciseCatalog.byId("quads_barbell_back_squat")!!.template())
        assertEquals(SetTemplate(3, 12, 60), ExerciseCatalog.byId("biceps_hammer_curl")!!.template())
    }
}
