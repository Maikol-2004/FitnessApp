package com.maikol.fitnessapp.feature.sleep

import com.maikol.fitnessapp.core.database.SleepEntryEntity
import com.maikol.fitnessapp.core.tracking.MAX_NOTE_LENGTH
import com.maikol.fitnessapp.core.tracking.MAX_SLEEP_MS
import com.maikol.fitnessapp.core.tracking.TrackingRepository
import com.maikol.fitnessapp.core.tracking.sleepDurationMs
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class SleepError { END_NOT_AFTER_START, TOO_LONG, NOTE_TOO_LONG }

/** Inicio y fin elegidos como fecha + hora locales de la zona del dispositivo. */
data class SleepForm(
    val existing: SleepEntryEntity? = null,
    val startDate: LocalDate,
    val startTime: LocalTime,
    val endDate: LocalDate,
    val endTime: LocalTime,
    val quality: Int? = null,
    val note: String = "",
    val error: SleepError? = null,
    private val initial: List<Any?> = emptyList(),
) {
    private fun snapshot() = listOf(startDate, startTime, endDate, endTime, quality, note)

    val dirty get() = snapshot() != initial

    fun withInitial() = copy(initial = snapshot())

    companion object {
        /** Propuesta inicial: anoche 23:00 → hoy 07:00 (editable). */
        fun new(today: LocalDate) =
            SleepForm(startDate = today.minusDays(1), startTime = LocalTime.of(23, 0), endDate = today, endTime = LocalTime.of(7, 0)).withInitial()

        fun of(entry: SleepEntryEntity): SleepForm {
            val zone = ZoneId.of(entry.zoneId)
            val start = Instant.ofEpochMilli(entry.startAt).atZone(zone)
            val end = Instant.ofEpochMilli(entry.endAt).atZone(zone)
            return SleepForm(entry, start.toLocalDate(), start.toLocalTime(), end.toLocalDate(), end.toLocalTime(), entry.quality, entry.note.orEmpty()).withInitial()
        }
    }
}

data class SleepValues(val startAt: Long, val endAt: Long, val quality: Int?, val note: String?)

sealed interface SleepValidation {
    data class Valid(val values: SleepValues) : SleepValidation
    data class Invalid(val error: SleepError) : SleepValidation
}

/**
 * Convierte fecha+hora locales a instantes en [zone] y valida con la duración entre instantes (no restando
 * horas de reloj): cruce de medianoche y cambio de hora dan la duración real.
 */
fun validateSleep(form: SleepForm, zone: ZoneId): SleepValidation {
    val start = TrackingRepository.epochOf(form.startDate, form.startTime, zone)
    val end = TrackingRepository.epochOf(form.endDate, form.endTime, zone)
    val duration = sleepDurationMs(start, end)
    return when {
        duration <= 0 -> SleepValidation.Invalid(SleepError.END_NOT_AFTER_START)
        duration > MAX_SLEEP_MS -> SleepValidation.Invalid(SleepError.TOO_LONG)
        form.note.trim().length > MAX_NOTE_LENGTH -> SleepValidation.Invalid(SleepError.NOTE_TOO_LONG)
        else -> SleepValidation.Valid(SleepValues(start, end, form.quality, form.note.trim().takeIf { it.isNotEmpty() }))
    }
}
