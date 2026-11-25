package com.maikol.fitnessapp.feature.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.core.database.SleepEntryEntity
import com.maikol.fitnessapp.core.tracking.SleepSaveResult
import com.maikol.fitnessapp.core.tracking.TrackingRepository
import com.maikol.fitnessapp.core.tracking.sleepWeekTotalMs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SleepUiState(
    val loading: Boolean = true,
    val today: LocalDate,
    val entries: List<SleepEntryEntity> = emptyList(),
    val weekTotalMs: Long = 0,
    val weekNights: Int = 0,
    val form: SleepForm? = null,
    val saving: Boolean = false,
    val failed: Boolean = false,
    /** Registro con el que se solapa el intervalo propuesto (no se guarda). */
    val overlap: SleepEntryEntity? = null,
    val deleteFailed: Boolean = false,
)

private data class SleepEdit(
    val form: SleepForm? = null,
    val saving: Boolean = false,
    val failed: Boolean = false,
    val overlap: SleepEntryEntity? = null,
    val deleteFailed: Boolean = false,
)

class SleepViewModel(private val repository: TrackingRepository) : ViewModel() {
    private val today = repository.today()
    private val edit = MutableStateFlow(SleepEdit())

    val state: StateFlow<SleepUiState> = combine(repository.observeRecentSleep(), edit) { entries, e ->
        val from = today.minusDays(6)
        SleepUiState(
            loading = false,
            today = today,
            entries = entries,
            weekTotalMs = sleepWeekTotalMs(entries, today),
            weekNights = entries.count { LocalDate.parse(it.wakeDate) in from..today },
            form = e.form,
            saving = e.saving,
            failed = e.failed,
            overlap = e.overlap,
            deleteFailed = e.deleteFailed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SleepUiState(today = today))

    fun add() {
        edit.value = SleepEdit(form = SleepForm.new(today))
    }

    fun edit(entry: SleepEntryEntity) {
        edit.value = SleepEdit(form = SleepForm.of(entry))
    }

    fun closeForm() {
        if (!edit.value.saving) edit.value = SleepEdit()
    }

    /** Corregir una fecha u hora conserva el resto del formulario. */
    fun update(transform: (SleepForm) -> SleepForm) = edit.update { e ->
        e.form?.let { e.copy(form = transform(it).copy(error = null), failed = false, overlap = null) } ?: e
    }

    fun save() {
        val e = edit.value
        val form = e.form ?: return
        if (e.saving) return
        when (val v = validateSleep(form, repository.zone())) {
            is SleepValidation.Invalid -> edit.value = e.copy(form = form.copy(error = v.error))
            is SleepValidation.Valid -> {
                edit.value = e.copy(saving = true, failed = false, overlap = null)
                viewModelScope.launch {
                    try {
                        val x = v.values
                        edit.value = when (val r = repository.saveSleep(form.existing, x.startAt, x.endAt, x.quality, x.note)) {
                            SleepSaveResult.Saved -> SleepEdit()
                            is SleepSaveResult.Overlap -> edit.value.copy(saving = false, overlap = r.other)
                            SleepSaveResult.Invalid -> edit.value.copy(saving = false, form = form.copy(error = SleepError.END_NOT_AFTER_START))
                        }
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
                repository.deleteSleep(id)
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
                SleepViewModel((this[APPLICATION_KEY] as FitnessApplication).container.trackingRepository)
            }
        }
    }
}
