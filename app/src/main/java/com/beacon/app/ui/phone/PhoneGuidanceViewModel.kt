package com.beacon.app.ui.phone

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beacon.core.result.OperationResult
import com.beacon.domain.device.SetPhoneOnlyModeUseCase
import com.beacon.domain.speech.Speaker
import com.beacon.domain.speech.awaitNotSpeaking
import com.beacon.domain.vision.CapturedImage
import com.beacon.domain.vision.DetectedObject
import com.beacon.domain.vision.usecase.DescribeSceneUseCase
import com.beacon.domain.vision.usecase.DetectObjectsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import javax.inject.Inject

data class PhoneGuidanceUiState(
    val detectedObjects: List<DetectedObject> = emptyList(),
    val lastSpoken: String? = null,
    val cameraReady: Boolean = false,
)

@HiltViewModel
class PhoneGuidanceViewModel @Inject constructor(
    private val detectObjects: DetectObjectsUseCase,
    private val describeScene: DescribeSceneUseCase,
    private val speaker: Speaker,
    private val setPhoneOnlyMode: SetPhoneOnlyModeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhoneGuidanceUiState())
    val uiState: StateFlow<PhoneGuidanceUiState> = _uiState.asStateFlow()

    private val detectMutex = Mutex()
    private var narrationJob: Job? = null
    @Volatile private var latestJpeg: ByteArray? = null
    private var lastDetectAtMs = 0L

    fun onCameraReady() {
        _uiState.value = _uiState.value.copy(cameraReady = true)
        startNarrationLoop()
    }

    fun onFrame(bitmap: Bitmap) {
        val now = System.currentTimeMillis()
        if (now - lastDetectAtMs < DETECT_INTERVAL_MS) return
        lastDetectAtMs = now

        latestJpeg = bitmap.toJpegBytes()

        viewModelScope.launch {
            detectMutex.withLock {
                val objects = detectObjects(bitmap)
                _uiState.value = _uiState.value.copy(detectedObjects = objects)
            }
        }
    }

    fun exitPhoneMode(onDone: () -> Unit) {
        narrationJob?.cancel()
        speaker.stop()
        viewModelScope.launch {
            setPhoneOnlyMode(false)
            onDone()
        }
    }

    private fun startNarrationLoop() {
        narrationJob?.cancel()
        narrationJob = viewModelScope.launch {
            delay(NARRATION_START_DELAY_MS)
            while (isActive) {
                runNarrationTick()
                delay(NARRATION_INTERVAL_MS)
            }
        }
    }

    private suspend fun runNarrationTick() {
        val jpeg = latestJpeg ?: return
        when (val result = describeScene(CapturedImage(jpeg))) {
            is OperationResult.Failure -> Unit
            is OperationResult.Success -> {
                val line = result.value.spokenSummary
                _uiState.value = _uiState.value.copy(lastSpoken = line)
                speaker.speak(line, interrupt = true)
                speaker.awaitNotSpeaking()
            }
        }
    }

    override fun onCleared() {
        narrationJob?.cancel()
        speaker.stop()
        super.onCleared()
    }

    private fun Bitmap.toJpegBytes(): ByteArray {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return stream.toByteArray()
    }

    private companion object {
        const val DETECT_INTERVAL_MS = 250L
        const val NARRATION_INTERVAL_MS = 4_500L
        const val NARRATION_START_DELAY_MS = 1_500L
    }
}
