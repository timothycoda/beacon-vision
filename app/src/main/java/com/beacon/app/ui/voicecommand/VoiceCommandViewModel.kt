package com.beacon.app.ui.voicecommand

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beacon.domain.speech.Speaker
import com.beacon.domain.voice.SpeechToText
import com.beacon.domain.voice.VoiceCommand
import com.beacon.domain.voice.VoiceCommandMatcher
import com.beacon.domain.voice.VoiceError
import com.beacon.domain.voice.VoiceListeningState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface VoiceStatus {
    data object Idle : VoiceStatus
    data object Preparing : VoiceStatus
    data object Listening : VoiceStatus
    data class Hearing(val partial: String) : VoiceStatus
    data class Recognized(val label: String) : VoiceStatus
    data class NotUnderstood(val transcript: String) : VoiceStatus
    data class Error(val message: String) : VoiceStatus
}

data class VoiceCommandUiState(
    val status: VoiceStatus = VoiceStatus.Idle,
    val available: Boolean = true,
) {
    val isListening: Boolean
        get() = status is VoiceStatus.Preparing ||
            status is VoiceStatus.Listening ||
            status is VoiceStatus.Hearing
}

@HiltViewModel
class VoiceCommandViewModel @Inject constructor(
    private val speechToText: SpeechToText,
    private val speaker: Speaker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        VoiceCommandUiState(available = speechToText.isAvailable()),
    )
    val uiState: StateFlow<VoiceCommandUiState> = _uiState.asStateFlow()

    /** Navigable commands the screen should act on. Stop/Unknown are handled here. */
    private val _commands = MutableSharedFlow<VoiceCommand>(extraBufferCapacity = 1)
    val commands: SharedFlow<VoiceCommand> = _commands.asSharedFlow()

    private var listenJob: Job? = null

    fun startListening() {
        if (!_uiState.value.available) {
            _uiState.update {
                it.copy(status = VoiceStatus.Error("Voice commands are not available on this device."))
            }
            return
        }
        if (listenJob?.isActive == true) return

        listenJob = viewModelScope.launch {
            speaker.stop()
            speechToText.listenOnce().collect { state ->
                when (state) {
                    VoiceListeningState.Preparing ->
                        _uiState.update { it.copy(status = VoiceStatus.Preparing) }
                    VoiceListeningState.Listening ->
                        _uiState.update { it.copy(status = VoiceStatus.Listening) }
                    is VoiceListeningState.Partial ->
                        _uiState.update { it.copy(status = VoiceStatus.Hearing(state.transcript)) }
                    is VoiceListeningState.Result -> handleTranscript(state.transcript)
                    is VoiceListeningState.Error -> handleError(state.reason)
                }
            }
        }
    }

    fun cancelListening() {
        listenJob?.cancel()
        listenJob = null
        _uiState.update { it.copy(status = VoiceStatus.Idle) }
    }

    private fun handleTranscript(transcript: String) {
        when (val command = VoiceCommandMatcher.match(transcript)) {
            VoiceCommand.StopSpeaking -> {
                speaker.stop()
                _uiState.update { it.copy(status = VoiceStatus.Recognized("Stopped speaking")) }
            }
            is VoiceCommand.Unknown -> {
                _uiState.update { it.copy(status = VoiceStatus.NotUnderstood(command.transcript)) }
                speaker.speak(
                    "Sorry, I didn't understand. Try saying: what is ahead, read this, " +
                        "walking mode, or emergency.",
                )
            }
            else -> {
                _uiState.update { it.copy(status = VoiceStatus.Recognized(label(command))) }
                _commands.tryEmit(command)
            }
        }
    }

    private fun handleError(reason: VoiceError) {
        val message = when (reason) {
            VoiceError.NO_MATCH, VoiceError.NO_SPEECH ->
                "I didn't hear anything. Tap the microphone and try again."
            VoiceError.PERMISSION_DENIED ->
                "Microphone permission is needed for voice commands."
            VoiceError.UNAVAILABLE ->
                "Voice commands are not available on this device."
            VoiceError.BUSY -> "The microphone is busy. Try again in a moment."
            VoiceError.NETWORK -> "Speech recognition needs a moment. Please try again."
            VoiceError.UNKNOWN -> "Something went wrong. Tap the microphone to try again."
        }
        _uiState.update { it.copy(status = VoiceStatus.Error(message)) }
        if (reason != VoiceError.NO_MATCH && reason != VoiceError.NO_SPEECH) {
            speaker.speak(message)
        }
    }

    private fun label(command: VoiceCommand): String = when (command) {
        VoiceCommand.WhatIsAhead -> "What is ahead"
        VoiceCommand.ReadText -> "Read this"
        VoiceCommand.WalkingMode -> "Walking mode"
        VoiceCommand.Emergency -> "Emergency"
        VoiceCommand.GlassesStatus -> "Glasses status"
        VoiceCommand.VoiceSettings -> "Voice settings"
        VoiceCommand.GoHome -> "Home"
        VoiceCommand.StopSpeaking -> "Stop"
        is VoiceCommand.Unknown -> command.transcript
    }

    override fun onCleared() {
        listenJob?.cancel()
        speaker.stop()
    }
}
