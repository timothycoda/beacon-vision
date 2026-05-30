package com.beacon.data.glasses

import com.beacon.core.result.OperationResult
import com.beacon.domain.glasses.GlassesRepository
import com.beacon.domain.glasses.model.BatteryStatus
import com.beacon.domain.glasses.model.ConnectionState
import com.beacon.domain.glasses.model.DeviceInfo
import com.beacon.domain.glasses.model.GlassesDevice
import com.beacon.domain.vision.CapturedImage
import com.beacon.glasses.GlassesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GlassesRepositoryImpl @Inject constructor(
    private val manager: GlassesManager,
    private val preferences: GlassesPreferences,
) : GlassesRepository {

    override val connectionState: StateFlow<ConnectionState> = manager.connectionState
    override val battery: StateFlow<BatteryStatus?> = manager.battery

    override fun scan(): Flow<List<GlassesDevice>> = manager.scan()
    override fun stopScan() = manager.stopScan()

    override suspend fun connect(device: GlassesDevice): OperationResult<Unit> =
        manager.connect(device)

    override suspend fun reconnectLastKnown(): OperationResult<Unit> {
        val device = preferences.lastDevice.first()
            ?: return OperationResult.Failure("No glasses paired yet.")
        val result = manager.reconnectLastKnown(device.address, device.name)
        if (result is OperationResult.Success) {
            preferences.save(device)
        }
        return result
    }

    override suspend fun disconnect() = manager.disconnect()
    override suspend fun unbind() = manager.unbind()

    override suspend fun refreshBattery() = manager.refreshBattery()
    override suspend fun fetchDeviceInfo(): OperationResult<DeviceInfo> = manager.fetchDeviceInfo()
    override fun isConnected(): Boolean = manager.isConnected()

    override suspend fun capturePhoto(): OperationResult<CapturedImage> = manager.capturePhoto()

    override val voiceTriggers: Flow<Unit> = manager.voiceTriggers

    override val lastConnectedAddress: Flow<String?> = preferences.lastAddress
    override val lastPairedDevice: Flow<GlassesDevice?> = preferences.lastDevice
    override suspend fun rememberDevice(device: GlassesDevice) = preferences.save(device)
    override suspend fun forgetDevice() = preferences.clear()
}
