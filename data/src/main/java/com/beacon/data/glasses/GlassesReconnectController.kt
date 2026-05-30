package com.beacon.data.glasses

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates background auto-reconnect with explicit user actions.
 *
 * When the user manually scans for glasses on the pairing screen, auto-reconnect
 * must pause and free the BLE radio; otherwise its repeated direct-connect
 * attempts (and the [GlassesManager.stopScan] they trigger) starve the scan and
 * no devices are found.
 */
@Singleton
class GlassesReconnectController @Inject constructor() {
    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused

    /** Suspend background auto-reconnect (e.g. while a manual scan is running). */
    fun pause() {
        _paused.value = true
    }

    /** Resume background auto-reconnect once the user is done scanning. */
    fun resume() {
        _paused.value = false
    }
}
