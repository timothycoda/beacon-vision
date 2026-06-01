package com.beacon.app.accessibility

import com.beacon.domain.accessibility.AccessibilityPhraseProvider
import com.beacon.domain.accessibility.AccessibilityVoiceGuide
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AccessibilityEntryPoint {
    fun accessibilityVoiceGuide(): AccessibilityVoiceGuide
    fun accessibilityPhraseProvider(): AccessibilityPhraseProvider
}
