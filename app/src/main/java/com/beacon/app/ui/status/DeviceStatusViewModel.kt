package com.beacon.app.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beacon.core.result.OperationResult
import com.beacon.domain.glasses.model.BatteryStatus
import com.beacon.domain.glasses.model.ConnectionState
import com.beacon.domain.glasses.model.DeviceInfo
import com.beacon.domain.glasses.model.isReady
import com.beacon.domain.glasses.usecase.DisconnectGlassesUseCase
import com.beacon.domain.glasses.usecase.FetchDeviceInfoUseCase
import com.beacon.domain.glasses.usecase.ObserveBatteryUseCase
import com.beacon.domain.glasses.usecase.ObserveConnectionStateUseCase
import com.beacon.domain.glasses.usecase.RefreshBatteryUseCase
import com.beacon.domain.glasses.usecase.UnbindGlassesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceStatusUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val battery: BatteryStatus? = null,
    val deviceInfo: DeviceInfo? = null,
    val isBusy: Boolean = false,
)

@HiltViewModel
class DeviceStatusViewModel @Inject constructor(
    observeConnectionState: ObserveConnectionStateUseCase,
    observeBattery: ObserveBatteryUseCase,
    private val refreshBattery: RefreshBatteryUseCase,
    private val fetchDeviceInfo: FetchDeviceInfoUseCase,
    private val disconnectGlasses: DisconnectGlassesUseCase,
    private val unbindGlasses: UnbindGlassesUseCase,
) : ViewModel() {

    private val deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    private val busy = MutableStateFlow(false)

    val uiState: StateFlow<DeviceStatusUiState> =
        combine(
            observeConnectionState(),
            observeBattery(),
            deviceInfo,
            busy,
        ) { connection, battery, info, isBusy ->
            DeviceStatusUiState(
                connectionState = connection,
                battery = battery,
                deviceInfo = info,
                isBusy = isBusy,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeviceStatusUiState())

    init {
        // When the link becomes ready, pull battery and device info once.
        observeConnectionState()
            .map { it.isReady }
            .distinctUntilChanged()
            .onEach { ready -> if (ready) loadDetails() }
            .launchIn(viewModelScope)
    }

    private fun loadDetails() {
        viewModelScope.launch {
            refreshBattery()
            when (val result = fetchDeviceInfo()) {
                is OperationResult.Success -> deviceInfo.value = result.value
                is OperationResult.Failure -> Unit
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            busy.value = true
            refreshBattery()
            when (val result = fetchDeviceInfo()) {
                is OperationResult.Success -> deviceInfo.value = result.value
                is OperationResult.Failure -> Unit
            }
            busy.value = false
        }
    }

    fun disconnect() {
        viewModelScope.launch { disconnectGlasses() }
    }

    fun unbind(onDone: () -> Unit) {
        viewModelScope.launch {
            unbindGlasses()
            deviceInfo.value = null
            onDone()
        }
    }
}
