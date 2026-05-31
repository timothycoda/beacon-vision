package com.beacon.data.speech

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.beacon.core.concurrency.DispatcherProvider
import com.beacon.core.log.BeaconLog
import com.beacon.data.guidance.GuidanceLanguagePreferences
import com.beacon.data.modelpack.ModelPackPaths
import com.beacon.domain.guidance.GuidanceLanguage
import com.beacon.domain.speech.Speaker
import com.beacon.domain.speech.SpeechSettings
import com.beacon.domain.speech.SpeechSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes Hausa guidance to on-device MMS when the voice pack is installed; otherwise
 * Android [TextToSpeech]. Avoids rebuilding TTS when switching to Hausa+MMS (prevents
 * vendor ROM crashes). Falls back to system TTS if MMS inference fails.
 */
@Singleton
class SpeechController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SpeechSettingsRepository,
    private val languagePrefs: GuidanceLanguagePreferences,
    private val modelPackPaths: ModelPackPaths,
    private val hausaMms: HausaMmsTtsEngine,
    private val dispatchers: DispatcherProvider,
) : Speaker {

    @Volatile
    private var guidanceLanguage: GuidanceLanguage = GuidanceLanguage.English

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)
    private val androidSpeaking = MutableStateFlow(false)

    override val isSpeaking: StateFlow<Boolean> =
        combine(androidSpeaking, hausaMms.isSpeaking) { android, hausa -> android || hausa }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), false)

    @Volatile
    private var ready = false

    @Volatile
    private var currentSettings = SpeechSettings()

    private var pending: Pair<String, Boolean>? = null
    private val utteranceCounter = AtomicInteger(0)
    private var tts: TextToSpeech? = null

    private val speechAudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    init {
        scope.launch {
            combine(
                settingsRepository.settings.distinctUntilChanged(),
                languagePrefs.language,
            ) { settings, language ->
                settings to language
            }.collect { (settings, language) ->
                val wasHausa = guidanceLanguage == GuidanceLanguage.Hausa
                currentSettings = settings
                guidanceLanguage = language

                if (wasHausa && language == GuidanceLanguage.English) {
                    hausaMms.release()
                }

                if (shouldUseAndroidTts()) {
                    withContext(dispatchers.main) {
                        runCatching { recreateEngine(settings) }
                            .onFailure { BeaconLog.e(TAG, "TTS recreate failed", it) }
                    }
                }
            }
        }
    }

    private fun shouldUseAndroidTts(): Boolean = !useHausaMms()

    private fun useHausaMms(): Boolean =
        guidanceLanguage == GuidanceLanguage.Hausa &&
            modelPackPaths.isHausaVoiceInstalled() &&
            hausaMms.isAvailable() &&
            Build.SUPPORTED_ABIS.contains("arm64-v8a")

    private fun recreateEngine(settings: SpeechSettings) {
        ready = false
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        val engine = TextToSpeech(
            context.applicationContext,
            { status -> onInit(status, settings) },
            settings.enginePackage,
        )
        runCatching { engine.setAudioAttributes(speechAudioAttributes) }
        attachProgressListener(engine)
        tts = engine
    }

    private fun attachProgressListener(engine: TextToSpeech) {
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { androidSpeaking.value = true }
            override fun onDone(utteranceId: String?) { androidSpeaking.value = false }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { androidSpeaking.value = false }
            override fun onError(utteranceId: String?, errorCode: Int) { androidSpeaking.value = false }
        })
    }

    private fun onInit(status: Int, settings: SpeechSettings) {
        val engine = tts ?: return
        ready = status == TextToSpeech.SUCCESS
        if (!ready) {
            BeaconLog.e(TAG, "TextToSpeech init failed: $status engine=${settings.enginePackage}")
            return
        }
        applyVoiceAndRate(engine, settings)
        pending?.let { (text, interrupt) ->
            pending = null
            speak(text, interrupt)
        }
    }

    private fun applyVoiceAndRate(engine: TextToSpeech, settings: SpeechSettings) {
        val locale = when (guidanceLanguage) {
            GuidanceLanguage.Hausa -> Locale.forLanguageTag("ha-NG")
            GuidanceLanguage.English -> Locale.getDefault()
        }
        val langResult = runCatching { engine.language = locale }
        if (langResult.isFailure || engine.language?.language != locale.language) {
            runCatching { engine.language = Locale.getDefault() }
        }
        val voiceName = settings.voiceName
        if (!voiceName.isNullOrBlank()) {
            val voice = engine.voices?.firstOrNull { it.name == voiceName }
            if (voice != null) {
                runCatching { engine.voice = voice }
            }
        }
        runCatching { engine.setSpeechRate(settings.speechRate) }
    }

    override fun speak(text: String, interrupt: Boolean) {
        if (text.isBlank()) return
        if (useHausaMms()) {
            if (interrupt) {
                hausaMms.stop()
                runCatching { tts?.stop() }
            }
            hausaMms.speak(text, currentSettings.speechRate, interrupt) { mmsOk ->
                if (!mmsOk) {
                    BeaconLog.w(TAG, "Hausa MMS unavailable, falling back to system TTS")
                    scope.launch(dispatchers.main) {
                        speakWithAndroid(text, interrupt)
                    }
                }
            }
            return
        }
        speakWithAndroid(text, interrupt)
    }

    private fun speakWithAndroid(text: String, interrupt: Boolean) {
        val engine = tts
        if (!ready || engine == null) {
            pending = text to interrupt
            if (tts == null && shouldUseAndroidTts()) {
                scope.launch(dispatchers.main) {
                    runCatching { recreateEngine(currentSettings) }
                }
            }
            return
        }
        val mode = if (interrupt) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val id = "beacon-${utteranceCounter.incrementAndGet()}"
        runCatching { engine.speak(text, mode, null, id) }
            .onFailure { BeaconLog.e(TAG, "speak failed", it) }
    }

    override fun stop() {
        pending = null
        hausaMms.stop()
        runCatching { tts?.stop() }
        androidSpeaking.value = false
    }

    fun speakSample(text: String = SAMPLE_UTTERANCE) {
        speak(text, interrupt = true)
    }

    fun currentEnginePackage(): String? = currentSettings.enginePackage

    private companion object {
        const val TAG = "SpeechController"
        const val SAMPLE_UTTERANCE = "This is how Beacon will sound."
    }
}
