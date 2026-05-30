package com.beacon.domain.glasses

import com.beacon.core.result.OperationResult
import com.beacon.domain.glasses.model.BatteryStatus
import com.beacon.domain.glasses.model.ConnectionState
import com.beacon.domain.glasses.model.DeviceInfo
import com.beacon.domain.glasses.model.GlassesDevice
import com.beacon.domain.vision.CapturedImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain contract for interacting with the smart glasses. Implemented in :data
 * on top of the vendor-agnostic GlassesManager so the rest of the app never
 * touches vendor APIs.
 */
interface GlassesRepository {
    val connectionState: StateFlow<ConnectionState>
    val battery: StateFlow<BatteryStatus?>

    /** Cold flow emitting the growing list of discovered devices until cancelled. */
    fun scan(): Flow<List<GlassesDevice>>
    fun stopScan()

    suspend fun connect(device: GlassesDevice): OperationResult<Unit>
    suspend fun reconnectLastKnown(): OperationResult<Unit>
    suspend fun disconnect()
    suspend fun unbind()

    suspend fun refreshBattery()
    suspend fun fetchDeviceInfo(): OperationResult<DeviceInfo>
    fun isConnected(): Boolean

    /** Capture a single still image from the glasses camera. */
    suspend fun capturePhoto(): OperationResult<CapturedImage>

    /** Emits when the glasses report their voice/mic trigger (hands-free start). */
    val voiceTriggers: Flow<Unit>

    /** Address of the last successfully connected device, if any. */
    val lastConnectedAddress: Flow<String?>
    val lastPairedDevice: Flow<GlassesDevice?>
    suspend fun rememberDevice(device: GlassesDevice)
    suspend fun forgetDevice()
}
