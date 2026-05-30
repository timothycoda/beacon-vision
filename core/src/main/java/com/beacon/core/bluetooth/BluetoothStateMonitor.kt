package com.beacon.core.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Observes the system Bluetooth adapter on/off state. Kept in :core so both the
 * glasses layer and UI can react to Bluetooth being disabled.
 */
class BluetoothStateMonitor(private val context: Context) : BluetoothStateProvider {

    private val adapter: BluetoothAdapter?
        get() = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    override fun isEnabled(): Boolean = adapter?.isEnabled == true

    override fun enabledState(): Flow<Boolean> = callbackFlow {
        trySend(isEnabled())
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    trySend(state == BluetoothAdapter.STATE_ON)
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        awaitClose { runCatching { context.unregisterReceiver(receiver) } }
    }
}
