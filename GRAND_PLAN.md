# Beacon Grand Plan

Master roadmap for Beacon (offline vision assistant for X01 / HeyCyan glasses and
phone-only use). Module-level detail lives in [ARCHITECTURE.md](ARCHITECTURE.md);
pack definitions in [MODEL_PACKS.md](MODEL_PACKS.md).

## Vision

A calm, accessibility-first guide: short spoken cues from on-device vision, with
optional richer AI after the user chooses to download packs. Works with smart
glasses **or** the phone camera alone.

---

## Phase status

| Phase | Focus | Status |
|-------|--------|--------|
| 0 | Project + SDK wrapper | Done |
| 1 | Glasses connect (scan, pair, battery, status) | Done |
| 2 | Core guidance (read, ahead, walking, voice, emergency) | Built; harden on hardware |
| **3** | **Optional model packs + download manager** | **In progress (this sprint)** |
| 4 | Gemma 4 Lite integration (on-device LLM for richer narration) | Planned |
| 5 | Phone-only mode (full-screen camera + coloured bounding boxes) | Planned |
| 6 | Release signing, polish, broader language packs | Planned |

---

## Phase 3 — Optional offline model packs (current)

**Principle:** The base APK stays small. ML Kit covers baseline OCR + scene labels.
Everything heavier is **downloaded only after install**, when the user opts in.

### Pack catalogue

| Pack ID | Name | Bundled in APK? | Download |
|---------|------|-----------------|----------|
| `lite` | Lite Offline Pack | No | User-initiated |
| `balanced` | Balanced Offline Pack | No | User-initiated |
| `gemma_4_lite` | Gemma 4 Lite | No | User-initiated (see below) |
| `hausa_voice` | Hausa Voice Pack | No | Optional locale add-on |

### Gemma 4 on-device (E2B / E4B — “lite” = E2B)

Google's mobile edge weights are **Gemma 4 E2B** (~2.4 GB) and **Gemma 4 E4B**
(~3.4 GB), shipped as `.litertlm` for [LiteRT-LM](https://ai.google.dev/edge/litert-lm/android).
There is no separate SKU named “Gemma 4 Lite”; Beacon maps that to **E2B**.

- **Not** included in the APK.
- **Settings → Offline AI packs** → download from Hugging Face (pinned URL + SHA-256 in
  `ModelPackCatalog.kt`). See [docs/GEMMA_4_ON_DEVICE.md](docs/GEMMA_4_ON_DEVICE.md).
- Optional **E4B** pack for higher-quality narration on 6 GB+ RAM devices.
- Runs fully on-device when installed; same privacy stance as [PRIVACY.md](PRIVACY.md).
- `SceneDescriber` / narration pipeline stays one contract: ML Kit by default,
  Gemma-backed implementation when the pack is installed and loaded.
- Download requirements: Wi‑Fi-only option, pause/resume, checksum verify,
  delete to free space (see [MODEL_PACKS.md](MODEL_PACKS.md)).

### Download manager (implemented in `:data`)

- Catalog metadata (version, size, SHA-256, URL) in code until a remote manifest exists.
- Files under `files/model_packs/<packId>/`.
- Progress + state exposed via `ModelPackRepository` / Settings UI.
- No blocking work on the main thread.

---

## Phase 5 — Phone-only mode (non-wearable toggle)

**User story:** User does not have glasses (or turns them off) but still wants
the same guidance: detection, description, and narration.

### Toggle

- **Settings → “Use phone camera”** (stored in `DevicePreferences.phoneOnlyMode`).
- When **on**:
  - Home and guidance features use the **phone rear camera** full-screen
    (CameraX), not glasses capture.
  - **Object detection** draws **distinct coloured bounding boxes** per object
    (stable colour per `trackId` / label hash).
  - **Same pipeline** as glasses mode: detect → `SceneDescription` / labels →
    `Speaker` (TTS to phone speaker or connected audio).
  - Read-this uses phone camera frame → OCR (unchanged recognizer).
- When **off** (default for users who paired glasses):
  - Existing glasses capture path (`CapturePhotoUseCase` → HeyCyan SDK).

### UI sketch

```
┌─────────────────────────────┐
│  [Full-screen camera]       │
│   ┌───┐     ┌──────────┐   │  ← lime / cyan / amber boxes per object
│   │chair│   │  door    │   │
│   └───┘     └──────────┘   │
│                             │
│  Spoken: "Ahead: chair,     │
│  door on the right."        │
└─────────────────────────────┘
     [Stop]  [Repeat]  [Back]
```

### Technical notes

- New module surface: `ObjectDetector` (bounding boxes + labels), optional
  ML Kit Object Detection or pack-backed detector when Balanced is installed.
- `DetectedObject` in `:domain` with normalised bounds + display colour index.
- Compose overlay: `DetectionOverlay` on top of `AndroidView` (CameraX preview).
- Permissions: `CAMERA` when phone-only mode is enabled.
- Glasses connection UI remains available; toggle does not unpair.

---

## Cross-cutting

- **Safety:** [SAFETY.md](SAFETY.md) disclaimers on all spoken guidance.
- **Accessibility:** [`.cursor/rules/beacon-ui.mdc`](.cursor/rules/beacon-ui.mdc).
- **Testing:** [TESTING.md](TESTING.md) + pack download unit tests.

---

## Suggested implementation order

1. Phase 3 foundation — catalog, repository, Settings → Offline AI packs UI.
2. Wire checksum + Wi‑Fi-only + delete; host pack files on CDN/GitHub releases.
3. Phase 4 — Gemma 4 Lite runtime + `GemmaSceneDescriber` behind `SceneDescriber`.
4. Phase 5 — phone-only toggle, CameraX screen, bounding-box overlay.
5. End-to-end QA on low-RAM and 6GB+ devices; update README phase table.
