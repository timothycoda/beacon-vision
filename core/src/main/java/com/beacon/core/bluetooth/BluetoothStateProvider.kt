package com.beacon.core.bluetooth

import kotlinx.coroutines.flow.Flow

/** Testable abstraction over the system Bluetooth adapter state. */
interface BluetoothStateProvider {
    fun isEnabled(): Boolean
    fun enabledState(): Flow<Boolean>
}
