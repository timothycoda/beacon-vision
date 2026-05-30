package com.beacon.app.ui.read

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beacon.core.result.OperationResult
import com.beacon.domain.history.HistoryEntry
import com.beacon.domain.history.HistoryRepository
import com.beacon.domain.history.HistoryType
import com.beacon.domain.speech.Speaker
import com.beacon.domain.text.usecase.ReadTextUseCase
import com.beacon.domain.vision.usecase.CapturePhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ReadStatus {
    data object Idle : ReadStatus
    data object Capturing : ReadStatus
    data object Reading : ReadStatus
    data class Result(val fullText: String, val spokenText: String) : ReadStatus
    data class Error(val message: String) : ReadStatus
}

data class ReadThisUiState(
    val status: ReadStatus = ReadStatus.Idle,
    val imageBytes: ByteArray? = null,
) {
    val isBusy: Boolean get() = status is ReadStatus.Capturing || status is ReadStatus.Reading
}

@HiltViewModel
class ReadThisViewModel @Inject constructor(
    private val capturePhoto: CapturePhotoUseCase,
    private val readText: ReadTextUseCase,
    private val historyRepository: HistoryRepository,
    private val speaker: Speaker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadThisUiState())
    val uiState: StateFlow<ReadThisUiState> = _uiState.asStateFlow()

    fun readText() {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            speaker.speak("Reading text. One moment.")
            _uiState.value = ReadThisUiState(status = ReadStatus.Capturing)

            when (val capture = capturePhoto()) {
                is OperationResult.Failure -> fail(capture.message)
                is OperationResult.Success -> {
                    val image = capture.value
                    _uiState.value = ReadThisUiState(
                        status = ReadStatus.Reading,
                        imageBytes = image.jpegBytes,
                    )
                    when (val recognized = readText(image)) {
                        is OperationResult.Failure -> fail(recognized.message)
                        is OperationResult.Success -> {
                            val text = recognized.value
                            speaker.speak(text.spokenText)
                            historyRepository.add(
                                HistoryEntry(type = HistoryType.READ, summary = text.spokenText),
                            )
                            _uiState.value = ReadThisUiState(
                                status = ReadStatus.Result(text.fullText, text.spokenText),
                                imageBytes = image.jpegBytes,
                            )
                        }
                    }
                }
            }
        }
    }

    fun repeatAloud() {
        val spoken = (_uiState.value.status as? ReadStatus.Result)?.spokenText ?: return
        speaker.speak(spoken)
    }

    private fun fail(message: String) {
        speaker.speak(message)
        _uiState.value = ReadThisUiState(status = ReadStatus.Error(message))
    }

    override fun onCleared() {
        speaker.stop()
    }
}
