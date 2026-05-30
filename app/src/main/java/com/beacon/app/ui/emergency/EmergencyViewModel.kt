package com.beacon.app.ui.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beacon.core.haptics.Haptics
import com.beacon.core.result.OperationResult
import com.beacon.data.emergency.EmergencyPreferences
import com.beacon.domain.emergency.EmergencyAlertDraft
import com.beacon.domain.emergency.EmergencyRepository
import com.beacon.domain.emergency.TrustedContact
import com.beacon.domain.history.HistoryEntry
import com.beacon.domain.history.HistoryRepository
import com.beacon.domain.history.HistoryType
import com.beacon.domain.speech.Speaker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EmergencyUiState(
    val contactName: String = "",
    val contactPhone: String = "",
    val includeSceneFromGlasses: Boolean = true,
    val autoSendSms: Boolean = true,
    val confirmBeforeSend: Boolean = false,
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
)

@HiltViewModel
class EmergencyViewModel @Inject constructor(
    private val emergencyRepository: EmergencyRepository,
    private val emergencyPreferences: EmergencyPreferences,
    private val historyRepository: HistoryRepository,
    private val speaker: Speaker,
    private val haptics: Haptics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmergencyUiState())
    val uiState: StateFlow<EmergencyUiState> = _uiState.asStateFlow()

    private val _openSmsRequests = MutableSharedFlow<EmergencyAlertDraft>(extraBufferCapacity = 1)
    val openSmsRequests: SharedFlow<EmergencyAlertDraft> = _openSmsRequests.asSharedFlow()

    init {
        viewModelScope.launch {
            emergencyRepository.trustedContact.collect { saved ->
                if (saved != null) {
                    _uiState.update {
                        it.copy(
                            contactName = saved.name,
                            contactPhone = saved.phoneNumber,
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            emergencyPreferences.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        includeSceneFromGlasses = settings.includeSceneFromGlasses,
                        autoSendSms = settings.autoSendSms,
                        confirmBeforeSend = settings.confirmBeforeSend,
                    )
                }
            }
        }
    }

    fun updateContactName(value: String) {
        _uiState.update { it.copy(contactName = value, statusMessage = null) }
    }

    fun updateContactPhone(value: String) {
        _uiState.update { it.copy(contactPhone = value, statusMessage = null) }
    }

    fun setIncludeScene(enabled: Boolean) {
        viewModelScope.launch { emergencyPreferences.setIncludeScene(enabled) }
    }

    fun setAutoSendSms(enabled: Boolean) {
        viewModelScope.launch { emergencyPreferences.setAutoSendSms(enabled) }
    }

    fun setConfirmBeforeSend(enabled: Boolean) {
        viewModelScope.launch { emergencyPreferences.setConfirmBeforeSend(enabled) }
    }

    fun saveContact() {
        val state = _uiState.value
        if (state.contactPhone.isBlank()) {
            _uiState.update { it.copy(statusMessage = "Enter a phone number for your trusted contact.") }
            return
        }
        viewModelScope.launch {
            emergencyRepository.saveTrustedContact(
                TrustedContact(
                    name = state.contactName.trim(),
                    phoneNumber = state.contactPhone.trim(),
                ),
            )
            speaker.speak("Trusted contact saved.")
            _uiState.update { it.copy(statusMessage = "Trusted contact saved.") }
        }
    }

    fun requestHelp() {
        val state = _uiState.value
        if (state.contactPhone.isBlank()) {
            _uiState.update { it.copy(statusMessage = "Enter a phone number for your trusted contact.") }
            return
        }
        viewModelScope.launch {
            emergencyRepository.saveTrustedContact(
                TrustedContact(
                    name = state.contactName.trim(),
                    phoneNumber = state.contactPhone.trim(),
                ),
            )
            onSendEmergencyReady()
        }
    }

    fun onSendEmergencyReady() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.isBusy) return@launch
            _uiState.update { it.copy(isBusy = true, statusMessage = "Preparing your emergency message…") }
            speaker.speak("Emergency. Preparing your message. One moment.")
            haptics.error()

            when (val result = emergencyRepository.prepareAlert(state.includeSceneFromGlasses)) {
                is OperationResult.Failure -> {
                    speaker.speak(result.message)
                    _uiState.update {
                        it.copy(isBusy = false, statusMessage = result.message)
                    }
                }
                is OperationResult.Success -> {
                    historyRepository.add(
                        HistoryEntry(
                            type = HistoryType.EMERGENCY,
                            summary = "Emergency message prepared for ${state.contactName.ifBlank { "trusted contact" }}.",
                        ),
                    )
                    _openSmsRequests.tryEmit(result.value)
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            statusMessage = "Opening your messaging app. Review and tap send.",
                        )
                    }
                }
            }
        }
    }

    fun onSmsLaunchResult(success: Boolean, failureReason: String?) {
        if (success) {
            speaker.speak("Your message is ready. Review and tap send.")
        } else {
            val msg = failureReason ?: "Could not open your messaging app."
            speaker.speak(msg)
            _uiState.update { it.copy(statusMessage = msg) }
        }
    }
}
