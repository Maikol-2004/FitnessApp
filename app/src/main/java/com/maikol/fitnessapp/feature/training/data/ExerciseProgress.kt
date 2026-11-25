package com.maikol.fitnessapp.feature.training.data

import com.maikol.fitnessapp.core.database.ExerciseSetRow
import com.maikol.fitnessapp.feature.training.catalog.LoadSemantics

/*
 * G2: progreso de un ejercicio con registros reales (docs/14_EXERCISE_MEDIA_AND_PROGRESS.md).
 * Entrada: series COMPLETADAS de sesiones TERMINADAS (la consulta ya excluye activas, descartadas y pendientes).
 * - Serie de trabajo: no calentamiento y repeticiones ≥ 1.
 * - Serie con carga: serie de trabajo con loadKg > 0. Nunca se usa el objetivo planificado como realizado.
 * - null no es 0: sin series con carga no hay «0 kg» de récord.
 */

enum class ProgressPeriod(val days: Long?) { ALL(null), DAYS_90(90), DAYS_30(30) }

data class LoadMark(val loadKg: Double, val reps: Int, val endedAt: Long)
data class VolumeMark(val volumeKg: Double, val sets: Int, val endedAt: Long)
data class AverageLoad(val loadKg: Double, val sets: Int)
data class ChartPoint(val endedAt: Long, val valueKg: Double)
data class PracticeRef(val sessionId: String, val endedAt: Long)
data class DoneSet(val reps: Int?, val loadKg: Double?, val warmup: Boolean)

/** Una sesión en la que aparece el ejercicio (una vez aunque esté dos veces en la rutina). */
data class ExerciseSessionEntry(
    val sessionId: String,
    val routineName: String,
    val endedAt: Long,
    val sets: List<DoneSet>,
    /** Suma carga × reps de sus series con carga; null si ninguna llevaba carga externa. */
    val volumeKg: Double?,
    val maxLoadKg: Double?,
    val workSets: Int,
)

data class ExerciseProgress(
    val semantics: LoadSemantics,
    val period: ProgressPeriod,
    /** Primera y última práctica de TODO el historial (sesión con ≥1 serie de trabajo). */
    val firstPractice: PracticeRef?,
    val lastPractice: PracticeRef?,
    val sessionsInPeriod: Int,
    val workSetsInPeriod: Int,
    /** Solo carga externa; null con asistencia o sin series con carga. */
    val maxLoad: LoadMark?,
    val bestVolume: VolumeMark?,
    val averageLoad: AverageLoad?,
    /** Carga máx. por sesión (externa) o asistencia mínima por sesión (asistida), en orden de fecha. */
    val chart: List<ChartPoint>,
    /** Todas las sesiones, la más reciente primero (sin filtro de periodo). */
    val history: List<ExerciseSessionEntry>,
) {
    val hasPractice: Boolean get() = firstPractice != null
}

private fun ExerciseSetRow.isWork() = !warmup && (reps ?: 0) >= 1
private fun ExerciseSetRow.hasLoad() = isWork() && (loadKg ?: 0.0) > 0.0

fun exerciseProgress(
    rows: List<ExerciseSetRow>,
    semantics: LoadSemantics,
    period: ProgressPeriod = ProgressPeriod.ALL,
    nowMs: Long = System.currentTimeMillis(),
): ExerciseProgress {
    val bySession = rows.groupBy { it.sessionId }.values
        .map { sets -> sets.sortedWith(compareBy({ it.exercisePosition }, { it.setPosition })) }
        .sortedBy { it.first().endedAt }
    val history = bySession.map { sets ->
        val loaded = sets.filter { it.hasLoad() }
        ExerciseSessionEntry(
            sessionId = sets.first().sessionId,
            routineName = sets.first().routineName,
            endedAt = sets.first().endedAt,
            sets = sets.map { DoneSet(it.reps, it.loadKg, it.warmup) },
            volumeKg = if (semantics == LoadSemantics.EXTERNAL && loaded.isNotEmpty()) loaded.sumOf { it.loadKg!! * it.reps!! } else null,
            maxLoadKg = loaded.maxOfOrNull { it.loadKg!! },
            workSets = sets.count { it.isWork() },
        )
    }
    val practiced = history.filter { it.workSets > 0 }
    val cutoff = period.days?.let { nowMs - it * 86_400_000L }
    val inPeriod = practiced.filter { cutoff == null || it.endedAt >= cutoff }
    val periodRows = rows.filter { r -> cutoff == null || r.endedAt >= cutoff }
    val loadedRows = periodRows.filter { it.hasLoad() }

    val external = semantics == LoadSemantics.EXTERNAL
    return ExerciseProgress(
        semantics = semantics,
        period = period,
        firstPractice = practiced.firstOrNull()?.let { PracticeRef(it.sessionId, it.endedAt) },
        lastPractice = practiced.lastOrNull()?.let { PracticeRef(it.sessionId, it.endedAt) },
        sessionsInPeriod = inPeriod.size,
        workSetsInPeriod = periodRows.count { it.isWork() },
        // Empate: cuenta la primera vez que se alcanzó.
        maxLoad = if (!external) null else loadedRows.sortedBy { it.endedAt }.maxByOrNull { it.loadKg!! }
            ?.let { LoadMark(it.loadKg!!, it.reps!!, it.endedAt) },
        bestVolume = if (!external) null else inPeriod.filter { it.volumeKg != null }.maxByOrNull { it.volumeKg!! }
            ?.let { e -> VolumeMark(e.volumeKg!!, e.sets.count { it.hasLoad() }, e.endedAt) },
        averageLoad = if (!external || loadedRows.isEmpty()) null
        else AverageLoad(loadedRows.sumOf { it.loadKg!! } / loadedRows.size, loadedRows.size),
        chart = inPeriod.mapNotNull { e ->
            val loaded = e.sets.filter { it.hasLoad() }.mapNotNull { it.loadKg }
            val value = if (external) loaded.maxOrNull() else loaded.minOrNull()
            value?.let { ChartPoint(e.endedAt, it) }
        },
        history = history.reversed(),
    )
}

private fun DoneSet.hasLoad() = !warmup && (reps ?: 0) >= 1 && (loadKg ?: 0.0) > 0.0
