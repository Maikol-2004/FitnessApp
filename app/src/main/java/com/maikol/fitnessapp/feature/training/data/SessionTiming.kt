package com.maikol.fitnessapp.feature.training.data

import android.os.SystemClock
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Instante doble: reloj de pared (persistible) y reloj monotónico desde el arranque (inmune a cambios de hora). */
data class ClockReading(val wallMs: Long, val realtimeMs: Long)

fun interface TrainingClock {
    fun now(): ClockReading
}

val SystemTrainingClock = TrainingClock { ClockReading(System.currentTimeMillis(), SystemClock.elapsedRealtime()) }

/**
 * Estado temporal persistido de una sesión. [activeMs] y [restRemainingMs] son los valores en el instante
 * del ancla; si el ancla es null la sesión está en pausa y ambos quedan congelados.
 */
data class SessionTimer(
    val activeMs: Long,
    val anchorWallMs: Long?,
    val anchorRealtimeMs: Long?,
    val restTotalMs: Long?,
    val restRemainingMs: Long?,
) {
    val running: Boolean get() = anchorWallMs != null && anchorRealtimeMs != null
    val hasRest: Boolean get() = restTotalMs != null && restRemainingMs != null
}

/** Tiempo transcurrido desde el ancla. [uncertain]: no se pudo verificar (reinicio del móvil o cambio de hora). */
data class AnchorDelta(val ms: Long, val uncertain: Boolean)

object SessionTiming {
    /** Diferencia admitida entre ambos relojes antes de considerar que la hora cambió. */
    const val TOLERANCE_MS = 120_000L

    /**
     * Mientras el proceso y el arranque son los mismos, manda el reloj monotónico. Si retrocede (reinicio)
     * o discrepa del de pared (cambio de hora), se toma el menor valor no negativo y se marca como dudoso
     * para que la persona confirme: nunca se inventa exactitud ni se infla la duración.
     */
    fun delta(timer: SessionTimer, now: ClockReading): AnchorDelta {
        val anchorWall = timer.anchorWallMs ?: return AnchorDelta(0, false)
        val anchorRt = timer.anchorRealtimeMs ?: return AnchorDelta(0, false)
        val dRt = now.realtimeMs - anchorRt
        val dWall = now.wallMs - anchorWall
        return when {
            dRt >= 0 && abs(dWall - dRt) <= TOLERANCE_MS -> AnchorDelta(dRt, false)
            dRt >= 0 -> AnchorDelta(min(dRt, max(dWall, 0)), true)
            else -> AnchorDelta(max(dWall, 0), true)
        }
    }

    fun elapsedMs(timer: SessionTimer, now: ClockReading): Long =
        max(0, timer.activeMs + delta(timer, now).ms)

    /** null si no hay descanso; 0 cuando ha terminado. */
    fun restRemainingMs(timer: SessionTimer, now: ClockReading): Long? {
        val remaining = timer.restRemainingMs ?: return null
        return max(0, remaining - delta(timer, now).ms)
    }

    /** Consolida lo transcurrido en los valores acumulados y vuelve a anclar en [now] si seguía en marcha. */
    fun fold(timer: SessionTimer, now: ClockReading): SessionTimer {
        if (!timer.running) return timer
        val d = delta(timer, now).ms
        return timer.copy(
            activeMs = max(0, timer.activeMs + d),
            anchorWallMs = now.wallMs,
            anchorRealtimeMs = now.realtimeMs,
            restRemainingMs = timer.restRemainingMs?.let { max(0, it - d) },
        )
    }

    fun pause(timer: SessionTimer, now: ClockReading): SessionTimer =
        fold(timer, now).copy(anchorWallMs = null, anchorRealtimeMs = null)

    fun resume(timer: SessionTimer, now: ClockReading): SessionTimer =
        if (timer.running) timer else timer.copy(anchorWallMs = now.wallMs, anchorRealtimeMs = now.realtimeMs)

    fun startRest(timer: SessionTimer, now: ClockReading, seconds: Int): SessionTimer {
        val total = seconds * 1_000L
        return fold(timer, now).copy(restTotalMs = total, restRemainingMs = total)
    }

    fun clearRest(timer: SessionTimer, now: ClockReading): SessionTimer =
        fold(timer, now).copy(restTotalMs = null, restRemainingMs = null)

    /** Ajusta el descanso en curso: total y restante se mueven juntos, sin bajar de 0. */
    fun adjustRest(timer: SessionTimer, now: ClockReading, deltaSeconds: Int): SessionTimer {
        val folded = fold(timer, now)
        val total = folded.restTotalMs ?: return folded
        val remaining = folded.restRemainingMs ?: return folded
        val d = deltaSeconds * 1_000L
        val newRemaining = max(0, remaining + d)
        return folded.copy(restTotalMs = max(newRemaining, max(1_000, total + d)), restRemainingMs = newRemaining)
    }
}
