package com.maikol.fitnessapp.feature.training.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * G2-01: cada catalogId del catálogo está en design/exercises-v1/manifest.json, sus rutas coinciden con las que
 * usa la app, el archivo empaquetado es idéntico al de diseño y no hay vídeo. Solo se empaquetan medios runtime.
 */
class ExerciseMediaTest {
    // Las unitarias se ejecutan con el módulo app como directorio de trabajo.
    private val design = File("../design/exercises-v1")
    private val assets = File("src/main/assets")
    private val manifest = File(design, "manifest.json").readText()

    /** Objetos de items[] del manifiesto, sin dependencia JSON: cada uno empieza por "catalogId". */
    private val items: Map<String, String> = Regex("""\{\s*"catalogId":\s*"([a-z0-9_]+)"(.*?)"autoplayDefault":\s*(true|false)""", RegexOption.DOT_MATCHES_ALL)
        .findAll(manifest)
        .associate { it.groupValues[1] to it.value }

    private fun field(item: String, name: String) = Regex(""""$name":\s*"([^"]+)"""").find(item)?.groupValues?.get(1)

    @Test
    fun everyCatalogId_hasManifestEntry_matchingRuntimeAssets_andNoVideo() {
        assertEquals(ExerciseCatalog.all.map { it.id }.toSet(), items.keys)
        ExerciseCatalog.all.forEach { e ->
            val item = items.getValue(e.id)
            val media = e.mediaAssets
            assertTrue(e.id, Regex(""""video":\s*null""").containsMatchIn(item))
            assertEquals(null, media.video)
            assertTrue(e.id, Regex(""""autoplayDefault":\s*false""").containsMatchIn(item))
            assertEquals("exercises/" + field(item, "thumbnail"), media.thumbnail)
            assertEquals("exercises/" + field(item, "anatomy"), media.anatomy)
            val frames = Regex(""""frames":\s*\[(.*?)]""", RegexOption.DOT_MATCHES_ALL).find(item)!!.groupValues[1]
                .split(",").map { "exercises/" + it.trim().trim('"') }
            assertEquals(frames, media.frames)
            val semantics = field(item, "loadSemantics")
            assertEquals(e.id, if (semantics == "assistance_context_required") LoadSemantics.ASSISTANCE else LoadSemantics.EXTERNAL, e.loadSemantics)
            (listOf(media.thumbnail, media.anatomy) + media.frames).forEach { path ->
                val packaged = File(assets, path)
                val source = File(design, path.removePrefix("exercises/"))
                assertTrue("falta $path", packaged.isFile)
                assertTrue("$path: contenido distinto del diseño", packaged.readBytes().contentEquals(source.readBytes()))
                // Cabecera RIFF/WEBP: archivo de imagen real, no un marcador vacío.
                assertEquals(path, "WEBP", String(packaged.readBytes().copyOfRange(8, 12)))
            }
        }
    }

    /** audit.json registra tamaño y SHA-256 de cada medio; una corrección (p. ej. el mapa de G2.1) debe quedar reflejada. */
    @Test
    fun packagedMedia_matchAuditSizeAndHash() {
        val audit = File(design, "audit.json").readText()
        val entries = Regex(""""path":\s*"([^"]+\.webp)"[^}]*?"bytes":\s*(\d+),\s*"sha256":\s*"([0-9a-f]{64})"""")
            .findAll(audit).associate { it.groupValues[1] to (it.groupValues[2].toLong() to it.groupValues[3]) }
        val sha = MessageDigest.getInstance("SHA-256")
        ExerciseCatalog.all.flatMap { e -> e.mediaAssets.let { listOf(it.thumbnail, it.anatomy) + it.frames } }.forEach { path ->
            val bytes = File(assets, path).readBytes()
            val (size, hash) = entries[path.removePrefix("exercises/")] ?: error("sin entrada en audit.json: $path")
            assertEquals(path, size, bytes.size.toLong())
            assertEquals(path, hash, sha.digest(bytes).joinToString("") { "%02x".format(it) })
        }
    }

    @Test
    fun thumbnails_areDistinctPerVariant() {
        val hashes = ExerciseCatalog.all.map { File(assets, it.mediaAssets.thumbnail).readBytes().contentHashCode() }
        assertEquals("ninguna variante reutiliza la miniatura de otra", hashes.size, hashes.toSet().size)
    }

    @Test
    fun apkOnlyPacksRuntimeMedia() {
        val packaged = File(assets, "exercises").walkTopDown().filter { it.isFile }.map { it.relativeTo(File(assets, "exercises")).invariantSeparatorsPath }.toList()
        assertEquals(ExerciseCatalog.all.size * 5, packaged.size)
        assertTrue(packaged.all { it.matches(Regex("""[a-z0-9_]+/(thumbnail|anatomy|frames/0[123])\.webp""")) })
    }

    @Test
    fun sequence_isHonestLoopOfThreePositions() {
        val media = ExerciseMedia.of("chest_barbell_bench_press")
        assertEquals(3, media.frames.size)
        assertEquals(listOf(0, 1, 2, 1), media.sequence)
        assertEquals(1_200L, ExerciseMedia.FRAME_DURATION_MS)
    }
}
