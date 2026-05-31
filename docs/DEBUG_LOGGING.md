# Debug logging (feature testing)

Session traces help verify Gemma, Hausa MMS, and the guidance-language toggle without guessing from code.

## On device

- **Logcat:** filter tag `BeaconDebug`
- **File:** `files/debug-b98d12.log` inside the app data directory (NDJSON, one event per line)

## Pull into the workspace (after reproducing)

From the project root, with USB debugging and the app installed:

```bash
./scripts/pull-debug-logs.sh
```

This writes `.cursor/debug-b98d12.log` for analysis in Cursor. The on-device log is cleared on each app start; delete the workspace file before a new pull if you want only the latest session.

## Add traces for a new feature

Call `DebugTrace.event(...)` at decision points, rebuild, reproduce, then pull logs. Remove or comment those calls before shipping if they are noisy.

Live tail on device:

```bash
adb logcat -s BeaconDebug:I
```

## Install a debug build

```bash
./gradlew :app:installDebug
```

Restart the app after installing so `DebugTrace` and new instrumentation load.

## Hypothesis IDs in logs

| ID | What it tests |
|----|----------------|
| H1 | Android TTS `recreateEngine` on language change |
| H2 | Sherpa MMS `release` / native speak |
| H3 | DataStore guidance language toggle |
| H4 | Speech route: MMS vs system TTS / fallback |
| H5 | Scene text (ML Kit Hausa vs phone loop) |
| H6 | Gemma enhancement on English path |
