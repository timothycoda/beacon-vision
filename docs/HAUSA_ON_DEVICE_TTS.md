# Hausa on-device TTS (MMS)

Beacon speaks Hausa using Meta **MMS-TTS** (`facebook/mms-tts-hau`) converted to ONNX and run with [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) v1.12.9.

## User flow

1. Settings → **Offline AI packs** → download **Beacon Hausa Voice** (~109 MB `model.onnx`).
2. Enable **Hausa guidance** on the same screen.
3. Guidance cues use the MMS voice fully offline (no system TTS required).

## Performance

- **Lazy load**: sherpa `OfflineTts` is created on the first Hausa utterance, not at app start.
- **Release**: engine is freed when guidance switches back to English.
- **Threads**: 1 inference thread; short cues capped at 280 characters.
- **Fallback**: if the pack is missing or load fails, Android `TextToSpeech` with `ha-NG` is used.

## Files

| Location | Content |
|----------|---------|
| `files/model_packs/hausa_voice/model.onnx` | Downloaded ONNX (~109 MB) |
| `files/model_packs/hausa_voice/tokens.txt` | Copied from APK assets on install |
| `sherpa/src/main/jniLibs/arm64-v8a/*.so` | sherpa-onnx + onnxruntime (~24 MB in APK, arm64 only) |

Re-fetch JNI libs: `./scripts/fetch-sherpa-android.sh`

## Upgrading from older builds

Earlier builds downloaded `mms-tts-hau.safetensors`, which cannot be used for inference. Remove the old pack and download again to get `model.onnx`.
