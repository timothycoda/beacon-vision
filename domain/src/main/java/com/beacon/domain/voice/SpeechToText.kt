package com.beacon.domain.voice

import kotlinx.coroutines.flow.Flow

/**
 * Progress of a single hands-free listening session.
 */
sealed interface VoiceListeningState {
    /** Recogniser is initialising. */
    data object Preparing : VoiceListeningState

    /** Microphone is open and waiting for speech. */
    data object Listening : VoiceListeningState

    /** Live, not-yet-final transcript for on-screen feedback. */
    data class Partial(val transcript: String) : VoiceListeningState

    /** Final transcript; the session is complete. */
    data class Result(val transcript: String) : VoiceListeningState

    /** Listening failed or nothing was understood. */
    data class Error(val reason: VoiceError) : VoiceListeningState
}

/** Coarse, user-presentable failure reasons (no raw platform codes leak out). */
enum class VoiceError {
    NO_MATCH,
    NO_SPEECH,
    PERMISSION_DENIED,
    UNAVAILABLE,
    BUSY,
    NETWORK,
    UNKNOWN,
}

/**
 * On-device speech-to-text for hands-free control. Implemented in :data on top
 * of the Android [android.speech.SpeechRecognizer], preferring the offline
 * recogniser when the device provides one.
 */
interface SpeechToText {

    /** True if any recogniser is present on this device. */
    fun isAvailable(): Boolean

    /**
     * Listen for a single utterance. The returned [Flow] emits progress states
     * and completes after a [VoiceListeningState.Result] or
     * [VoiceListeningState.Error]. Cancelling the collection stops listening.
     */
    fun listenOnce(): Flow<VoiceListeningState>
}
