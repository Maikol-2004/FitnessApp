package com.maikol.fitnessapp.core.tracking

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maikol.fitnessapp.core.database.FitnessDatabase
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import com.maikol.fitnessapp.feature.health.HealthViewModel
import com.maikol.fitnessapp.feature.hydration.HydrationViewModel
import com.maikol.fitnessapp.feature.sleep.SleepViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Corrección F1: «Hoy» avanza con un reloj controlado (sin tocar el del sistema) y la cantidad de agua
 * personalizada sobrevive a un fallo de escritura. Base en memoria y preferencias temporales: no toca datos reales.
 */
@RunWith(AndroidJUnit4::class)
class DayChangeAndWaterTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, FitnessDatabase::class.java).build()
    private val storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val storeFile = File(context.cacheDir, "day-change-test.preferences_pb").apply { delete() }
    private val prefs = UserPreferencesRepository(PreferenceDataStoreFactory.create(scope = storeScope) { storeFile })

    private val zone = ZoneId.of("Europe/Madrid")
    private val clock = AtomicLong(at(2026, 11, 25, 23, 50))
    private val nov24 = LocalDate.of(2026, 11, 24)
    private val nov25 = LocalDate.of(2026, 11, 25)
    private val nov26 = LocalDate.of(2026, 11, 26)

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int) =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

    private fun repo() = TrackingRepository(db, now = clock::get, zone = { zone }, todayRecheckMs = 50)

    @After
    fun close() {
        storeScope.cancel()
        db.close()
        storeFile.delete()
    }

    @Test
    fun today_followsMidnight_keepsChosenDate_andOpenFormKeepsItsDay() = runBlocking {
        val repo = repo()
        withContext(Dispatchers.Main) {
            val vm = HealthViewModel(repo, prefs)
            val sleep = SleepViewModel(repo)
            val collector = launch { vm.state.collect {} }
            val sleepCollector = launch { sleep.state.collect {} }
            withTimeout(5_000) { vm.state.first { !it.loading && it.date == nov25 } }

            // 1) Sin fecha elegida, «Hoy» pasa al día nuevo a medianoche (Salud y Sueño).
            clock.set(at(2026, 11, 26, 0, 1))
            val next = withTimeout(5_000) { vm.state.first { it.today == nov26 } }
            assertEquals(nov26, next.date)
            withTimeout(5_000) { sleep.state.first { it.today == nov26 } }

            // 2) Una fecha anterior elegida a propósito se conserva al cambiar de día.
            vm.selectDate(nov24)
            withTimeout(5_000) { vm.state.first { it.date == nov24 } }
            clock.set(at(2026, 11, 27, 0, 1))
            val kept = withTimeout(5_000) { vm.state.first { it.today == LocalDate.of(2026, 11, 27) } }
            assertEquals(nov24, kept.date)

            // 3) Volver a «Hoy» y abrir el formulario: si pasa la medianoche, sigue en su día y guarda ahí.
            vm.selectDate(LocalDate.of(2026, 11, 27))
            withTimeout(5_000) { vm.state.first { it.date == LocalDate.of(2026, 11, 27) } }
            vm.openForm()
            vm.update { it.copy(steps = "500") }
            clock.set(at(2026, 11, 28, 0, 1))
            val pinned = withTimeout(5_000) { vm.state.first { it.today == LocalDate.of(2026, 11, 28) } }
            assertEquals(LocalDate.of(2026, 11, 27), pinned.date)
            vm.save()
            withTimeout(5_000) { vm.state.first { it.form == null && !it.saving } }
            assertEquals(500, repo.observeActivity(LocalDate.of(2026, 11, 27)).first()?.steps)
            assertNull(repo.observeActivity(LocalDate.of(2026, 11, 28)).first())
            // Al cerrar el formulario vuelve a seguir a «Hoy».
            withTimeout(5_000) { vm.state.first { it.date == LocalDate.of(2026, 11, 28) } }

            collector.cancel()
            sleepCollector.cancel()
        }
    }

    /** Primera escritura bloqueada y fallida; las siguientes, normales. */
    private class FlakyRepository(db: FitnessDatabase, now: () -> Long, zone: ZoneId) :
        TrackingRepository(db, now = now, zone = { zone }) {
        val gate = CompletableDeferred<Unit>()
        val calls = AtomicInteger()

        override suspend fun addWater(date: LocalDate, ml: Int): String {
            if (calls.incrementAndGet() == 1) {
                gate.await()
                throw IOException("disco lleno (simulado)")
            }
            return super.addWater(date, ml)
        }
    }

    @Test
    fun customWater_keptWhileSavingAndOnFailure_clearedOnlyAfterSuccess() = runBlocking {
        val repo = FlakyRepository(db, clock::get, zone)
        withContext(Dispatchers.Main) {
            val vm = HydrationViewModel(repo, prefs)
            val collector = launch { vm.state.collect {} }
            withTimeout(5_000) { vm.state.first { !it.loading } }

            vm.onCustom("330")
            vm.addCustom()
            vm.addCustom() // doble toque mientras guarda: se ignora
            val saving = withTimeout(5_000) { vm.state.first { it.adding } }
            assertEquals("330", saving.custom)

            repo.gate.complete(Unit)
            val failed = withTimeout(5_000) { vm.state.first { it.failed && !it.adding } }
            assertEquals("330", failed.custom)
            assertEquals(0, failed.totalMl)
            assertEquals(1, repo.calls.get())

            vm.addCustom()
            val ok = withTimeout(5_000) { vm.state.first { it.totalMl == 330 && !it.adding } }
            assertEquals("", ok.custom)
            assertFalse(ok.failed)
            assertEquals(1, ok.entries.size)
            assertTrue(ok.lastAddedId != null)
            collector.cancel()
        }
    }
}
