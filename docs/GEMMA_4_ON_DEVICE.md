# Gemma 4 on-device (Beacon)

Beacon’s optional **Gemma 4** pack uses Google’s official **LiteRT-LM** builds from
Hugging Face — the same stack as the [Google AI Edge Gallery](https://play.google.com/store/apps/details?id=com.google.ai.edge.gallery) app.

## Which model is “Gemma 4 Lite”?

Google does not ship a separate weight named “Gemma 4 Lite”. For phones, the
**edge / on-device** variant is:

| Variant | Use | Android file | Size (approx.) |
|---------|-----|--------------|----------------|
| **Gemma 4 E2B** | Default for Beacon (`GEMMA_4_LITE` pack) | `gemma-4-E2B-it.litertlm` | ~2.4 GB |
| **Gemma 4 E4B** | Higher quality, more RAM (`BALANCED` pack) | `gemma-4-E4B-it.litertlm` | ~3.4 GB |

Base model cards: [google/gemma-4-E2B-it](https://huggingface.co/google/gemma-4-E2B-it),
[google/gemma-4-E4B-it](https://huggingface.co/google/gemma-4-E4B-it).

LiteRT-ready repos (what Beacon downloads):

- [litert-community/gemma-4-E2B-it-litert-lm](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm)
- [litert-community/gemma-4-E4B-it-litert-lm](https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm)

## Direct download URLs (resolve/main)

These are the files Beacon’s model-pack downloader uses:

```
https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm
https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm
```

Checksums and sizes are pinned in `ModelPackCatalog.kt` (from Hugging Face LFS
metadata on `main`).

## Runtime integration (Phase 4)

After download, load the file from app storage, e.g.:

`files/model_packs/gemma_4_lite/gemma-4-E2B-it.litertlm`

Gradle dependency (Google Maven):

```kotlin
implementation("com.google.ai.edge.litertlm:litertlm-android:latest.release")
```

Docs: [LiteRT-LM on Android](https://ai.google.dev/edge/litert-lm/android)

```kotlin
val engineConfig = EngineConfig(
    modelPath = downloadedLitertLmPath,
    backend = Backend.GPU(),
)
```

Initialize `Engine` off the main thread (load can take several seconds).

## License

Apache 2.0 on the `litert-community` repos. You may still need to accept terms on
the base `google/gemma-4-*` model pages on Hugging Face before downloading.

## Production note

On supported devices, Google also exposes Gemma 4 via **Android AI Core** (system
service). Beacon currently uses the **self-hosted download** path so the app
works the same on all supported phones without relying on AI Core availability.
