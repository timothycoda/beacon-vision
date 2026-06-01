package com.beacon.domain.accessibility

data class AccessibilitySettings(
    val voiceGuideEnabled: Boolean = true,
    val voiceGuideMode: VoiceGuideMode = VoiceGuideMode.WhenTalkBackOff,
    val hapticFeedbackEnabled: Boolean = true,
    val repeatScreenIntro: Boolean = false,
    val speakFocusedControls: Boolean = true,
    val focusSpeechThrottleMs: Long = DEFAULT_FOCUS_THROTTLE_MS,
) {
    companion object {
        const val DEFAULT_FOCUS_THROTTLE_MS = 4_000L
    }
}
