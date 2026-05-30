package com.beacon.domain.speech

/**
 * User-chosen text-to-speech options. Null engine/voice means use the system default.
 */
data class SpeechSettings(
    val enginePackage: String? = null,
    val voiceName: String? = null,
    val speechRate: Float = DEFAULT_SPEECH_RATE,
) {
    companion object {
        const val DEFAULT_SPEECH_RATE = 1f
        const val MIN_SPEECH_RATE = 0.5f
        const val MAX_SPEECH_RATE = 1.5f
    }
}

data class TtsEngineOption(
    val packageName: String,
    val label: String,
)

data class TtsVoiceOption(
    val name: String,
    val label: String,
    val localeTag: String?,
)
