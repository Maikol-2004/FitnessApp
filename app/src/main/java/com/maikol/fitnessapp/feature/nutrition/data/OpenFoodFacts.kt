package com.maikol.fitnessapp.feature.nutrition.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException

/*
 * N2 — Open Food Facts, solo lectura (contrato consultado el 27-11-2026 en
 * https://openfoodfacts.github.io/openfoodfacts-server/api/):
 * - Producto: GET /api/v3/product/{código} (15 lecturas/min/IP). Búsqueda de texto: Search-a-licious
 *   (search.openfoodfacts.org), siempre por acción explícita (10 búsquedas/min/IP, nunca al escribir).
 * - User-Agent propio. Datos ODbL/DbCL; imágenes CC BY-SA (no se descargan ni se muestran en este lote).
 * - No se envía nada del usuario (diario, objetivos, peso) y no hay escrituras.
 */

/** Producto normalizado: código como texto (ceros iniciales), base documentada y nutrientes null si faltan. */
data class RemoteProduct(
    val code: String,
    val name: String,
    val brand: String?,
    val basisAmount: Double,
    val basisUnit: QtyUnit,
    /** Producto de volumen publicado «por 100 g»: el usuario confirma la base antes de registrarlo. */
    val basisNeedsReview: Boolean,
    val nutrients: Nutrients,
    val kcalFromKj: Boolean,
    val servingAmount: Double?,
    val servingUnit: QtyUnit?,
    val servingLabel: String?,
    val url: String,
) {
    val missing: List<Nutrient> get() = Nutrient.entries.filter { nutrients[it] == null }
}

data class RemoteHit(val code: String, val name: String, val brand: String?, val quantity: String?)

sealed interface RemoteResult<out T> {
    data class Ok<T>(val value: T) : RemoteResult<T>
    data object NotFound : RemoteResult<Nothing>
    /** Sin nutrientes utilizables: se ofrece crear el alimento con su código. */
    data object NoNutrition : RemoteResult<Nothing>
    data object Offline : RemoteResult<Nothing>
    data object Timeout : RemoteResult<Nothing>
    data object RateLimited : RemoteResult<Nothing>
    data object Unavailable : RemoteResult<Nothing>
    /** Límite local de peticiones por minuto: espera sin llamar al servidor. */
    data class Throttled(val waitSeconds: Int) : RemoteResult<Nothing>
    data class Failed(val code: Int) : RemoteResult<Nothing>
    /** 200 con cuerpo que no es el JSON esperado (HTML de un proxy, JSON truncado o sin «hits»): se puede reintentar. */
    data object InvalidResponse : RemoteResult<Nothing>
}

/** Ventana deslizante de 60 s: respeta los límites por minuto antes de enviar nada. */
class MinuteLimiter(private val max: Int, private val now: () -> Long = System::currentTimeMillis) {
    private val stamps = ArrayDeque<Long>()

    @Synchronized
    fun tryAcquire(): Int? {
        val t = now()
        while (stamps.isNotEmpty() && t - stamps.first() >= 60_000) stamps.removeFirst()
        if (stamps.size >= max) return (((60_000 - (t - stamps.first())) + 999) / 1000).toInt().coerceAtLeast(1)
        stamps.addLast(t)
        return null
    }
}

/** GTIN alimentario: 8, 12, 13 o 14 dígitos, conservado como texto. */
fun normalizeBarcode(text: String): String? = text.filter { !it.isWhitespace() }.takeIf { it.matches(Regex("\\d{8}|\\d{12,14}")) }

open class OpenFoodFactsClient(
    private val userAgent: String,
    private val productBase: String = "https://world.openfoodfacts.org",
    private val searchBase: String = "https://search.openfoodfacts.org",
    private val productLimiter: MinuteLimiter = MinuteLimiter(14),
    private val searchLimiter: MinuteLimiter = MinuteLimiter(9),
) {
    open suspend fun product(code: String): RemoteResult<RemoteProduct> {
        productLimiter.tryAcquire()?.let { return RemoteResult.Throttled(it) }
        val url = "$productBase/api/v3/product/$code?fields=$PRODUCT_FIELDS"
        return when (val r = get(url)) {
            is HttpOutcome.Body -> parseProductResponse(r.code, r.text, code)
            is HttpOutcome.Error -> r.result
        }
    }

    open suspend fun search(query: String): RemoteResult<List<RemoteHit>> {
        searchLimiter.tryAcquire()?.let { return RemoteResult.Throttled(it) }
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$searchBase/search?q=$q&langs=es&page_size=25&fields=code,product_name,product_name_es,brands,quantity"
        return when (val r = get(url)) {
            is HttpOutcome.Body -> parseSearchResponse(r.code, r.text)
            is HttpOutcome.Error -> r.result
        }
    }

    sealed interface HttpOutcome {
        data class Body(val code: Int, val text: String) : HttpOutcome
        data class Error(val result: RemoteResult<Nothing>) : HttpOutcome
    }

    /** Una sola petición, con tiempo límite; la cancelación interrumpe la espera. Sin reintentos automáticos. */
    protected open suspend fun get(url: String): HttpOutcome = runInterruptible(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 8_000
            c.readTimeout = 10_000
            c.setRequestProperty("User-Agent", userAgent)
            c.setRequestProperty("Accept", "application/json")
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            HttpOutcome.Body(code, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } catch (_: UnknownHostException) {
            HttpOutcome.Error(RemoteResult.Offline)
        } catch (_: SocketTimeoutException) {
            HttpOutcome.Error(RemoteResult.Timeout)
        } catch (_: IOException) {
            HttpOutcome.Error(RemoteResult.Offline)
        } finally {
            c.disconnect()
        }
    }

    companion object {
        /**
         * Identificación de la app. OFF pide «AppName/Version (ContactEmail)»: el proyecto aún no tiene un correo de
         * contacto autorizado para publicar (requisito externo documentado en quality/NUTRITION_REPORT.md).
         */
        const val USER_AGENT = "FitnessApp/0.1.0 (Android; app personal local, sin backend)"

        const val PRODUCT_FIELDS = "code,product_name,product_name_es,brands,nutriments,nutrition_data_per,serving_size," +
            "serving_quantity,serving_quantity_unit,product_quantity_unit,quantity"

        fun productUrl(code: String) = "https://world.openfoodfacts.org/product/$code"

        fun statusResult(code: Int): RemoteResult<Nothing> = when (code) {
            404 -> RemoteResult.NotFound
            429 -> RemoteResult.RateLimited
            502, 503, 504 -> RemoteResult.Unavailable
            else -> RemoteResult.Failed(code)
        }

        /**
         * Respuesta de búsqueda. Un 200 que no es un objeto JSON con la lista «hits» es una respuesta inválida
         * (recuperable), nunca «sin resultados»: solo una lista «hits» real, aunque esté vacía, significa cero coincidencias.
         */
        fun parseSearchResponse(status: Int, text: String): RemoteResult<List<RemoteHit>> {
            if (status != 200) return statusResult(status)
            val hits = runCatching { JSONObject(text) }.getOrNull()?.let { it.opt("hits") as? JSONArray }
                ?: return RemoteResult.InvalidResponse
            return RemoteResult.Ok(parseHits(hits))
        }

        private fun parseHits(hits: JSONArray): List<RemoteHit> {
            return (0 until hits.length()).mapNotNull { i ->
                val h = hits.optJSONObject(i) ?: return@mapNotNull null
                val code = normalizeBarcode(h.optString("code")) ?: return@mapNotNull null
                val name = h.str("product_name_es") ?: h.str("product_name") ?: return@mapNotNull null
                RemoteHit(code, name, brandOf(h.opt("brands")), h.str("quantity"))
            }
        }

        /** Respuesta v3: 200 con producto, 404 «product_not_found»; otros estados según su código. */
        fun parseProductResponse(status: Int, text: String, requested: String): RemoteResult<RemoteProduct> {
            if (status == 404) return RemoteResult.NotFound
            if (status != 200) return statusResult(status)
            val root = runCatching { JSONObject(text) }.getOrNull() ?: return RemoteResult.InvalidResponse
            val product = root.optJSONObject("product") ?: return RemoteResult.NotFound
            // Un campo con tipo inesperado no debe romper la pantalla: se trata como respuesta inválida.
            val normalized = runCatching { normalizeProduct(product, requested) }.getOrElse { return RemoteResult.InvalidResponse }
            return normalized?.let { RemoteResult.Ok(it) } ?: RemoteResult.NoNutrition
        }

        /**
         * Valores «tal como se vende» por 100 g (o por ración si es la única base publicada). kJ → kcal solo si
         * faltan las kcal. Sal y sodio no se usan en este lote. Nada ausente se rellena.
         */
        fun normalizeProduct(p: JSONObject, requested: String): RemoteProduct? {
            val code = normalizeBarcode(p.optString("code")) ?: requested
            val name = p.str("product_name_es") ?: p.str("product_name") ?: return null
            val n = p.optJSONObject("nutriments") ?: return null
            val per = p.str("nutrition_data_per") ?: "100g"
            val servingQty = p.num("serving_quantity")?.takeIf { it > 0 }
            val servingUnitRaw = p.str("serving_quantity_unit")?.lowercase()
            val productUnit = p.str("product_quantity_unit")?.lowercase()
            val isVolume = productUnit == "ml" || productUnit == "l" || productUnit == "cl" ||
                p.str("quantity")?.lowercase()?.let { Regex("\\d\\s*(ml|cl|l)\\b").containsMatchIn(it) } == true
            // Sin unidad explícita se lee del texto de la ración («30 g», «250 ml»); si no consta, no hay equivalencia.
            val servingUnit = when (servingUnitRaw ?: p.str("serving_size")?.lowercase()?.let { Regex("\\d\\s*(ml|g)\\b").find(it)?.groupValues?.get(1) }) {
                "ml" -> QtyUnit.ML
                "g" -> QtyUnit.G
                else -> null
            }
            fun read(suffix: String): Nutrients {
                val kcal = n.num("energy-kcal$suffix")
                val kj = n.num("energy-kj$suffix")
                return Nutrients(kcal ?: kj?.let(::kjToKcal), n.num("proteins$suffix"), n.num("carbohydrates$suffix"), n.num("fat$suffix"))
            }
            fun fromKj(suffix: String) = n.num("energy-kcal$suffix") == null && n.num("energy-kj$suffix") != null
            val per100 = read("_100g")
            val perServing = read("_serving")
            val useServing = per.startsWith("serving") && !per100.anyKnown && perServing.anyKnown
            val values = if (useServing) perServing else per100
            if (!values.anyKnown) return null
            val basisUnit = if (useServing) QtyUnit.SERVING else if (isVolume) QtyUnit.ML else QtyUnit.G
            return RemoteProduct(
                code = code,
                name = name,
                brand = brandOf(p.opt("brands")),
                basisAmount = if (useServing) 1.0 else 100.0,
                basisUnit = basisUnit,
                // OFF publica las bebidas en campos «_100g»: la base de volumen se confirma con la etiqueta.
                basisNeedsReview = !useServing && isVolume,
                nutrients = values,
                kcalFromKj = fromKj(if (useServing) "_serving" else "_100g"),
                servingAmount = servingQty?.takeIf { servingUnit != null },
                servingUnit = servingUnit?.takeIf { servingQty != null },
                servingLabel = p.str("serving_size"),
                url = productUrl(code),
            )
        }

        private fun brandOf(raw: Any?): String? {
            val first = when (raw) {
                is JSONArray -> if (raw.length() > 0) raw.optString(0) else null
                is String -> raw.split(',').firstOrNull()
                else -> null
            }
            return first?.substringAfter("xx:")?.trim()?.takeIf { it.isNotEmpty() }
        }

        private fun JSONObject.str(key: String): String? = if (isNull(key)) null else optString(key).trim().takeIf { it.isNotEmpty() }

        private fun JSONObject.num(key: String): Double? {
            if (!has(key) || isNull(key)) return null
            val v = when (val raw = opt(key)) {
                is Number -> raw.toDouble()
                is String -> raw.replace(',', '.').toDoubleOrNull()
                else -> null
            }
            return v?.takeIf { it.isFinite() && it >= 0 }
        }
    }
}
