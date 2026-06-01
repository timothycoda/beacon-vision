package com.beacon.data.accessibility

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.beacon.core.concurrency.DispatcherProvider
import com.beacon.core.log.BeaconLog
import com.beacon.domain.accessibility.AccessibilityPhraseKey
import com.beacon.domain.accessibility.AccessibilitySettings
import com.beacon.domain.accessibility.AccessibilitySettingsRepository
import com.beacon.domain.accessibility.AccessibilityVoiceGuide
import com.beacon.domain.accessibility.DangerLevel
import com.beacon.domain.accessibility.SpeakableAction
import com.beacon.domain.accessibility.VoiceGuideMode
import com.beacon.domain.speech.Speaker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityVoiceGuideImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val phraseProvider: com.beacon.domain.accessibility.AccessibilityPhraseProvider,
    private val settingsRepository: AccessibilitySettingsRepository,
    private val talkBack: TalkBackStateProvider,
    private val speaker: Speaker,
    dispatchers: DispatcherProvider,
) : AccessibilityVoiceGuide {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    @Volatile
    private var cachedSettings = AccessibilitySettings()

    @Volatile
    private var lastScreenId: String? = null

    @Volatile
    private var lastScreenIntroAtMs: Long = 0L

    private val lastFocusPhraseAtMs = mutableMapOf<String, Long>()

    init {
        scope.launch {
            settingsRepository.settings.collect { cachedSettings = it }
        }
    }

    override suspend fun onScreenOpened(screenId: String) {
        val settings = settingsRepository.settings.first()
        if (!settings.voiceGuideEnabled) return

        val phraseKey = ScreenIntroPhrases.key(screenId) ?: return
        val now = System.currentTimeMillis()
        val sameScreen = screenId == lastScreenId
        if (sameScreen && !settings.repeatScreenIntro) return
        if (sameScreen && now - lastScreenIntroAtMs < SCREEN_INTRO_COOLDOWN_MS) return

        lastScreenId = screenId
        lastScreenIntroAtMs = now
        speakPhrase(phraseKey, important = false)
    }

    override fun speakPhrase(key: AccessibilityPhraseKey, important: Boolean) {
        if (!shouldSpeak(important)) return
        val text = phraseProvider.phrase(key)
        if (text.isBlank()) return
        enqueueSpeech(text, important)
    }

    override fun speakRaw(text: String, important: Boolean) {
        if (!shouldSpeak(important)) return
        if (text.isBlank()) return
        enqueueSpeech(text, important)
    }

    override fun speakFocusedAction(action: SpeakableAction) {
        val settings = cachedSettings
        if (!settings.voiceGuideEnabled || !settings.speakFocusedControls) return
        if (!shouldSpeak(important = action.dangerLevel != DangerLevel.Normal)) return

        val phraseKey = action.phraseKey ?: return
        val throttleKey = phraseKey.name
        val now = System.currentTimeMillis()
        synchronized(lastFocusPhraseAtMs) {
            val last = lastFocusPhraseAtMs[throttleKey] ?: 0L
            if (now - last < settings.focusSpeechThrottleMs) return
            lastFocusPhraseAtMs[throttleKey] = now
        }

        val text = action.spokenDescription ?: phraseProvider.phrase(phraseKey)
        enqueueSpeech(text, action.dangerLevel != DangerLevel.Normal)
        maybeHaptic(settings, light = true)
    }

    override fun speakActionHelp(action: SpeakableAction) {
        val key = action.phraseKey ?: return
        speakPhrase(key, important = false)
    }

    override fun isTalkBackActive(): Boolean = talkBack.isTalkBackTouchExplorationEnabled()

    private fun shouldSpeak(important: Boolean): Boolean {
        val settings = cachedSettings
        if (!settings.voiceGuideEnabled) return false
        return when (settings.voiceGuideMode) {
            VoiceGuideMode.Always -> true
            VoiceGuideMode.ImportantOnly -> important
            VoiceGuideMode.WhenTalkBackOff -> !talkBack.isTalkBackTouchExplorationEnabled()
        }
    }

    private fun enqueueSpeech(text: String, important: Boolean) {
        if (speaker.isSpeaking.value && !important) return
        speaker.speak(text, interrupt = important)
    }

    private fun maybeHaptic(settings: AccessibilitySettings, light: Boolean) {
        if (!settings.hapticFeedbackEnabled) return
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val duration = if (light) 20L else 40L
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        }.onFailure { BeaconLog.e(TAG, "haptic failed", it) }
    }

    private object ScreenIntroPhrases {
        fun key(screenId: String): AccessibilityPhraseKey? = when (screenId) {
            com.beacon.domain.accessibility.AccessibilityScreenIds.HOME ->
                AccessibilityPhraseKey.HOME_INTRO
            com.beacon.domain.accessibility.AccessibilityScreenIds.PHONE_GUIDANCE ->
                AccessibilityPhraseKey.PHONE_MODE_INTRO
            com.beacon.domain.accessibility.AccessibilityScreenIds.READ_THIS ->
                AccessibilityPhraseKey.DOCUMENT_READER_INTRO
            com.beacon.domain.accessibility.AccessibilityScreenIds.WALKING_MODE ->
                AccessibilityPhraseKey.WALKING_MODE_INTRO
            com.beacon.domain.accessibility.AccessibilityScreenIds.EMERGENCY ->
                AccessibilityPhraseKey.EMERGENCY_INTRO
            com.beacon.domain.accessibility.AccessibilityScreenIds.TRUSTED_HELPERS ->
                AccessibilityPhraseKey.TRUSTED_HELPERS_INTRO
            com.beacon.domain.accessibility.AccessibilityScreenIds.SETTINGS ->
                AccessibilityPhraseKey.SETTINGS_INTRO
            com.beacon.domain.accessibility.AccessibilityScreenIds.LIVE_HELP ->
                AccessibilityPhraseKey.LIVE_HELP_INTRO
            else -> null
        }
    }

    private companion object {
        const val TAG = "AccessibilityVoiceGuide"
        const val SCREEN_INTRO_COOLDOWN_MS = 30_000L
    }
}
