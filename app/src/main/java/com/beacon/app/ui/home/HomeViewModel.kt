package com.beacon.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beacon.domain.glasses.model.BatteryStatus
import com.beacon.domain.glasses.model.ConnectionState
import com.beacon.domain.glasses.usecase.ObserveBatteryUseCase
import com.beacon.domain.glasses.usecase.ObserveConnectionStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val battery: BatteryStatus? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeConnectionState: ObserveConnectionStateUseCase,
    observeBattery: ObserveBatteryUseCase,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(observeConnectionState(), observeBattery()) { connection, battery ->
            HomeUiState(connectionState = connection, battery = battery)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
