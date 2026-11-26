package com.maikol.fitnessapp.feature.training.ui

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import com.maikol.fitnessapp.feature.training.catalog.AssistMode
import com.maikol.fitnessapp.feature.training.catalog.CatalogExercise
import com.maikol.fitnessapp.feature.training.catalog.Equipment
import com.maikol.fitnessapp.feature.training.catalog.ExerciseCatalog
import com.maikol.fitnessapp.feature.training.catalog.MuscleGroup
import com.maikol.fitnessapp.feature.training.catalog.template
import com.maikol.fitnessapp.feature.training.data.LoadText
import com.maikol.fitnessapp.feature.training.data.TrainingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val notFound: Boolean = false,
    val unit: UnitSystem = UnitSystem.METRIC,
    val form: RoutineForm? = null,
    val initialForm: RoutineForm? = null,
    /** Los errores se muestran tras el primer intento de guardar y después se actualizan en vivo. */
    val showErrors: Boolean = false,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
    /** Evento: rutina guardada (se consume al navegar). */
    val savedRoutineId: String? = null,
    /** Selector del catálogo abierto (null = cerrado). Cerrarlo sin confirmar no toca el borrador. */
    val picker: PickerState? = null,
    /** Ficha abierta encima del selector o del editor. */
    val guideId: String? = null,
) {
    /** Definiciones del catálogo ya presentes en el borrador. */
    val catalogIdsInForm: Set<String> get() = form?.exercises?.mapNotNull { it.catalogId }?.toSet().orEmpty()

    val errors: FormErrors get() = (form?.validate(unit) as? FormResult.Invalid)?.errors ?: FormErrors()
    val visibleErrors: FormErrors get() = if (showErrors) errors else FormErrors()
    val hasChanges: Boolean get() = form != null && form != initialForm
}

/**
 * Editor de rutina. El formulario vive aquí y en SavedStateHandle (sobrevive a rotación y a la recreación
 * del proceso por el sistema). Solo se guarda una rutina válida; el éxito se confirma tras la transacción.
 */
class RoutineEditorViewModel(
    private val savedState: SavedStateHandle,
    private val repository: TrainingRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val routineId: String? = savedState.get<String>(ARG_ROUTINE_ID)?.takeIf { it.isNotBlank() }
    private val _state = MutableStateFlow(EditorUiState(isNew = routineId == null))
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val unit = savedState.get<String>(KEY_UNIT)?.let { UnitSystem.valueOf(it) }
                ?: preferences.userPreferences.first().unitSystem.also { savedState[KEY_UNIT] = it.name }
            val restoredForm = savedState.get<Bundle>(KEY_FORM)?.toRoutineForm()
            val restoredInitial = savedState.get<Bundle>(KEY_INITIAL)?.toRoutineForm()
            if (restoredForm != null && restoredInitial != null) {
                _state.update {
                    it.copy(
                        loading = false, unit = unit, form = restoredForm, initialForm = restoredInitial,
                        showErrors = savedState[KEY_SHOW_ERRORS] ?: false,
                        picker = savedState.get<Bundle>(KEY_PICKER)?.toPickerState(),
                        guideId = savedState[KEY_GUIDE],
                    )
                }
                return@launch
            }
            val initial = if (routineId == null) emptyForm() else repository.getRoutine(routineId)?.toForm(unit)
            if (initial == null) {
                _state.update { it.copy(loading = false, unit = unit, notFound = true) }
            } else {
                savedState[KEY_INITIAL] = initial.toBundle()
                _state.update { it.copy(loading = false, unit = unit, form = initial, initialForm = initial) }
                persist()
            }
        }
    }

    fun onNameChange(value: String) = edit { it.copy(name = value.take(80)) }
    fun onNotesChange(value: String) = edit { it.copy(notes = value.take(500)) }

    /** «Crear ejercicio personalizado»: camino secundario con nombre libre (como antes de G1). */
    fun addExercise() = edit { it.copy(exercises = it.exercises + newExercise()) }

    // ---- Selector del catálogo (G1) ----

    fun openPicker() = updatePicker { PickerState() }
    fun closePicker() = updatePicker { null }
    fun onPickerQuery(value: String) = updatePicker { it?.copy(query = value.take(60)) }
    fun onPickerMuscle(value: MuscleGroup?) = updatePicker { it?.copy(muscle = value) }
    fun onPickerEquipment(value: Equipment?) = updatePicker { it?.copy(equipment = value) }
    fun clearPickerFilters() = updatePicker { it?.copy(query = "", muscle = null, equipment = null) }

    /** Marca o desmarca conservando el orden de selección. Lo ya presente en el borrador no se vuelve a añadir. */
    fun togglePicked(catalogId: String) = updatePicker { p ->
        if (p == null || catalogId in _state.value.catalogIdsInForm || ExerciseCatalog.byId(catalogId) == null) p
        else p.copy(selected = if (catalogId in p.selected) p.selected - catalogId else p.selected + catalogId)
    }

    /** Inserta lo elegido, en el orden de selección, con plantilla de series editable y carga vacía. */
    fun confirmPicker() {
        val picker = _state.value.picker ?: return
        val present = _state.value.catalogIdsInForm
        val added = picker.selected.distinct().filterNot { it in present }.mapNotNull { ExerciseCatalog.byId(it) }.map { catalogExercise(it) }
        _state.update { it.copy(picker = null, guideId = null) }
        savedState[KEY_PICKER] = null
        savedState[KEY_GUIDE] = null
        if (added.isNotEmpty()) edit { it.copy(exercises = it.exercises + added) }
    }

    fun openGuide(catalogId: String) {
        if (ExerciseCatalog.byId(catalogId) == null) return
        savedState[KEY_GUIDE] = catalogId
        _state.update { it.copy(guideId = catalogId) }
    }

    /** G2.1: ficha de un ejercicio personalizado, identificado por su exerciseKey (sin imágenes del catálogo). */
    fun openCustomGuide(exerciseKey: String) {
        if (_state.value.form?.exercises?.none { it.key == exerciseKey && it.catalogId == null } != false) return
        val token = customGuideToken(exerciseKey)
        savedState[KEY_GUIDE] = token
        _state.update { it.copy(guideId = token) }
    }

    fun closeGuide() {
        savedState[KEY_GUIDE] = null
        _state.update { it.copy(guideId = null) }
    }

    private fun updatePicker(transform: (PickerState?) -> PickerState?) {
        val next = transform(_state.value.picker)
        savedState[KEY_PICKER] = next?.toBundle()
        _state.update { it.copy(picker = next) }
    }

    private fun catalogExercise(def: CatalogExercise): ExerciseForm {
        val t = def.template()
        return ExerciseForm(
            id = repository.newId(),
            key = repository.newId(),
            name = def.name,
            notes = "",
            sets = List(t.sets) { SetForm(repository.newId(), t.reps.toString(), LoadText("", null, ""), t.restSeconds.toString(), false) },
            catalogId = def.id,
        )
    }

    fun removeExercise(id: String) = edit { f -> f.copy(exercises = f.exercises.filterNot { it.id == id }) }

    /** Copia con IDs y clave nuevos: es otra entrada del mismo plan, editable por separado. */
    fun duplicateExercise(id: String) = edit { f ->
        val index = f.exercises.indexOfFirst { it.id == id }
        if (index < 0) return@edit f
        val source = f.exercises[index]
        val copy = source.copy(id = repository.newId(), key = repository.newId(), sets = source.sets.map { it.copy(id = repository.newId()) })
        f.copy(exercises = f.exercises.toMutableList().apply { add(index + 1, copy) })
    }

    fun moveExercise(id: String, delta: Int) = edit { f ->
        val from = f.exercises.indexOfFirst { it.id == id }
        val to = from + delta
        if (from < 0 || to !in f.exercises.indices) return@edit f
        f.copy(exercises = f.exercises.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun onExerciseNameChange(id: String, value: String) = editExercise(id) { it.copy(name = value.take(80)) }
    fun onExerciseNotesChange(id: String, value: String) = editExercise(id) { it.copy(notes = value.take(300)) }

    /** G2.1: máquina o banda (o volver a «sin indicar») en la dominada asistida. */
    fun onAssistModeChange(id: String, mode: AssistMode?) = editExercise(id) { it.copy(assistMode = mode) }

    /** Nueva serie con los valores de la anterior (lo habitual al planificar varias series iguales). */
    fun addSet(exerciseId: String) = editExercise(exerciseId) { e ->
        val last = e.sets.lastOrNull()
        // Copia también la base en kg de la carga: «44,1 lb» sigue siendo exactamente 20 kg.
        val set = last?.copy(id = repository.newId(), warmup = false)
            ?: SetForm(repository.newId(), "", LoadText("", null, ""), DEFAULT_REST, false)
        e.copy(sets = e.sets + set)
    }

    fun removeSet(exerciseId: String, setId: String) = editExercise(exerciseId) { e -> e.copy(sets = e.sets.filterNot { it.id == setId }) }

    fun onRepsChange(exerciseId: String, setId: String, value: String) = editSet(exerciseId, setId) { it.copy(reps = value) }
    fun onLoadChange(exerciseId: String, setId: String, value: String) = editSet(exerciseId, setId) { it.copy(load = it.load.edited(value)) }
    fun onRestChange(exerciseId: String, setId: String, value: String) = editSet(exerciseId, setId) { it.copy(rest = value) }
    fun onWarmupToggle(exerciseId: String, setId: String) = editSet(exerciseId, setId) { it.copy(warmup = !it.warmup) }

    fun save() {
        val current = _state.value
        val form = current.form ?: return
        if (current.saving) return
        when (val result = form.validate(current.unit)) {
            is FormResult.Invalid -> {
                savedState[KEY_SHOW_ERRORS] = true
                _state.update { it.copy(showErrors = true, saveFailed = false) }
            }
            is FormResult.Valid -> {
                _state.update { it.copy(saving = true, saveFailed = false, showErrors = true) }
                viewModelScope.launch {
                    try {
                        repository.saveRoutine(result.routine)
                        _state.update { it.copy(saving = false, initialForm = it.form, savedRoutineId = result.routine.id) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        _state.update { it.copy(saving = false, saveFailed = true) }
                    }
                }
            }
        }
    }

    fun onSavedHandled() = _state.update { it.copy(savedRoutineId = null) }

    /** Sin ejercicios: el camino principal es «Añadir ejercicio» desde el catálogo. */
    private fun emptyForm() = RoutineForm(repository.newId(), "", "", emptyList())

    private fun newExercise() = ExerciseForm(
        id = repository.newId(),
        key = repository.newId(),
        name = "",
        notes = "",
        sets = listOf(SetForm(repository.newId(), "", LoadText("", null, ""), DEFAULT_REST, false)),
    )

    private fun edit(transform: (RoutineForm) -> RoutineForm) {
        val form = _state.value.form ?: return
        _state.update { it.copy(form = transform(form), saveFailed = false) }
        persist()
    }

    private fun editExercise(id: String, transform: (ExerciseForm) -> ExerciseForm) =
        edit { f -> f.copy(exercises = f.exercises.map { if (it.id == id) transform(it) else it }) }

    private fun editSet(exerciseId: String, setId: String, transform: (SetForm) -> SetForm) =
        editExercise(exerciseId) { e -> e.copy(sets = e.sets.map { if (it.id == setId) transform(it) else it }) }

    private fun persist() {
        _state.value.form?.let { savedState[KEY_FORM] = it.toBundle() }
    }

    companion object {
        const val ARG_ROUTINE_ID = "routineId"
        private const val KEY_FORM = "editor_form"
        private const val KEY_INITIAL = "editor_initial"
        private const val KEY_UNIT = "editor_unit"
        private const val KEY_SHOW_ERRORS = "editor_show_errors"
        private const val KEY_PICKER = "editor_picker"
        private const val KEY_GUIDE = "editor_guide"
        private const val DEFAULT_REST = "90"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FitnessApplication).container
                RoutineEditorViewModel(createSavedStateHandle(), container.trainingRepository, container.userPreferencesRepository)
            }
        }
    }
}
