# Testing

## Unit tests

- `FakeGlassesManager` driven tests (connect/disconnect/battery/error states)
- ViewModel tests (e.g. `PairingViewModel`) driving the fake backend
- Later: `SafetyGuidanceFormatterTest`, `VoiceCommandParserTest`,
  `ModelPackRepositoryTest`, `DownloadVerifierTest`, `EmergencyMessageBuilderTest`

Run: `./gradlew testDebugUnitTest`

## Fake hardware

`FakeGlassesManager` simulates scan results, connect -> ready, battery ticks, and
error paths so the full flow runs on an emulator without glasses. Select it with
`USE_FAKE_GLASSES=true` in `glasses/build.gradle.kts`.

## Manual hardware tests

On a physical phone with the X01 glasses:

1. Scan and list the glasses
2. Connect; confirm status reaches "Connected / ready"
3. Read battery and firmware/hardware versions
4. Disconnect and unbind
5. Toggle Bluetooth off mid-session; confirm graceful handling
6. (Later phases) capture image, retrieve thumbnail, Wi-Fi sync, PCM callback,
   walking mode endurance, low-battery behaviour
