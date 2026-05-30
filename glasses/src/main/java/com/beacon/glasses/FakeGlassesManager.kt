package com.beacon.glasses

import com.beacon.core.result.OperationResult
import com.beacon.domain.glasses.model.BatteryStatus
import com.beacon.domain.glasses.model.ConnectionState
import com.beacon.domain.glasses.model.DeviceInfo
import com.beacon.domain.glasses.model.GlassesDevice
import com.beacon.domain.vision.CapturedImage
import java.util.Base64
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Hardware-free implementation used on the emulator and in unit tests. Simulates
 * scanning, a connect -> ready transition, and battery updates. Timings are
 * configurable so tests can run instantly.
 */
class FakeGlassesManager @Inject constructor() : GlassesManager {

    // Tunable for tests (set to 0 for instant transitions).
    var scanStepDelayMs: Long = 600
    var connectDelayMs: Long = 700
    var readyDelayMs: Long = 400
    var captureDelayMs: Long = 300
    var failNextConnect: Boolean = false

    private val devices = listOf(
        GlassesDevice(name = "O_Beacon-X01", address = "AA:BB:CC:DD:EE:01", rssi = -48),
        GlassesDevice(name = "X01-Glasses", address = "AA:BB:CC:DD:EE:02", rssi = -71),
    )

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _battery = MutableStateFlow<BatteryStatus?>(null)
    override val battery: StateFlow<BatteryStatus?> = _battery.asStateFlow()

    private val _voiceTriggers = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val voiceTriggers: SharedFlow<Unit> = _voiceTriggers.asSharedFlow()

    /** Test/emulator hook to simulate a glasses voice trigger. */
    fun emitVoiceTrigger() {
        _voiceTriggers.tryEmit(Unit)
    }

    override fun scan(): Flow<List<GlassesDevice>> = flow {
        val emitted = mutableListOf<GlassesDevice>()
        for (device in devices) {
            delay(scanStepDelayMs)
            emitted.add(device)
            emit(emitted.toList())
        }
    }

    override fun stopScan() = Unit

    override suspend fun reconnectLastKnown(address: String, name: String?): OperationResult<Unit> =
        connect(
            GlassesDevice(
                name = name.orEmpty().ifBlank { "Glasses" },
                address = address,
                rssi = -50,
            ),
        )

    override suspend fun connect(device: GlassesDevice): OperationResult<Unit> {
        _connectionState.value = ConnectionState.Connecting(device.name)
        delay(connectDelayMs)
        if (failNextConnect) {
            failNextConnect = false
            _connectionState.value = ConnectionState.Failed("Simulated connection failure")
            return OperationResult.Failure("Simulated connection failure")
        }
        _connectionState.value = ConnectionState.Connected(ready = false, deviceName = device.name)
        delay(readyDelayMs)
        _connectionState.value = ConnectionState.Connected(ready = true, deviceName = device.name)
        _battery.value = BatteryStatus(levelPercent = 82, isCharging = false)
        return OperationResult.Success(Unit)
    }

    override suspend fun disconnect() {
        _connectionState.value = ConnectionState.Disconnected
        _battery.value = null
    }

    override suspend fun unbind() = disconnect()

    override suspend fun refreshBattery() {
        if (_connectionState.value is ConnectionState.Connected) {
            _battery.value = BatteryStatus(levelPercent = (60..95).random(), isCharging = false)
        }
    }

    override suspend fun fetchDeviceInfo(): OperationResult<DeviceInfo> =
        if (_connectionState.value is ConnectionState.Connected) {
            OperationResult.Success(
                DeviceInfo(
                    firmwareVersion = "1.2.0",
                    hardwareVersion = "X01-A1",
                    wifiFirmwareVersion = "0.9.3",
                    wifiHardwareVersion = "WX-B2",
                ),
            )
        } else {
            OperationResult.Failure("Not connected")
        }

    override fun isConnected(): Boolean = _connectionState.value is ConnectionState.Connected

    override suspend fun capturePhoto(): OperationResult<CapturedImage> {
        if (_connectionState.value !is ConnectionState.Connected) {
            return OperationResult.Failure("Not connected")
        }
        delay(captureDelayMs)
        val jpeg = runCatching { Base64.getDecoder().decode(SAMPLE_JPEG_BASE64) }
            .getOrDefault(ByteArray(0))
        return OperationResult.Success(CapturedImage(jpegBytes = jpeg))
    }

    private companion object {
        // A tiny 1x1 JPEG so the emulator/tests have decodable image bytes.
        const val SAMPLE_JPEG_BASE64 =
            "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAP" +
                "//////////////////////////////////////////////////////" +
                "//////////////////////////////////////2wBDAf//////////" +
                "//////////////////////////////////////////////////////" +
                "//////////////////////////wAARCAABAAEDAREAAhEBAxEB/8QA" +
                "FAABAAAAAAAAAAAAAAAAAAAAAv/EABQQAQAAAAAAAAAAAAAAAAAAAAD/" +
                "xAAUAQEAAAAAAAAAAAAAAAAAAAAA/8QAFBEBAAAAAAAAAAAAAAAAAAAA" +
                "AP/aAAwDAQACEQMRAD8AvwA//9k="
    }
}
