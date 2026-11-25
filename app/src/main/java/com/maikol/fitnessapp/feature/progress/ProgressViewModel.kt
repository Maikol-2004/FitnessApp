package com.maikol.fitnessapp.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.core.database.BodyWeightEntity
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import com.maikol.fitnessapp.core.tracking.MAX_NOTE_LENGTH
import com.maikol.fitnessapp.core.tracking.MAX_WEIGHT_KG
import com.maikol.fitnessapp.core.tracking.MIN_WEIGHT_KG
import com.maikol.fitnessapp.core.tracking.TrackingRepository
import com.maikol.fitnessapp.core.tracking.WeekTraining
import com.maikol.fitnessapp.core.tracking.weekTraining
import com.maikol.fitnessapp.feature.training.data.LoadText
import com.maikol.fitnessapp.feature.training.data.Parsed
import com.maikol.fitnessapp.feature.training.data.TrainingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Peso en kg normalizado; [weight] conserva los kg exactos si el texto mostrado no cambia (sin deriva lb). */
data class WeightForm(
    val existing: BodyWeightEntity?,
    val date: LocalDate,
    val weight: LoadText,
    val note: String,
    val weightError: Boolean = false,
) {
    val dirty get() = weight.text != weight.baseText || note != existing?.note.orEmpty() || (existing != null && date.toString() != existing.date)
}

/** Resultado de leer el peso: kg válidos o null si no es >0, finito y dentro de rango. */
fun resolveWeightKg(form: WeightForm, unit: UnitSystem): Double? =
    (form.weight.resolveKg(unit) as? Parsed.Value)?.value?.takeIf { it.isFinite() && it in MIN_WEIGHT_KG..MAX_WEIGHT_KG }

data class ProgressUiState(
    val loading: Boolean = true,
    val today: LocalDate,
    val unit: UnitSystem = UnitSystem.METRIC,
    val week: WeekTraining = WeekTraining(0, 0.0),
    val weights: List<BodyWeightEntity> = emptyList(),
    val form: WeightForm? = null,
    val saving: Boolean = false,
    val failed: Boolean = false,
    val deleteFailed: Boolean = false,
)

private data class WeightEdit(val form: WeightForm? = null, val saving: Boolean = false, val failed: Boolean = false, val deleteFailed: Boolean = false)

class ProgressViewModel(
    private val tracking: TrackingRepository,
    training: TrainingRepository,
    preferences: UserPreferencesRepository,
) : ViewModel() {
    private val edit = MutableStateFlow(WeightEdit())

    // «Hoy» vivo: la semana de entrenamiento avanza a medianoche; el formulario abierto conserva su fecha.
    val state: StateFlow<ProgressUiState> = combine(
        tracking.observeToday(),
        training.observeHistory(),
        tracking.observeWeights(),
        preferences.userPreferences,
        edit,
    ) { today, history, weights, prefs, e ->
        ProgressUiState(
            loading = false,
            today = today,
            unit = prefs.unitSystem,
            // Mismo historial y volumen que H2: solo sesiones COMPLETED.
            week = weekTraining(history, today, tracking.zone()),
            weights = weights,
            form = e.form,
            saving = e.saving,
            failed = e.failed,
            deleteFailed = e.deleteFailed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState(today = tracking.today()))

    fun add() {
        edit.value = WeightEdit(form = WeightForm(null, state.value.today, LoadText.of(null, state.value.unit), ""))
    }

    fun edit(entry: BodyWeightEntity) {
        edit.value = WeightEdit(form = WeightForm(entry, LocalDate.parse(entry.date), LoadText.of(entry.kg, state.value.unit), entry.note.orEmpty()))
    }

    fun closeForm() {
        if (!edit.value.saving) edit.value = WeightEdit()
    }

    fun update(transform: (WeightForm) -> WeightForm) = edit.update { e ->
        e.form?.let { e.copy(form = transform(it).copy(weightError = false), failed = false) } ?: e
    }

    fun save() {
        val e = edit.value
        val form = e.form ?: return
        if (e.saving) return
        val kg = resolveWeightKg(form, state.value.unit)
        if (kg == null || form.note.length > MAX_NOTE_LENGTH) {
            edit.value = e.copy(form = form.copy(weightError = kg == null))
            return
        }
        edit.value = e.copy(saving = true, failed = false)
        viewModelScope.launch {
            try {
                tracking.saveWeight(form.existing, form.date, kg, form.note)
                edit.value = WeightEdit()
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                edit.update { it.copy(saving = false, failed = true) }
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            try {
                tracking.deleteWeight(id)
                edit.update { it.copy(deleteFailed = false) }
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                edit.update { it.copy(deleteFailed = true) }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as FitnessApplication).container
                ProgressViewModel(c.trackingRepository, c.trainingRepository, c.userPreferencesRepository)
            }
        }
    }
}
