package com.beacon.domain.glasses.usecase

import com.beacon.core.result.OperationResult
import com.beacon.domain.glasses.GlassesRepository
import com.beacon.domain.glasses.model.BatteryStatus
import com.beacon.domain.glasses.model.ConnectionState
import com.beacon.domain.glasses.model.DeviceInfo
import com.beacon.domain.glasses.model.GlassesDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveConnectionStateUseCase @Inject constructor(
    private val repository: GlassesRepository,
) {
    operator fun invoke(): StateFlow<ConnectionState> = repository.connectionState
}

class ObserveBatteryUseCase @Inject constructor(
    private val repository: GlassesRepository,
) {
    operator fun invoke(): StateFlow<BatteryStatus?> = repository.battery
}

class ScanForGlassesUseCase @Inject constructor(
    private val repository: GlassesRepository,
) {
    operator fun invoke(): Flow<List<GlassesDevice>> = repository.scan()
    fun stop() = repository.stopScan()
}

class ConnectGlassesUseCase @Inject constructor(
    private val repository: GlassesRepository,
) {
    suspend operator fun invoke(device: GlassesDevice): OperationResult<Unit> {
        val result = repository.connect(device)
        if (result is OperationResult.Success) repository.rememberDevice(device)
        return result
    }
}

class ReconnectLastGlassesUseCase @Inject constructor(
    private val repository: GlassesRepository,
) {
    suspend operator fun invoke(): OperationResult<Unit> = repository.reconnectLastKnown()
}

class ObserveLastPairedDeviceUseCase @Inject constructor(
    private val repository: GlassesRepository,
) {
    operator fun invoke(): Flow<GlassesDevice?> = repository.lastPairedDevice
}

class DisconnectGlassesUseCase @Inject constructor(
    private val repository: GlassesRepository,
) {
    suspend operator fun invoke() = repository.disconnect()
}

class UnbindGlassesUseCase @Inject constructor(
    private val repository: GlassesRepository,
) {
    suspend operator fun invoke() {
        repository.unbind()
        repository.forgetDevice()
    }
}

class RefreshBatteryUseCase @Inject constructor(
    private val repository: GlassesRepository,
) {
    suspend operator fun invoke() = repository.refreshBattery()
}

class FetchDeviceInfoUseCase @Inject constructor(
    private val repository: GlassesRepository,
) {
    suspend operator fun invoke(): OperationResult<DeviceInfo> = repository.fetchDeviceInfo()
}

class ObserveLastDeviceUseCase @Inject constructor(
    private val repository: GlassesRepository,
) {
    operator fun invoke(): Flow<String?> = repository.lastConnectedAddress
}
