package com.maikol.fitnessapp.feature.training

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maikol.fitnessapp.core.database.FitnessDatabase
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import com.maikol.fitnessapp.feature.training.catalog.Equipment
import com.maikol.fitnessapp.feature.training.catalog.MuscleGroup
import com.maikol.fitnessapp.feature.training.data.SystemTrainingClock
import com.maikol.fitnessapp.feature.training.data.TrainingRepository
import com.maikol.fitnessapp.feature.training.ui.EditorUiState
import com.maikol.fitnessapp.feature.training.ui.RoutineEditorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * G1-01/03/04: crear una rutina eligiendo del catálogo sin escribir nombres de ejercicio. Orden de selección,
 * sin duplicados accidentales, cancelar sin cambios, estado conservado al recrear el ViewModel (rotación o
 * muerte del proceso) y guardado/reapertura. Base en memoria y preferencias temporales: no toca datos reales.
 */
@RunWith(AndroidJUnit4::class)
class ExercisePickerFlowTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, FitnessDatabase::class.java).build()
    private val repo = TrainingRepository(db, SystemTrainingClock)
    private val storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val storeFile = File(context.cacheDir, "picker-test.preferences_pb").apply { delete() }
    private val prefs = UserPreferencesRepository(PreferenceDataStoreFactory.create(scope = storeScope) { storeFile })

    @After
    fun close() {
        storeScope.cancel()
        db.close()
        storeFile.delete()
    }

    private suspend fun RoutineEditorViewModel.ready(): EditorUiState = withTimeout(5_000) { state.first { !it.loading } }

    @Test
    fun pickFromCatalog_order_noDuplicates_cancel_restore_saveReopen() = runBlocking {
        withContext(Dispatchers.Main) {
            val saved = SavedStateHandle()
            val vm = RoutineEditorViewModel(saved, repo, prefs)
            assertEquals("borrador nuevo sin ejercicios", 0, vm.ready().form!!.exercises.size)

            // Cancelar el selector no modifica el borrador.
            vm.openPicker()
            vm.togglePicked("back_lat_pulldown")
            vm.closePicker()
            assertNull(vm.state.value.picker)
            assertEquals(0, vm.state.value.form!!.exercises.size)

            // Hombros: elegir tres en un orden concreto; desmarcar y volver a marcar mueve al final.
            vm.openPicker()
            vm.onPickerQuery("Hombro")
            vm.onPickerMuscle(MuscleGroup.SHOULDERS)
            vm.togglePicked("shoulders_dumbbell_rear_delt_fly")
            vm.togglePicked("shoulders_seated_dumbbell_press")
            vm.togglePicked("shoulders_dumbbell_lateral_raise")
            vm.togglePicked("shoulders_seated_dumbbell_press")
            vm.togglePicked("shoulders_seated_dumbbell_press")
            vm.openGuide("shoulders_dumbbell_lateral_raise")

            // Recrear el ViewModel con el mismo SavedStateHandle conserva consulta, filtros, selección y ficha.
            val restored = RoutineEditorViewModel(saved, repo, prefs)
            val r = restored.ready()
            assertEquals("Hombro", r.picker!!.query)
            assertEquals(MuscleGroup.SHOULDERS, r.picker!!.muscle)
            assertEquals(
                listOf("shoulders_dumbbell_rear_delt_fly", "shoulders_dumbbell_lateral_raise", "shoulders_seated_dumbbell_press"),
                r.picker!!.selected,
            )
            assertEquals("shoulders_dumbbell_lateral_raise", r.guideId)
            restored.closeGuide()
            restored.confirmPicker()
            val afterAdd = restored.state.value
            assertNull(afterAdd.picker)
            val names = afterAdd.form!!.exercises.map { it.name }
            assertEquals(listOf("Pájaros con mancuernas", "Elevaciones laterales", "Press sentado con mancuernas"), names)
            assertTrue(afterAdd.form!!.exercises.all { it.catalogId != null && it.sets.size == 3 && it.sets.all { s -> s.load.text.isEmpty() } })
            assertEquals(3, afterAdd.form!!.exercises.map { it.key }.toSet().size)

            // Lo ya presente no se vuelve a añadir; mezclar grupos sí.
            restored.openPicker()
            restored.togglePicked("shoulders_dumbbell_lateral_raise")
            assertEquals(emptyList<String>(), restored.state.value.picker!!.selected)
            restored.onPickerMuscle(MuscleGroup.BACK)
            restored.onPickerEquipment(Equipment.CABLE)
            restored.togglePicked("back_seated_cable_row")
            restored.confirmPicker()
            assertEquals(4, restored.state.value.form!!.exercises.size)

            // Personalizado sigue disponible como camino secundario.
            restored.addExercise()
            val custom = restored.state.value.form!!.exercises.last()
            assertNull(custom.catalogId)
            restored.onExerciseNameChange(custom.id, "Mi ejercicio Prueba")
            restored.removeExercise(custom.id)

            // Reordenar, notas y descanso editables; guardar y reabrir conserva todo.
            val first = restored.state.value.form!!.exercises.first()
            restored.moveExercise(first.id, 1)
            restored.onExerciseNotesChange(first.id, "Prueba nota")
            restored.onRestChange(first.id, first.sets.first().id, "75")
            restored.onNameChange("Prueba hombro y espalda")
            restored.save()
            val routineId = withTimeout(5_000) { restored.state.first { it.savedRoutineId != null } }.savedRoutineId!!
            val reopened = repo.getRoutine(routineId)!!
            assertEquals(
                listOf("Elevaciones laterales", "Pájaros con mancuernas", "Press sentado con mancuernas", "Remo sentado en polea"),
                reopened.exercises.map { it.name },
            )
            val moved = reopened.exercises[1]
            assertEquals("shoulders_dumbbell_rear_delt_fly", moved.catalogId)
            assertEquals("Prueba nota", moved.notes)
            assertEquals(75, moved.sets.first().restSeconds)
            assertNull(moved.sets.first().loadKg)
        }
    }
}
