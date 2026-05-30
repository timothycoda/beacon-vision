package com.beacon.glasses.internal

import com.beacon.domain.glasses.model.BatteryStatus
import com.beacon.domain.glasses.model.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for live glasses state, shared between the vendor
 * broadcast receiver ([com.beacon.glasses.internal.BeaconGlassesReceiver]) and
 * [com.beacon.glasses.HeyCyanGlassesManager]. The receiver pushes vendor
 * callbacks here; the manager exposes them as flows.
 */
@Singleton
class GlassesStateHolder @Inject constructor() {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _battery = MutableStateFlow<BatteryStatus?>(null)
    val battery: StateFlow<BatteryStatus?> = _battery.asStateFlow()

    @Volatile
    private var deviceName: String? = null

    fun onScanning() {
        _connectionState.value = ConnectionState.Scanning
    }

    fun onConnecting(name: String?) {
        deviceName = name
        _connectionState.value = ConnectionState.Connecting(name)
    }

    /** Link established (vendor connectStatue=true), but not yet ready for commands. */
    fun onLinkConnected(name: String?) {
        if (name != null) deviceName = name
        _connectionState.value = ConnectionState.Connected(ready = false, deviceName = deviceName)
    }

    /** Services discovered: the glasses are ready to receive commands. */
    fun onReady() {
        _connectionState.value = ConnectionState.Connected(ready = true, deviceName = deviceName)
    }

    fun onDisconnected() {
        deviceName = null
        _battery.value = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun onBluetoothOff() {
        _battery.value = null
        _connectionState.value = ConnectionState.BluetoothOff
    }

    fun onFailed(reason: String) {
        _connectionState.value = ConnectionState.Failed(reason)
    }

    fun updateBattery(levelPercent: Int, isCharging: Boolean) {
        _battery.value = BatteryStatus(levelPercent, isCharging)
    }
}
