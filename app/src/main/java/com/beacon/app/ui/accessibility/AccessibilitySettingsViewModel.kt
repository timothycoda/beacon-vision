package com.beacon.app.ui.accessibility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beacon.domain.accessibility.AccessibilitySettings
import com.beacon.domain.accessibility.AccessibilityVoiceGuide
import com.beacon.domain.accessibility.AccessibilityPhraseKey
import com.beacon.domain.accessibility.ObserveAccessibilitySettingsUseCase
import com.beacon.domain.accessibility.UpdateAccessibilitySettingsUseCase
import com.beacon.domain.accessibility.VoiceGuideMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccessibilitySettingsViewModel @Inject constructor(
    observeSettings: ObserveAccessibilitySettingsUseCase,
    private val updateSettings: UpdateAccessibilitySettingsUseCase,
    private val voiceGuide: AccessibilityVoiceGuide,
) : ViewModel() {

    val settings: StateFlow<AccessibilitySettings> =
        observeSettings()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccessibilitySettings())

    fun setVoiceGuideEnabled(enabled: Boolean) {
        viewModelScope.launch {
            updateSettings { it.copy(voiceGuideEnabled = enabled) }
            voiceGuide.speakPhrase(
                if (enabled) AccessibilityPhraseKey.VOICE_GUIDE_ENABLED
                else AccessibilityPhraseKey.VOICE_GUIDE_DISABLED,
                important = false,
            )
        }
    }

    fun setVoiceGuideMode(mode: VoiceGuideMode) {
        viewModelScope.launch { updateSettings { it.copy(voiceGuideMode = mode) } }
    }

    fun setHaptics(enabled: Boolean) {
        viewModelScope.launch { updateSettings { it.copy(hapticFeedbackEnabled = enabled) } }
    }

    fun setRepeatScreenIntro(enabled: Boolean) {
        viewModelScope.launch { updateSettings { it.copy(repeatScreenIntro = enabled) } }
    }

    fun setSpeakFocusedControls(enabled: Boolean) {
        viewModelScope.launch { updateSettings { it.copy(speakFocusedControls = enabled) } }
    }
}
