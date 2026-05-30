package com.beacon.data.speech

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.beacon.domain.speech.TtsEngineOption
import com.beacon.domain.speech.TtsVoiceOption

/** Lists installed TTS engines and their voices for the settings screen. */
object TtsCatalog {

    fun listEngines(context: Context): List<TtsEngineOption> {
        val pm = context.packageManager
        val probe = TextToSpeech(context, {})
        val engines = runCatching { probe.engines }.getOrNull().orEmpty()
        probe.shutdown()
        return engines.map { engine ->
            val label = runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(engine.name, 0)).toString()
            }.getOrElse { engine.label ?: engine.name }
            TtsEngineOption(packageName = engine.name, label = label)
        }.sortedBy { it.label.lowercase() }
    }

    fun listVoices(context: Context, enginePackage: String?): List<TtsVoiceOption> {
        val probe = TextToSpeech(context, {}, enginePackage)
        val voices = runCatching { probe.voices }.getOrNull().orEmpty()
        probe.shutdown()
        return voices
            .filter { it.name.isNotBlank() }
            .map { it.toOption() }
            .distinctBy { it.name }
            .sortedBy { it.label.lowercase() }
    }

    private fun Voice.toOption(): TtsVoiceOption {
        val localeLabel = locale?.displayName ?: locale?.toLanguageTag()
        val quality = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                when (quality) {
                    Voice.QUALITY_VERY_HIGH -> "very high"
                    Voice.QUALITY_HIGH -> "high"
                    Voice.QUALITY_NORMAL -> "normal"
                    Voice.QUALITY_LOW -> "low"
                    else -> null
                }
            }
            else -> null
        }
        val parts = listOfNotNull(name, localeLabel, quality)
        return TtsVoiceOption(
            name = name,
            label = parts.joinToString(" · "),
            localeTag = locale?.toLanguageTag(),
        )
    }
}
