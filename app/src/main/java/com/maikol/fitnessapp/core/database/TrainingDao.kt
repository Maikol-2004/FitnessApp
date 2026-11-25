package com.maikol.fitnessapp.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Acceso a rutinas y sesiones. Las operaciones compuestas se agrupan en transacciones en TrainingRepository. */
@Dao
interface TrainingDao {

    // ---- Rutinas ----

    @Query(
        """
        SELECT r.id, r.name, r.archivedAt, r.updatedAt,
            (SELECT COUNT(*) FROM routine_exercises e WHERE e.routineId = r.id) AS exerciseCount,
            (SELECT COUNT(*) FROM planned_sets s JOIN routine_exercises e ON s.exerciseId = e.id
                WHERE e.routineId = r.id) AS setCount
        FROM routines r
        ORDER BY r.archivedAt IS NOT NULL, r.updatedAt DESC
        """,
    )
    fun observeRoutineSummaries(): Flow<List<RoutineSummaryRow>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    fun observeRoutine(id: String): Flow<RoutineWithExercises?>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutine(id: String): RoutineWithExercises?

    @Upsert
    suspend fun upsertRoutine(routine: RoutineEntity)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteRoutineExercises(routineId: String)

    @Insert
    suspend fun insertRoutineExercises(exercises: List<RoutineExerciseEntity>)

    @Insert
    suspend fun insertPlannedSets(sets: List<PlannedSetEntity>)

    @Query("UPDATE routines SET archivedAt = :archivedAt, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: String, archivedAt: Long?, now: Long)

    // ---- Sesiones ----

    @Query("SELECT * FROM workout_sessions WHERE status IN ('ACTIVE', 'PAUSED') ORDER BY startedAt DESC LIMIT 1")
    fun observeOpenSession(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE status IN ('ACTIVE', 'PAUSED') ORDER BY startedAt DESC LIMIT 1")
    suspend fun getOpenSession(): WorkoutSessionEntity?

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun observeSession(id: String): Flow<SessionWithExercises?>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSessionEntity(id: String): WorkoutSessionEntity?

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity)

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Insert
    suspend fun insertSessionExercises(exercises: List<SessionExerciseEntity>)

    @Insert
    suspend fun insertPerformedSets(sets: List<PerformedSetEntity>)

    @Query("SELECT * FROM performed_sets WHERE id = :id")
    suspend fun getPerformedSet(id: String): PerformedSetEntity?

    @Query("SELECT * FROM performed_sets WHERE sessionExerciseId = :sessionExerciseId ORDER BY position")
    suspend fun getPerformedSets(sessionExerciseId: String): List<PerformedSetEntity>

    @Query("UPDATE performed_sets SET reps = :reps, loadKg = :loadKg WHERE id = :id")
    suspend fun updateSetValues(id: String, reps: Int?, loadKg: Double?)

    /** Devuelve 1 solo la primera vez: completar dos veces no duplica el evento ni el descanso. */
    @Query("UPDATE performed_sets SET reps = :reps, loadKg = :loadKg, completedAt = :now WHERE id = :id AND completedAt IS NULL")
    suspend fun completeSet(id: String, reps: Int, loadKg: Double?, now: Long): Int

    @Query("UPDATE performed_sets SET completedAt = NULL WHERE id = :id")
    suspend fun uncompleteSet(id: String)

    @Query(
        """
        SELECT s.id, s.routineNameSnapshot, s.startedAt, s.endedAt, s.activeMs,
            (SELECT COUNT(*) FROM performed_sets p JOIN session_exercises e ON p.sessionExerciseId = e.id
                WHERE e.sessionId = s.id AND p.completedAt IS NOT NULL) AS completedSets,
            (SELECT COUNT(DISTINCT e.id) FROM session_exercises e JOIN performed_sets p ON p.sessionExerciseId = e.id
                WHERE e.sessionId = s.id AND p.completedAt IS NOT NULL) AS exerciseCount,
            (SELECT COALESCE(SUM(p.loadKg * p.reps), 0) FROM performed_sets p
                JOIN session_exercises e ON p.sessionExerciseId = e.id
                WHERE e.sessionId = s.id AND p.completedAt IS NOT NULL AND p.warmup = 0
                    AND p.loadKg IS NOT NULL AND p.reps IS NOT NULL) AS volumeKg
        FROM workout_sessions s
        WHERE s.status = 'COMPLETED' AND s.endedAt IS NOT NULL
        ORDER BY s.endedAt DESC
        """,
    )
    fun observeHistory(): Flow<List<SessionHistoryRow>>

    // ---- G2: series completadas de un ejercicio en sesiones terminadas (sin reescribir snapshots) ----

    @Query(
        """
        SELECT s.id AS sessionId, s.routineNameSnapshot AS routineName, s.endedAt AS endedAt,
            e.position AS exercisePosition, p.position AS setPosition, p.reps AS reps, p.loadKg AS loadKg,
            p.warmup AS warmup
        FROM performed_sets p
        JOIN session_exercises e ON p.sessionExerciseId = e.id
        JOIN workout_sessions s ON e.sessionId = s.id
        WHERE s.status = 'COMPLETED' AND s.endedAt IS NOT NULL AND p.completedAt IS NOT NULL AND e.catalogId = :catalogId
        ORDER BY s.endedAt, e.position, p.position
        """,
    )
    fun observeCompletedSetsByCatalogId(catalogId: String): Flow<List<ExerciseSetRow>>

    /** Ejercicios personalizados o anteriores a G1: su identidad sigue siendo exerciseKey. */
    @Query(
        """
        SELECT s.id AS sessionId, s.routineNameSnapshot AS routineName, s.endedAt AS endedAt,
            e.position AS exercisePosition, p.position AS setPosition, p.reps AS reps, p.loadKg AS loadKg,
            p.warmup AS warmup
        FROM performed_sets p
        JOIN session_exercises e ON p.sessionExerciseId = e.id
        JOIN workout_sessions s ON e.sessionId = s.id
        WHERE s.status = 'COMPLETED' AND s.endedAt IS NOT NULL AND p.completedAt IS NOT NULL AND e.catalogId IS NULL AND e.exerciseKey = :exerciseKey
        ORDER BY s.endedAt, e.position, p.position
        """,
    )
    fun observeCompletedSetsByExerciseKey(exerciseKey: String): Flow<List<ExerciseSetRow>>
}
