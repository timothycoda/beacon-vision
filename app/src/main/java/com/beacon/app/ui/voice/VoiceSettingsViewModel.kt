package com.beacon.app.ui.voice

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beacon.data.speech.SpeechController
import com.beacon.data.speech.TtsCatalog
import com.beacon.data.speech.TtsSnapshot
import com.beacon.domain.speech.SpeechSettings
import com.beacon.domain.speech.SpeechSettingsRepository
import com.beacon.domain.speech.TtsEngineOption
import com.beacon.domain.speech.TtsVoiceOption
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VoiceSettingsUiState(
    val engines: List<TtsEngineOption> = emptyList(),
    val voices: List<TtsVoiceOption> = emptyList(),
    val selectedEngine: String? = null,
    val selectedVoice: String? = null,
    val speechRate: Float = SpeechSettings.DEFAULT_SPEECH_RATE,
    val statusMessage: String? = null,
) {
    val selectedEngineLabel: String
        get() = engines.firstOrNull { it.packageName == selectedEngine }?.label ?: "System default"

    val selectedVoiceLabel: String
        get() = voices.firstOrNull { it.name == selectedVoice }?.label ?: "Engine default"
}

@HiltViewModel
class VoiceSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SpeechSettingsRepository,
    private val speechController: SpeechController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceSettingsUiState())
    val uiState: StateFlow<VoiceSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { saved ->
                val engines = TtsCatalog.listEngines(context)
                val engine = saved.enginePackage
                val voices = TtsCatalog.listVoices(context, engine)
                _uiState.update {
                    it.copy(
                        engines = engines,
                        voices = voices,
                        selectedEngine = engine,
                        selectedVoice = saved.voiceName,
                        speechRate = saved.speechRate,
                    )
                }
            }
        }
    }

    fun selectEngine(packageName: String?) {
        viewModelScope.launch {
            val voices = TtsCatalog.listVoices(context, packageName)
            _uiState.update {
                it.copy(
                    selectedEngine = packageName,
                    selectedVoice = null,
                    voices = voices,
                    statusMessage = null,
                )
            }
            persist()
        }
    }

    fun selectVoice(voiceName: String?) {
        _uiState.update { it.copy(selectedVoice = voiceName, statusMessage = null) }
        viewModelScope.launch { persist() }
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(SpeechSettings.MIN_SPEECH_RATE, SpeechSettings.MAX_SPEECH_RATE)
        _uiState.update { it.copy(speechRate = clamped) }
        viewModelScope.launch { persist() }
    }

    fun resetToSystemDefault() {
        viewModelScope.launch {
            settingsRepository.resetToSystemDefault()
            _uiState.update { it.copy(statusMessage = "Using system default voice.") }
        }
    }

    fun testVoice() {
        speechController.speakSample()
        _uiState.update { it.copy(statusMessage = "Playing sample…") }
    }

    /** Saves the phone's current default TTS engine + voice (what you hear outside Beacon). */
    fun savePhonesCurrentVoice() {
        viewModelScope.launch {
            val snapshot = TtsSnapshot.captureCurrent(context)
            settingsRepository.save(snapshot)
            _uiState.update {
                it.copy(statusMessage = "Saved your phone's current voice for Beacon.")
            }
        }
    }

    private suspend fun persist() {
        val state = _uiState.value
        settingsRepository.save(
            SpeechSettings(
                enginePackage = state.selectedEngine,
                voiceName = state.selectedVoice,
                speechRate = state.speechRate,
            ),
        )
    }
}
