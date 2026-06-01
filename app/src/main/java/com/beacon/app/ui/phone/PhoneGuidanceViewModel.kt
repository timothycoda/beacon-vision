package com.beacon.app.ui.phone

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beacon.app.emergency.DialerIntentLauncher
import com.beacon.app.emergency.HelperHandoffManager
import com.beacon.app.emergency.WhatsAppLaunchResult
import com.beacon.core.result.OperationResult
import com.beacon.data.phone.PhoneModeLatestImageHolder
import com.beacon.data.phone.PhoneModeSessionController
import com.beacon.domain.guidance.GuidanceLanguage
import com.beacon.data.guidance.GuidanceLanguagePreferences
import com.beacon.domain.helper.ObserveTrustedHelpersUseCase
import com.beacon.domain.helper.TrustedHelper
import com.beacon.domain.speech.Speaker
import com.beacon.domain.speech.awaitNotSpeaking
import com.beacon.domain.vision.CapturedImage
import com.beacon.domain.vision.DetectedObject
import com.beacon.data.vision.PhoneObjectNarration
import com.beacon.domain.vision.usecase.DescribeSceneUseCase
import com.beacon.domain.vision.usecase.DetectObjectsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import javax.inject.Inject

enum class HelperPickerMode { CALL, MESSAGE }

data class PhoneGuidanceUiState(
    val detectedObjects: List<DetectedObject> = emptyList(),
    val guidanceLanguage: GuidanceLanguage = GuidanceLanguage.English,
    val lastSpoken: String? = null,
    val cameraReady: Boolean = false,
    val primaryHelperName: String? = null,
    val showHelperPicker: Boolean = false,
    val helperPickerMode: HelperPickerMode = HelperPickerMode.CALL,
    val helperPickerCandidates: List<TrustedHelper> = emptyList(),
    val statusMessage: String? = null,
)

@HiltViewModel
class PhoneGuidanceViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val detectObjects: DetectObjectsUseCase,
    private val describeScene: DescribeSceneUseCase,
    private val speaker: Speaker,
    private val latestImageHolder: PhoneModeLatestImageHolder,
    private val sessionController: PhoneModeSessionController,
    private val helperHandoff: HelperHandoffManager,
    private val observeHelpers: ObserveTrustedHelpersUseCase,
    private val languagePrefs: GuidanceLanguagePreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhoneGuidanceUiState())
    val uiState: StateFlow<PhoneGuidanceUiState> = _uiState.asStateFlow()

    private val detectMutex = Mutex()
    private var narrationJob: Job? = null
    @Volatile private var latestJpeg: ByteArray? = null
    private var lastDetectAtMs = 0L

    init {
        viewModelScope.launch {
            languagePrefs.language.collect { language ->
                _uiState.update { it.copy(guidanceLanguage = language) }
                if (language == GuidanceLanguage.Hausa) {
                    speaker.prepareHausaVoice()
                }
            }
        }
        viewModelScope.launch {
            observeHelpers().collect { helpers ->
                val primary = helpers.firstOrNull { it.isPrimaryHelper }
                    ?: helpers.firstOrNull()
                _uiState.update { it.copy(primaryHelperName = primary?.displayName) }
            }
        }
    }

    fun onCameraReady() {
        sessionController.markPhoneModeActive()
        _uiState.value = _uiState.value.copy(cameraReady = true)
        startNarrationLoop()
    }

    fun onFrame(bitmap: Bitmap) {
        if (sessionController.isAnalysisPaused()) return
        val now = System.currentTimeMillis()
        if (now - lastDetectAtMs < DETECT_INTERVAL_MS) return
        lastDetectAtMs = now

        val jpeg = bitmap.toJpegBytes()
        latestJpeg = jpeg
        latestImageHolder.updateFrame(jpeg)

        viewModelScope.launch {
            detectMutex.withLock {
                val objects = detectObjects(bitmap)
                _uiState.value = _uiState.value.copy(detectedObjects = objects)
            }
        }
    }

    fun onHostResumed() {
        helperHandoff.onReturnedFromWhatsApp(appContext)
    }

    fun callHelperOnWhatsApp() {
        viewModelScope.launch { startHelperFlow(HelperPickerMode.CALL) }
    }

    fun messageHelper() {
        viewModelScope.launch { startHelperFlow(HelperPickerMode.MESSAGE) }
    }

    fun dismissHelperPicker() {
        _uiState.update {
            it.copy(showHelperPicker = false, helperPickerCandidates = emptyList())
        }
    }

    fun onHelperPicked(helper: TrustedHelper) {
        val mode = _uiState.value.helperPickerMode
        dismissHelperPicker()
        viewModelScope.launch {
            when (mode) {
                HelperPickerMode.CALL -> executeCallHelper(helper)
                HelperPickerMode.MESSAGE -> executeMessageHelper(helper)
            }
        }
    }

    private suspend fun startHelperFlow(mode: HelperPickerMode) {
        val helpers = observeHelpers().first().filter { it.whatsappEnabled }
        if (helpers.isEmpty()) {
            speaker.speak("Please add a trusted helper first.")
            _uiState.update { it.copy(statusMessage = "Add a trusted helper in Settings.") }
            return
        }
        if (helpers.size > 1) {
            _uiState.update {
                it.copy(
                    showHelperPicker = true,
                    helperPickerMode = mode,
                    helperPickerCandidates = helpers,
                )
            }
            return
        }
        when (mode) {
            HelperPickerMode.CALL -> executeCallHelper(helpers.first())
            HelperPickerMode.MESSAGE -> executeMessageHelper(helpers.first())
        }
    }

    private suspend fun executeCallHelper(helper: TrustedHelper) {
        when (val result = helperHandoff.openCallHelperOnWhatsApp(appContext, helper, pauseCamera = true)) {
            WhatsAppLaunchResult.Success -> Unit
            WhatsAppLaunchResult.WhatsAppNotInstalled -> handleWhatsAppMissing(helper)
            is WhatsAppLaunchResult.Failed -> {
                speaker.speak(result.reason)
                sessionController.tryResumeAfterWhatsAppReturn()
            }
        }
    }

    private suspend fun executeMessageHelper(helper: TrustedHelper) {
        when (val result = helperHandoff.openMessageHelper(appContext, helper)) {
            WhatsAppLaunchResult.Success -> Unit
            WhatsAppLaunchResult.WhatsAppNotInstalled -> handleWhatsAppMissing(helper)
            is WhatsAppLaunchResult.Failed -> speaker.speak(result.reason)
        }
    }

    private fun handleWhatsAppMissing(helper: TrustedHelper) {
        speaker.speak(
            "WhatsApp is not installed. You can call or message your helper using normal phone options.",
        )
        sessionController.tryResumeAfterWhatsAppReturn()
        DialerIntentLauncher.openDialer(appContext, helper.phoneNumber)
    }

    fun exitPhoneMode(onDone: () -> Unit) {
        narrationJob?.cancel()
        speaker.stop()
        sessionController.markPhoneModeStopped()
        onDone()
    }

    private fun startNarrationLoop() {
        narrationJob?.cancel()
        narrationJob = viewModelScope.launch {
            delay(NARRATION_START_DELAY_MS)
            while (isActive) {
                if (!sessionController.isAnalysisPaused() && !speaker.isSpeaking.value) {
                    runNarrationTick()
                }
                delay(NARRATION_INTERVAL_MS)
            }
        }
    }

    private suspend fun runNarrationTick() {
        val jpeg = latestJpeg ?: return
        val objects = _uiState.value.detectedObjects
        val language = _uiState.value.guidanceLanguage
        when (val result = describeScene(CapturedImage(jpeg))) {
            is OperationResult.Failure -> Unit
            is OperationResult.Success -> {
                val line = PhoneObjectNarration.enrichSpokenSummary(
                    sceneSummary = result.value.spokenSummary,
                    objects = objects,
                    language = language,
                )
                latestImageHolder.updateSceneSummary(line)
                _uiState.value = _uiState.value.copy(lastSpoken = line)
                val useHausaMms = language == GuidanceLanguage.Hausa
                speaker.speak(line, interrupt = !useHausaMms)
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
