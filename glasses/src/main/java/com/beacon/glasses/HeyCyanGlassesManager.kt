package com.beacon.glasses

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import com.beacon.core.concurrency.DispatcherProvider
import com.beacon.core.log.BeaconLog
import com.beacon.core.result.OperationResult
import com.beacon.domain.glasses.model.BatteryStatus
import com.beacon.domain.glasses.model.DeviceInfo
import com.beacon.domain.glasses.model.GlassesDevice
import com.beacon.domain.vision.CapturedImage
import com.beacon.glasses.internal.GlassesStateHolder
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.bluetooth.DeviceManager
import com.oudmon.ble.base.communication.LargeDataHandler
import com.oudmon.ble.base.communication.bigData.resp.GlassesDeviceNotifyListener
import com.oudmon.ble.base.communication.bigData.resp.GlassesDeviceNotifyRsp
import com.oudmon.ble.base.scan.BleScannerHelper
import com.oudmon.ble.base.scan.ScanRecord
import com.oudmon.ble.base.scan.ScanWrapperCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import javax.inject.Inject

/**
 * Real glasses backend wrapping the HeyCyan/X01 SDK. All vendor calls run off
 * the main thread. Connection/battery state is sourced from [GlassesStateHolder],
 * which the [com.beacon.glasses.internal.BeaconGlassesReceiver] updates from the
 * vendor's broadcast callbacks.
 */
class HeyCyanGlassesManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val state: GlassesStateHolder,
    private val dispatchers: DispatcherProvider,
) : GlassesManager {

    override val connectionState: StateFlow<com.beacon.domain.glasses.model.ConnectionState> =
        state.connectionState
    override val battery: StateFlow<BatteryStatus?> = state.battery

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val _voiceTriggers = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val voiceTriggers: SharedFlow<Unit> = _voiceTriggers.asSharedFlow()

    init {
        // Register the device-notify listener as soon as the link is ready, so we
        // receive the glasses' voice-trigger (0x03) events without needing a photo
        // capture first.
        scope.launch {
            state.connectionState.collect { conn ->
                if (conn is com.beacon.domain.glasses.model.ConnectionState.Connected && conn.ready) {
                    ensureImageListener()
                }
            }
        }
    }

    override fun scan(): Flow<List<GlassesDevice>> = callbackFlow {
        val found = LinkedHashMap<String, GlassesDevice>()
        var active = true

        fun emitFound() {
            trySend(found.values.sortedByDescending { it.rssi })
        }

        fun recordDevice(address: String, name: String?, rssi: Int) {
            if (name.isNullOrBlank()) return
            val existing = found[address]
            found[address] = GlassesDevice(
                name = name,
                address = address,
                rssi = if (rssi != 0) rssi else (existing?.rssi ?: 0),
            )
            emitFound()
        }

        lateinit var scanCallback: ScanWrapperCallback

        fun startVendorScan() {
            runCatching {
                BleScannerHelper.getInstance().reSetCallback()
                BleScannerHelper.getInstance().scanDevice(context, null, scanCallback)
            }.onFailure {
                BeaconLog.e(TAG, "Unable to start scan", it)
                close(it)
            }
        }

        seedBondedGlasses(found)
        if (found.isNotEmpty()) emitFound()

        scanCallback = object : ScanWrapperCallback {
            override fun onStart() = Unit
            override fun onStop() = Unit

            override fun onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
                val address = device?.address ?: return
                val name = resolveScanName(device, scanRecordBytes = scanRecord)
                recordDevice(address, name, rssi)
            }

            override fun onScanFailed(errorCode: Int) {
                BeaconLog.w(TAG, "scan failed: $errorCode")
                if (active && errorCode == 0) {
                    startVendorScan()
                }
            }

            override fun onParsedData(device: BluetoothDevice?, scanRecord: ScanRecord?) {
                val address = device?.address ?: return
                val name = resolveScanName(device, parsedRecord = scanRecord)
                recordDevice(address, name, rssi = 0)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) = Unit
        }

        startVendorScan()

        awaitClose {
            active = false
            runCatching { BleScannerHelper.getInstance().stopScan(context) }
        }
    }

    /** Resolve a human-readable name from the bonded device or BLE advert bytes. */
    private fun resolveScanName(
        device: BluetoothDevice?,
        scanRecordBytes: ByteArray? = null,
        parsedRecord: ScanRecord? = null,
    ): String? {
        val fromDevice = runCatching { device?.name }.getOrNull()
        if (!fromDevice.isNullOrBlank()) return fromDevice
        val fromParsed = parsedRecord?.deviceName?.takeIf { it.isNotBlank() }
        if (fromParsed != null) return fromParsed
        val bytes = scanRecordBytes ?: parsedRecord?.bytes
        if (bytes != null) {
            val fromBytes = runCatching { ScanRecord.parseFromBytes(bytes)?.deviceName }.getOrNull()
            if (!fromBytes.isNullOrBlank()) return fromBytes
        }
        return null
    }

    /**
     * Glasses paired in the phone's Bluetooth settings appear here with a stable
     * name even when BLE adverts omit the name (common until pairing mode).
     */
    private fun seedBondedGlasses(found: MutableMap<String, GlassesDevice>) {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
            .adapter ?: return
        runCatching {
            for (device in adapter.bondedDevices) {
                val name = runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() }
                    ?: continue
                if (!isLikelyGlassesName(name)) continue
                found[device.address] = GlassesDevice(
                    name = name,
                    address = device.address,
                    rssi = found[device.address]?.rssi ?: 0,
                )
            }
        }.onFailure { BeaconLog.e(TAG, "seedBondedGlasses failed", it) }
    }

    private fun isLikelyGlassesName(name: String): Boolean =
        name.startsWith("X01_", ignoreCase = true)

    override fun stopScan() {
        runCatching { BleScannerHelper.getInstance().stopScan(context) }
    }

    override suspend fun connect(device: GlassesDevice): OperationResult<Unit> =
        connectInternal(device)

    override suspend fun reconnectLastKnown(address: String, name: String?): OperationResult<Unit> =
        connectInternal(
            GlassesDevice(name = name.orEmpty().ifBlank { "Glasses" }, address = address, rssi = 0),
        )

    /**
     * Connect by address using the vendor's direct-connect path. We deliberately
     * do NOT use `connectWithScan`: it starts an internal BLE scan that competes
     * with our own [scan] and can leave the scanner starved (no devices found).
     */
    private suspend fun connectInternal(
        device: GlassesDevice,
    ): OperationResult<Unit> = connectMutex.withLock {
        withContext(dispatchers.io) {
            stopScan()
            val current = state.connectionState.value
            if (current is com.beacon.domain.glasses.model.ConnectionState.Connected &&
                current.ready &&
                DeviceManager.getInstance().deviceAddress == device.address
            ) {
                return@withContext OperationResult.Success(Unit)
            }
            runCatching {
                state.onConnecting(device.name)
                DeviceManager.getInstance().deviceAddress = device.address
                DeviceManager.getInstance().deviceName = device.name
                BleOperateManager.getInstance().setNeedConnect(true)
                BleOperateManager.getInstance().connectDirectly(device.address)
            }.fold(
                onSuccess = { OperationResult.Success(Unit) },
                onFailure = {
                    val message = it.message ?: "Could not connect"
                    state.onFailed(message)
                    OperationResult.Failure(message, it)
                },
            )
        }
    }

    private val connectMutex = Mutex()

    override suspend fun disconnect() {
        withContext(dispatchers.io) {
            runCatching {
                // Stop any in-flight scan/connect intent so the radio is fully freed
                // (otherwise a later scan can come back empty).
                stopScan()
                BleOperateManager.getInstance().setNeedConnect(false)
                BleOperateManager.getInstance().disconnect()
            }
            state.onDisconnected()
        }
    }

    override suspend fun unbind() {
        withContext(dispatchers.io) {
            runCatching {
                stopScan()
                BleOperateManager.getInstance().setNeedConnect(false)
                BleOperateManager.getInstance().unBindDevice()
            }
            state.onDisconnected()
        }
    }

    override suspend fun refreshBattery() {
        withContext(dispatchers.io) {
            runCatching {
                LargeDataHandler.getInstance().addBatteryCallBack(BATTERY_KEY) { _, response ->
                    response?.let { state.updateBattery(it.battery, it.isCharging) }
                }
                LargeDataHandler.getInstance().syncBattery()
            }.onFailure { BeaconLog.e(TAG, "refreshBattery failed", it) }
        }
    }

    override suspend fun fetchDeviceInfo(): OperationResult<DeviceInfo> =
        withContext(dispatchers.io) {
            val info = withTimeoutOrNull(DEVICE_INFO_TIMEOUT_MS) {
                suspendCoroutine { cont ->
                    runCatching {
                        LargeDataHandler.getInstance().syncDeviceInfo { _, response ->
                            if (response != null) {
                                cont.resume(
                                    DeviceInfo(
                                        firmwareVersion = response.firmwareVersion.orEmpty(),
                                        hardwareVersion = response.hardwareVersion.orEmpty(),
                                        wifiFirmwareVersion = response.wifiFirmwareVersion.orEmpty(),
                                        wifiHardwareVersion = response.wifiHardwareVersion.orEmpty(),
                                    ),
                                )
                            } else {
                                cont.resume(null)
                            }
                        }
                    }.onFailure { cont.resume(null) }
                }
            }
            if (info != null) {
                OperationResult.Success(info)
            } else {
                OperationResult.Failure("Could not read device info")
            }
        }

    override fun isConnected(): Boolean =
        runCatching { BleOperateManager.getInstance().isConnected }.getOrDefault(false)

    // --- Photo capture -------------------------------------------------------

    private val captureMutex = Mutex()
    private val imageListenerRegistered = AtomicBoolean(false)
    private val pendingCapture = AtomicReference<CancellableContinuation<ByteArray>?>(null)

    /**
     * Registers the device-notify listener once. When the glasses report a
     * "quick recognition / photo ready" event (loadData[6] == 0x02), we pull the
     * JPEG thumbnail and hand it to whatever capture is currently awaiting it.
     */
    private fun ensureImageListener() {
        if (!imageListenerRegistered.compareAndSet(false, true)) return
        runCatching {
            val listener = object : GlassesDeviceNotifyListener() {
                override fun parseData(cmdType: Int, response: GlassesDeviceNotifyRsp) {
                    val load = runCatching { response.loadData }.getOrNull() ?: return
                    // Log every notify so we can diagnose the capture handshake on-device.
                    val marker = if (load.size > 6) load[6].toInt() and 0xFF else -1
                    val sub = if (load.size > 7) load[7].toInt() and 0xFF else -1
                    BeaconLog.d(TAG, "notify cmdType=$cmdType marker=0x${marker.toString(16)} sub=$sub load=${load.toHex()}")
                    // 0x02 == "quick recognition / thumbnail ready" -> pull the JPEG.
                    if (marker == 0x02) {
                        BeaconLog.d(TAG, "thumbnail-ready notify -> fetching thumbnail")
                        fetchThumbnail()
                    }
                    // 0x03 (sub==1) == "glasses activated microphone, user speaking"
                    // -> the glasses voice trigger. Start hands-free voice commands.
                    if (marker == 0x03 && sub == 1) {
                        BeaconLog.i(TAG, "glasses voice trigger (0x03) -> emitting")
                        _voiceTriggers.tryEmit(Unit)
                    }
                }
            }
            LargeDataHandler.getInstance().addOutDeviceListener(NOTIFY_KEY, listener)
            BeaconLog.i(TAG, "device-notify listener registered (key=$NOTIFY_KEY)")
        }.onFailure {
            imageListenerRegistered.set(false)
            BeaconLog.e(TAG, "addOutDeviceListener failed", it)
        }
    }

    private fun fetchThumbnail() {
        runCatching {
            // The thumbnail arrives as a stream of chunks: each callback carries a
            // slice of the JPEG with success=false, and the final callback has
            // success=true. We accumulate every chunk and assemble on completion.
            val buffer = java.io.ByteArrayOutputStream()
            LargeDataHandler.getInstance().getPictureThumbnails { cmdType, success, data ->
                if (data != null && data.isNotEmpty()) buffer.write(data)
                BeaconLog.d(TAG, "thumb chunk cmdType=$cmdType success=$success size=${data?.size ?: -1} total=${buffer.size()}")
                if (success) {
                    val full = buffer.toByteArray()
                    if (full.isNotEmpty()) {
                        BeaconLog.i(TAG, "thumbnail complete: ${full.size} bytes")
                        pendingCapture.getAndSet(null)?.let { cont ->
                            if (cont.isActive) cont.resume(full)
                        }
                    } else {
                        BeaconLog.w(TAG, "thumbnail complete but buffer empty")
                    }
                }
            }
        }.onFailure { BeaconLog.e(TAG, "getPictureThumbnails failed", it) }
    }

    override suspend fun capturePhoto(): OperationResult<CapturedImage> =
        withContext(dispatchers.io) {
            if (!isConnected()) return@withContext OperationResult.Failure("Glasses not connected")
            captureMutex.withLock {
                ensureImageListener()
                val bytes = withTimeoutOrNull(CAPTURE_TIMEOUT_MS) {
                    suspendCancellableCoroutine { cont ->
                        pendingCapture.set(cont)
                        cont.invokeOnCancellation { pendingCapture.compareAndSet(cont, null) }
                        runCatching {
                            BeaconLog.d(TAG, "sending capture cmd ${PHOTO_CMD.toHex()}")
                            LargeDataHandler.getInstance().glassesControl(PHOTO_CMD) { _, resp ->
                                val type = runCatching { resp.dataType }.getOrNull()
                                val err = runCatching { resp.errorCode }.getOrNull()
                                val work = runCatching { resp.workTypeIng }.getOrNull()
                                BeaconLog.d(TAG, "glassesControl ack dataType=$type errorCode=$err workTypeIng=$work")
                            }
                        }.onFailure {
                            pendingCapture.compareAndSet(cont, null)
                            if (cont.isActive) cont.resume(ByteArray(0))
                        }
                    }
                }
                pendingCapture.set(null)
                when {
                    bytes == null -> OperationResult.Failure("Timed out waiting for the photo")
                    bytes.isEmpty() -> OperationResult.Failure("Could not capture a photo")
                    else -> OperationResult.Success(persist(bytes))
                }
            }
        }

    private fun persist(jpeg: ByteArray): CapturedImage {
        val path = runCatching {
            val dir = File(context.cacheDir, "captures").apply { mkdirs() }
            val file = File(dir, "scene_${System.currentTimeMillis()}.jpg")
            file.writeBytes(jpeg)
            file.absolutePath
        }.getOrNull()
        return CapturedImage(jpegBytes = jpeg, filePath = path)
    }

    private fun ByteArray.toHex(): String =
        joinToString(" ") { "%02x".format(it.toInt() and 0xFF) }

    private companion object {
        const val TAG = "HeyCyanGlassesManager"
        const val BATTERY_KEY = "beacon"
        const val DEVICE_INFO_TIMEOUT_MS = 6_000L
        const val CAPTURE_TIMEOUT_MS = 20_000L
        const val NOTIFY_KEY = 100

        // Thumbnail size, range 0..6 (see SDK doc "Control intelligent image
        // recognition and report thumbnails"). 0x02 is a good small preview.
        const val THUMBNAIL_SIZE: Byte = 0x02

        // "Trigger AI to take a photo and report the thumbnail" — this both
        // captures and pushes the JPEG thumbnail back over BLE (notify 0x02 ->
        // getPictureThumbnails). The plain photo command (0x02,0x01,0x01) does
        // NOT report a thumbnail, which is why earlier captures timed out.
        val PHOTO_CMD = byteArrayOf(0x02, 0x01, 0x06, THUMBNAIL_SIZE, THUMBNAIL_SIZE, 0x02)
    }
}
