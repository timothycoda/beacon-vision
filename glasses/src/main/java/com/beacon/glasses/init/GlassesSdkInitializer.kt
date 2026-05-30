package com.beacon.glasses.init

import android.content.Context
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.beacon.core.log.BeaconLog
import com.beacon.glasses.BuildConfig
import com.beacon.glasses.internal.BeaconGlassesReceiver
import com.beacon.glasses.internal.GlassesStateHolder
import com.oudmon.ble.base.bluetooth.BleAction
import com.oudmon.ble.base.bluetooth.BleBaseControl
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.communication.LargeDataHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-time initialisation of the HeyCyan/X01 vendor BLE stack and registration
 * of the lifecycle bridge receiver. Mirrors the required start-up sequence from
 * the SDK sample's `MyApplication` (verified against the AAR). Runs once, early
 * in Application.onCreate, before any scan/connect/command call.
 */
@Singleton
class GlassesSdkInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stateHolder: GlassesStateHolder,
) {

    @Volatile
    private var initialized = false

    fun initialize() {
        // On the emulator / tests the fake backend is used; skip vendor init,
        // which requires real BLE hardware.
        if (BuildConfig.USE_FAKE_GLASSES) {
            BeaconLog.i(TAG, "USE_FAKE_GLASSES=true; skipping HeyCyan SDK init")
            return
        }
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val app = context.applicationContext as? android.app.Application
            if (app == null) {
                BeaconLog.e(TAG, "Application context unavailable; cannot init SDK")
                return
            }
            runCatching {
                LargeDataHandler.getInstance()
                BleOperateManager.getInstance(app).apply {
                    setApplication(app)
                    init()
                }
                BleBaseControl.getInstance(app).setmContext(app)

                // Register the lifecycle bridge that turns vendor callbacks into
                // GlassesStateHolder updates.
                LocalBroadcastManager.getInstance(app)
                    .registerReceiver(BeaconGlassesReceiver(stateHolder), BleAction.getIntentFilter())

                initialized = true
                BeaconLog.i(TAG, "HeyCyan SDK initialised")
            }.onFailure {
                BeaconLog.e(TAG, "Failed to initialise HeyCyan SDK", it)
            }
        }
    }

    private companion object {
        const val TAG = "GlassesSdkInitializer"
    }
}
