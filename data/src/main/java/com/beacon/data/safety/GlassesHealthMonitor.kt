package com.beacon.data.safety

import com.beacon.core.bluetooth.BluetoothStateProvider
import com.beacon.core.concurrency.DispatcherProvider
import com.beacon.core.log.BeaconLog
import com.beacon.domain.glasses.GlassesRepository
import com.beacon.domain.glasses.model.ConnectionState
import com.beacon.domain.glasses.usecase.RefreshBatteryUseCase
import com.beacon.domain.speech.Speaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Background monitor for glasses connection, Bluetooth, and low battery. Speaks
 * short alerts so the user knows when hands-free features may not work.
 */
@Singleton
class GlassesHealthMonitor @Inject constructor(
    private val glassesRepository: GlassesRepository,
    private val refreshBattery: RefreshBatteryUseCase,
    private val bluetoothState: BluetoothStateProvider,
    private val speaker: Speaker,
    dispatchers: DispatcherProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private var wasReady = false
    private var lowBatteryAnnounced = false

    init {
        scope.launch {
            glassesRepository.connectionState.collect { state -> onConnectionChanged(state) }
        }
        scope.launch {
            glassesRepository.battery.collect { battery -> onBatteryChanged(battery) }
        }
        scope.launch {
            bluetoothState.enabledState().collect { enabled ->
                if (!enabled) {
                    speaker.speak(
                        "Bluetooth is off. Turn on Bluetooth to use your glasses.",
                        interrupt = true,
                    )
                }
            }
        }
        scope.launch {
            while (isActive) {
                delay(BATTERY_POLL_MS)
                if (glassesRepository.isConnected()) {
                    runCatching { refreshBattery() }
                        .onFailure { BeaconLog.w(TAG, "battery refresh failed") }
                }
            }
        }
        BeaconLog.i(TAG, "Glasses health monitor started")
    }

    private fun onConnectionChanged(state: ConnectionState) {
        val ready = state is ConnectionState.Connected && state.ready
        if (wasReady && !ready) {
            when (state) {
                ConnectionState.Disconnected ->
                    speaker.speak("Glasses disconnected.", interrupt = true)
                is ConnectionState.Failed ->
                    speaker.speak("Glasses connection failed.", interrupt = true)
                else -> Unit
            }
            lowBatteryAnnounced = false
        }
        wasReady = ready
    }

    private fun onBatteryChanged(battery: com.beacon.domain.glasses.model.BatteryStatus?) {
        if (battery == null) {
            lowBatteryAnnounced = false
            return
        }
        if (battery.isCharging) {
            lowBatteryAnnounced = false
            return
        }
        when {
            battery.levelPercent <= LOW_BATTERY_PERCENT && !lowBatteryAnnounced -> {
                lowBatteryAnnounced = true
                speaker.speak("Glasses battery is low, ${battery.levelPercent} percent.")
            }
            battery.levelPercent > LOW_BATTERY_CLEAR_PERCENT -> {
                lowBatteryAnnounced = false
            }
        }
    }

    private companion object {
        const val TAG = "GlassesHealthMonitor"
        const val LOW_BATTERY_PERCENT = 20
        const val LOW_BATTERY_CLEAR_PERCENT = 25
        const val BATTERY_POLL_MS = 5 * 60_000L
    }
}
