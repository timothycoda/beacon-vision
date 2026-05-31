package com.beacon.domain.modelpack

enum class ModelPackKind {
    /** Richer scene narration (on-device LLM). */
    Narration,
    /** Offline voice for text-to-speech. */
    Voice,
    /** Other detectors / helpers (future). */
    Utility,
}
