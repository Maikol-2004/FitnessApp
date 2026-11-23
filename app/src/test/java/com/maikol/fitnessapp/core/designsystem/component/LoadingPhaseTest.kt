package com.maikol.fitnessapp.core.designsystem.component

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** H1-07: umbrales de la mancuerna (M04) con operaciones falsas y tiempo virtual. */
@OptIn(ExperimentalCoroutinesApi::class)
class LoadingPhaseTest {

    @Test
    fun fastOperation_50ms_neverShowsLoader() = runTest {
        val loading = MutableStateFlow(true)
        val phases = mutableListOf<LoadingPhase>()
        val job = launch { loading.toLoadingPhase().collect { phases += it } }
        runCurrent()
        advanceTimeBy(50)
        loading.value = false
        runCurrent()
        advanceTimeBy(1_000)
        job.cancel()
        assertFalse(LoadingPhase.VISIBLE in phases)
        assertEquals(LoadingPhase.HIDDEN, phases.last())
    }

    @Test
    fun slowOperation_800ms_showsLoaderAfter200msAndHidesImmediately() = runTest {
        val loading = MutableStateFlow(true)
        var phase = LoadingPhase.HIDDEN
        val job = launch { loading.toLoadingPhase().collect { phase = it } }
        advanceTimeBy(199)
        runCurrent()
        assertEquals(LoadingPhase.HIDDEN, phase)
        advanceTimeBy(2)
        runCurrent()
        assertEquals(LoadingPhase.VISIBLE, phase)
        advanceTimeBy(600) // ~800 ms en total
        loading.value = false
        runCurrent() // sin avanzar tiempo: no espera a terminar ningún ciclo
        assertEquals(LoadingPhase.HIDDEN, phase)
        job.cancel()
    }

    @Test
    fun verySlowOperation_showsSlowNoticeAt10s() = runTest {
        val loading = MutableStateFlow(true)
        var phase = LoadingPhase.HIDDEN
        val job = launch { loading.toLoadingPhase().collect { phase = it } }
        advanceTimeBy(9_999)
        runCurrent()
        assertEquals(LoadingPhase.VISIBLE, phase)
        advanceTimeBy(2)
        runCurrent()
        assertEquals(LoadingPhase.SLOW, phase)
        job.cancel()
    }

    @Test
    fun cancellation_stopsTimersWithoutLeavingLoaderState() = runTest {
        val loading = MutableStateFlow(true)
        val phases = mutableListOf<LoadingPhase>()
        val job = launch { loading.toLoadingPhase().collect { phases += it } }
        advanceTimeBy(100)
        job.cancel() // la pantalla se cierra durante la espera
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(listOf(LoadingPhase.HIDDEN), phases)
    }
}
