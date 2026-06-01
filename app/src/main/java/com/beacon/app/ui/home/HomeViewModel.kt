package com.beacon.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beacon.domain.glasses.model.BatteryStatus
import com.beacon.domain.glasses.model.ConnectionState
import com.beacon.domain.glasses.usecase.ObserveBatteryUseCase
import com.beacon.domain.glasses.usecase.ObserveConnectionStateUseCase
import com.beacon.domain.guidance.GuidanceLanguage
import com.beacon.domain.device.GuidanceInputMode
import com.beacon.domain.device.ObserveGuidanceInputModeUseCase
import com.beacon.domain.modelpack.ObserveActiveModelPacksSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val battery: BatteryStatus? = null,
    val activeIntelligenceName: String = "Built-in vision",
    val activeVoiceName: String? = null,
    val guidanceLanguage: GuidanceLanguage = GuidanceLanguage.English,
    val phoneOnlyMode: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeConnectionState: ObserveConnectionStateUseCase,
    observeBattery: ObserveBatteryUseCase,
    observeActiveModelPacks: ObserveActiveModelPacksSummaryUseCase,
    observeGuidanceMode: ObserveGuidanceInputModeUseCase,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(
            observeConnectionState(),
            observeBattery(),
            observeActiveModelPacks(),
            observeGuidanceMode(),
        ) { connection, battery, packs, mode ->
            HomeUiState(
                connectionState = connection,
                battery = battery,
                activeIntelligenceName = packs.intelligenceName,
                activeVoiceName = packs.voiceName,
                guidanceLanguage = packs.guidanceLanguage,
                phoneOnlyMode = mode == GuidanceInputMode.PhoneCamera,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun enterPhoneMode(onNavigate: () -> Unit) {
        onNavigate()
    }
}
