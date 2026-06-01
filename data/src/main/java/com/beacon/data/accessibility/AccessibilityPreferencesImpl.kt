package com.beacon.data.accessibility

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.beacon.domain.accessibility.AccessibilitySettings
import com.beacon.domain.accessibility.AccessibilitySettingsRepository
import com.beacon.domain.accessibility.VoiceGuideMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.accessibilityStore by preferencesDataStore("accessibility_prefs")

@Singleton
class AccessibilityPreferencesImpl @Inject constructor(
    @ApplicationContext context: Context,
) : AccessibilitySettingsRepository {

    private val store = context.accessibilityStore

    override val settings: Flow<AccessibilitySettings> =
        store.data.map { prefs ->
            AccessibilitySettings(
                voiceGuideEnabled = prefs[KEY_ENABLED] ?: true,
                voiceGuideMode = VoiceGuideMode.fromStorageKey(prefs[KEY_MODE]),
                hapticFeedbackEnabled = prefs[KEY_HAPTICS] ?: true,
                repeatScreenIntro = prefs[KEY_REPEAT_INTRO] ?: false,
                speakFocusedControls = prefs[KEY_SPEAK_FOCUS] ?: true,
                focusSpeechThrottleMs = prefs[KEY_FOCUS_THROTTLE]
                    ?: AccessibilitySettings.DEFAULT_FOCUS_THROTTLE_MS,
            )
        }

    override suspend fun update(transform: (AccessibilitySettings) -> AccessibilitySettings) {
        store.edit { prefs ->
            val next = transform(
                AccessibilitySettings(
                    voiceGuideEnabled = prefs[KEY_ENABLED] ?: true,
                    voiceGuideMode = VoiceGuideMode.fromStorageKey(prefs[KEY_MODE]),
                    hapticFeedbackEnabled = prefs[KEY_HAPTICS] ?: true,
                    repeatScreenIntro = prefs[KEY_REPEAT_INTRO] ?: false,
                    speakFocusedControls = prefs[KEY_SPEAK_FOCUS] ?: true,
                    focusSpeechThrottleMs = prefs[KEY_FOCUS_THROTTLE]
                        ?: AccessibilitySettings.DEFAULT_FOCUS_THROTTLE_MS,
                ),
            )
            prefs[KEY_ENABLED] = next.voiceGuideEnabled
            prefs[KEY_MODE] = next.voiceGuideMode.storageKey
            prefs[KEY_HAPTICS] = next.hapticFeedbackEnabled
            prefs[KEY_REPEAT_INTRO] = next.repeatScreenIntro
            prefs[KEY_SPEAK_FOCUS] = next.speakFocusedControls
            prefs[KEY_FOCUS_THROTTLE] = next.focusSpeechThrottleMs
        }
    }

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("voice_guide_enabled")
        val KEY_MODE = stringPreferencesKey("voice_guide_mode")
        val KEY_HAPTICS = booleanPreferencesKey("haptic_enabled")
        val KEY_REPEAT_INTRO = booleanPreferencesKey("repeat_screen_intro")
        val KEY_SPEAK_FOCUS = booleanPreferencesKey("speak_focused_controls")
        val KEY_FOCUS_THROTTLE = longPreferencesKey("focus_throttle_ms")
    }
}
