package com.maikol.fitnessapp.feature.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.core.database.DailyActivityEntity
import com.maikol.fitnessapp.core.preferences.Goal
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import com.maikol.fitnessapp.core.tracking.TrackingRepository
import com.maikol.fitnessapp.core.tracking.visibleDay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HealthUiState(
    val loading: Boolean = true,
    val today: LocalDate,
    val date: LocalDate,
    val activity: DailyActivityEntity? = null,
    val unit: UnitSystem = UnitSystem.METRIC,
    val stepGoal: Int? = null,
    val form: ActivityForm? = null,
    val saving: Boolean = false,
    val failed: Boolean = false,
    val deleteFailed: Boolean = false,
)

private data class Edit(val day: LocalDate? = null, val form: ActivityForm? = null, val saving: Boolean = false, val failed: Boolean = false, val deleteFailed: Boolean = false)

/** Registro diario manual: un registro por fecha; editar sustituye, eliminar borra solo ese día. */
@OptIn(ExperimentalCoroutinesApi::class)
class HealthViewModel(
    private val repository: TrackingRepository,
    preferences: UserPreferencesRepository,
) : ViewModel() {
    /** null = sigue a «hoy»; una fecha anterior elegida se conserva al cambiar de día. */
    private val chosen = MutableStateFlow<LocalDate?>(null)
    private val edit = MutableStateFlow(Edit())

    val state: StateFlow<HealthUiState> = combine(
        combine(repository.observeToday(), chosen, edit.map { it.day }.distinctUntilChanged()) { t, c, p -> t to visibleDay(t, c, p) }
            .distinctUntilChanged()
            .flatMapLatest { (t, d) -> repository.observeActivity(d).map { Triple(t, d, it) } },
        preferences.userPreferences,
        edit,
    ) { (t, d, activity), prefs, e ->
        HealthUiState(
            loading = false,
            today = t,
            date = d,
            activity = activity,
            unit = prefs.unitSystem,
            stepGoal = prefs.goals[Goal.STEPS],
            form = e.form,
            saving = e.saving,
            failed = e.failed,
            deleteFailed = e.deleteFailed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.today().let { HealthUiState(today = it, date = it) })

    fun selectDate(value: LocalDate) {
        val today = state.value.today
        if (value <= today && edit.value.form == null) chosen.value = value.takeIf { it < today }
    }

    /** El formulario fija su día: si pasa la medianoche con él abierto, guarda en el día que se abrió. */
    fun openForm() {
        val s = state.value
        edit.value = Edit(day = s.date, form = ActivityForm.of(s.activity, s.unit))
    }

    fun closeForm() {
        if (!edit.value.saving) edit.value = Edit()
    }

    fun update(transform: (ActivityForm) -> ActivityForm) = edit.update { e ->
        e.form?.let { e.copy(form = transform(it).copy(errors = emptySet(), noValues = false), failed = false) } ?: e
    }

    fun onDistance(text: String) = update { it.copy(distance = it.distance.copy(text = text)) }

    fun save() {
        val e = edit.value
        val form = e.form ?: return
        if (e.saving) return
        when (val v = validateActivity(form, state.value.unit)) {
            is ActivityValidation.Invalid -> edit.value = e.copy(form = v.form)
            is ActivityValidation.Valid -> {
                edit.value = e.copy(saving = true, failed = false)
                val day = e.day ?: state.value.date
                viewModelScope.launch {
                    try {
                        repository.saveActivity(day, v.values.steps, v.values.distanceMeters, v.values.minutes, v.values.kcal)
                        edit.value = Edit()
                    } catch (c: CancellationException) {
                        throw c
                    } catch (_: Exception) {
                        // El borrador se conserva para reintentar.
                        edit.update { it.copy(saving = false, failed = true) }
                    }
                }
            }
        }
    }

    fun delete() {
        val day = state.value.date
        viewModelScope.launch {
            try {
                repository.deleteActivity(day)
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
                HealthViewModel(c.trackingRepository, c.userPreferencesRepository)
            }
        }
    }
}
