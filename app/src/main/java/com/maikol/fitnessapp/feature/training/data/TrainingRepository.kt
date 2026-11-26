package com.maikol.fitnessapp.feature.training.data

import androidx.room.withTransaction
import com.maikol.fitnessapp.core.database.ExerciseSetRow
import com.maikol.fitnessapp.core.database.FitnessDatabase
import com.maikol.fitnessapp.core.database.PerformedSetEntity
import com.maikol.fitnessapp.core.database.PlannedSetEntity
import com.maikol.fitnessapp.core.database.RoutineEntity
import com.maikol.fitnessapp.core.database.RoutineExerciseEntity
import com.maikol.fitnessapp.core.database.RoutineWithExercises
import com.maikol.fitnessapp.core.database.SessionExerciseEntity
import com.maikol.fitnessapp.core.database.SessionStatus
import com.maikol.fitnessapp.core.database.SessionWithExercises
import com.maikol.fitnessapp.core.database.WorkoutSessionEntity
import com.maikol.fitnessapp.feature.training.catalog.AssistMode
import com.maikol.fitnessapp.feature.training.catalog.LoadSemantics
import com.maikol.fitnessapp.feature.training.catalog.loadSemanticsOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.UUID

// ---- Modelos de dominio (cargas en kg) ----

data class RoutineSummary(
    val id: String,
    val name: String,
    val exerciseCount: Int,
    val setCount: Int,
    val archived: Boolean,
)

data class PlannedSet(val id: String, val reps: Int, val loadKg: Double?, val restSeconds: Int, val warmup: Boolean)

data class RoutineExercise(
    val id: String,
    val key: String,
    val name: String,
    val notes: String,
    val sets: List<PlannedSet>,
    val catalogId: String? = null,
    /** G2.1: solo en la dominada asistida; null = sin indicar. */
    val assistMode: AssistMode? = null,
)

data class Routine(
    val id: String,
    val name: String,
    val notes: String,
    val archived: Boolean,
    val exercises: List<RoutineExercise>,
) {
    val setCount: Int get() = exercises.sumOf { it.sets.size }
}

data class PerformedSet(
    val id: String,
    val position: Int,
    val targetReps: Int?,
    val targetLoadKg: Double?,
    val restSeconds: Int,
    val warmup: Boolean,
    val reps: Int?,
    val loadKg: Double?,
    val completed: Boolean,
)

data class SessionExercise(
    val id: String,
    val key: String,
    val name: String,
    val notes: String,
    val sets: List<PerformedSet>,
    val catalogId: String? = null,
    val assistMode: AssistMode? = null,
)

data class Session(
    val id: String,
    val routineId: String?,
    val routineName: String,
    val status: String,
    val startedAt: Long,
    val endedAt: Long?,
    val timer: SessionTimer,
    val currentExercise: Int,
    val exercises: List<SessionExercise>,
) {
    val isOpen: Boolean get() = status == SessionStatus.ACTIVE || status == SessionStatus.PAUSED
    val isPaused: Boolean get() = status == SessionStatus.PAUSED
    val totalSets: Int get() = exercises.sumOf { it.sets.size }
    val completedSets: Int get() = exercises.sumOf { e -> e.sets.count { it.completed } }
    val volumeKg: Double
        get() = volumeKg(
            exercises.flatMap { e ->
                val external = loadSemanticsOf(e.catalogId) == LoadSemantics.EXTERNAL
                e.sets.map { SetValues(it.reps, it.loadKg, it.warmup, it.completed, external) }
            },
        )
}

data class OpenSession(val id: String, val routineId: String?, val routineName: String, val paused: Boolean)

data class HistoryEntry(
    val id: String,
    val routineName: String,
    val endedAt: Long,
    val durationMs: Long,
    val completedSets: Int,
    val exerciseCount: Int,
    val volumeKg: Double,
)

sealed interface StartResult {
    data class Started(val sessionId: String) : StartResult
    /** Ya había una sesión abierta: se continúa esa, nunca se crea otra en silencio. */
    data class AlreadyOpen(val sessionId: String) : StartResult
    data object NotRunnable : StartResult
}

sealed interface FinishResult {
    data object Completed : FinishResult
    data object AlreadyCompleted : FinishResult
    data object NotOpen : FinishResult
}

/**
 * Fuente única de rutinas y sesiones (Room). Todas las operaciones compuestas son transacciones y las
 * de sesión son idempotentes: repetirlas (doble toque, reintento) no duplica datos ni eventos.
 */
open class TrainingRepository(
    private val db: FitnessDatabase,
    private val clock: TrainingClock,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private val dao = db.trainingDao()

    fun newId(): String = newId.invoke()

    // ---- Rutinas ----

    fun observeRoutines(): Flow<List<RoutineSummary>> = dao.observeRoutineSummaries().map { rows ->
        rows.map { RoutineSummary(it.id, it.name, it.exerciseCount, it.setCount, it.archivedAt != null) }
    }

    fun observeRoutine(id: String): Flow<Routine?> = dao.observeRoutine(id).map { it?.toDomain() }.distinctUntilChanged()

    suspend fun getRoutine(id: String): Routine? = dao.getRoutine(id)?.toDomain()

    /** Guarda la rutina completa (ya validada) reemplazando ejercicios y series en una transacción. */
    open suspend fun saveRoutine(routine: Routine) {
        require(routine.name.isNotBlank() && routine.exercises.isNotEmpty() && routine.exercises.all { it.sets.isNotEmpty() })
        val now = clock.now().wallMs
        db.withTransaction {
            val existing = dao.getRoutine(routine.id)?.routine
            dao.upsertRoutine(
                RoutineEntity(
                    id = routine.id,
                    name = routine.name.trim(),
                    notes = routine.notes.trim(),
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                    archivedAt = existing?.archivedAt,
                ),
            )
            dao.deleteRoutineExercises(routine.id)
            dao.insertRoutineExercises(
                routine.exercises.mapIndexed { i, e ->
                    RoutineExerciseEntity(e.id, routine.id, e.key, i, e.name.trim(), e.notes.trim(), e.catalogId, e.assistMode?.name)
                },
            )
            dao.insertPlannedSets(
                routine.exercises.flatMap { e ->
                    e.sets.mapIndexed { i, s -> PlannedSetEntity(s.id, e.id, i, s.reps, s.loadKg, s.restSeconds, s.warmup) }
                },
            )
        }
    }

    /** Copia con IDs nuevos; conserva exerciseKey (mismo ejercicio) y no copia el archivado. */
    suspend fun duplicateRoutine(id: String, nameSuffix: String): String? {
        val original = getRoutine(id) ?: return null
        val copy = original.copy(
            id = newId(),
            name = "${original.name}$nameSuffix",
            archived = false,
            exercises = original.exercises.map { e ->
                e.copy(id = newId(), sets = e.sets.map { it.copy(id = newId()) })
            },
        )
        saveRoutine(copy)
        return copy.id
    }

    open suspend fun setArchived(id: String, archived: Boolean) {
        val now = clock.now().wallMs
        dao.setArchived(id, if (archived) now else null, now)
    }

    // ---- Sesiones ----

    fun observeOpenSession(): Flow<OpenSession?> = dao.observeOpenSession()
        .map { s -> s?.let { OpenSession(it.id, it.routineId, it.routineNameSnapshot, it.status == SessionStatus.PAUSED) } }
        .distinctUntilChanged()

    fun observeSession(id: String): Flow<Session?> = dao.observeSession(id).map { it?.toDomain() }.distinctUntilChanged()

    /** Crea la sesión con copia de la rutina en una transacción. Si ya hay una abierta, devuelve esa. */
    open suspend fun startSession(routineId: String): StartResult = db.withTransaction {
        dao.getOpenSession()?.let { return@withTransaction StartResult.AlreadyOpen(it.id) }
        val routine = dao.getRoutine(routineId)?.toDomain()
        if (routine == null || routine.archived || routine.exercises.isEmpty()) return@withTransaction StartResult.NotRunnable
        val now = clock.now()
        val sessionId = newId()
        dao.insertSession(
            WorkoutSessionEntity(
                id = sessionId,
                routineId = routine.id,
                routineNameSnapshot = routine.name,
                status = SessionStatus.ACTIVE,
                startedAt = now.wallMs,
                endedAt = null,
                activeMs = 0,
                anchorWallMs = now.wallMs,
                anchorRealtimeMs = now.realtimeMs,
                restTotalMs = null,
                restRemainingMs = null,
                currentExercise = 0,
            ),
        )
        val exercises = routine.exercises.mapIndexed { i, e -> SessionExerciseEntity(newId(), sessionId, e.key, i, e.name, e.notes, e.catalogId, e.assistMode?.name) }
        dao.insertSessionExercises(exercises)
        dao.insertPerformedSets(
            routine.exercises.zip(exercises).flatMap { (planned, copy) ->
                planned.sets.mapIndexed { i, s ->
                    PerformedSetEntity(
                        id = newId(), sessionExerciseId = copy.id, position = i,
                        targetReps = s.reps, targetLoadKg = s.loadKg, restSeconds = s.restSeconds, warmup = s.warmup,
                        reps = s.reps, loadKg = s.loadKg, completedAt = null,
                    )
                }
            },
        )
        StartResult.Started(sessionId)
    }

    /** Guarda valores válidos de una serie (pendiente o completada). Completada: el volumen se recalcula. */
    open suspend fun updateSetValues(setId: String, reps: Int?, loadKg: Double?) {
        dao.updateSetValues(setId, reps, loadKg)
    }

    /**
     * Completa la serie con sus valores y, solo si es la primera vez, arranca su descanso.
     * Devuelve false si ya estaba completada (doble toque): ni duplica ni reinicia el descanso.
     */
    open suspend fun completeSet(sessionId: String, setId: String, reps: Int, loadKg: Double?): Boolean = db.withTransaction {
        val session = dao.getSessionEntity(sessionId)?.takeIf { it.isOpen() } ?: return@withTransaction false
        val set = dao.getPerformedSet(setId) ?: return@withTransaction false
        val now = clock.now()
        val changed = dao.completeSet(setId, reps, loadKg, now.wallMs) == 1
        if (changed) {
            val timer = session.timer()
            val next = if (set.restSeconds > 0) SessionTiming.startRest(timer, now, set.restSeconds) else SessionTiming.clearRest(timer, now)
            dao.updateSession(session.withTimer(next))
        }
        changed
    }

    open suspend fun uncompleteSet(setId: String) = dao.uncompleteSet(setId)

    /** Añade una serie al final copiando valores y descanso de la última (sin objetivo planificado). */
    suspend fun addSet(sessionExerciseId: String) = db.withTransaction {
        val sets = dao.getPerformedSets(sessionExerciseId)
        val last = sets.lastOrNull()
        dao.insertPerformedSets(
            listOf(
                PerformedSetEntity(
                    id = newId(), sessionExerciseId = sessionExerciseId, position = (last?.position ?: -1) + 1,
                    targetReps = null, targetLoadKg = null, restSeconds = last?.restSeconds ?: 0, warmup = false,
                    reps = last?.reps, loadKg = last?.loadKg, completedAt = null,
                ),
            ),
        )
    }

    suspend fun setCurrentExercise(sessionId: String, index: Int) = mutateOpen(sessionId) { it.copy(currentExercise = index) }

    open suspend fun pause(sessionId: String) = mutateOpen(sessionId) {
        it.withTimer(SessionTiming.pause(it.timer(), clock.now())).copy(status = SessionStatus.PAUSED)
    }

    open suspend fun resume(sessionId: String) = mutateOpen(sessionId) {
        it.withTimer(SessionTiming.resume(it.timer(), clock.now())).copy(status = SessionStatus.ACTIVE)
    }

    suspend fun skipRest(sessionId: String) = mutateOpen(sessionId) { it.withTimer(SessionTiming.clearRest(it.timer(), clock.now())) }

    suspend fun adjustRest(sessionId: String, deltaSeconds: Int) =
        mutateOpen(sessionId) { it.withTimer(SessionTiming.adjustRest(it.timer(), clock.now(), deltaSeconds)) }

    /** Acepta el tiempo mostrado cuando no se pudo verificar (reinicio/cambio de hora) y vuelve a anclar. */
    suspend fun confirmTime(sessionId: String) = mutateOpen(sessionId) { it.withTimer(SessionTiming.fold(it.timer(), clock.now())) }

    /** Idempotente: la segunda llamada devuelve AlreadyCompleted sin tocar nada. */
    open suspend fun finish(sessionId: String): FinishResult = db.withTransaction {
        val s = dao.getSessionEntity(sessionId) ?: return@withTransaction FinishResult.NotOpen
        when (s.status) {
            SessionStatus.COMPLETED -> FinishResult.AlreadyCompleted
            SessionStatus.DISCARDED -> FinishResult.NotOpen
            else -> {
                val now = clock.now()
                val stopped = SessionTiming.pause(s.timer(), now)
                dao.updateSession(
                    s.withTimer(stopped.copy(restTotalMs = null, restRemainingMs = null))
                        .copy(status = SessionStatus.COMPLETED, endedAt = now.wallMs),
                )
                FinishResult.Completed
            }
        }
    }

    /** Marca DISCARDED (no se borra): queda fuera del historial y del volumen. */
    open suspend fun discard(sessionId: String) = mutateOpen(sessionId) {
        val now = clock.now()
        it.withTimer(SessionTiming.pause(it.timer(), now).copy(restTotalMs = null, restRemainingMs = null))
            .copy(status = SessionStatus.DISCARDED, endedAt = now.wallMs)
    }

    fun observeHistory(): Flow<List<HistoryEntry>> = dao.observeHistory().map { rows ->
        rows.map { HistoryEntry(it.id, it.routineNameSnapshot, it.endedAt, it.activeMs, it.completedSets, it.exerciseCount, it.volumeKg) }
    }

    /** G2: series completadas por definición del catálogo, en cualquier rutina. */
    fun observeCatalogExerciseSets(catalogId: String): Flow<List<ExerciseSetRow>> =
        dao.observeCompletedSetsByCatalogId(catalogId).distinctUntilChanged()

    /** G2: personalizados/legado por su exerciseKey; nunca se mezclan con una definición del catálogo. */
    fun observeCustomExerciseSets(exerciseKey: String): Flow<List<ExerciseSetRow>> =
        dao.observeCompletedSetsByExerciseKey(exerciseKey).distinctUntilChanged()

    /** G2.1: indicar máquina o banda durante la sesión (solo sesiones abiertas). */
    open suspend fun setSessionAssistMode(sessionExerciseId: String, mode: AssistMode?): Boolean =
        dao.setSessionAssistMode(sessionExerciseId, mode?.name) > 0

    fun now(): ClockReading = clock.now()

    private suspend fun mutateOpen(sessionId: String, transform: (WorkoutSessionEntity) -> WorkoutSessionEntity) {
        db.withTransaction {
            val s = dao.getSessionEntity(sessionId)?.takeIf { it.isOpen() } ?: return@withTransaction
            dao.updateSession(transform(s))
        }
    }
}

private fun WorkoutSessionEntity.isOpen() = status == SessionStatus.ACTIVE || status == SessionStatus.PAUSED

private fun WorkoutSessionEntity.timer() = SessionTimer(activeMs, anchorWallMs, anchorRealtimeMs, restTotalMs, restRemainingMs)

private fun WorkoutSessionEntity.withTimer(t: SessionTimer) = copy(
    activeMs = t.activeMs,
    anchorWallMs = t.anchorWallMs,
    anchorRealtimeMs = t.anchorRealtimeMs,
    restTotalMs = t.restTotalMs,
    restRemainingMs = t.restRemainingMs,
)

private fun RoutineWithExercises.toDomain() = Routine(
    id = routine.id,
    name = routine.name,
    notes = routine.notes,
    archived = routine.archivedAt != null,
    exercises = exercises.sortedBy { it.exercise.position }.map { e ->
        RoutineExercise(
            id = e.exercise.id, key = e.exercise.exerciseKey, name = e.exercise.name, notes = e.exercise.notes,
            catalogId = e.exercise.catalogId,
            assistMode = AssistMode.fromStored(e.exercise.assistMode),
            sets = e.sets.sortedBy { it.position }.map { PlannedSet(it.id, it.reps, it.loadKg, it.restSeconds, it.warmup) },
        )
    },
)

private fun SessionWithExercises.toDomain() = Session(
    id = session.id,
    routineId = session.routineId,
    routineName = session.routineNameSnapshot,
    status = session.status,
    startedAt = session.startedAt,
    endedAt = session.endedAt,
    timer = session.timer(),
    currentExercise = session.currentExercise,
    exercises = exercises.sortedBy { it.exercise.position }.map { e ->
        SessionExercise(
            id = e.exercise.id, key = e.exercise.exerciseKey, name = e.exercise.name, notes = e.exercise.notes,
            catalogId = e.exercise.catalogId,
            assistMode = AssistMode.fromStored(e.exercise.assistMode),
            sets = e.sets.sortedBy { it.position }.map {
                PerformedSet(it.id, it.position, it.targetReps, it.targetLoadKg, it.restSeconds, it.warmup, it.reps, it.loadKg, it.completedAt != null)
            },
        )
    },
)
