package com.beacon.data.modelpack

import com.beacon.domain.modelpack.ModelPackId
import com.beacon.domain.modelpack.ModelPackKind

/**
 * Pack metadata for post-install downloads.
 *
 * User-facing names hide vendor model IDs (Gemma, MMS-TTS, etc.).
 */
data class ModelPackEntry(
    val id: ModelPackId,
    val displayName: String,
    val capability: String,
    val ramHint: String,
    val kind: ModelPackKind,
    val sizeBytes: Long,
    val downloadUrl: String,
    val sha256Hex: String,
    val fileName: String,
    /** When true, [sha256Hex] may be all zeros and install is verified by size only. */
    val verifySizeOnly: Boolean = false,
    val huggingFaceRepo: String? = null,
    val showInUi: Boolean = true,
)

object ModelPackCatalog {
    private const val HF_E2B_REPO = "litert-community/gemma-4-E2B-it-litert-lm"
    private const val HF_E4B_REPO = "litert-community/gemma-4-E4B-it-litert-lm"
    private const val HF_MMS_HAU = "facebook/mms-tts-hau"

    val all: List<ModelPackEntry> = listOf(
        ModelPackEntry(
            id = ModelPackId.LITE,
            displayName = "Beacon Lite",
            capability = "Extra detectors (coming soon)",
            ramHint = "~2 GB RAM",
            kind = ModelPackKind.Utility,
            sizeBytes = 48L * 1024 * 1024,
            downloadUrl = "https://example.com/beacon/packs/lite-1.0.0.pack",
            sha256Hex = PLACEHOLDER_SHA256,
            fileName = "lite.pack",
            showInUi = false,
        ),
        ModelPackEntry(
            id = ModelPackId.BALANCED,
            displayName = "Beacon Vision Pro",
            capability = "Highest-quality scene descriptions",
            ramHint = "~8 GB RAM · ~3.4 GB storage",
            kind = ModelPackKind.Narration,
            sizeBytes = 3_659_530_240L,
            downloadUrl = "https://huggingface.co/$HF_E4B_REPO/resolve/main/gemma-4-E4B-it.litertlm",
            sha256Hex = "0b2a8980ce155fd97673d8e820b4d29d9c7d99b8fa6806f425d969b145bd52e0",
            fileName = "gemma-4-E4B-it.litertlm",
            huggingFaceRepo = HF_E4B_REPO,
        ),
        ModelPackEntry(
            id = ModelPackId.GEMMA_4_LITE,
            displayName = "Beacon Vision Plus",
            capability = "Richer scene descriptions offline",
            ramHint = "~6 GB RAM · ~2.4 GB storage",
            kind = ModelPackKind.Narration,
            sizeBytes = 2_588_147_712L,
            downloadUrl = "https://huggingface.co/$HF_E2B_REPO/resolve/main/gemma-4-E2B-it.litertlm",
            sha256Hex = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
            fileName = "gemma-4-E2B-it.litertlm",
            huggingFaceRepo = HF_E2B_REPO,
        ),
        ModelPackEntry(
            id = ModelPackId.HAUSA_VOICE,
            displayName = "Beacon Hausa Voice",
            capability = "Speak guidance in Hausa",
            ramHint = "~2 GB RAM · ~140 MB storage",
            kind = ModelPackKind.Voice,
            sizeBytes = 145_224_440L,
            downloadUrl = "https://huggingface.co/$HF_MMS_HAU/resolve/main/model.safetensors",
            sha256Hex = PLACEHOLDER_SHA256,
            fileName = "mms-tts-hau.safetensors",
            verifySizeOnly = true,
            huggingFaceRepo = HF_MMS_HAU,
        ),
    )

    val visibleInUi: List<ModelPackEntry> = all.filter { it.showInUi }

    fun find(id: ModelPackId): ModelPackEntry? = all.firstOrNull { it.id == id }

    private const val PLACEHOLDER_SHA256 =
        "0000000000000000000000000000000000000000000000000000000000000000"
}
