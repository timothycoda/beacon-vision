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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.beacon.app.R
import com.beacon.app.ui.theme.bentoWhiteCardColor
import com.beacon.app.ui.theme.bentoWhiteCardOnColor
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beacon.app.ui.components.BeaconChip
import com.beacon.app.ui.components.BentoCard
import com.beacon.app.ui.components.SecondaryActionButton
import com.beacon.app.ui.theme.BeaconDimens
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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onHostResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (state.showHelperPicker) {
        HelperPickerBottomSheet(
            helpers = state.helperPickerCandidates,
            onDismiss = viewModel::dismissHelperPicker,
            onPick = viewModel::onHelperPicked,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (hasCameraPermission) {
            PhoneCameraPreview(
                onFrame = viewModel::onFrame,
                onReady = viewModel::onCameraReady,
                modifier = Modifier.fillMaxSize(),
            )
            DetectionOverlay(
                objects = state.detectedObjects,
                guidanceLanguage = state.guidanceLanguage,
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
                accent = MaterialTheme.colorScheme.primary,
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(12.dp)
                .fillMaxWidth(),
        ) {
            state.primaryHelperName?.let { name ->
                Text(
                    text = "Helper: $name",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Text(
                text = stringResource(R.string.phone_whatsapp_pause),
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            PhoneModeHelperButton(
                title = "Call helper on WhatsApp",
                onClick = viewModel::callHelperOnWhatsApp,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                contentDescription = "Call helper on WhatsApp. Opens WhatsApp with a prefilled message.",
            )
            PhoneModeHelperButton(
                title = "Message helper",
                onClick = viewModel::messageHelper,
                containerColor = bentoWhiteCardColor(),
                contentColor = bentoWhiteCardOnColor(),
                contentDescription = "Message helper on WhatsApp",
            )
            if (!hasCameraPermission) {
                SecondaryActionButton(
                    label = "Grant camera access",
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private const val PHONE_HELPER_BUTTON_ALPHA = 0.38f

@Composable
private fun PhoneModeHelperButton(
    title: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    contentDescription: String,
) {
    BentoCard(
        title = title,
        onClick = onClick,
        containerColor = containerColor.copy(alpha = PHONE_HELPER_BUTTON_ALPHA),
        contentColor = contentColor,
        minHeight = BeaconDimens.bentoWideMinHeight,
        modifier = Modifier.fillMaxWidth(),
        contentDescription = contentDescription,
    )
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
