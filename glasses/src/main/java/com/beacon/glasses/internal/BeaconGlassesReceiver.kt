package com.beacon.glasses.internal

import android.bluetooth.BluetoothDevice
import com.beacon.core.log.BeaconLog
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.bluetooth.QCBluetoothCallbackCloneReceiver
import com.oudmon.ble.base.communication.LargeDataHandler

/**
 * Bridges the vendor BLE lifecycle callbacks (delivered via LocalBroadcastManager)
 * into [GlassesStateHolder]. Registered once by [com.beacon.glasses.init.GlassesSdkInitializer].
 */
class BeaconGlassesReceiver(
    private val state: GlassesStateHolder,
) : QCBluetoothCallbackCloneReceiver() {

    override fun connectStatue(device: BluetoothDevice?, connected: Boolean) {
        BeaconLog.d(TAG, "connectStatue connected=$connected name=${runCatching { device?.name }.getOrNull()}")
        if (connected) {
            state.onLinkConnected(runCatching { device?.name }.getOrNull())
        } else {
            state.onDisconnected()
        }
    }

    override fun onServiceDiscovered() {
        // Critical: enable the command channel and mark the link ready before any
        // command (battery/device info/photo) is sent.
        runCatching {
            LargeDataHandler.getInstance().initEnable()
            BleOperateManager.getInstance().isReady = true
        }.onFailure { BeaconLog.e(TAG, "onServiceDiscovered init failed", it) }
        BeaconLog.i(TAG, "onServiceDiscovered -> ready")
        state.onReady()
    }

    private companion object {
        const val TAG = "BeaconGlassesReceiver"
    }
}
