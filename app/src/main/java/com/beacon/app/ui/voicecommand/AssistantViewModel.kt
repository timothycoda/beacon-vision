package com.beacon.app.ui.voicecommand

import androidx.lifecycle.ViewModel
import com.beacon.domain.glasses.GlassesRepository
import com.beacon.domain.glasses.model.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Exposes glasses connection state to the navigation root so the always-on
 * hands-free [com.beacon.app.assistant.BeaconAssistantService] can be started
 * once the glasses are connected (while the app is in the foreground, which
 * satisfies the OS rules for starting a microphone foreground service).
 */
@HiltViewModel
class AssistantViewModel @Inject constructor(
    glassesRepository: GlassesRepository,
) : ViewModel() {
    val connectionState: StateFlow<ConnectionState> = glassesRepository.connectionState
}
