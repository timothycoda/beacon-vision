# Narration, detection, and memory

## Can Gemma speak Hausa?

Yes. `GemmaNarrationEngine` already prompts for **Hausa-only** replies (`A gaban ka…`, under 25 words).  
`CompositeSceneDescriber` now uses Gemma for Hausa when the intelligence pack is installed, and **falls back to ML Kit Hausa** if Gemma answers in English (`GemmaNarrationGuard`).

Voice is still **Hausa MMS** when the voice pack is installed; Gemma only writes the sentence.

## Can ML Kit detect “everything”?

ML Kit’s image labeler ships a **fixed Google model** (~1k general categories). You cannot drop in a custom “very large” label list in-app.

What we do instead:

| Layer | Role | Memory |
|-------|------|--------|
| ML Kit labels | Fast baseline (up to 5 labels, 0.55 confidence) | Small, bundled |
| ML Kit **object detection** | Phone camera boxes (separate API) | Moderate |
| Optional **Balanced** pack (future) | Stronger detector when downloaded | User-opt-in |
| **Gemma** | Natural sentence from labels (not new detections) | Large; one run ~every 8s in loops |

More **Hausa names** for common ML Kit English tags live in `HausaLabelTranslator` (offline map, not a bigger ML model).

## Speed and memory rules

1. **One heavy model at a time** — describe path: ML Kit → (optional) Gemma → speak MMS/TTS. No parallel Gemma + MMS inference.
2. **Gemma throttle** — continuous modes call Gemma at most once per **8 seconds**; ML Kit speaks between runs.
3. **Gemma loaded once** per process while the narration pack is set; released on pack change via `GemmaNarrationEngine.reset()`.
4. **Hausa MMS** loads ONNX on first Hausa speech; JNI + model stay until `release()` (e.g. switching back to English).
5. **No echo** — one `AudioTrack` for MMS; phone/walking loops `awaitNotSpeaking()` before the next phrase.

## Tuning detection further (later)

- Lower `CONFIDENCE_THRESHOLD` slightly → more labels, more false positives.
- Add a downloadable **object-detection** pack (YOLO / ML Kit custom) for “miss fewer objects”.
- Use Gemma only on **“What is ahead?”** (single shot), not walking/phone loops, for maximum speed.
