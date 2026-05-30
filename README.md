# Beacon

Beacon is an offline-first Android app that turns X01 / HeyCyan smart AI glasses
into a calm, natural guide for visually impaired users. It gives short, useful
audio cues from on-device OCR, scene understanding, and object awareness.

> Beacon is an assistive companion. It can help describe surroundings, but it may
> be wrong. Always use your cane, guide dog, caregiver support, and personal
> judgment. See [SAFETY.md](SAFETY.md).

## Status

- Phase 0 - Project + SDK setup: complete
- Phase 1 - Glasses connection MVP (scan / connect / battery / status): in progress

## Tech stack

- Native Android, Kotlin, Jetpack Compose
- Clean Architecture + MVVM, Hilt DI, Coroutines/Flow
- Modules: `:app`, `:core`, `:glasses`, `:domain`, `:data`
- HeyCyan / X01 vendor SDK (`LIB_GLASSES_SDK-release_3.aar`) wrapped behind a
  `GlassesManager` abstraction (see [SDK_NOTES.md](SDK_NOTES.md))

## Requirements

- JDK 17
- Android SDK with Platform 35 and Build-Tools 35
- A physical Android phone (Bluetooth LE is required for the glasses; the
  emulator can only run the UI and the `FakeGlassesManager` path)

## Setup

### macOS

```bash
brew install --cask temurin@17        # or: brew install openjdk@17
# Install Android Studio, then via SDK Manager install:
#   - SDK Platform 35
#   - Android SDK Build-Tools 35.0.0
#   - Platform-Tools
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

### Linux

```bash
sudo apt-get install -y openjdk-17-jdk
# Install Android Studio / command-line tools, then via SDK Manager install
# Platform 35, Build-Tools 35.0.0, Platform-Tools.
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

### Windows (PowerShell)

```powershell
winget install EclipseAdoptium.Temurin.17.JDK
# Install Android Studio, then via SDK Manager install Platform 35,
# Build-Tools 35.0.0, Platform-Tools.
setx ANDROID_HOME "$env:LOCALAPPDATA\Android\Sdk"
```

The build reads the SDK location from the `ANDROID_HOME` (or `ANDROID_SDK_ROOT`)
environment variable, or from a `local.properties` file containing
`sdk.dir=/absolute/path/to/Android/sdk`. `local.properties` is git-ignored - do
not commit machine-specific paths.

## Build & run

```bash
# Build the debug APK
./gradlew :app:assembleDebug

# Install on a connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Run unit tests
./gradlew testDebugUnitTest
```

To test on the emulator without hardware, set the glasses backend to the fake
implementation by changing `USE_FAKE_GLASSES` to `true` in
[glasses/build.gradle.kts](glasses/build.gradle.kts).

## UI styling (preferred)

Beacon uses an **accessibility-first** Compose design system: high contrast, large
type (see `app/.../ui/theme/`), 88dp primary buttons, rounded cards with visible
borders, and shared components (`BeaconScreen`, `BigActionButton`,
`BeaconStatusCard`). Future UI work should follow
[`.cursor/rules/beacon-ui.mdc`](.cursor/rules/beacon-ui.mdc).

## Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) - module layout and data flow
- [SDK_NOTES.md](SDK_NOTES.md) - verified HeyCyan/X01 SDK API surface
- [SAFETY.md](SAFETY.md) - safety promises and limitations
- [PRIVACY.md](PRIVACY.md) - offline-first privacy stance
- [MODEL_PACKS.md](MODEL_PACKS.md) - offline AI pack plan (later phases)
- [TESTING.md](TESTING.md) - test strategy
