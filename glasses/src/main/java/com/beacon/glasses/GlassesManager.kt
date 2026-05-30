package com.beacon.glasses

import com.beacon.core.result.OperationResult
import com.beacon.domain.glasses.model.BatteryStatus
import com.beacon.domain.glasses.model.ConnectionState
import com.beacon.domain.glasses.model.DeviceInfo
import com.beacon.domain.glasses.model.GlassesDevice
import com.beacon.domain.vision.CapturedImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Vendor-agnostic abstraction over the smart glasses. The real implementation
 * ([HeyCyanGlassesManager]) wraps the HeyCyan/X01 SDK; [FakeGlassesManager]
 * provides a hardware-free implementation for the emulator and tests.
 */
interface GlassesManager {
    val connectionState: StateFlow<ConnectionState>
    val battery: StateFlow<BatteryStatus?>

    /**
     * Emits when the glasses report that the user triggered their voice/mic
     * (device-notify marker 0x03). Used to start hands-free voice commands.
     */
    val voiceTriggers: Flow<Unit>

    fun scan(): Flow<List<GlassesDevice>>
    fun stopScan()

    suspend fun connect(device: GlassesDevice): OperationResult<Unit>

    /** Reconnect to a previously paired device (no BLE scan). */
    suspend fun reconnectLastKnown(address: String, name: String?): OperationResult<Unit>

    suspend fun disconnect()
    suspend fun unbind()

    suspend fun refreshBattery()
    suspend fun fetchDeviceInfo(): OperationResult<DeviceInfo>
    fun isConnected(): Boolean

    /** Capture a single still image (JPEG) from the glasses camera. */
    suspend fun capturePhoto(): OperationResult<CapturedImage>
}
