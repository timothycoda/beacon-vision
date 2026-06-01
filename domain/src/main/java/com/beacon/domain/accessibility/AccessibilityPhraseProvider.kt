package com.beacon.domain.accessibility

/** Resolves localized phrase text for Voice Guide and semantics. */
interface AccessibilityPhraseProvider {
    fun phrase(key: AccessibilityPhraseKey): String
}
