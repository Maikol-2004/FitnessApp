package com.maikol.fitnessapp.feature.nutrition.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.feature.nutrition.data.normalizeBarcode
import com.maikol.fitnessapp.feature.training.ui.GlassButton
import com.maikol.fitnessapp.feature.training.ui.InlineMessage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private enum class CameraState { CHECKING, GRANTED, DENIED, NO_CAMERA, FAILED }

/**
 * N2: lectura de códigos alimentarios (EAN-13/8, UPC-A/E) con el modelo de ML Kit incluido en la app (funciona
 * sin red). El permiso de cámara se pide solo aquí. Un único código por apertura; la cámara se libera al salir.
 * Escribir el código a mano siempre está disponible.
 */
@Composable
fun ScannerRoute(onBack: () -> Unit, onCode: (String) -> Unit) {
    val context = LocalContext.current
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    var camera by rememberSaveable {
        mutableStateOf(
            when {
                !hasCamera -> CameraState.NO_CAMERA
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> CameraState.GRANTED
                else -> CameraState.CHECKING
            },
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        camera = if (granted) CameraState.GRANTED else CameraState.DENIED
    }
    LaunchedEffect(Unit) { if (camera == CameraState.CHECKING) launcher.launch(Manifest.permission.CAMERA) }
    var manual by rememberSaveable { mutableStateOf(false) }
    val delivered = remember { AtomicBoolean(false) }
    val deliver: (String) -> Unit = { code -> if (delivered.compareAndSet(false, true)) onCode(code) }

    NutritionPage(
        title = stringResource(R.string.n_scan),
        subtitle = stringResource(R.string.n_scan_subtitle),
        onBack = onBack,
        bottomBar = { GlassButton(stringResource(R.string.n_enter_code), R.drawable.ic_edit, { manual = true }, Modifier.fillMaxWidth(), centered = true) },
    ) {
        when (camera) {
            CameraState.GRANTED -> {
                CameraPreview(onCode = deliver, onFailure = { camera = CameraState.FAILED })
                Text(stringResource(R.string.n_scan_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            CameraState.CHECKING -> Text(stringResource(R.string.n_scan_permission), style = MaterialTheme.typography.bodyLarge)
            CameraState.DENIED -> {
                InlineMessage(visible = true, text = stringResource(R.string.n_scan_denied), iconRes = R.drawable.ic_info, color = MaterialTheme.colorScheme.onSurface)
                GlassButton(stringResource(R.string.n_scan_ask_again), null, { launcher.launch(Manifest.permission.CAMERA) }, Modifier.fillMaxWidth(), centered = true)
            }
            CameraState.NO_CAMERA -> InlineMessage(visible = true, text = stringResource(R.string.n_scan_no_camera), iconRes = R.drawable.ic_info, color = MaterialTheme.colorScheme.onSurface)
            CameraState.FAILED -> InlineMessage(visible = true, text = stringResource(R.string.n_scan_failed), iconRes = R.drawable.ic_info, color = MaterialTheme.colorScheme.onSurface)
        }
    }
    if (manual) {
        ManualCodeDialog(onDismiss = { manual = false }, onConfirm = { manual = false; normalizeBarcode(it)?.let(deliver) ?: deliver(it) })
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(onCode: (String) -> Unit, onFailure: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val description = stringResource(R.string.n_scan_preview)
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    DisposableEffect(owner) {
        val executor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
                .build(),
        )
        val busy = AtomicBoolean(false)
        val found = AtomicBoolean(false)
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            try {
                val p = future.get().also { provider = it }
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(executor) { proxy ->
                    val media = proxy.image
                    // Un fotograma a la vez y un único código entregado: sin búsquedas repetidas del mismo frame.
                    if (media == null || found.get() || !busy.compareAndSet(false, true)) {
                        proxy.close()
                        return@setAnalyzer
                    }
                    scanner.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                        .addOnSuccessListener { codes ->
                            val code = codes.firstNotNullOfOrNull { it.rawValue?.let(::normalizeBarcode) }
                            if (code != null && found.compareAndSet(false, true)) {
                                ContextCompat.getMainExecutor(context).execute { onCode(code) }
                            }
                        }
                        .addOnCompleteListener {
                            busy.set(false)
                            proxy.close()
                        }
                }
                p.unbindAll()
                p.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (_: Exception) {
                onFailure()
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            provider?.unbindAll()
            scanner.close()
            executor.shutdown()
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .semantics { contentDescription = description },
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
    }
}
