package com.beacon.domain.speech

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Suspends until the current utterance finishes (TTS or on-device MMS). */
suspend fun Speaker.awaitNotSpeaking(timeoutMs: Long = 20_000L) {
    if (!isSpeaking.value) return
    withTimeoutOrNull(timeoutMs) {
        isSpeaking.first { !it }
    }
}
