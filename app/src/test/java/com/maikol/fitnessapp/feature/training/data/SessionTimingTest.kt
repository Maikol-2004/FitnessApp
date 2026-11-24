package com.maikol.fitnessapp.feature.training.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** H2-07: tiempo efectivo y descanso por ancla; pausa, reinicio del móvil y cambio de hora. */
class SessionTimingTest {

    private val start = ClockReading(wallMs = 1_700_000_000_000, realtimeMs = 50_000)
    private fun later(ms: Long, wallOffset: Long = 0) = ClockReading(start.wallMs + ms + wallOffset, start.realtimeMs + ms)
    private val running = SessionTimer(0, start.wallMs, start.realtimeMs, null, null)

    @Test
    fun normalProgress_usesMonotonicClock() {
        val d = SessionTiming.delta(running, later(65_000))
        assertEquals(AnchorDelta(65_000, uncertain = false), d)
        assertEquals(65_000, SessionTiming.elapsedMs(running, later(65_000)))
    }

    @Test
    fun pause_freezesTimeAndRest() {
        val withRest = SessionTiming.startRest(running, later(10_000), 90)
        val paused = SessionTiming.pause(withRest, later(40_000))
        assertFalse(paused.running)
        // Diez minutos después sigue igual: 40 s efectivos y 60 s de descanso.
        val muchLater = later(640_000)
        assertEquals(40_000, SessionTiming.elapsedMs(paused, muchLater))
        assertEquals(60_000L, SessionTiming.restRemainingMs(paused, muchLater))
        val resumed = SessionTiming.resume(paused, muchLater)
        val oneMore = ClockReading(muchLater.wallMs + 5_000, muchLater.realtimeMs + 5_000)
        assertEquals(45_000, SessionTiming.elapsedMs(resumed, oneMore))
        assertEquals(55_000L, SessionTiming.restRemainingMs(resumed, oneMore))
    }

    @Test
    fun reboot_realtimeGoesBack_isUncertainAndUsesWallClock() {
        // Tras reiniciar, elapsedRealtime vuelve a empezar desde casi 0.
        val afterReboot = ClockReading(start.wallMs + 300_000, 2_000)
        val d = SessionTiming.delta(running, afterReboot)
        assertTrue(d.uncertain)
        assertEquals(300_000, d.ms)
    }

    @Test
    fun wallClockJumpsForward_isUncertainAndNeverInflatesDuration() {
        // Cambio de hora +1 h con solo 60 s reales: se toma el menor (60 s) y se marca dudoso.
        val d = SessionTiming.delta(running, later(60_000, wallOffset = 3_600_000))
        assertTrue(d.uncertain)
        assertEquals(60_000, d.ms)
    }

    @Test
    fun smallClockDrift_withinTolerance_isCertain() {
        assertFalse(SessionTiming.delta(running, later(60_000, wallOffset = 5_000)).uncertain)
    }

    @Test
    fun confirmTime_foldsAndReanchors() {
        val now = later(60_000, wallOffset = 3_600_000)
        val folded = SessionTiming.fold(running, now)
        assertEquals(60_000, folded.activeMs)
        assertFalse(SessionTiming.delta(folded, now).uncertain)
    }

    @Test
    fun restAdjust_movesTotalAndRemaining_andNeverBelowZero() {
        val rest = SessionTiming.startRest(running, start, 90)
        val plus = SessionTiming.adjustRest(rest, later(10_000), 15)
        assertEquals(95_000L, plus.restRemainingMs)
        assertEquals(105_000L, plus.restTotalMs)
        val minus = SessionTiming.adjustRest(plus, later(10_000), -200)
        assertEquals(0L, minus.restRemainingMs)
        assertEquals(0L, SessionTiming.restRemainingMs(minus, later(20_000)))
        assertNull(SessionTiming.restRemainingMs(SessionTiming.clearRest(minus, later(20_000)), later(30_000)))
    }
}
