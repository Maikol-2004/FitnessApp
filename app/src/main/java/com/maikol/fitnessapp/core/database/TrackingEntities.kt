package com.maikol.fitnessapp.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Registros manuales F1 (v3). Política de fechas (docs/10_FUNCTIONAL_MVP.md):
 * - Actividad diaria y peso: fecha local elegida por el usuario («2026-11-25»); no cambia al viajar.
 * - Comidas y agua: instante UTC + zona de origen; se agrupan por [localDate], calculada en esa zona al
 *   guardar, de modo que lista y total usan siempre el mismo día.
 * - Sueño: inicio/fin UTC + zona; la noche se atribuye a la fecha local del despertar ([wakeDate]).
 * Cantidades normalizadas: metros, kg, ml, kcal y gramos. Nunca números ya formateados.
 */

object RecordSource {
    const val MANUAL = "MANUAL"
}

/** Un registro manual por fecha. Cada campo es opcional: null = no introducido (distinto de 0). */
@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey val date: String,
    val steps: Int?,
    val distanceMeters: Double?,
    val activeMinutes: Int?,
    val activeKcal: Double?,
    val source: String,
    val updatedAt: Long,
)

/** Valores totales de esta comida (no por 100 g). Macros null = desconocido, no cero. */
@Entity(tableName = "meal_entries", indices = [Index("localDate")])
data class MealEntryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val occurredAt: Long,
    val zoneId: String,
    val localDate: String,
    val kcal: Double?,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "water_entries", indices = [Index("localDate")])
data class WaterEntryEntity(
    @PrimaryKey val id: String,
    val ml: Int,
    val occurredAt: Long,
    val zoneId: String,
    val localDate: String,
    val createdAt: Long,
)

/** [quality]: «Cómo has descansado», 1–5 subjetivo y opcional; no es un diagnóstico. */
@Entity(tableName = "sleep_entries", indices = [Index("wakeDate"), Index("startAt"), Index("endAt")])
data class SleepEntryEntity(
    @PrimaryKey val id: String,
    val startAt: Long,
    val endAt: Long,
    val zoneId: String,
    val wakeDate: String,
    val quality: Int?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "body_weight_entries", indices = [Index("date")])
data class BodyWeightEntity(
    @PrimaryKey val id: String,
    val date: String,
    val kg: Double,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
