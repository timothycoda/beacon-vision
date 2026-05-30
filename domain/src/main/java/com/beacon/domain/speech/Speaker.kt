package com.beacon.domain.speech

import kotlinx.coroutines.flow.StateFlow

/**
 * Offline text-to-speech for audio cues. Implemented in :data on top of the
 * Android TextToSpeech engine. Voice-first UX depends on this being reliable and
 * interruptible.
 */
interface Speaker {
    /** True while an utterance is being spoken. */
    val isSpeaking: StateFlow<Boolean>

    /**
     * Speak [text]. When [interrupt] is true, any current speech is flushed and
     * this utterance starts immediately; otherwise it is queued after current speech.
     */
    fun speak(text: String, interrupt: Boolean = true)

    /** Stop any current and queued speech. */
    fun stop()
}
