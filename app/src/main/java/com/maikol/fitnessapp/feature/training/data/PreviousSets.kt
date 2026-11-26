package com.maikol.fitnessapp.feature.training.data

import com.maikol.fitnessapp.core.database.ExerciseSetRow
import com.maikol.fitnessapp.feature.training.catalog.AssistMode
import com.maikol.fitnessapp.feature.training.catalog.LoadSemantics

/** V2: serie completada de la sesión anterior, tal como se registró (carga null = sin carga; 0 es un valor). */
data class PreviousSet(val reps: Int?, val loadKg: Double?, val warmup: Boolean)

/** V2: última sesión terminada con series comparables del mismo ejercicio. Solo referencia: no cambia objetivos. */
data class PreviousSession(val sessionId: String, val endedAt: Long, val sets: List<PreviousSet>) {
    /** La n-ésima serie de calentamiento se compara con la n-ésima de calentamiento; la de trabajo, con la de trabajo. */
    fun counterpart(warmup: Boolean, ordinal: Int): PreviousSet? = sets.filter { it.warmup == warmup }.getOrNull(ordinal)
}

/** V2: resultado de la consulta; [session] null = no hay datos comparables («Sin registro anterior»). */
data class PreviousRef(val session: PreviousSession?)

/**
 * Referencia «Anterior» a partir de las series completadas en sesiones terminadas (consultas G2 por catalogId o
 * exerciseKey; la sesión activa no está incluida). En la dominada asistida solo es comparable la misma modalidad
 * indicada: sin modalidad, o con otra, no hay referencia (máquina y banda no se mezclan).
 * Si el ejercicio se repitió en esa sesión, se toma su primera aparición.
 */
fun previousSession(rows: List<ExerciseSetRow>, semantics: LoadSemantics, assistMode: AssistMode?): PreviousSession? {
    val comparable = if (semantics == LoadSemantics.ASSISTANCE) {
        if (assistMode == null) return null
        rows.filter { AssistMode.fromStored(it.assistMode) == assistMode }
    } else {
        rows
    }
    val latest = comparable.maxByOrNull { it.endedAt } ?: return null
    val inSession = comparable.filter { it.sessionId == latest.sessionId }
    val first = inSession.minOf { it.exercisePosition }
    val sets = inSession.filter { it.exercisePosition == first }
        .sortedBy { it.setPosition }
        .map { PreviousSet(it.reps, it.loadKg, it.warmup) }
    return PreviousSession(latest.sessionId, latest.endedAt, sets)
}
