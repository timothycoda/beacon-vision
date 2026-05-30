package com.beacon.data.speech

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.beacon.domain.speech.SpeechSettings
import com.beacon.domain.speech.SpeechSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.speechDataStore by preferencesDataStore(name = "speech_prefs")

@Singleton
class SpeechPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) : SpeechSettingsRepository {

    private val store = context.speechDataStore

    override val settings: Flow<SpeechSettings> = store.data.map { prefs ->
        SpeechSettings(
            enginePackage = prefs[KEY_ENGINE],
            voiceName = prefs[KEY_VOICE],
            speechRate = prefs[KEY_RATE] ?: SpeechSettings.DEFAULT_SPEECH_RATE,
        )
    }

    override suspend fun save(settings: SpeechSettings) {
        val engine = settings.enginePackage
        val voice = settings.voiceName
        store.edit {
            if (engine != null) {
                it[KEY_ENGINE] = engine
            } else {
                it.remove(KEY_ENGINE)
            }
            if (voice != null) {
                it[KEY_VOICE] = voice
            } else {
                it.remove(KEY_VOICE)
            }
            it[KEY_RATE] = settings.speechRate.coerceIn(
                SpeechSettings.MIN_SPEECH_RATE,
                SpeechSettings.MAX_SPEECH_RATE,
            )
        }
    }

    override suspend fun resetToSystemDefault() {
        store.edit {
            it.remove(KEY_ENGINE)
            it.remove(KEY_VOICE)
            it.remove(KEY_RATE)
        }
    }

    private companion object {
        val KEY_ENGINE = stringPreferencesKey("tts_engine_package")
        val KEY_VOICE = stringPreferencesKey("tts_voice_name")
        val KEY_RATE = floatPreferencesKey("tts_speech_rate")
    }
}
