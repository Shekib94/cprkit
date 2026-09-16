package com.cyberpunk.gmtool.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.cyberpunk.gmtool.data.gtr
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.util.concurrent.Executors

private val CRed = Color(0xFFE53935)
private val CCard = Color(0xFF1A1A1A)
private val CWhite = Color(0xFFEEEEEE)
private val CMuted = Color(0xFF9E9E9E)

/**
 * اسکنر QR با دوربین.
 * تولید QR داخل خود پروژه است؛ فقط خواندن از zxing استفاده می‌کند
 * چون رمزگشایی از تصویر واقعی بسیار خطاپذیر است.
 */
@Composable
fun QrScannerDialog(
    onResult: (String) -> Unit,
    onManualEntry: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CCard,
        shape = CutCornerShape(12.dp),
        title = {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(gtr("Scan the GM code"), color = CRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (hasPermission) {
                    Box(
                        Modifier.fillMaxWidth().height(280.dp),
                        contentAlignment = Alignment.Center
                    ) { CameraQrPreview(onResult) }
                } else {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            gtr("Camera access is needed to scan. You can also enter the code manually."),
                            color = CMuted, fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onManualEntry,
                    shape = CutCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CRed),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(gtr("enter code manually"), color = CWhite) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(gtr("cancel"), color = CMuted) }
        }
    )
}

@Composable
private fun CameraQrPreview(onResult: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    var delivered by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { proxy ->
                    if (!delivered) {
                        decodeQr(proxy)?.let { text ->
                            delivered = true
                            previewView.post { onResult(text) }
                        }
                    }
                    proxy.close()
                }
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                    )
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

private val qrReader by lazy {
    QRCodeReader()
}

private fun decodeQr(proxy: ImageProxy): String? {
    return try {
        val buffer = proxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val source = PlanarYUVLuminanceSource(
            bytes, proxy.planes[0].rowStride, proxy.height,
            0, 0,
            minOf(proxy.width, proxy.planes[0].rowStride), proxy.height,
            false
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val hints = mapOf(DecodeHintType.TRY_HARDER to true)
        synchronized(qrReader) {
            qrReader.reset()
            qrReader.decode(bitmap, hints)
        }.text
    } catch (e: Exception) {
        null
    }
}
