# Offline AI Model Packs

Optional on-device assets downloaded **after** the base app is installed. The APK
ships with ML Kit only (baseline OCR + image labels). See [GRAND_PLAN.md](GRAND_PLAN.md)
for the full roadmap including **Gemma 4 Lite** and **phone-only camera mode**.

## Catalogue

| Pack ID | Display name | Purpose | Approx. size |
|---------|--------------|---------|--------------|
| `lite` | Lite Offline Pack | Basic detectors + OCR helpers | ~48 MB |
| `balanced` | Balanced Offline Pack | Better scene / object models | ~180 MB |
| `gemma_4_lite` | **Gemma 4 E2B** | Official LiteRT-LM build for richer narration | ~2.4 GB |
| `balanced` | **Gemma 4 E4B** | Higher-quality Gemma 4 (more RAM) | ~3.4 GB |
| `hausa_voice` | Hausa Voice Pack | Offline Hausa TTS | ~32 MB |

### Gemma 4 (E2B / E4B)

- **Not bundled** in the APK.
- Download URLs (Hugging Face, `resolve/main`):
  - E2B: `https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm`
  - E4B: `https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm`
- Checksums pinned in `data/.../ModelPackCatalog.kt` (from HF LFS on `main`).
- Full reference: [docs/GEMMA_4_ON_DEVICE.md](docs/GEMMA_4_ON_DEVICE.md).
- Runtime: `com.google.ai.edge.litertlm:litertlm-android` (Phase 4).

## Download manager (implemented)

| Requirement | Implementation |
|-------------|----------------|
| User-initiated download | Settings → Offline AI packs → Download |
| Wi‑Fi only | Toggle + `NetworkPolicy.isUnmetered()` |
| Progress | `ModelPackPreferences.bytesDownloaded` + UI progress bar |
| Checksum | `ModelPackVerifier` (SHA-256; placeholder `0…0` until real assets) |
| Delete | Delete action removes files + prefs |
| No main-thread I/O | `ModelPackDownloader` on `DispatcherProvider.io` |
| Pause / resume | Planned (cancel + retry today) |

Files are stored under: `files/model_packs/<packId>/`.

## Phone-only mode (planned UI: toggle live, camera screen Phase 5)

Settings → Offline AI packs → **Use phone camera**:

- Full-screen rear camera (CameraX).
- Coloured bounding box per detected object.
- Same detection → narration → TTS pipeline as glasses mode.

Domain types: `DetectedObject`, `SceneAnalysis` in `:domain`.
