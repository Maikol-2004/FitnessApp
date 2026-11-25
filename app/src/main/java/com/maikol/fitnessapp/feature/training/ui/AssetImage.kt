package com.maikol.fitnessapp.feature.training.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Fondo de los medios de ejercicio (#121416, igual que las ilustraciones): Fit sin bandas visibles. */
internal val ExerciseMediaBackground = Color(0xFF121416)

/**
 * Caché pequeña por tamaño en bytes: las miniaturas visibles y los frames de la ficha abierta. Las imágenes
 * se decodifican fuera del hilo principal y solo cuando se componen (las filas de la lista que no se ven
 * no cargan nada).
 */
private object AssetBitmapCache {
    private val cache = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    fun get(key: String): ImageBitmap? = cache.get(key)

    fun load(context: Context, path: String, opaque: Boolean): ImageBitmap? {
        cache.get(path)?.let { return it }
        val options = BitmapFactory.Options().apply {
            // Miniaturas opacas a 16 bits: la mitad de memoria y sin diferencia visible a ese tamaño.
            if (opaque) inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmap = runCatching { context.assets.open(path).use { BitmapFactory.decodeStream(it, null, options) } }.getOrNull()
            ?: return null
        return bitmap.asImageBitmap().also { cache.put(path, it) }
    }
}

/** Imagen local de assets. Si falta o no decodifica, deja el fondo vacío (nunca una imagen rota). */
@Composable
internal fun ExerciseAssetImage(
    path: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    opaque: Boolean = false,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val context = LocalContext.current.applicationContext
    val image by produceState(AssetBitmapCache.get(path), path) {
        if (value == null) value = withContext(Dispatchers.IO) { AssetBitmapCache.load(context, path, opaque) }
    }
    Box(modifier.background(ExerciseMediaBackground)) {
        image?.let { Image(it, contentDescription = contentDescription, contentScale = contentScale, modifier = Modifier.matchParentSize()) }
    }
}
