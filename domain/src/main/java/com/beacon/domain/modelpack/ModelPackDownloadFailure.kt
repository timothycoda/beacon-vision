package com.beacon.domain.modelpack

/**
 * Typed reasons a pack download stopped. [userMessage] is safe to show in UI and TTS.
 */
enum class ModelPackDownloadFailure {
    NETWORK,
    WIFI_REQUIRED,
    INSUFFICIENT_STORAGE,
    VERIFICATION_FAILED,
    UNAVAILABLE,
    CANCELLED,
    UNKNOWN,
    ;

    fun userMessage(detail: String? = null): String = when (this) {
        NETWORK -> detail?.takeIf { it.isNotBlank() }
            ?: "Download failed. Check your connection and tap Retry."
        WIFI_REQUIRED ->
            "Connect to Wi‑Fi to download, or turn off “Download on Wi‑Fi only”."
        INSUFFICIENT_STORAGE ->
            "Not enough free storage for this pack. Free space and try again."
        VERIFICATION_FAILED ->
            "Download verification failed. The file may be incomplete — tap Retry."
        UNAVAILABLE ->
            "This pack is not available to download yet."
        CANCELLED -> "Download cancelled."
        UNKNOWN -> detail?.takeIf { it.isNotBlank() } ?: "Download failed."
    }
}
