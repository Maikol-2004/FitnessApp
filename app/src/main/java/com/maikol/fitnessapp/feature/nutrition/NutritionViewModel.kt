package com.maikol.fitnessapp.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.core.database.MealEntryEntity
import com.maikol.fitnessapp.core.preferences.Goal
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import com.maikol.fitnessapp.core.tracking.NutritionTotals
import com.maikol.fitnessapp.core.tracking.TrackingRepository
import com.maikol.fitnessapp.core.tracking.nutritionTotals
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class NutritionUiState(
    val loading: Boolean = true,
    val today: LocalDate,
    val date: LocalDate,
    val meals: List<MealEntryEntity> = emptyList(),
    val totals: NutritionTotals = nutritionTotals(emptyList()),
    val goals: Map<Goal, Int> = emptyMap(),
    val form: MealForm? = null,
    val saving: Boolean = false,
    val failed: Boolean = false,
    val deleteFailed: Boolean = false,
)

private data class MealEdit(val form: MealForm? = null, val saving: Boolean = false, val failed: Boolean = false, val deleteFailed: Boolean = false)

/** Comidas del día con totales recalculados desde Room (editar/eliminar nunca acumula). */
@OptIn(ExperimentalCoroutinesApi::class)
class NutritionViewModel(
    private val repository: TrackingRepository,
    preferences: UserPreferencesRepository,
) : ViewModel() {
    private val today = repository.today()
    private val date = MutableStateFlow(today)
    private val edit = MutableStateFlow(MealEdit())

    val state: StateFlow<NutritionUiState> = combine(
        date.flatMapLatest { d -> repository.observeMeals(d).map { d to it } },
        preferences.userPreferences,
        edit,
    ) { (d, meals), prefs, e ->
        NutritionUiState(
            loading = false,
            today = today,
            date = d,
            meals = meals,
            totals = nutritionTotals(meals),
            goals = prefs.goals,
            form = e.form,
            saving = e.saving,
            failed = e.failed,
            deleteFailed = e.deleteFailed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionUiState(today = today, date = today))

    fun selectDate(value: LocalDate) {
        if (value <= today && edit.value.form == null) date.value = value
    }

    fun add() {
        edit.value = MealEdit(form = MealForm.new(LocalTime.now(repository.zone())))
    }

    fun edit(meal: MealEntryEntity) {
        edit.value = MealEdit(form = MealForm.of(meal))
    }

    fun closeForm() {
        if (!edit.value.saving) edit.value = MealEdit()
    }

    fun update(transform: (MealForm) -> MealForm) = edit.update { e ->
        e.form?.let { e.copy(form = transform(it).copy(errors = emptySet(), noValues = false), failed = false) } ?: e
    }

    fun save() {
        val e = edit.value
        val form = e.form ?: return
        if (e.saving) return
        when (val v = validateMeal(form)) {
            is MealValidation.Invalid -> edit.value = e.copy(form = v.form)
            is MealValidation.Valid -> {
                edit.value = e.copy(saving = true, failed = false)
                val day = date.value
                viewModelScope.launch {
                    try {
                        val x = v.values
                        repository.saveMeal(form.existing, x.name, day, form.time, x.kcal, x.protein, x.carbs, x.fat)
                        edit.value = MealEdit()
                    } catch (c: CancellationException) {
                        throw c
                    } catch (_: Exception) {
                        edit.update { it.copy(saving = false, failed = true) }
                    }
                }
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteMeal(id)
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
                NutritionViewModel(c.trackingRepository, c.userPreferencesRepository)
            }
        }
    }
}
