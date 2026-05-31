package com.beacon.app.ui.emergency

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beacon.app.emergency.DialerIntentLauncher
import com.beacon.app.emergency.EmergencySharePreview
import com.beacon.app.emergency.EmergencyWhatsAppShareManager
import com.beacon.app.emergency.SmsIntentLauncher
import com.beacon.app.emergency.WhatsAppLaunchResult
import com.beacon.core.haptics.Haptics
import com.beacon.core.result.OperationResult
import com.beacon.data.emergency.EmergencyPreferences
import com.beacon.domain.emergency.EmergencyAlertDraft
import com.beacon.domain.emergency.EmergencyRepository
import com.beacon.domain.emergency.TrustedContact
import com.beacon.domain.helper.GetPrimaryEmergencyContactUseCase
import com.beacon.domain.helper.ListWhatsAppEmergencyContactsUseCase
import com.beacon.domain.history.HistoryEntry
import com.beacon.domain.history.HistoryRepository
import com.beacon.domain.history.HistoryType
import com.beacon.domain.speech.Speaker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EmergencyUiState(
    val contactName: String = "",
    val contactPhone: String = "",
    val includeSceneFromGlasses: Boolean = true,
    val autoSendSms: Boolean = true,
    val confirmBeforeSend: Boolean = false,
    val allowWhatsAppEmergencySharing: Boolean = false,
    val includeLocationInAlerts: Boolean = true,
    val includeLatestImageInAlerts: Boolean = true,
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val countdownSeconds: Int? = null,
    val sharePreview: EmergencySharePreview? = null,
)

@HiltViewModel
class EmergencyViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val emergencyRepository: EmergencyRepository,
    private val emergencyPreferences: EmergencyPreferences,
    private val historyRepository: HistoryRepository,
    private val speaker: Speaker,
    private val haptics: Haptics,
    private val whatsAppShareManager: EmergencyWhatsAppShareManager,
    private val listWhatsAppContacts: ListWhatsAppEmergencyContactsUseCase,
    private val getPrimaryEmergencyContact: GetPrimaryEmergencyContactUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmergencyUiState())
    val uiState: StateFlow<EmergencyUiState> = _uiState.asStateFlow()

    private val _openSmsRequests = MutableSharedFlow<EmergencyAlertDraft>(extraBufferCapacity = 1)
    val openSmsRequests: SharedFlow<EmergencyAlertDraft> = _openSmsRequests.asSharedFlow()

    private var countdownJob: Job? = null

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
                        allowWhatsAppEmergencySharing = settings.allowWhatsAppEmergencySharing,
                        includeLocationInAlerts = settings.includeLocationInEmergencyAlerts,
                        includeLatestImageInAlerts = settings.includeLatestImageInEmergencyAlerts,
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

    fun setAllowWhatsAppEmergencySharing(enabled: Boolean) {
        viewModelScope.launch { emergencyPreferences.setAllowWhatsAppEmergencySharing(enabled) }
    }

    fun setIncludeLocationInAlerts(enabled: Boolean) {
        viewModelScope.launch { emergencyPreferences.setIncludeLocationInAlerts(enabled) }
    }

    fun setIncludeLatestImageInAlerts(enabled: Boolean) {
        viewModelScope.launch { emergencyPreferences.setIncludeLatestImageInAlerts(enabled) }
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

    fun callEmergencyContact() {
        val phone = _uiState.value.contactPhone
        if (phone.isBlank()) {
            speaker.speak("Add a trusted contact first.")
            return
        }
        haptics.emergencyPulse()
        if (!DialerIntentLauncher.openDialer(appContext, phone)) {
            speaker.speak("Could not open the dialer.")
        }
    }

    fun prepareWhatsAppSharePreview() {
        viewModelScope.launch {
            val settings = emergencyPreferences.settings.first()
            val contacts = listWhatsAppContacts()
            val preview = whatsAppShareManager.buildPreview(settings, contacts)
            _uiState.update { it.copy(sharePreview = preview) }
        }
    }

    fun startWhatsAppEmergencyCountdown() {
        countdownJob?.cancel()
        if (!_uiState.value.allowWhatsAppEmergencySharing) {
            speaker.speak("Turn on WhatsApp emergency sharing in settings first.")
            return
        }
        countdownJob = viewModelScope.launch {
            speaker.speak(
                "Emergency mode. Sending alert in 3 seconds. Press cancel to stop.",
            )
            haptics.emergencyPulse()
            for (seconds in 3 downTo 1) {
                _uiState.update { it.copy(countdownSeconds = seconds) }
                delay(1_000)
            }
            _uiState.update { it.copy(countdownSeconds = null) }
            shareEmergencyViaWhatsApp()
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _uiState.update { it.copy(countdownSeconds = null) }
        speaker.speak("Emergency cancelled.")
    }

    fun shareEmergencyViaWhatsApp() {
        viewModelScope.launch {
            val settings = emergencyPreferences.settings.first()
            val contact = getPrimaryEmergencyContact()
                ?: listWhatsAppContacts().firstOrNull()
            if (contact == null) {
                speaker.speak("Add a trusted helper first. Dialer and SMS are still available.")
                _uiState.update { it.copy(statusMessage = "No WhatsApp emergency contact.") }
                return@launch
            }
            _uiState.update { it.copy(isBusy = true) }
            when (val result = whatsAppShareManager.shareToWhatsApp(appContext, settings, contact)) {
                WhatsAppLaunchResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            statusMessage = "WhatsApp opened. Review and tap send.",
                        )
                    }
                }
                WhatsAppLaunchResult.WhatsAppNotInstalled -> {
                    speaker.speak(
                        "WhatsApp is not available. I can send SMS, open the dialer, or share using another app.",
                    )
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            statusMessage = "WhatsApp not installed. Try SMS or dialer.",
                        )
                    }
                }
                is WhatsAppLaunchResult.Failed -> {
                    speaker.speak(result.reason)
                    _uiState.update { it.copy(isBusy = false, statusMessage = result.reason) }
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
