package com.beacon.domain.vision

/**
 * A still image captured from the glasses camera. [jpegBytes] is the raw JPEG;
 * [filePath] is where it was persisted (if at all).
 */
class CapturedImage(
    val jpegBytes: ByteArray,
    val filePath: String? = null,
    val timestampMs: Long = System.currentTimeMillis(),
) {
    val sizeBytes: Int get() = jpegBytes.size
}

/** A single thing recognised in a scene. */
data class SceneLabel(
    val text: String,
    val confidence: Float,
)

/**
 * The result of understanding a captured scene. [spokenSummary] is a short,
 * natural sentence suitable for text-to-speech; [labels] is the raw detection.
 */
data class SceneDescription(
    val spokenSummary: String,
    val labels: List<SceneLabel> = emptyList(),
)
