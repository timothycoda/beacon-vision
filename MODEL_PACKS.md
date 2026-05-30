# Offline AI Model Packs

Planned for later phases (Phase 3+). Models are not bundled in the base APK and
are downloaded on demand via "Download Offline AI Pack".

| Pack | Purpose | Target devices |
|------|---------|----------------|
| Lite Offline Pack | Basic OCR, basic object detection, English TTS/STT, core safety commands | Low / mid-range phones |
| Balanced Offline Pack | Better scene understanding, OCR, offline language model, better TTS | 6GB+ RAM phones |
| Hausa Voice Pack | Offline Hausa TTS (Meta MMS-TTS / converted mobile runtime) | Optional, Hausa users |

Requirements: pause/resume, checksum verification, versioning, deletion to free
storage, Wi-Fi-only option, and no main-thread blocking during download/load.
