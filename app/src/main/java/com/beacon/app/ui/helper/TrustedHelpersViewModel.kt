package com.beacon.app.ui.helper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beacon.domain.helper.AddTrustedHelperUseCase
import com.beacon.domain.helper.DeleteTrustedHelperUseCase
import com.beacon.domain.helper.ObserveTrustedHelpersUseCase
import com.beacon.domain.helper.TrustedHelper
import com.beacon.domain.helper.UpdateTrustedHelperUseCase
import com.beacon.domain.speech.Speaker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrustedHelpersUiState(
    val helpers: List<TrustedHelper> = emptyList(),
    val editing: TrustedHelper? = null,
    val displayName: String = "",
    val phoneNumber: String = "",
    val whatsappEnabled: Boolean = true,
    val receiveEmergencyWhatsApp: Boolean = true,
    val isPrimaryHelper: Boolean = false,
    val isPrimaryEmergency: Boolean = false,
    val relationship: String = "",
    val statusMessage: String? = null,
)

@HiltViewModel
class TrustedHelpersViewModel @Inject constructor(
    observeHelpers: ObserveTrustedHelpersUseCase,
    private val addHelper: AddTrustedHelperUseCase,
    private val updateHelper: UpdateTrustedHelperUseCase,
    private val deleteHelper: DeleteTrustedHelperUseCase,
    private val speaker: Speaker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrustedHelpersUiState())
    val uiState: StateFlow<TrustedHelpersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeHelpers().collect { list ->
                _uiState.update { it.copy(helpers = list) }
            }
        }
    }

    fun startNew() {
        _uiState.update {
            it.copy(
                editing = TrustedHelper(displayName = "", phoneNumber = ""),
                displayName = "",
                phoneNumber = "",
                whatsappEnabled = true,
                receiveEmergencyWhatsApp = true,
                isPrimaryHelper = it.helpers.isEmpty(),
                isPrimaryEmergency = it.helpers.isEmpty(),
                relationship = "",
                statusMessage = null,
            )
        }
    }

    fun startEdit(helper: TrustedHelper) {
        _uiState.update {
            it.copy(
                editing = helper,
                displayName = helper.displayName,
                phoneNumber = helper.phoneNumber,
                whatsappEnabled = helper.whatsappEnabled,
                receiveEmergencyWhatsApp = helper.receiveEmergencyWhatsAppAlerts,
                isPrimaryHelper = helper.isPrimaryHelper,
                isPrimaryEmergency = helper.isPrimaryEmergencyContact,
                relationship = helper.relationship.orEmpty(),
                statusMessage = null,
            )
        }
    }

    fun updateDisplayName(v: String) = _uiState.update { it.copy(displayName = v) }
    fun updatePhone(v: String) = _uiState.update { it.copy(phoneNumber = v) }
    fun setWhatsappEnabled(v: Boolean) = _uiState.update { it.copy(whatsappEnabled = v) }
    fun setReceiveEmergency(v: Boolean) = _uiState.update { it.copy(receiveEmergencyWhatsApp = v) }
    fun setPrimaryHelper(v: Boolean) = _uiState.update { it.copy(isPrimaryHelper = v) }
    fun setPrimaryEmergency(v: Boolean) = _uiState.update { it.copy(isPrimaryEmergency = v) }
    fun updateRelationship(v: String) = _uiState.update { it.copy(relationship = v) }

    fun save() {
        val state = _uiState.value
        if (state.phoneNumber.isBlank()) {
            _uiState.update { it.copy(statusMessage = "Enter a phone number.") }
            return
        }
        val name = state.displayName.trim().ifBlank { "Trusted helper" }
        viewModelScope.launch {
            val helper = state.editing?.copy(
                displayName = name,
                phoneNumber = state.phoneNumber.trim(),
                whatsappEnabled = state.whatsappEnabled,
                receiveEmergencyWhatsAppAlerts = state.receiveEmergencyWhatsApp,
                isPrimaryHelper = state.isPrimaryHelper,
                isPrimaryEmergencyContact = state.isPrimaryEmergency,
                relationship = state.relationship.trim().ifBlank { null },
                updatedAt = System.currentTimeMillis(),
            ) ?: TrustedHelper(
                displayName = name,
                phoneNumber = state.phoneNumber.trim(),
                whatsappEnabled = state.whatsappEnabled,
                receiveEmergencyWhatsAppAlerts = state.receiveEmergencyWhatsApp,
                isPrimaryHelper = state.isPrimaryHelper,
                isPrimaryEmergencyContact = state.isPrimaryEmergency,
                relationship = state.relationship.trim().ifBlank { null },
            )
            if (state.editing == null) {
                addHelper(helper)
            } else {
                updateHelper(helper)
            }
            speaker.speak("Trusted helper saved.")
            _uiState.update {
                it.copy(editing = null, statusMessage = "Saved.")
            }
        }
    }

    fun delete(helper: TrustedHelper) {
        viewModelScope.launch {
            deleteHelper(helper.id)
            speaker.speak("Helper removed.")
            _uiState.update { it.copy(editing = null, statusMessage = "Removed.") }
        }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(editing = null, statusMessage = null) }
    }
}
