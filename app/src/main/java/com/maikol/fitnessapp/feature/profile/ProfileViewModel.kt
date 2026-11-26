package com.maikol.fitnessapp.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.core.preferences.Goal
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.core.preferences.UserPreferences
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import com.maikol.fitnessapp.feature.nutrition.data.GoalValues
import com.maikol.fitnessapp.feature.nutrition.data.NutritionRepository
import com.maikol.fitnessapp.feature.nutrition.data.goalFor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

enum class NameSaveStatus { IDLE, SAVING, SAVED, FAILED }

private data class GoalEdit(val drafts: Map<Goal, String>, val errors: Set<Goal>, val status: NameSaveStatus)

/** Ajuste inmediato (no el nombre) cuya última escritura falló. */
enum class FailedSetting { UNITS, MOTION }

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Ready(
        val nameDraft: String,
        val savedName: String?,
        val nameStatus: NameSaveStatus,
        val unitSystem: UnitSystem,
        val reduceMotion: Boolean,
        val failedSetting: FailedSetting?,
        /** Borradores de metas (texto); vacío = sin meta. */
        val goalDrafts: Map<Goal, String> = emptyMap(),
        val goalErrors: Set<Goal> = emptySet(),
        val goalStatus: NameSaveStatus = NameSaveStatus.IDLE,
        val savedGoals: Map<Goal, Int> = emptyMap(),
    ) : ProfileUiState {
        val canSave: Boolean
            get() = nameStatus != NameSaveStatus.SAVING && nameDraft.trim() != (savedName ?: "")
    }
}

/**
 * Perfil H1. El borrador del nombre vive en SavedStateHandle (sobrevive a rotación y muerte de
 * proceso); solo se confirma «Guardado» tras la escritura correcta en DataStore.
 */
class ProfileViewModel(
    private val repository: UserPreferencesRepository,
    private val savedStateHandle: SavedStateHandle,
    /** N3: los objetivos nutricionales viven en Room con su historial; Perfil y Nutrición comparten este escritor. */
    private val nutrition: NutritionRepository? = null,
) : ViewModel() {

    /** Objetivo nutricional vigente hoy (null si aún no hay historial ni metas previas). */
    private val nutritionGoals = nutrition?.observeGoalHistory()?.map { h -> goalFor(LocalDate.now(nutrition.zone()), h)?.values }
        ?: flowOf(null)

    private val nameStatus = MutableStateFlow(NameSaveStatus.IDLE)
    private val failedSetting = MutableStateFlow<FailedSetting?>(null)
    private val draft: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_DRAFT, null)

    /** null = aún no editadas: se muestran las metas guardadas. */
    private val goalEdit = MutableStateFlow<GoalEdit?>(null)

    val uiState: StateFlow<ProfileUiState> = combine(
        combine(repository.userPreferences, nutritionGoals) { prefs, n -> prefs.withNutritionGoals(n) },
        draft,
        nameStatus,
        failedSetting,
        goalEdit,
    ) { prefs: UserPreferences, draft, status, failed, goals ->
        ProfileUiState.Ready(
            nameDraft = draft ?: prefs.displayName.orEmpty(),
            savedName = prefs.displayName,
            nameStatus = status,
            unitSystem = prefs.unitSystem,
            reduceMotion = prefs.reduceMotion,
            failedSetting = failed,
            goalDrafts = goals?.drafts ?: savedGoalTexts(prefs.goals),
            savedGoals = prefs.goals,
            goalErrors = goals?.errors.orEmpty(),
            goalStatus = goals?.status ?: NameSaveStatus.IDLE,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState.Loading)

    fun onNameChange(value: String) {
        savedStateHandle[KEY_DRAFT] = value
        // Un error de guardado sigue visible hasta el siguiente intento; «Guardado» deja de aplicar.
        if (nameStatus.value == NameSaveStatus.SAVED) nameStatus.value = NameSaveStatus.IDLE
    }

    fun saveName() {
        val state = uiState.value as? ProfileUiState.Ready ?: return
        if (!state.canSave) return
        nameStatus.value = NameSaveStatus.SAVING
        viewModelScope.launch {
            nameStatus.value = try {
                repository.setDisplayName(state.nameDraft)
                savedStateHandle[KEY_DRAFT] = state.nameDraft.trim()
                NameSaveStatus.SAVED
            } catch (e: IOException) {
                NameSaveStatus.FAILED
            }
        }
    }

    fun onUnitSystemSelected(unitSystem: UnitSystem) = writeSetting(FailedSetting.UNITS) {
        repository.setUnitSystem(unitSystem)
    }

    fun onReduceMotionChange(enabled: Boolean) = writeSetting(FailedSetting.MOTION) {
        repository.setReduceMotion(enabled)
    }

    fun onGoalChange(goal: Goal, text: String) {
        val current = uiState.value as? ProfileUiState.Ready ?: return
        val drafts = (goalEdit.value?.drafts ?: current.goalDrafts) + (goal to text.filter { it.isDigit() }.take(6))
        goalEdit.value = GoalEdit(drafts, emptySet(), NameSaveStatus.IDLE)
    }

    /** Todas las metas a la vez; vacío elimina la meta; fuera de rango marca el campo sin guardar nada. */
    fun saveGoals() {
        val edit = goalEdit.value ?: return
        if (edit.status == NameSaveStatus.SAVING) return
        val parsed = Goal.entries.associateWith { goal -> edit.drafts[goal].orEmpty().trim().takeIf { it.isNotEmpty() }?.toIntOrNull() }
        val errors = Goal.entries.filter { goal ->
            val text = edit.drafts[goal].orEmpty().trim()
            text.isNotEmpty() && parsed[goal]?.let(goal::isValid) != true
        }.toSet()
        if (errors.isNotEmpty()) {
            goalEdit.value = edit.copy(errors = errors)
            return
        }
        goalEdit.value = edit.copy(status = NameSaveStatus.SAVING)
        viewModelScope.launch {
            goalEdit.value = try {
                val n = nutrition
                if (n == null) {
                    repository.setGoals(parsed)
                } else {
                    // Pasos y agua siguen en preferencias; energía y macros se guardan como un cambio con fecha.
                    repository.setGoals(parsed.filterKeys { it !in NutritionGoals })
                    val values = GoalValues(parsed[Goal.KCAL], parsed[Goal.PROTEIN], parsed[Goal.CARBS], parsed[Goal.FAT])
                    val current = (uiState.value as? ProfileUiState.Ready)?.let { r -> NutritionGoals.associateWith { r.savedGoals[it] } }
                    if (current != NutritionGoals.associateWith { parsed[it] }) n.setGoals(values, source = NutritionRepository.SOURCE_PROFILE)
                }
                edit.copy(status = NameSaveStatus.SAVED)
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                edit.copy(status = NameSaveStatus.FAILED)
            }
        }
    }

    private fun UserPreferences.withNutritionGoals(n: GoalValues?): UserPreferences {
        if (nutrition == null) return this
        val room = mapOf(Goal.KCAL to n?.kcal, Goal.PROTEIN to n?.proteinG, Goal.CARBS to n?.carbsG, Goal.FAT to n?.fatG)
        return copy(goals = goals.filterKeys { it !in NutritionGoals } + room.mapNotNull { (k, v) -> v?.let { k to it } })
    }

    private fun savedGoalTexts(goals: Map<Goal, Int>) = Goal.entries.associateWith { goals[it]?.toString().orEmpty() }

    private fun writeSetting(setting: FailedSetting, write: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                write()
                if (failedSetting.value == setting) failedSetting.value = null
            } catch (e: IOException) {
                failedSetting.value = setting
            }
        }
    }

    companion object {
        private val NutritionGoals = setOf(Goal.KCAL, Goal.PROTEIN, Goal.CARBS, Goal.FAT)
        private const val KEY_DRAFT = "name_draft"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FitnessApplication).container
                ProfileViewModel(container.userPreferencesRepository, createSavedStateHandle(), container.nutritionRepository)
            }
        }
    }
}
