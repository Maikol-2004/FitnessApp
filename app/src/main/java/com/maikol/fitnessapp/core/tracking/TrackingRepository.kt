package com.maikol.fitnessapp.core.tracking

import androidx.room.withTransaction
import com.maikol.fitnessapp.core.database.BodyWeightEntity
import com.maikol.fitnessapp.core.database.DailyActivityEntity
import com.maikol.fitnessapp.core.database.FitnessDatabase
import com.maikol.fitnessapp.core.database.RecordSource
import com.maikol.fitnessapp.core.database.SleepEntryEntity
import com.maikol.fitnessapp.core.database.WaterEntryEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

sealed interface SleepSaveResult {
    data object Saved : SleepSaveResult
    data object Invalid : SleepSaveResult

    /** No se suma dos veces la misma noche: se rechaza hasta que el usuario corrija las horas. */
    data class Overlap(val other: SleepEntryEntity) : SleepSaveResult
}

/**
 * Registros manuales F1 sobre Room. Todas las escrituras lanzan si fallan: el ViewModel conserva el
 * formulario y muestra el error (nunca un éxito simulado). Reloj y zona inyectables para pruebas.
 */
open class TrackingRepository(
    private val db: FitnessDatabase,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val todayRecheckMs: Long = TODAY_RECHECK_MS,
) {
    private val dao = db.trackingDao()

    fun today(): LocalDate = localDateOf(now(), zone())

    fun zone(): ZoneId = zone.invoke()

    /**
     * «Hoy» vivo: emite al suscribirse (al volver a la app la pantalla se resuscribe) y otra vez al pasar la
     * medianoche local. Revisa al menos cada minuto para seguir cambios manuales de reloj o de zona.
     */
    open fun observeToday(): Flow<LocalDate> = flow {
        while (true) {
            val z = zone()
            val nowMs = now()
            val day = localDateOf(nowMs, z)
            emit(day)
            val nextMidnight = day.plusDays(1).atStartOfDay(z).toInstant().toEpochMilli()
            delay((nextMidnight - nowMs).coerceIn(1L, todayRecheckMs))
        }
    }.distinctUntilChanged()

    /** Instante para un registro hecho «ahora» en [date]: la hora actual de ese día. */
    private fun instantOn(date: LocalDate, time: LocalTime = LocalTime.now(zone())): Long =
        if (date == today()) now() else date.atTime(time).atZone(zone()).toInstant().toEpochMilli()

    // ---- Actividad ----

    fun observeActivity(date: LocalDate): Flow<DailyActivityEntity?> = dao.observeActivity(date.toString())

    /** Editar sustituye los valores del día (no suma). Requiere al menos un valor. */
    open suspend fun saveActivity(date: LocalDate, steps: Int?, distanceMeters: Double?, activeMinutes: Int?, activeKcal: Double?) {
        require(steps != null || distanceMeters != null || activeMinutes != null || activeKcal != null)
        dao.upsertActivity(DailyActivityEntity(date.toString(), steps, distanceMeters, activeMinutes, activeKcal, RecordSource.MANUAL, now()))
    }

    suspend fun deleteActivity(date: LocalDate) {
        dao.deleteActivity(date.toString())
    }

    // ---- Agua ----

    fun observeWater(date: LocalDate): Flow<List<WaterEntryEntity>> = dao.observeWater(date.toString())

    /** Una alta por llamada; devuelve su ID para poder deshacer exactamente esa entrada. */
    open suspend fun addWater(date: LocalDate, ml: Int): String {
        require(ml in 1..MAX_WATER_ML)
        val id = newId()
        dao.insertWater(WaterEntryEntity(id, ml, instantOn(date), zone().id, date.toString(), now()))
        return id
    }

    suspend fun updateWater(id: String, ml: Int) {
        require(ml in 1..MAX_WATER_ML)
        dao.updateWaterMl(id, ml)
    }

    suspend fun deleteWater(id: String) {
        dao.deleteWater(id)
    }

    // ---- Sueño ----

    fun observeRecentSleep(limit: Int = 30): Flow<List<SleepEntryEntity>> = dao.observeRecentSleep(limit)

    open suspend fun saveSleep(existing: SleepEntryEntity?, startAt: Long, endAt: Long, quality: Int?, note: String?): SleepSaveResult {
        val duration = sleepDurationMs(startAt, endAt)
        if (duration <= 0 || duration > MAX_SLEEP_MS || (quality != null && quality !in 1..5)) return SleepSaveResult.Invalid
        return db.withTransaction {
            val id = existing?.id ?: newId()
            dao.findOverlappingSleep(startAt, endAt, id).firstOrNull()?.let { return@withTransaction SleepSaveResult.Overlap(it) }
            val zoneId = zone()
            val t = now()
            dao.upsertSleep(
                SleepEntryEntity(
                    id = id,
                    startAt = startAt,
                    endAt = endAt,
                    zoneId = zoneId.id,
                    wakeDate = localDateOf(endAt, zoneId).toString(),
                    quality = quality,
                    note = note?.trim()?.takeIf { it.isNotEmpty() },
                    createdAt = existing?.createdAt ?: t,
                    updatedAt = t,
                ),
            )
            SleepSaveResult.Saved
        }
    }

    suspend fun deleteSleep(id: String) {
        dao.deleteSleep(id)
    }

    // ---- Peso ----

    fun observeWeights(): Flow<List<BodyWeightEntity>> = dao.observeWeights()

    open suspend fun saveWeight(existing: BodyWeightEntity?, date: LocalDate, kg: Double, note: String?) {
        require(kg.isFinite() && kg in MIN_WEIGHT_KG..MAX_WEIGHT_KG)
        val t = now()
        dao.upsertWeight(
            BodyWeightEntity(
                id = existing?.id ?: newId(),
                date = date.toString(),
                kg = kg,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                createdAt = existing?.createdAt ?: t,
                updatedAt = t,
            ),
        )
    }

    suspend fun deleteWeight(id: String) {
        dao.deleteWeight(id)
    }

    companion object {
        const val TODAY_RECHECK_MS = 60_000L

        fun epochOf(date: LocalDate, time: LocalTime, zone: ZoneId): Long = date.atTime(time).atZone(zone).toInstant().toEpochMilli()

        fun timeOf(epochMs: Long, zone: ZoneId): LocalTime = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalTime()
    }
}
