package com.beacon.data.speech

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.beacon.core.concurrency.DispatcherProvider
import com.beacon.core.log.BeaconLog
import com.beacon.domain.speech.Speaker
import com.beacon.domain.speech.SpeechSettings
import com.beacon.domain.speech.SpeechSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline text-to-speech on Android [TextToSpeech]. Applies the user's saved engine,
 * voice, and speech rate from [SpeechSettingsRepository].
 */
@Singleton
class SpeechController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SpeechSettingsRepository,
    dispatchers: DispatcherProvider,
) : Speaker {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)
    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    @Volatile
    private var ready = false

    @Volatile
    private var currentSettings = SpeechSettings()

    private var pending: Pair<String, Boolean>? = null
    private val utteranceCounter = AtomicInteger(0)
    private var tts: TextToSpeech? = null

    // Tag speech as media/spoken content so Android routes it to the active media
    // output. When the glasses are connected as a Bluetooth audio (A2DP) device,
    // this lets Beacon's voice play through the glasses instead of the phone.
    private val speechAudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    init {
        scope.launch {
            settingsRepository.settings
                .distinctUntilChanged()
                .collect { settings ->
                    currentSettings = settings
                    recreateEngine(settings)
                }
        }
    }

    private fun recreateEngine(settings: SpeechSettings) {
        ready = false
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
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
            override fun onStart(utteranceId: String?) { _isSpeaking.value = true }
            override fun onDone(utteranceId: String?) { _isSpeaking.value = false }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { _isSpeaking.value = false }
            override fun onError(utteranceId: String?, errorCode: Int) { _isSpeaking.value = false }
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
        runCatching { engine.language = Locale.getDefault() }
        val voiceName = settings.voiceName
        if (!voiceName.isNullOrBlank()) {
            val voice = engine.voices?.firstOrNull { it.name == voiceName }
            if (voice != null) {
                runCatching { engine.voice = voice }
            } else {
                BeaconLog.w(TAG, "Voice not found: $voiceName")
            }
        }
        runCatching { engine.setSpeechRate(settings.speechRate) }
        BeaconLog.i(
            TAG,
            "TTS ready engine=${settings.enginePackage ?: "default"} voice=${settings.voiceName ?: "default"} rate=${settings.speechRate}",
        )
    }

    override fun speak(text: String, interrupt: Boolean) {
        if (text.isBlank()) return
        val engine = tts
        if (!ready || engine == null) {
            pending = text to interrupt
            return
        }
        val mode = if (interrupt) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val id = "beacon-${utteranceCounter.incrementAndGet()}"
        runCatching { engine.speak(text, mode, null, id) }
            .onFailure { BeaconLog.e(TAG, "speak failed", it) }
    }

    override fun stop() {
        pending = null
        runCatching { tts?.stop() }
        _isSpeaking.value = false
    }

    /** Speak a short sample using current settings (for the voice settings screen). */
    fun speakSample(text: String = SAMPLE_UTTERANCE) {
        speak(text, interrupt = true)
    }

    fun currentEnginePackage(): String? = currentSettings.enginePackage

    private companion object {
        const val TAG = "SpeechController"
        const val SAMPLE_UTTERANCE = "This is how Beacon will sound."
    }
}
