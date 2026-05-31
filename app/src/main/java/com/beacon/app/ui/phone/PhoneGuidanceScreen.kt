package com.beacon.app.ui.phone

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beacon.app.ui.components.BeaconChip
import com.beacon.app.ui.components.SecondaryActionButton
import com.beacon.app.ui.theme.BeaconBackground
import com.beacon.app.ui.theme.BeaconLime
import com.beacon.app.ui.theme.BeaconOnDarkMuted
import java.util.concurrent.Executors

@Composable
fun PhoneGuidanceScreen(
    onBackToHome: () -> Unit,
    viewModel: PhoneGuidanceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    BackHandler { viewModel.exitPhoneMode(onBackToHome) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BeaconBackground),
    ) {
        if (hasCameraPermission) {
            PhoneCameraPreview(
                onFrame = viewModel::onFrame,
                onReady = viewModel::onCameraReady,
                modifier = Modifier.fillMaxSize(),
            )
            DetectionOverlay(
                objects = state.detectedObjects,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = "Camera permission is needed for phone guidance.",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp)
                .align(Alignment.TopCenter),
        ) {
            BeaconChip(
                label = "Glasses home",
                selected = true,
                accent = BeaconLime,
                onClick = { viewModel.exitPhoneMode(onBackToHome) },
                modifier = Modifier.semantics {
                    contentDescription = "Return to glasses home screen"
                },
            )
            state.lastSpoken?.let { spoken ->
                Text(
                    text = spoken,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(8.dp),
                )
            }
        }

        if (!hasCameraPermission) {
            SecondaryActionButton(
                label = "Grant camera access",
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .fillMaxWidth(),
            )
        }
    }

}

@Composable
private fun PhoneCameraPreview(
    onFrame: (Bitmap) -> Unit,
    onReady: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    var bound by remember { mutableStateOf(false) }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = modifier,
        update = { previewView ->
            if (bound) return@AndroidView
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also { useCase ->
                        useCase.setAnalyzer(executor) { imageProxy ->
                            val bitmap = imageProxy.toRgbaBitmap()
                            imageProxy.close()
                            if (bitmap != null) onFrame(bitmap)
                        }
                    }
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
                bound = true
                onReady()
            }, ContextCompat.getMainExecutor(context))
        },
    )

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }
}

private fun androidx.camera.core.ImageProxy.toRgbaBitmap(): Bitmap? {
    val plane = image?.planes?.firstOrNull() ?: return null
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    plane.buffer.rewind()
    bitmap.copyPixelsFromBuffer(plane.buffer)
    val rotation = imageInfo.rotationDegrees
    if (rotation == 0) return bitmap
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
