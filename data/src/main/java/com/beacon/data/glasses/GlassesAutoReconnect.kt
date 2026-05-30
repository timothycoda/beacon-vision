package com.beacon.data.glasses

import com.beacon.core.bluetooth.BluetoothStateProvider
import com.beacon.core.concurrency.DispatcherProvider
import com.beacon.core.log.BeaconLog
import com.beacon.data.prefs.AppPreferences
import com.beacon.domain.glasses.GlassesRepository
import com.beacon.domain.glasses.model.ConnectionState
import com.beacon.domain.glasses.usecase.ReconnectLastGlassesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps trying to reconnect to the last paired glasses when Bluetooth is on and
 * the link is down, without starting a BLE scan (avoids connect/disconnect loops).
 */
@Singleton
class GlassesAutoReconnect @Inject constructor(
    private val repository: GlassesRepository,
    private val reconnectLastGlasses: ReconnectLastGlassesUseCase,
    private val appPreferences: AppPreferences,
    private val glassesPreferences: GlassesPreferences,
    private val bluetoothState: BluetoothStateProvider,
    private val controller: GlassesReconnectController,
    dispatchers: DispatcherProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private var loopJob: Job? = null

    init {
        scope.launch {
            combine(
                appPreferences.onboardingComplete,
                glassesPreferences.lastDevice,
                bluetoothState.enabledState(),
                controller.paused,
            ) { onboarded, device, btOn, paused ->
                onboarded && device != null && btOn && !paused
            }.collect { shouldRun ->
                if (shouldRun) {
                    ensureLoop()
                } else {
                    stopLoop()
                }
            }
        }
        BeaconLog.i(TAG, "Glasses auto-reconnect started")
    }

    private fun ensureLoop() {
        if (loopJob?.isActive == true) return
        loopJob = scope.launch { reconnectLoop() }
    }

    private fun stopLoop() {
        loopJob?.cancel()
        loopJob = null
    }

    private suspend fun reconnectLoop() {
        while (currentCoroutineContext().isActive) {
            val onboarded = appPreferences.onboardingComplete.first()
            val device = glassesPreferences.lastDevice.first()
            if (!onboarded || device == null || !bluetoothState.isEnabled() || controller.paused.value) {
                delay(RETRY_MS)
                continue
            }
            when (val state = repository.connectionState.value) {
                is ConnectionState.Connected -> {
                    if (state.ready) {
                        delay(RETRY_MS)
                        continue
                    }
                    delay(CONNECTING_POLL_MS)
                }
                is ConnectionState.Connecting -> delay(CONNECTING_POLL_MS)
                ConnectionState.Disconnected,
                is ConnectionState.Failed,
                -> {
                    BeaconLog.d(TAG, "attempting reconnect to ${device.name}")
                    reconnectLastGlasses()
                    delay(RETRY_MS)
                }
                ConnectionState.BluetoothOff,
                ConnectionState.Scanning,
                -> delay(CONNECTING_POLL_MS)
            }
        }
    }

    private companion object {
        const val TAG = "GlassesAutoReconnect"
        const val RETRY_MS = 12_000L
        const val CONNECTING_POLL_MS = 2_000L
    }
}
