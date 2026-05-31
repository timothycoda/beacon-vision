package com.beacon.domain.modelpack

/**
 * Identifiers for optional on-device packs downloaded after install.
 */
enum class ModelPackId(val storageKey: String) {
    LITE("lite"),
    BALANCED("balanced"),
    GEMMA_4_LITE("gemma_4_lite"),
    HAUSA_VOICE("hausa_voice"),
    ;

    companion object {
        fun fromStorageKey(key: String): ModelPackId? =
            entries.firstOrNull { it.storageKey == key }
    }
}

enum class ModelPackInstallState {
    /** Listed but never downloaded on this device. */
    NotInstalled,
    /** Download in progress (see [ModelPackStatus.bytesDownloaded]). */
    Downloading,
    /** Paused by user or after an interrupted session; partial file kept for resume. */
    Paused,
    /** Files verified and ready to load. */
    Installed,
    /** Download or verification failed. */
    Failed,
}

/**
 * User-visible metadata and live install state for one pack.
 */
data class ModelPackStatus(
    val id: ModelPackId,
    val displayName: String,
    val capability: String,
    val ramHint: String,
    val kind: ModelPackKind,
    val sizeBytes: Long,
    val state: ModelPackInstallState,
    val bytesDownloaded: Long = 0L,
    val errorMessage: String? = null,
    val failureKind: ModelPackDownloadFailure? = null,
    /** When true, download only starts on unmetered (Wi‑Fi) networks. */
    val wifiOnly: Boolean = true,
    val isDefaultNarration: Boolean = false,
    val isDefaultVoice: Boolean = false,
)
