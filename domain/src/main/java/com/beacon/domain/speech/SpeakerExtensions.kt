package com.beacon.domain.speech

import kotlinx.coroutines.flow.first

/** Suspends until the current utterance finishes (TTS or on-device MMS). */
suspend fun Speaker.awaitNotSpeaking() {
    if (!isSpeaking.value) return
    isSpeaking.first { !it }
}
