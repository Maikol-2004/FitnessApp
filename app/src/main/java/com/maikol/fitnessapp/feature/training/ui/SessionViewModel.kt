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
import com.maikol.fitnessapp.feature.training.data.ClockReading
import com.maikol.fitnessapp.feature.training.data.FinishResult
import com.maikol.fitnessapp.feature.training.data.LoadText
import com.maikol.fitnessapp.feature.training.data.Parsed
import com.maikol.fitnessapp.feature.training.data.PerformedSet
import com.maikol.fitnessapp.feature.training.data.Session
import com.maikol.fitnessapp.feature.training.data.TrainingRepository
import com.maikol.fitnessapp.feature.training.data.parseReps
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Texto de una serie mientras se edita. Las ediciones válidas se guardan en Room al momento. */
data class SetDraft(val reps: String, val load: LoadText, val unit: UnitSystem) {
    fun repsValue(): Int? = parseReps(reps)
    fun loadValue(): Parsed<Double> = load.resolveKg(unit)
    val invalid: Boolean get() = repsValue() == null || loadValue() is Parsed.Invalid
}

enum class SessionDialog { NONE, FINISH, FINISH_EMPTY, INVALID_DRAFTS, DISCARD }

data class SessionUiState(
    val loading: Boolean = true,
    val session: Session? = null,
    val unit: UnitSystem = UnitSystem.METRIC,
    val drafts: Map<String, SetDraft> = emptyMap(),
    /** Series con escritura en curso: un segundo toque se ignora. */
    val pending: Set<String> = emptySet(),
    /** Series cuyo intento de completar falló (escritura o valor): se muestra el error, nunca el check. */
    val failed: Set<String> = emptySet(),
    /** Series en las que se intentó completar con valores no válidos: se marcan los campos. */
    val attempted: Set<String> = emptySet(),
    val dialog: SessionDialog = SessionDialog.NONE,
    val finishing: Boolean = false,
    val finishFailed: Boolean = false,
    /** Eventos de navegación. */
    val finished: Boolean = false,
    val discarded: Boolean = false,
) {
    /**
     * Borrador de la serie, o sus valores guardados. Un borrador válido escrito en otra unidad ya está en Room:
     * se vuelve a mostrar desde ahí en la unidad actual.
     */
    fun draftFor(set: PerformedSet): SetDraft =
        drafts[set.id]?.takeIf { it.unit == unit || it.invalid }
            ?: SetDraft(set.reps?.toString().orEmpty(), LoadText.of(set.loadKg, unit), unit)

    /** Campos editados con valores no válidos (aún sin guardar). */
    val invalidDraftIds: Set<String>
        get() = drafts.filterValues { it.invalid }.keys
}

/**
 * Sesión activa. Room es la fuente de verdad: el check, el progreso y el descanso se pintan a partir de lo
 * guardado (nunca de forma optimista). El texto en edición vive aquí y en SavedStateHandle.
 */
class SessionViewModel(
    private val savedState: SavedStateHandle,
    private val repository: TrainingRepository,
    preferences: UserPreferencesRepository,
) : ViewModel() {

    val sessionId: String = checkNotNull(savedState[ARG_SESSION_ID])
    private val local = MutableStateFlow(SessionUiState(drafts = restoreDrafts()))

    val state: StateFlow<SessionUiState> = combine(
        repository.observeSession(sessionId),
        preferences.userPreferences,
        local,
    ) { session, prefs, l ->
        l.copy(loading = false, session = session, unit = prefs.unitSystem)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), local.value)

    fun now(): ClockReading = repository.now()

    fun onRepsChange(set: PerformedSet, text: String) = editDraft(set) { it.copy(reps = text) }

    fun onLoadChange(set: PerformedSet, text: String) = editDraft(set) { it.copy(load = it.load.edited(text)) }

    /**
     * Completa la serie: valida, escribe y solo entonces el flujo de Room trae el check y el descanso.
     * Doble toque: ignorado mientras la escritura está en curso y, además, el repositorio es idempotente.
     */
    fun complete(set: PerformedSet) {
        val s = state.value.copy(drafts = local.value.drafts, pending = local.value.pending)
        if (set.id in s.pending || set.completed) return
        val draft = s.draftFor(set)
        val reps = draft.repsValue()
        val load = draft.loadValue()
        if (reps == null || load is Parsed.Invalid) {
            local.update { it.copy(attempted = it.attempted + set.id) }
            return
        }
        val kg = (load as? Parsed.Value)?.value
        local.update { it.copy(pending = it.pending + set.id, failed = it.failed - set.id, attempted = it.attempted - set.id) }
        viewModelScope.launch {
            try {
                repository.completeSet(sessionId, set.id, reps, kg)
                local.update { it.copy(pending = it.pending - set.id, drafts = it.drafts - set.id) }
                persistDrafts()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                local.update { it.copy(pending = it.pending - set.id, failed = it.failed + set.id) }
            }
        }
    }

    fun uncomplete(set: PerformedSet) {
        if (set.id in local.value.pending || !set.completed) return
        local.update { it.copy(pending = it.pending + set.id, failed = it.failed - set.id) }
        viewModelScope.launch {
            try {
                repository.uncompleteSet(set.id)
                local.update { it.copy(pending = it.pending - set.id) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                local.update { it.copy(pending = it.pending - set.id, failed = it.failed + set.id) }
            }
        }
    }

    fun addSet(sessionExerciseId: String) = act { repository.addSet(sessionExerciseId) }
    fun goToExercise(index: Int) = act { repository.setCurrentExercise(sessionId, index) }
    fun pause() = act { repository.pause(sessionId) }
    fun resume() = act { repository.resume(sessionId) }
    fun skipRest() = act { repository.skipRest(sessionId) }
    fun adjustRest(deltaSeconds: Int) = act { repository.adjustRest(sessionId, deltaSeconds) }
    fun confirmTime() = act { repository.confirmTime(sessionId) }

    /** Antes de finalizar, toda edición pendiente debe ser válida o descartarse de forma explícita. */
    fun requestFinish() {
        val s = state.value.copy(drafts = local.value.drafts)
        val session = s.session ?: return
        val dialog = when {
            s.invalidDraftIds.isNotEmpty() -> SessionDialog.INVALID_DRAFTS
            session.completedSets == 0 -> SessionDialog.FINISH_EMPTY
            else -> SessionDialog.FINISH
        }
        local.update { it.copy(dialog = dialog, finishFailed = false) }
    }

    fun requestDiscard() = local.update { it.copy(dialog = SessionDialog.DISCARD) }

    fun dismissDialog() = local.update { it.copy(dialog = SessionDialog.NONE) }

    fun discardInvalidDrafts() {
        local.update { s -> s.copy(drafts = s.drafts - s.invalidDraftIds, attempted = emptySet(), dialog = SessionDialog.NONE) }
        persistDrafts()
        requestFinish()
    }

    /** Transacción idempotente: dos toques o un reintento dan un único resumen. */
    fun confirmFinish() {
        if (local.value.finishing) return
        local.update { it.copy(finishing = true, dialog = SessionDialog.NONE, finishFailed = false) }
        viewModelScope.launch {
            try {
                when (repository.finish(sessionId)) {
                    FinishResult.Completed, FinishResult.AlreadyCompleted -> local.update { it.copy(finishing = false, finished = true) }
                    FinishResult.NotOpen -> local.update { it.copy(finishing = false, finishFailed = true) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                local.update { it.copy(finishing = false, finishFailed = true) }
            }
        }
    }

    fun confirmDiscard() {
        local.update { it.copy(dialog = SessionDialog.NONE) }
        viewModelScope.launch {
            try {
                repository.discard(sessionId)
                local.update { it.copy(discarded = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                local.update { it.copy(finishFailed = true) }
            }
        }
    }

    fun onNavigationHandled() = local.update { it.copy(finished = false, discarded = false) }

    private fun editDraft(set: PerformedSet, transform: (SetDraft) -> SetDraft) {
        val s = state.value.copy(drafts = local.value.drafts)
        val draft = transform(s.draftFor(set))
        local.update { it.copy(drafts = it.drafts + (set.id to draft), failed = it.failed - set.id) }
        persistDrafts()
        val reps = draft.repsValue()
        val load = draft.loadValue()
        if (reps != null && load !is Parsed.Invalid) {
            // Guardar valores válidos al momento: lo escrito sobrevive al cierre del proceso.
            act { repository.updateSetValues(set.id, reps, (load as? Parsed.Value)?.value) }
        }
    }

    private fun act(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                local.update { it.copy(finishFailed = true) }
            }
        }
    }

    private fun persistDrafts() {
        savedState[KEY_DRAFTS] = Bundle().apply {
            local.value.drafts.forEach { (id, d) ->
                putBundle(id, Bundle().apply {
                    putString("reps", d.reps)
                    putString("text", d.load.text)
                    putString("baseText", d.load.baseText)
                    putString("unit", d.unit.name)
                    d.load.baseKg?.let { putDouble("baseKg", it) }
                })
            }
        }
    }

    private fun restoreDrafts(): Map<String, SetDraft> {
        val bundle = savedState.get<Bundle>(KEY_DRAFTS) ?: return emptyMap()
        return bundle.keySet().associateWith { id ->
            val b = bundle.getBundle(id) ?: Bundle()
            SetDraft(
                reps = b.getString("reps").orEmpty(),
                load = LoadText(b.getString("text").orEmpty(), if (b.containsKey("baseKg")) b.getDouble("baseKg") else null, b.getString("baseText").orEmpty()),
                unit = runCatching { UnitSystem.valueOf(b.getString("unit").orEmpty()) }.getOrDefault(UnitSystem.METRIC),
            )
        }
    }

    companion object {
        const val ARG_SESSION_ID = "sessionId"
        private const val KEY_DRAFTS = "session_drafts"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FitnessApplication).container
                SessionViewModel(createSavedStateHandle(), container.trainingRepository, container.userPreferencesRepository)
            }
        }
    }
}
