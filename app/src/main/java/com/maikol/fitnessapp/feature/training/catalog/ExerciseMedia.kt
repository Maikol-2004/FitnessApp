package com.maikol.fitnessapp.feature.training.catalog

/*
 * G2 (docs/14_EXERCISE_MEDIA_AND_PROGRESS.md): medios locales por catalogId, copiados de design/exercises-v1
 * (manifest.json) a assets/exercises/<id>/. Solo los archivos que usa la app: miniatura, mapa muscular y tres
 * posiciones. Son ilustraciones generadas, sin validación profesional, y NO un vídeo: no hay clip (video = null).
 */

/** Cómo leer la carga registrada para comparar marcas. */
enum class LoadSemantics {
    /** Carga externa (o vacía en peso corporal): se puede comparar como fuerza. */
    EXTERNAL,

    /** La carga es ayuda (dominada asistida): menos puede ser progreso; no hay récord ni volumen comparable. */
    ASSISTANCE,
}

/**
 * Modalidad de la ayuda en la dominada asistida. Los kg de máquina y la tensión de una banda no son comparables,
 * así que cada modalidad tiene su propia evolución. En datos, null = sin indicar (registros anteriores a G2.1):
 * nunca se reclasifica automáticamente.
 */
enum class AssistMode {
    MACHINE,
    BAND,
    ;

    companion object {
        fun fromStored(value: String?): AssistMode? = entries.firstOrNull { it.name == value }
    }
}

data class ExerciseMedia(
    val catalogId: String,
    val thumbnail: String,
    val anatomy: String,
    val frames: List<String>,
) {
    /** Orden de reproducción en bucle: 1→2→3→2→1 (índices de [frames]). */
    val sequence: List<Int> get() = listOf(0, 1, 2, 1)

    /** No existe vídeo real para ningún ejercicio en esta versión. */
    val video: String? get() = null

    companion object {
        const val ROOT = "exercises"
        const val FRAME_DURATION_MS = 1_200L

        fun of(catalogId: String): ExerciseMedia {
            val base = "$ROOT/$catalogId"
            return ExerciseMedia(
                catalogId = catalogId,
                thumbnail = "$base/thumbnail.webp",
                anatomy = "$base/anatomy.webp",
                frames = listOf("$base/frames/01.webp", "$base/frames/02.webp", "$base/frames/03.webp"),
            )
        }
    }
}

val CatalogExercise.mediaAssets: ExerciseMedia get() = ExerciseMedia.of(id)

/** Única excepción del manifiesto (loadSemantics = assistance_context_required). */
val CatalogExercise.loadSemantics: LoadSemantics
    get() = if (id == "back_assisted_pull_up") LoadSemantics.ASSISTANCE else LoadSemantics.EXTERNAL

/** Con mancuernas la app compara la carga tal como se escribió en la serie, sin duplicarla ni dividirla. */
val CatalogExercise.usesDumbbells: Boolean get() = Equipment.DUMBBELL in equipment
