package com.maikol.fitnessapp.core.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/*
 * Esquema de entrenamiento (v2). IDs UUID en texto; instantes en epoch millis UTC; cargas siempre en kg.
 * Rutinas: plantilla editable. Sesiones: copia (snapshot) independiente, de modo que editar o archivar
 * una rutina nunca altera el historial. Las posiciones son únicas por padre para mantener el orden.
 */

@Entity(tableName = "routines", indices = [Index("archivedAt")])
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    /** null = activa. Archivar es reversible y no borra sesiones. */
    val archivedAt: Long?,
)

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(RoutineEntity::class, ["id"], ["routineId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["routineId", "position"], unique = true)],
)
data class RoutineExerciseEntity(
    @PrimaryKey val id: String,
    val routineId: String,
    /** Identidad estable del ejercicio; se conserva al duplicar. Base para comparar marcas en el futuro. */
    val exerciseKey: String,
    val position: Int,
    val name: String,
    val notes: String,
    /** G1: definición del catálogo local (null = ejercicio personalizado o anterior a G1). No sustituye a exerciseKey. */
    @ColumnInfo(defaultValue = "NULL") val catalogId: String? = null,
)

@Entity(
    tableName = "planned_sets",
    foreignKeys = [
        ForeignKey(RoutineExerciseEntity::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["exerciseId", "position"], unique = true)],
)
data class PlannedSetEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val position: Int,
    val reps: Int,
    val loadKg: Double?,
    val restSeconds: Int,
    val warmup: Boolean,
)

object SessionStatus {
    const val ACTIVE = "ACTIVE"
    const val PAUSED = "PAUSED"
    const val COMPLETED = "COMPLETED"
    const val DISCARDED = "DISCARDED"
}

/**
 * Sesión. El tiempo efectivo se guarda como [activeMs] acumulado hasta un ancla (reloj de pared +
 * elapsedRealtime). Mientras está activa, el tiempo transcurrido desde el ancla se suma al leer; en pausa
 * el ancla es null y el tiempo queda congelado. El descanso comparte el mismo ancla ([restRemainingMs]
 * es lo que quedaba en ese instante), así que pausar congela ambos.
 */
@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(RoutineEntity::class, ["id"], ["routineId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("routineId"), Index("status"), Index("endedAt")],
)
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val routineId: String?,
    val routineNameSnapshot: String,
    val status: String,
    val startedAt: Long,
    val endedAt: Long?,
    val activeMs: Long,
    val anchorWallMs: Long?,
    val anchorRealtimeMs: Long?,
    val restTotalMs: Long?,
    val restRemainingMs: Long?,
    val currentExercise: Int,
)

@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(WorkoutSessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["sessionId", "position"], unique = true)],
)
data class SessionExerciseEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseKey: String,
    val position: Int,
    val name: String,
    val notes: String,
    /** G1: definición del catálogo local (null = ejercicio personalizado o anterior a G1). No sustituye a exerciseKey. */
    @ColumnInfo(defaultValue = "NULL") val catalogId: String? = null,
)

@Entity(
    tableName = "performed_sets",
    foreignKeys = [
        ForeignKey(SessionExerciseEntity::class, ["id"], ["sessionExerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["sessionExerciseId", "position"], unique = true)],
)
data class PerformedSetEntity(
    @PrimaryKey val id: String,
    val sessionExerciseId: String,
    val position: Int,
    /** Objetivo copiado de la rutina; null en series añadidas durante la sesión. */
    val targetReps: Int?,
    val targetLoadKg: Double?,
    val restSeconds: Int,
    val warmup: Boolean,
    /** Valores realizados (precargados con el objetivo). */
    val reps: Int?,
    val loadKg: Double?,
    /** null = pendiente. Solo las completadas cuentan para volumen y progreso. */
    val completedAt: Long?,
)

data class ExerciseWithSets(
    @Embedded val exercise: RoutineExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "exerciseId")
    val sets: List<PlannedSetEntity>,
)

data class RoutineWithExercises(
    @Embedded val routine: RoutineEntity,
    @Relation(entity = RoutineExerciseEntity::class, parentColumn = "id", entityColumn = "routineId")
    val exercises: List<ExerciseWithSets>,
)

data class SessionExerciseWithSets(
    @Embedded val exercise: SessionExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionExerciseId")
    val sets: List<PerformedSetEntity>,
)

data class SessionWithExercises(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(entity = SessionExerciseEntity::class, parentColumn = "id", entityColumn = "sessionId")
    val exercises: List<SessionExerciseWithSets>,
)

/** Fila de lista de rutinas con sus conteos. */
data class RoutineSummaryRow(
    val id: String,
    val name: String,
    val archivedAt: Long?,
    val updatedAt: Long,
    val exerciseCount: Int,
    val setCount: Int,
)

/** Fila de historial: sesión completada con sus métricas calculadas en SQL. */
data class SessionHistoryRow(
    val id: String,
    val routineNameSnapshot: String,
    val startedAt: Long,
    val endedAt: Long,
    val activeMs: Long,
    val completedSets: Int,
    val exerciseCount: Int,
    val volumeKg: Double,
)
