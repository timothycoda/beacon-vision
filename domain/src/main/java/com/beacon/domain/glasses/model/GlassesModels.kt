package com.beacon.domain.glasses.model

/** A discoverable pair of X01 / HeyCyan glasses. */
data class GlassesDevice(
    val name: String,
    val address: String,
    val rssi: Int,
)

/** Current connection state of the glasses link. */
sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object BluetoothOff : ConnectionState
    data object Scanning : ConnectionState
    data class Connecting(val deviceName: String?) : ConnectionState

    /**
     * Linked to the glasses. [ready] becomes true only after the vendor
     * `onServiceDiscovered` callback, when commands may be sent.
     */
    data class Connected(val ready: Boolean, val deviceName: String?) : ConnectionState
    data class Failed(val reason: String) : ConnectionState
}

val ConnectionState.isConnected: Boolean
    get() = this is ConnectionState.Connected

val ConnectionState.isReady: Boolean
    get() = this is ConnectionState.Connected && ready

/** Battery level reported by the glasses. */
data class BatteryStatus(
    val levelPercent: Int,
    val isCharging: Boolean,
)

/** Firmware / hardware versions reported by the glasses. */
data class DeviceInfo(
    val firmwareVersion: String,
    val hardwareVersion: String,
    val wifiFirmwareVersion: String,
    val wifiHardwareVersion: String,
)
