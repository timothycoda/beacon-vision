package com.beacon.app.ui.ahead

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beacon.core.result.OperationResult
import com.beacon.domain.history.HistoryEntry
import com.beacon.domain.history.HistoryRepository
import com.beacon.domain.history.HistoryType
import com.beacon.domain.speech.Speaker
import com.beacon.domain.vision.SceneLabel
import com.beacon.domain.vision.usecase.CapturePhotoUseCase
import com.beacon.domain.vision.usecase.DescribeSceneUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AheadStatus {
    data object Idle : AheadStatus
    data object Capturing : AheadStatus
    data object Analyzing : AheadStatus
    data class Result(val summary: String, val labels: List<SceneLabel>) : AheadStatus
    data class Error(val message: String) : AheadStatus
}

data class WhatIsAheadUiState(
    val status: AheadStatus = AheadStatus.Idle,
    val imageBytes: ByteArray? = null,
) {
    val isBusy: Boolean get() = status is AheadStatus.Capturing || status is AheadStatus.Analyzing
}

@HiltViewModel
class WhatIsAheadViewModel @Inject constructor(
    private val capturePhoto: CapturePhotoUseCase,
    private val describeScene: DescribeSceneUseCase,
    private val historyRepository: HistoryRepository,
    private val speaker: Speaker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhatIsAheadUiState())
    val uiState: StateFlow<WhatIsAheadUiState> = _uiState.asStateFlow()

    fun lookAhead() {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            speaker.speak("Looking ahead. One moment.")
            _uiState.value = WhatIsAheadUiState(status = AheadStatus.Capturing)

            when (val capture = capturePhoto()) {
                is OperationResult.Failure -> fail(capture.message)
                is OperationResult.Success -> {
                    val image = capture.value
                    _uiState.value = WhatIsAheadUiState(
                        status = AheadStatus.Analyzing,
                        imageBytes = image.jpegBytes,
                    )
                    when (val described = describeScene(image)) {
                        is OperationResult.Failure -> fail(described.message)
                        is OperationResult.Success -> {
                            val scene = described.value
                            speaker.speak(scene.spokenSummary)
                            historyRepository.add(
                                HistoryEntry(type = HistoryType.AHEAD, summary = scene.spokenSummary),
                            )
                            _uiState.value = WhatIsAheadUiState(
                                status = AheadStatus.Result(scene.spokenSummary, scene.labels),
                                imageBytes = image.jpegBytes,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun fail(message: String) {
        speaker.speak(message)
        _uiState.value = WhatIsAheadUiState(status = AheadStatus.Error(message))
    }

    override fun onCleared() {
        speaker.stop()
    }
}
