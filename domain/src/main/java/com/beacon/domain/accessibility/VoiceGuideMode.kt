package com.beacon.domain.accessibility

/** When the in-app Voice Guide may speak (in addition to TalkBack). */
enum class VoiceGuideMode(val storageKey: String) {
    /** Speak intros, focus help, and important events. */
    Always("always"),
    /** Speak only warnings, errors, emergencies, and connection changes. */
    ImportantOnly("important"),
    /** Speak when TalkBack touch exploration is off. */
    WhenTalkBackOff("talkback_off"),
    ;

    companion object {
        fun fromStorageKey(key: String?): VoiceGuideMode =
            entries.firstOrNull { it.storageKey == key } ?: WhenTalkBackOff
    }
}
