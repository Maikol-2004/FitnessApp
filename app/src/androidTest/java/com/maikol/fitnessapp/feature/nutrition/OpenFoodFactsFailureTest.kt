package com.maikol.fitnessapp.feature.nutrition

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maikol.fitnessapp.core.database.FitnessDatabase
import com.maikol.fitnessapp.feature.nutrition.data.NutritionRepository
import com.maikol.fitnessapp.feature.nutrition.data.OpenFoodFactsClient
import com.maikol.fitnessapp.feature.nutrition.data.RemoteResult
import com.maikol.fitnessapp.feature.nutrition.ui.FoodSearchViewModel
import com.maikol.fitnessapp.feature.nutrition.ui.RemoteState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneOffset

/**
 * N1.1: un 200 con HTML o JSON inválido es un error recuperable, nunca una excepción ni «sin resultados».
 * Respuestas SINTÉTICAS («Prueba») servidas por un cliente falso: no hay red ni peticiones reales.
 */
@RunWith(AndroidJUnit4::class)
class OpenFoodFactsFailureTest {
    private val html = "<!DOCTYPE html><html><body>Mantenimiento</body></html>"
    private val validSearch = """{"hits":[{"code":"04963406","product_name":"Prueba uno","brands":"Marca Prueba"}]}"""

    /** Cliente que devuelve las respuestas indicadas, en orden, sin salir a la red. */
    private class FakeClient(vararg bodies: Pair<Int, String>) : OpenFoodFactsClient("Prueba/0") {
        private val queue = ArrayDeque(bodies.toList())
        var calls = 0
        override suspend fun get(url: String): HttpOutcome {
            calls++
            val (code, text) = queue.removeFirst()
            return HttpOutcome.Body(code, text)
        }
    }

    @Test
    fun searchResponse_invalidBodies_areRecoverableErrors() {
        for (body in listOf(html, "", "{\"hits\":[{\"code\":", "[]", "{}", "{\"hits\":\"no\"}", "null")) {
            assertEquals("cuerpo: $body", RemoteResult.InvalidResponse, OpenFoodFactsClient.parseSearchResponse(200, body))
        }
    }

    @Test
    fun searchResponse_realEmptyList_isZeroResults_andStatusesKeepMeaning() {
        assertEquals(RemoteResult.Ok(emptyList<Any>()), OpenFoodFactsClient.parseSearchResponse(200, """{"hits":[]}"""))
        assertEquals(RemoteResult.RateLimited, OpenFoodFactsClient.parseSearchResponse(429, html))
        assertEquals(RemoteResult.Unavailable, OpenFoodFactsClient.parseSearchResponse(503, html))
    }

    @Test
    fun productResponse_html200_isInvalid_notCrash() {
        assertEquals(RemoteResult.InvalidResponse, OpenFoodFactsClient.parseProductResponse(200, html, "0012345678905"))
        assertEquals(RemoteResult.InvalidResponse, OpenFoodFactsClient.parseProductResponse(200, "{\"product\":{", "0012345678905"))
    }

    @Test
    fun clientSearch_html200_returnsInvalidResponse() = runBlocking {
        val client = FakeClient(200 to html)
        assertEquals(RemoteResult.InvalidResponse, client.search("Prueba"))
        assertEquals(1, client.calls)
    }

    @Test
    fun viewModel_invalidThenRetry_showsErrorThenResults() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val db = Room.inMemoryDatabaseBuilder(context, FitnessDatabase::class.java).build()
        try {
            val client = FakeClient(200 to html, 200 to validSearch)
            val vm = FoodSearchViewModel(SavedStateHandle(mapOf("query" to "Prueba")), NutritionRepository(db, zone = { ZoneOffset.UTC }), client)
            vm.searchOnline()
            val error = withTimeout(5_000) { vm.state.first { it.remote is RemoteState.Error } }.remote as RemoteState.Error
            assertEquals(RemoteResult.InvalidResponse, error.result)
            // Reintentar con la misma consulta.
            vm.searchOnline()
            val ok = withTimeout(5_000) { vm.state.first { it.remote is RemoteState.Results } }.remote as RemoteState.Results
            assertEquals(listOf("04963406"), ok.hits.map { it.code })
            assertEquals(2, client.calls)
            assertTrue(ok.hits.single().name == "Prueba uno")
        } finally {
            db.close()
        }
    }
}
