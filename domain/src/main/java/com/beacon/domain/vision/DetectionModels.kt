package com.beacon.domain.vision

/**
 * One detected object with normalised bounds (0..1 relative to image width/height).
 * Used for phone-only full-screen overlay (Phase 5); colours assigned in UI layer.
 */
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class DetectedObject(
    val label: String,
    val confidence: Float,
    val bounds: NormalizedRect,
    /** Stable index 0..N for mapping to overlay colours. */
    val colorIndex: Int,
)

/**
 * Scene understanding with optional spatial boxes for visual overlay.
 */
data class SceneAnalysis(
    val spokenSummary: String,
    val labels: List<SceneLabel> = emptyList(),
    val objects: List<DetectedObject> = emptyList(),
)
