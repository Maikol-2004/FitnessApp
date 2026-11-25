package com.maikol.fitnessapp.core.tracking

import com.maikol.fitnessapp.core.database.MealEntryEntity
import com.maikol.fitnessapp.core.database.SleepEntryEntity
import com.maikol.fitnessapp.core.database.WaterEntryEntity
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.feature.training.data.AppLocale
import com.maikol.fitnessapp.feature.training.data.HistoryEntry
import com.maikol.fitnessapp.feature.training.data.Parsed
import com.maikol.fitnessapp.feature.training.data.parseDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/* Cálculos puros de los registros manuales F1 (sin Android): totales, metas, fechas y unidades. */

const val METERS_PER_MILE = 1_609.344

const val MAX_STEPS = 200_000
const val MAX_DISTANCE_KM = 1_000.0
const val MAX_ACTIVE_MINUTES = 1_440
const val MAX_ACTIVE_KCAL = 20_000.0
const val MAX_MEAL_KCAL = 10_000.0
const val MAX_MACRO_G = 1_000.0
const val MAX_WATER_ML = 5_000
const val MAX_SLEEP_MS = 24L * 60 * 60 * 1_000
const val MIN_WEIGHT_KG = 20.0
const val MAX_WEIGHT_KG = 500.0
const val MAX_NAME_LENGTH = 60
const val MAX_NOTE_LENGTH = 200

fun waterTotalMl(entries: List<WaterEntryEntity>): Int = entries.sumOf { it.ml }

/**
 * Totales del día: suma de lo conocido. [incomplete] marca un campo cuando unas comidas lo indican y otras
 * no (desconocido no es cero). Un campo sin ningún valor conocido es null y se muestra «—».
 */
data class NutritionTotals(
    val kcal: Double?,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    val kcalIncomplete: Boolean,
    val proteinIncomplete: Boolean,
    val carbsIncomplete: Boolean,
    val fatIncomplete: Boolean,
) {
    val anyIncomplete get() = kcalIncomplete || proteinIncomplete || carbsIncomplete || fatIncomplete
}

fun nutritionTotals(meals: List<MealEntryEntity>): NutritionTotals {
    fun total(pick: (MealEntryEntity) -> Double?): Double? =
        meals.mapNotNull(pick).takeIf { it.isNotEmpty() }?.sum()
    // Parcial: hay valores conocidos y otros desconocidos. Si ninguna comida lo indica, el total es null (—).
    fun incomplete(pick: (MealEntryEntity) -> Double?) = meals.any { pick(it) == null } && meals.any { pick(it) != null }
    return NutritionTotals(
        kcal = total { it.kcal },
        proteinG = total { it.proteinG },
        carbsG = total { it.carbsG },
        fatG = total { it.fatG },
        kcalIncomplete = incomplete { it.kcal },
        proteinIncomplete = incomplete { it.proteinG },
        carbsIncomplete = incomplete { it.carbsG },
        fatIncomplete = incomplete { it.fatG },
    )
}

/** Fracción visual 0..1 del aro o barra; null si falta el valor o la meta (no se inventa un porcentaje). */
fun goalFraction(value: Double?, goal: Double?): Float? {
    if (value == null || goal == null || goal <= 0.0) return null
    return (value / goal).toFloat().coerceIn(0f, 1f)
}

fun localDateOf(epochMs: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()

/** Duración real entre instantes: cruce de medianoche y cambio de hora incluidos. */
fun sleepDurationMs(startAt: Long, endAt: Long): Long = endAt - startAt

/** Total de las noches cuyo despertar cae en los 7 días que terminan en [today] (sin solapes: se rechazan). */
fun sleepWeekTotalMs(entries: List<SleepEntryEntity>, today: LocalDate): Long {
    val from = today.minusDays(6)
    return entries.filter { LocalDate.parse(it.wakeDate) in from..today }.sumOf { sleepDurationMs(it.startAt, it.endAt) }
}

data class WeekTraining(val sessions: Int, val volumeKg: Double)

/**
 * Sesiones COMPLETED (el historial H2 ya excluye las descartadas) terminadas en los últimos 7 días
 * naturales, hoy incluido, y su volumen con el mismo cálculo de H2.
 */
fun weekTraining(history: List<HistoryEntry>, today: LocalDate, zone: ZoneId): WeekTraining {
    val fromMs = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
    val inWeek = history.filter { it.endedAt >= fromMs }
    return WeekTraining(inWeek.size, inWeek.sumOf { it.volumeKg })
}

// ---- Distancia ----

fun UnitSystem.distanceUnit(): String = if (this == UnitSystem.IMPERIAL) "mi" else "km"

fun metersToDisplay(meters: Double, unit: UnitSystem): Double =
    if (unit == UnitSystem.IMPERIAL) meters / METERS_PER_MILE else meters / 1_000.0

fun displayToMeters(value: Double, unit: UnitSystem): Double =
    if (unit == UnitSystem.IMPERIAL) value * METERS_PER_MILE else value * 1_000.0

fun formatDistanceInput(meters: Double, unit: UnitSystem): String =
    NumberFormat.getNumberInstance(AppLocale).apply {
        isGroupingUsed = false
        maximumFractionDigits = 2
    }.format(metersToDisplay(meters, unit))

fun formatDistance(meters: Double, unit: UnitSystem): String = "${formatDistanceInput(meters, unit)} ${unit.distanceUnit()}"

/**
 * Campo de distancia que no deriva al cambiar km/mi: si el texto mostrado no cambia, conserva los metros
 * originales (mismo contrato que LoadText de H2).
 */
data class DistanceText(val text: String, val baseMeters: Double?, val baseText: String) {
    fun resolveMeters(unit: UnitSystem): Parsed<Double> {
        if (text == baseText) return baseMeters?.let { Parsed.Value(it) } ?: Parsed.Empty
        return when (val p = parseDecimal(text)) {
            is Parsed.Value -> {
                val m = displayToMeters(p.value, unit)
                if (m > MAX_DISTANCE_KM * 1_000) Parsed.Invalid else Parsed.Value(m)
            }
            else -> p
        }
    }

    companion object {
        fun of(meters: Double?, unit: UnitSystem): DistanceText {
            val text = meters?.let { formatDistanceInput(it, unit) } ?: ""
            return DistanceText(text, meters, text)
        }
    }
}

// ---- Formatos ----

private val Decimal1 = NumberFormat.getNumberInstance(AppLocale).apply { maximumFractionDigits = 1 }
private val Integer = NumberFormat.getIntegerInstance(AppLocale)

fun formatDecimal(value: Double): String = Decimal1.format(value)

fun formatInteger(value: Number): String = Integer.format(value)

fun formatKcal(value: Double): String = "${Integer.format(Math.round(value))} kcal"

fun formatGrams(value: Double): String = "${Decimal1.format(value)} g"

fun formatMl(value: Int): String = "${Integer.format(value)} ml"

/** Número editable sin separador de miles: «22,5», «0». */
fun formatInput(value: Double): String = NumberFormat.getNumberInstance(AppLocale).apply {
    isGroupingUsed = false
    maximumFractionDigits = 2
}.format(value)
