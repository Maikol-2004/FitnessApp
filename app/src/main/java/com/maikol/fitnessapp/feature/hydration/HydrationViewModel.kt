package com.maikol.fitnessapp.feature.hydration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.core.database.WaterEntryEntity
import com.maikol.fitnessapp.core.preferences.Goal
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import com.maikol.fitnessapp.core.tracking.MAX_WATER_ML
import com.maikol.fitnessapp.core.tracking.TrackingRepository
import com.maikol.fitnessapp.core.tracking.waterTotalMl
import com.maikol.fitnessapp.feature.training.data.Parsed
import com.maikol.fitnessapp.feature.training.data.parseInt
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

/** Edición de una entrada concreta (solo la cantidad). */
data class WaterEdit(val entry: WaterEntryEntity, val text: String, val error: Boolean = false) {
    val dirty get() = text != entry.ml.toString()
}

data class HydrationUiState(
    val loading: Boolean = true,
    val today: LocalDate,
    val date: LocalDate,
    val entries: List<WaterEntryEntity> = emptyList(),
    val totalMl: Int = 0,
    val goalMl: Int? = null,
    val custom: String = "",
    val customError: Boolean = false,
    /** Alta en curso: los atajos ignoran toques hasta que termina (sin duplicados accidentales). */
    val adding: Boolean = false,
    /** ID de la última alta, para deshacer exactamente esa entrada. */
    val lastAddedId: String? = null,
    val lastAddedMl: Int? = null,
    val edit: WaterEdit? = null,
    val saving: Boolean = false,
    val failed: Boolean = false,
)

private data class Local(
    val custom: String = "",
    val customError: Boolean = false,
    val adding: Boolean = false,
    val lastAddedId: String? = null,
    val lastAddedMl: Int? = null,
    val edit: WaterEdit? = null,
    val saving: Boolean = false,
    val failed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HydrationViewModel(
    private val repository: TrackingRepository,
    preferences: UserPreferencesRepository,
) : ViewModel() {
    private val today = repository.today()
    private val date = MutableStateFlow(today)
    private val local = MutableStateFlow(Local())

    val state: StateFlow<HydrationUiState> = combine(
        date.flatMapLatest { d -> repository.observeWater(d).map { d to it } },
        preferences.userPreferences,
        local,
    ) { (d, entries), prefs, l ->
        HydrationUiState(
            loading = false,
            today = today,
            date = d,
            entries = entries,
            totalMl = waterTotalMl(entries),
            goalMl = prefs.goals[Goal.WATER],
            custom = l.custom,
            customError = l.customError,
            adding = l.adding,
            lastAddedId = l.lastAddedId,
            lastAddedMl = l.lastAddedMl,
            edit = l.edit,
            saving = l.saving,
            failed = l.failed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HydrationUiState(today = today, date = today))

    fun selectDate(value: LocalDate) {
        if (value <= today && local.value.edit == null) {
            date.value = value
            local.update { it.copy(lastAddedId = null, lastAddedMl = null) }
        }
    }

    /** Una única alta por toque aceptado; error no suma ni rellena. */
    fun add(ml: Int) {
        if (local.value.adding || ml !in 1..MAX_WATER_ML) return
        local.update { it.copy(adding = true, failed = false) }
        val day = date.value
        viewModelScope.launch {
            try {
                val id = repository.addWater(day, ml)
                local.update { it.copy(adding = false, lastAddedId = id, lastAddedMl = ml) }
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                local.update { it.copy(adding = false, failed = true) }
            }
        }
    }

    fun onCustom(text: String) = local.update { it.copy(custom = text, customError = false) }

    fun addCustom() {
        val ml = (parseInt(local.value.custom) as? Parsed.Value)?.value?.takeIf { it in 1..MAX_WATER_ML }
        if (ml == null) {
            local.update { it.copy(customError = true) }
            return
        }
        local.update { it.copy(custom = "") }
        add(ml)
    }

    /** Deshace solo la alta indicada por su ID (no la última fila de la lista). */
    fun undo() {
        val id = local.value.lastAddedId ?: return
        local.update { it.copy(lastAddedId = null, lastAddedMl = null) }
        remove(id)
    }

    fun dismissUndo() = local.update { it.copy(lastAddedId = null, lastAddedMl = null) }

    fun startEdit(entry: WaterEntryEntity) = local.update { it.copy(edit = WaterEdit(entry, entry.ml.toString()), failed = false) }

    fun onEditText(text: String) = local.update { l -> l.copy(edit = l.edit?.copy(text = text, error = false), failed = false) }

    fun closeEdit() {
        if (!local.value.saving) local.update { it.copy(edit = null, failed = false) }
    }

    fun saveEdit() {
        val l = local.value
        val edit = l.edit ?: return
        if (l.saving) return
        val ml = (parseInt(edit.text) as? Parsed.Value)?.value?.takeIf { it in 1..MAX_WATER_ML }
        if (ml == null) {
            local.update { it.copy(edit = edit.copy(error = true)) }
            return
        }
        local.update { it.copy(saving = true, failed = false) }
        viewModelScope.launch {
            try {
                repository.updateWater(edit.entry.id, ml)
                local.update { it.copy(saving = false, edit = null) }
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                local.update { it.copy(saving = false, failed = true) }
            }
        }
    }

    fun remove(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteWater(id)
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                local.update { it.copy(failed = true) }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as FitnessApplication).container
                HydrationViewModel(c.trackingRepository, c.userPreferencesRepository)
            }
        }
    }
}
