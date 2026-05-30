# HeyCyan / X01 SDK Notes

These APIs were verified directly against `LIB_GLASSES_SDK-release_3.aar`
(via `javap` on the bundled `classes.jar`) and the official `GlassesSDKSample`.
Do not invent additional methods - extend this file as new APIs are verified.

Vendor packages: `com.oudmon.ble.*`, `com.oudmon.wifi.*`, plus bundled
`com.jieli.*` (audio codecs) and `com.androidnetworking.*`.

## Initialisation (once, at app start)

Replicated by `GlassesSdkInitializer` (mirrors the sample `MyApplication`):

```kotlin
LargeDataHandler.getInstance()
BleOperateManager.getInstance(application).apply { setApplication(application); init() }
BleBaseControl.getInstance(application).setmContext(application)
```

The vendor connection lifecycle is delivered through a
`QCBluetoothCallbackCloneReceiver` registered via `LocalBroadcastManager` with
`BleAction.getIntentFilter()`, plus a system `BroadcastReceiver` registered with
`BleAction.getDeviceIntentFilter()` for adapter on/off + bonding.

## Scanning

```kotlin
BleScannerHelper.getInstance().reSetCallback()
BleScannerHelper.getInstance().scanDevice(context, /* uuid */ null, scanWrapperCallback)
BleScannerHelper.getInstance().stopScan(context)
```

`ScanWrapperCallback` results arrive on
`onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?)`.

## Connect / disconnect / unbind

```kotlin
DeviceManager.getInstance().setDeviceAddress(address)   // set before connecting
BleOperateManager.getInstance().connectDirectly(address)
BleOperateManager.getInstance().connectWithScan(address) // alternative
BleOperateManager.getInstance().disconnect()
BleOperateManager.getInstance().unBindDevice()
BleOperateManager.getInstance().isConnected               // Boolean
BleOperateManager.getInstance().setNeedConnect(true)      // auto-reconnect intent
BleOperateManager.getInstance().setBluetoothTurnOff(false)
```

## Connection lifecycle callbacks (`QCBluetoothCallbackCloneReceiver`)

- `connectStatue(device: BluetoothDevice?, connected: Boolean)`
- `onServiceDiscovered()` - **critical**: call
  `LargeDataHandler.getInstance().initEnable()` and set
  `BleOperateManager.getInstance().isReady = true` here before sending any
  command. This is the signal that the link is fully ready.
- `onCharacteristicRead`, `bleStatus`, `onCharacteristicChange`, ...

## Battery

```kotlin
LargeDataHandler.getInstance().addBatteryCallBack("beacon") { _, response ->
    val level = response.battery        // Int
    val charging = response.isCharging  // Boolean
}
LargeDataHandler.getInstance().syncBattery()
LargeDataHandler.getInstance().removeBatteryCallBack("beacon")
```

Battery is also pushed via the device-notify stream (load byte `0x05`).

## Device info

```kotlin
LargeDataHandler.getInstance().syncDeviceInfo { _, response ->
    response.firmwareVersion
    response.hardwareVersion
    response.wifiFirmwareVersion
    response.wifiHardwareVersion
}
```

## Device-notify stream (battery / AI recognition / volume events)

```kotlin
LargeDataHandler.getInstance().addOutDeviceListener(100, object : GlassesDeviceNotifyListener() {
    override fun parseData(cmdType: Int, response: GlassesDeviceNotifyRsp) {
        when (response.loadData[6].toInt()) {
            0x05 -> { /* battery: loadData[7]=level, loadData[8]=charging */ }
            0x02 -> { /* AI recognition done -> getPictureThumbnails(...) */ }
            // 0x0c pause, 0x0d unbind, 0x0e low memory, 0x12 volume changed ...
        }
    }
})
```

## Glasses control commands (`glassesControl(byte[], ILargeDataResponse<GlassModelControlResponse>)`)

- Photo (store only, no BLE report): `byteArrayOf(0x02, 0x01, 0x01)`
- Video start/stop: `byteArrayOf(0x02, 0x01, 0x02)` / `... 0x03`
- Audio record start/stop: `byteArrayOf(0x02, 0x01, 0x08)` / `... 0x0c`
- **Photo + report thumbnail** (use this to get an image over BLE):
  `byteArrayOf(0x02, 0x01, 0x06, size, size, 0x02)` (size 0..6) — see "Capture a
  still image over BLE" below
- Unsynced media count: `byteArrayOf(0x02, 0x04)` ->
  `response.imageCount + videoCount + recordCount`

`GlassModelControlResponse`: `dataType`, `errorCode`, `workTypeIng`
(1/6 photo, 2 recording, 4 transfer, 5 OTA, 7 AI, 8 audio), `p2pIp`, counts.

## Capture a still image over BLE (verified on real X01 hardware)

The "What is ahead?" capture path. **Important gotchas learned on-device:**

1. Use the **thumbnail-trigger** command, NOT the plain photo command. The plain
   photo command `0x02,0x01,0x01` captures to glasses storage but does **not**
   report anything back over BLE, so you wait forever. The command that both
   captures and pushes the JPEG back is:
   ```kotlin
   // "Trigger AI to take a photo and report the thumbnail" (size 0..6)
   glassesControl(byteArrayOf(0x02, 0x01, 0x06, size, size, 0x02)) { _, resp -> /* ack */ }
   ```
2. A device-notify with `loadData[6] == 0x02` then fires ("thumbnail ready").
   In response, call `getPictureThumbnails`.
3. `getPictureThumbnails` is **streamed/chunked**: the callback fires many times
   with `success=false` carrying ~1013-byte JPEG slices, then once with
   `success=true` (final slice). **Accumulate every chunk** and assemble on
   `success=true`. A typical thumbnail is ~13 KB. Keeping only the final-chunk
   bytes yields a tiny invalid "image".

```kotlin
val buffer = ByteArrayOutputStream()
LargeDataHandler.getInstance().getPictureThumbnails { _, success, data ->
    if (data != null && data.isNotEmpty()) buffer.write(data)
    if (success) { /* buffer.toByteArray() is the full JPEG */ }
}
```

Implemented in `HeyCyanGlassesManager.capturePhoto()` (20s timeout, single-flight
mutex, persists JPEG to `cacheDir/captures`).

## Wi-Fi media sync (later phases) - `com.oudmon.wifi.GlassesControl`

```kotlin
GlassesControl.getInstance(application)?.initGlasses(albumDirAbsolutePath)
GlassesControl.getInstance(application)?.setWifiDownloadListener(object : GlassesControl.WifiFilesDownloadListener { ... })
GlassesControl.getInstance(application)?.importAlbum()
// PCM voice callbacks: voiceFromGlasses(pcmData: ByteArray), voiceFromGlassesStatus(status: Int)
```

## Packaging notes

- Flat AAR with no POM. Required runtime deps added manually:
  `com.google.code.gson:gson:2.8.9`, `com.squareup.okhttp3:okhttp:4.9.3`,
  `androidx.localbroadcastmanager:localbroadcastmanager:1.1.0`.
- Native `.so` ship for `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`.
- AAR `minSdkVersion` is 23; Beacon uses `minSdk` 26.
- Keep rules for `com.oudmon.**`, `com.jieli.**`, `com.androidnetworking.**`
  live in `glasses/consumer-rules.pro`.
