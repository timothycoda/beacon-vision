package com.beacon.data.speech

import android.content.Context
import android.provider.Settings
import android.speech.tts.TextToSpeech
import com.beacon.domain.speech.SpeechSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Captures whatever voice the phone is using right now for TTS. */
object TtsSnapshot {

    suspend fun captureCurrent(context: Context): SpeechSettings =
        suspendCancellableCoroutine { cont ->
            val enginePackage = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.TTS_DEFAULT_SYNTH,
            )
            var tts: TextToSpeech? = null
            tts = TextToSpeech(context.applicationContext, { status ->
                val settings = if (status == TextToSpeech.SUCCESS) {
                    SpeechSettings(
                        enginePackage = enginePackage,
                        voiceName = tts?.voice?.name,
                        speechRate = SpeechSettings.DEFAULT_SPEECH_RATE,
                    )
                } else {
                    SpeechSettings()
                }
                runCatching { tts?.shutdown() }
                if (cont.isActive) cont.resume(settings)
            })
            cont.invokeOnCancellation { runCatching { tts?.shutdown() } }
        }
}
