package com.beacon.domain.accessibility

/**
 * Describes a tappable control for TalkBack semantics and optional Voice Guide speech.
 */
data class SpeakableAction(
    val label: String,
    val phraseKey: AccessibilityPhraseKey? = null,
    val spokenDescription: String? = null,
    val dangerLevel: DangerLevel = DangerLevel.Normal,
    val contentDescription: String? = null,
) {
    fun resolveContentDescription(spokenFallback: String): String {
        val base = contentDescription ?: spokenFallback
        return when (dangerLevel) {
            DangerLevel.Emergency -> "Warning. Emergency. $base"
            DangerLevel.Caution -> "Caution. $base"
            DangerLevel.Normal -> base
        }
    }
}
