package com.beacon.app.pairing

import com.beacon.core.result.OperationResult
import com.beacon.domain.glasses.GlassesRepository
import com.beacon.domain.glasses.model.BatteryStatus
import com.beacon.domain.glasses.model.ConnectionState
import com.beacon.domain.glasses.model.DeviceInfo
import com.beacon.domain.glasses.model.GlassesDevice
import com.beacon.domain.vision.CapturedImage
import com.beacon.glasses.FakeGlassesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Test double wrapping [FakeGlassesManager] plus in-memory preferences. */
class FakeGlassesRepository(
    private val manager: FakeGlassesManager = FakeGlassesManager().apply {
        scanStepDelayMs = 0
        connectDelayMs = 0
        readyDelayMs = 0
    },
) : GlassesRepository {

    private val _lastAddress = MutableStateFlow<String?>(null)

    override val connectionState: StateFlow<ConnectionState> get() = manager.connectionState
    override val battery: StateFlow<BatteryStatus?> get() = manager.battery

    override fun scan(): Flow<List<GlassesDevice>> = manager.scan()
    override fun stopScan() = manager.stopScan()

    override suspend fun connect(device: GlassesDevice): OperationResult<Unit> =
        manager.connect(device)

    override suspend fun disconnect() = manager.disconnect()
    override suspend fun unbind() = manager.unbind()
    override suspend fun refreshBattery() = manager.refreshBattery()
    override suspend fun fetchDeviceInfo(): OperationResult<DeviceInfo> = manager.fetchDeviceInfo()
    override fun isConnected(): Boolean = manager.isConnected()
    override suspend fun capturePhoto(): OperationResult<CapturedImage> = manager.capturePhoto()

    override val lastConnectedAddress: Flow<String?> get() = _lastAddress
    override suspend fun rememberDevice(device: GlassesDevice) { _lastAddress.value = device.address }
    override suspend fun forgetDevice() { _lastAddress.value = null }

    fun failNextConnect() { manager.failNextConnect = true }
}
