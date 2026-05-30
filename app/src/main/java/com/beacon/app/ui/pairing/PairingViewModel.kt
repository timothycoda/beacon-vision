package com.beacon.app.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beacon.core.bluetooth.BluetoothStateProvider
import com.beacon.core.result.OperationResult
import com.beacon.data.prefs.AppPreferences
import com.beacon.domain.glasses.model.ConnectionState
import com.beacon.domain.glasses.model.GlassesDevice
import com.beacon.domain.glasses.usecase.ConnectGlassesUseCase
import com.beacon.domain.glasses.usecase.ObserveConnectionStateUseCase
import com.beacon.domain.glasses.usecase.ScanForGlassesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PairingUiState(
    val isBluetoothOn: Boolean = true,
    val isScanning: Boolean = false,
    val isReconnecting: Boolean = false,
    val lastPaired: GlassesDevice? = null,
    val devices: List<GlassesDevice> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val errorMessage: String? = null,
)

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val scanForGlasses: ScanForGlassesUseCase,
    private val connectGlasses: ConnectGlassesUseCase,
    observeConnectionState: ObserveConnectionStateUseCase,
    private val bluetoothStateMonitor: BluetoothStateProvider,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    init {
        viewModelScope.launch {
            observeConnectionState().collect { state ->
                if (state is ConnectionState.Connected && state.ready) {
                    appPreferences.setOnboardingComplete()
                    isReconnecting.value = false
                }
            }
        }
    }

    /**
     * Called by the screen once the Bluetooth permissions are confirmed granted.
     * A BLE scan started without BLUETOOTH_SCAN silently returns zero results
     * (no callback, no error), which looks like "no devices found".
     */
    fun onPermissionsReady() {
        if (initialActionDone) return
        initialActionDone = true
        startScan()
    }

    private val isScanning = MutableStateFlow(false)
    private val isReconnecting = MutableStateFlow(false)
    private val lastPaired = MutableStateFlow<GlassesDevice?>(null)
    private val devices = MutableStateFlow<List<GlassesDevice>>(emptyList())
    private val errorMessage = MutableStateFlow<String?>(null)
    private var initialActionDone = false

    private val bluetoothOn: StateFlow<Boolean> =
        bluetoothStateMonitor.enabledState()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), bluetoothStateMonitor.isEnabled())

    val uiState: StateFlow<PairingUiState> =
        combine(
            combine(bluetoothOn, isScanning, isReconnecting, lastPaired, devices) {
                    btOn, scanning, reconnecting, paired, deviceList ->
                Quint(btOn, scanning, reconnecting, paired, deviceList)
            },
            errorMessage,
            observeConnectionState(),
        ) { quint, error, connection ->
            PairingUiState(
                isBluetoothOn = quint.btOn,
                isScanning = quint.scanning,
                isReconnecting = quint.reconnecting,
                lastPaired = quint.paired,
                devices = quint.deviceList,
                connectionState = connection,
                errorMessage = error,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PairingUiState())

    private var scanJob: Job? = null

    fun startScan() {
        if (!bluetoothStateMonitor.isEnabled()) {
            errorMessage.value = "Please turn on Bluetooth to find your glasses."
            return
        }
        if (isScanning.value) return
        errorMessage.value = null
        devices.value = emptyList()
        isScanning.value = true

        scanJob = scanForGlasses()
            .onEach { devices.value = it }
            .onCompletion { isScanning.value = false }
            .launchIn(viewModelScope)
    }

    fun stopScan() {
        scanForGlasses.stop()
        scanJob?.cancel()
        scanJob = null
        isScanning.value = false
    }

    fun connect(device: GlassesDevice) {
        stopScan()
        errorMessage.value = null
        viewModelScope.launch {
            when (val result = connectGlasses(device)) {
                is OperationResult.Failure -> errorMessage.value = result.message
                is OperationResult.Success -> Unit
            }
        }
    }

    fun clearError() {
        errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }

    private data class Quint(
        val btOn: Boolean,
        val scanning: Boolean,
        val reconnecting: Boolean,
        val paired: GlassesDevice?,
        val deviceList: List<GlassesDevice>,
    )
}
