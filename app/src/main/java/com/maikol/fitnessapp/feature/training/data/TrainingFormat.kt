package com.maikol.fitnessapp.feature.training.data

import com.maikol.fitnessapp.core.preferences.UnitSystem
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

/** La interfaz es solo en español: fechas y cifras con convenciones es-ES (coma decimal, punto de miles). */
val AppLocale: Locale = Locale.forLanguageTag("es-ES")

const val KG_PER_LB = 0.45359237
const val MAX_LOAD_KG = 1_000.0
const val MAX_REPS = 999
const val MAX_REST_SECONDS = 3_600

fun UnitSystem.loadUnit(): String = if (this == UnitSystem.IMPERIAL) "lb" else "kg"

fun kgToDisplay(kg: Double, unit: UnitSystem): Double = if (unit == UnitSystem.IMPERIAL) kg / KG_PER_LB else kg

fun displayToKg(value: Double, unit: UnitSystem): Double = if (unit == UnitSystem.IMPERIAL) value * KG_PER_LB else value

/** Número para campos editables: sin separador de miles, hasta 2 decimales en kg y 1 en lb. */
fun formatLoadInput(kg: Double, unit: UnitSystem): String {
    val nf = NumberFormat.getNumberInstance(AppLocale).apply {
        isGroupingUsed = false
        maximumFractionDigits = if (unit == UnitSystem.IMPERIAL) 1 else 2
    }
    return nf.format(kgToDisplay(kg, unit))
}

/** Carga para lectura: «22,5 kg»; null/0 → «Sin carga». */
fun formatLoad(kg: Double?, unit: UnitSystem): String? =
    kg?.takeIf { it > 0.0 }?.let { "${formatLoadInput(it, unit)} ${unit.loadUnit()}" }

/** Volumen agregado: entero con separador de miles («2.400 kg»). */
fun formatVolume(kg: Double, unit: UnitSystem): String {
    val nf = NumberFormat.getIntegerInstance(AppLocale)
    return "${nf.format(kgToDisplay(kg, unit).roundToLong())} ${unit.loadUnit()}"
}

/** Cronómetro: «4:07», «12:34» o «1:02:09». */
fun formatClock(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0)) / 1_000
    val h = totalSeconds / 3_600
    val m = (totalSeconds % 3_600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(AppLocale, h, m, s) else "%d:%02d".format(AppLocale, m, s)
}

/** Cuenta atrás de descanso redondeando hacia arriba: «1:30» hasta que de verdad queda menos. */
fun formatCountdown(ms: Long): String = formatClock(((ms.coerceAtLeast(0) + 999) / 1_000) * 1_000)

/** Duración de resumen: «38 min», «1 h 05 min», «45 s». */
fun formatDuration(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0) / 1_000
    if (totalSeconds < 60) return "$totalSeconds s"
    val totalMinutes = totalSeconds / 60
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "%d h %02d min".format(AppLocale, h, m) else "$m min"
}

private val DateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", AppLocale)
private val DateYearFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy", AppLocale)
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm", AppLocale)

/** «lun 24 nov · 18:05» (año solo si no es el actual). */
fun formatSessionDate(epochMs: Long, zone: ZoneId = ZoneId.systemDefault(), nowMs: Long = System.currentTimeMillis()): String {
    val date = Instant.ofEpochMilli(epochMs).atZone(zone)
    val sameYear = date.year == Instant.ofEpochMilli(nowMs).atZone(zone).year
    val day = (if (sameYear) DateFormatter else DateYearFormatter).format(date).replace(".", "")
    return "${day.replaceFirstChar { it.titlecase(AppLocale) }} · ${TimeFormatter.format(date)}"
}

/** Resultado de leer un campo numérico. */
sealed interface Parsed<out T> {
    data class Value<T>(val value: T) : Parsed<T>
    data object Empty : Parsed<Nothing>
    data object Invalid : Parsed<Nothing>
}

private val DecimalPattern = Regex("""^\d{1,6}([.,]\d{0,3})?$""")
private val IntPattern = Regex("""^\d{1,6}$""")

/** Acepta coma o punto decimal; rechaza negativos, exponentes, NaN e infinito sin lanzar excepciones. */
fun parseDecimal(text: String): Parsed<Double> {
    val t = text.trim()
    if (t.isEmpty()) return Parsed.Empty
    if (!DecimalPattern.matches(t)) return Parsed.Invalid
    val v = t.replace(',', '.').trimEnd('.').toDoubleOrNull() ?: return Parsed.Invalid
    return if (v.isFinite()) Parsed.Value(v) else Parsed.Invalid
}

fun parseInt(text: String): Parsed<Int> {
    val t = text.trim()
    if (t.isEmpty()) return Parsed.Empty
    if (!IntPattern.matches(t)) return Parsed.Invalid
    return t.toIntOrNull()?.let { Parsed.Value(it) } ?: Parsed.Invalid
}

/**
 * Campo de carga que no deriva: guarda los kg de origen y el texto con que se mostraron. Si el texto no
 * cambia, se conservan los kg exactos (20 kg mostrados como «44,1 lb» siguen siendo 20 kg al guardar).
 */
data class LoadText(val text: String, val baseKg: Double?, val baseText: String) {
    /** Empty = sin carga (válido). Invalid = texto no numérico, negativo o por encima del máximo. */
    fun resolveKg(unit: UnitSystem): Parsed<Double> {
        if (text == baseText) return baseKg?.let { Parsed.Value(it) } ?: Parsed.Empty
        return when (val p = parseDecimal(text)) {
            is Parsed.Value -> {
                val kg = displayToKg(p.value, unit)
                if (kg > MAX_LOAD_KG) Parsed.Invalid else Parsed.Value(kg)
            }
            else -> p
        }
    }

    fun edited(newText: String) = copy(text = newText)

    companion object {
        fun of(kg: Double?, unit: UnitSystem): LoadText {
            val text = kg?.takeIf { it > 0.0 }?.let { formatLoadInput(it, unit) } ?: ""
            return LoadText(text, kg?.takeIf { it > 0.0 }, text)
        }
    }
}

fun parseReps(text: String): Int? = (parseInt(text) as? Parsed.Value)?.value?.takeIf { it in 1..MAX_REPS }

fun parseRestSeconds(text: String): Int? = when (val p = parseInt(text)) {
    is Parsed.Value -> p.value.takeIf { it in 0..MAX_REST_SECONDS }
    Parsed.Empty -> 0
    Parsed.Invalid -> null
}

/** Volumen = Σ kg externos × reps de series completadas que no son calentamiento. */
fun volumeKg(sets: Iterable<SetValues>): Double =
    sets.filter { it.completed && !it.warmup }.sumOf { (it.loadKg ?: 0.0) * (it.reps ?: 0) }

data class SetValues(val reps: Int?, val loadKg: Double?, val warmup: Boolean, val completed: Boolean)
