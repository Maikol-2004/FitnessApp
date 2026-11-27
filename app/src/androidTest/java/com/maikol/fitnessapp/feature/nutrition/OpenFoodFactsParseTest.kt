package com.maikol.fitnessapp.feature.nutrition

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maikol.fitnessapp.feature.nutrition.data.OpenFoodFactsClient
import com.maikol.fitnessapp.feature.nutrition.data.QtyUnit
import com.maikol.fitnessapp.feature.nutrition.data.RemoteResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * N2: normalización de respuestas con la forma de la API v3 de Open Food Facts. Las respuestas son SINTÉTICAS
 * («Prueba»): validan el adaptador, no son datos de productos. org.json es el de Android, por eso es instrumentado.
 */
@RunWith(AndroidJUnit4::class)
class OpenFoodFactsParseTest {
    private fun parse(product: String, status: Int = 200) =
        OpenFoodFactsClient.parseProductResponse(status, """{"code":"0012345678905","status":"success","product":$product}""", "0012345678905")

    @Test
    fun per100g_values_brand_serving_andZeroKept() {
        val r = parse(
            """{"code":"0012345678905","product_name":"Prueba","product_name_es":"Prueba ES","brands":"Marca Prueba, Otra",
               "nutrition_data_per":"100g","serving_size":"30 g","serving_quantity":30,
               "nutriments":{"energy-kcal_100g":380,"proteins_100g":8.5,"carbohydrates_100g":"62,5","fat_100g":0}}""",
        ) as RemoteResult.Ok
        val p = r.value
        assertEquals("Prueba ES", p.name)
        assertEquals("Marca Prueba", p.brand)
        assertEquals(100.0, p.basisAmount, 0.0)
        assertEquals(QtyUnit.G, p.basisUnit)
        assertFalse(p.basisNeedsReview)
        assertEquals(380.0, p.nutrients.kcal!!, 0.0)
        assertEquals(62.5, p.nutrients.carbsG!!, 0.0)
        assertEquals(0.0, p.nutrients.fatG!!, 0.0)
        assertEquals(30.0, p.servingAmount!!, 0.0)
        assertEquals(QtyUnit.G, p.servingUnit)
        assertFalse(p.kcalFromKj)
        assertEquals("0012345678905", p.code)
    }

    @Test
    fun missingValuesStayUnknown_kjFallback_volumeNeedsReview() {
        val p = (parse(
            """{"product_name":"Prueba bebida","quantity":"1 l","product_quantity_unit":"ml",
               "nutriments":{"energy-kj_100g":418.4,"proteins_100g":null,"carbohydrates_100g":10}}""",
        ) as RemoteResult.Ok).value
        assertEquals(100.0, p.nutrients.kcal!!, 1e-9)
        assertTrue(p.kcalFromKj)
        assertNull(p.nutrients.proteinG)
        assertNull(p.nutrients.fatG)
        assertEquals(QtyUnit.ML, p.basisUnit)
        assertTrue(p.basisNeedsReview)
        assertNull("sin ración no se inventa", p.servingAmount)
    }

    @Test
    fun servingWithoutUnit_noEquivalenceInvented() {
        val p = (parse(
            """{"product_name":"Prueba","serving_size":"1 unidad","serving_quantity":2,
               "nutriments":{"energy-kcal_100g":100}}""",
        ) as RemoteResult.Ok).value
        assertNull(p.servingUnit)
        assertNull(p.servingAmount)
    }

    @Test
    fun servingOnlyBasis_used_whenNoPer100() {
        val p = (parse(
            """{"product_name":"Prueba","nutrition_data_per":"serving","serving_size":"2 piezas",
               "nutriments":{"energy-kcal_serving":90,"proteins_serving":3}}""",
        ) as RemoteResult.Ok).value
        assertEquals(QtyUnit.SERVING, p.basisUnit)
        assertEquals(1.0, p.basisAmount, 0.0)
        assertEquals(90.0, p.nutrients.kcal!!, 0.0)
        assertFalse(p.basisNeedsReview)
    }

    @Test
    fun statuses_noNutrition_notFound_rateLimited() {
        assertEquals(RemoteResult.NoNutrition, parse("""{"product_name":"Prueba","nutriments":{}}"""))
        assertEquals(RemoteResult.NoNutrition, parse("""{"nutriments":{"energy-kcal_100g":1}}"""))
        assertEquals(RemoteResult.NotFound, OpenFoodFactsClient.parseProductResponse(404, "{}", "0012345678905"))
        assertEquals(RemoteResult.RateLimited, OpenFoodFactsClient.parseProductResponse(429, "", "0012345678905"))
        assertEquals(RemoteResult.Unavailable, OpenFoodFactsClient.parseProductResponse(503, "", "0012345678905"))
    }

    @Test
    fun search_hitsNormalized_invalidSkipped() {
        val hits = (OpenFoodFactsClient.parseSearchResponse(
            200,
            """{"hits":[{"code":"04963406","product_name":"Prueba uno","brands":["Marca Prueba"],"quantity":"500 g"},
                        {"code":"abc","product_name":"Inválido"},{"code":"0012345678905"}]}""",
        ) as RemoteResult.Ok).value
        assertEquals(1, hits.size)
        assertEquals("04963406", hits.single().code)
        assertEquals("Marca Prueba", hits.single().brand)
        assertEquals("500 g", hits.single().quantity)
    }
}
