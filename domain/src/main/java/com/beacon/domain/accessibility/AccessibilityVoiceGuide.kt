package com.beacon.domain.accessibility

/** Optional in-app spoken navigation layered on TalkBack. */
interface AccessibilityVoiceGuide {
    suspend fun onScreenOpened(screenId: String)
    fun speakPhrase(key: AccessibilityPhraseKey, important: Boolean = false)
    fun speakRaw(text: String, important: Boolean = false)
    fun speakFocusedAction(action: SpeakableAction)
    fun speakActionHelp(action: SpeakableAction)
    fun isTalkBackActive(): Boolean
}
