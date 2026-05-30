package com.beacon.data.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.beacon.core.log.BeaconLog
import com.beacon.domain.voice.SpeechToText
import com.beacon.domain.voice.VoiceError
import com.beacon.domain.voice.VoiceListeningState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hands-free speech-to-text on the platform [SpeechRecognizer].
 *
 * Devices differ widely in which languages and offline models they ship, so a
 * single configuration is unreliable (e.g. a phone set to en-NG has no offline
 * model and returns ERROR_LANGUAGE_NOT_SUPPORTED). We therefore cascade through
 * a short list of [Attempt]s — preferring the on-device, offline engine first
 * and progressively relaxing to a widely-supported language and online use —
 * advancing to the next attempt whenever one fails for a recoverable reason.
 *
 * The Android recogniser must be created and driven from the main thread, so
 * all engine calls are posted to the main [Looper]; recognition callbacks
 * already arrive on the main thread and are forwarded into the flow.
 */
@Singleton
class AndroidSpeechToText @Inject constructor(
    @ApplicationContext private val context: Context,
) : SpeechToText {

    private data class Attempt(
        val onDevice: Boolean,
        val language: String?,
        val preferOffline: Boolean,
    )

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun isAvailable(): Boolean =
        runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)

    override fun listenOnce(): Flow<VoiceListeningState> = callbackFlow {
        if (!isAvailable()) {
            trySend(VoiceListeningState.Error(VoiceError.UNAVAILABLE))
            close()
            return@callbackFlow
        }

        val attempts = buildAttempts()
        var index = 0
        var recognizer: SpeechRecognizer? = null

        fun destroyRecognizer() {
            runCatching { recognizer?.stopListening() }
            runCatching { recognizer?.destroy() }
            recognizer = null
        }

        lateinit var startAttempt: (Int) -> Unit

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(VoiceListeningState.Listening)
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onPartialResults(partialResults: Bundle?) {
                firstResult(partialResults)?.let { trySend(VoiceListeningState.Partial(it)) }
            }

            override fun onResults(results: Bundle?) {
                val text = firstResult(results)
                if (text.isNullOrBlank()) {
                    trySend(VoiceListeningState.Error(VoiceError.NO_MATCH))
                } else {
                    trySend(VoiceListeningState.Result(text))
                }
                close()
            }

            override fun onError(error: Int) {
                BeaconLog.w(TAG, "Recogniser error code=$error attempt=$index of ${attempts.size}")
                if (isRecoverable(error) && index < attempts.lastIndex) {
                    index += 1
                    destroyRecognizer()
                    startAttempt(index)
                    return
                }
                trySend(VoiceListeningState.Error(mapError(error)))
                close()
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        startAttempt = { i ->
            val attempt = attempts[i]
            mainHandler.post {
                val engine = createRecognizer(attempt.onDevice)
                recognizer = engine
                engine.setRecognitionListener(listener)
                runCatching { engine.startListening(buildIntent(attempt)) }
                    .onFailure {
                        BeaconLog.e(TAG, "startListening failed", it)
                        if (index < attempts.lastIndex) {
                            index += 1
                            destroyRecognizer()
                            startAttempt(index)
                        } else {
                            trySend(VoiceListeningState.Error(VoiceError.UNKNOWN))
                            close()
                        }
                    }
            }
        }

        trySend(VoiceListeningState.Preparing)
        startAttempt(0)

        awaitClose {
            mainHandler.post { destroyRecognizer() }
        }
    }

    private fun buildAttempts(): List<Attempt> = buildList {
        val deviceTag = Locale.getDefault().toLanguageTag()
        val enUs = Locale.US.toLanguageTag()
        if (onDeviceAvailable()) {
            add(Attempt(onDevice = true, language = deviceTag, preferOffline = true))
            add(Attempt(onDevice = true, language = enUs, preferOffline = true))
        }
        add(Attempt(onDevice = false, language = deviceTag, preferOffline = true))
        add(Attempt(onDevice = false, language = enUs, preferOffline = true))
        add(Attempt(onDevice = false, language = enUs, preferOffline = false))
        add(Attempt(onDevice = false, language = null, preferOffline = false))
    }

    private fun onDeviceAvailable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }.getOrDefault(false)

    private fun createRecognizer(onDevice: Boolean): SpeechRecognizer =
        if (onDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BeaconLog.i(TAG, "Using on-device speech recogniser")
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            BeaconLog.i(TAG, "Using default speech recogniser")
            SpeechRecognizer.createSpeechRecognizer(context)
        }

    private fun buildIntent(attempt: Attempt): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            attempt.language?.let {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, it)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, it)
            }
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, attempt.preferOffline)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

    private fun firstResult(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()

    /** Errors worth retrying with the next attempt (not user/no-speech errors). */
    private fun isRecoverable(code: Int): Boolean = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
        -> false
        else -> true
    }

    private fun mapError(code: Int): VoiceError = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH -> VoiceError.NO_MATCH
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceError.NO_SPEECH
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceError.PERMISSION_DENIED
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> VoiceError.BUSY
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> VoiceError.NETWORK
        else -> VoiceError.UNKNOWN
    }

    private companion object {
        const val TAG = "AndroidSpeechToText"
    }
}
