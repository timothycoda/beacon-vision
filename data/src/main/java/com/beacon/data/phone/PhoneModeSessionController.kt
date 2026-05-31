package com.beacon.data.phone

import com.beacon.domain.phone.PhoneModeSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneModeSessionController @Inject constructor() {

    private val _state = MutableStateFlow(PhoneModeSessionState.IDLE)
    val state: StateFlow<PhoneModeSessionState> = _state.asStateFlow()

    private val _analysisPaused = MutableStateFlow(false)
    val analysisPaused: StateFlow<Boolean> = _analysisPaused.asStateFlow()

    private var wasActiveBeforeWhatsAppHandoff = false
    private var userStoppedPhoneMode = false

    fun markPhoneModeActive() {
        userStoppedPhoneMode = false
        if (_state.value == PhoneModeSessionState.IDLE ||
            _state.value == PhoneModeSessionState.PAUSED_BY_USER
        ) {
            _state.value = PhoneModeSessionState.ACTIVE
        }
        _analysisPaused.value = false
    }

    fun markPhoneModeStopped() {
        userStoppedPhoneMode = true
        _state.value = PhoneModeSessionState.PAUSED_BY_USER
        _analysisPaused.value = true
        wasActiveBeforeWhatsAppHandoff = false
    }

    fun pauseForWhatsAppHandoff() {
        wasActiveBeforeWhatsAppHandoff =
            _state.value == PhoneModeSessionState.ACTIVE && !userStoppedPhoneMode
        _state.value = PhoneModeSessionState.PAUSED_FOR_WHATSAPP
        _analysisPaused.value = true
    }

    fun pauseForEmergency() {
        _state.value = PhoneModeSessionState.PAUSED_FOR_EMERGENCY
        _analysisPaused.value = true
    }

    fun tryResumeAfterWhatsAppReturn(): Boolean {
        if (!wasActiveBeforeWhatsAppHandoff || userStoppedPhoneMode) {
            wasActiveBeforeWhatsAppHandoff = false
            return false
        }
        wasActiveBeforeWhatsAppHandoff = false
        _state.value = PhoneModeSessionState.ACTIVE
        _analysisPaused.value = false
        return true
    }

    fun isAnalysisPaused(): Boolean = _analysisPaused.value
}
