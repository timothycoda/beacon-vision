package com.beacon.domain.safety

import com.beacon.domain.vision.SceneLabel

/** Formats scene descriptions with safety-first wording (see SAFETY.md). */
object SceneSafety {

    const val LOW_CONFIDENCE_THRESHOLD = 0.65f

    fun spokenSummary(labels: List<SceneLabel>, baseSummary: String): String {
        if (labels.isEmpty()) {
            return "I am not sure what is ahead. Please scan again."
        }
        val topConfidence = labels.maxOfOrNull { it.confidence } ?: 0f
        return if (topConfidence < LOW_CONFIDENCE_THRESHOLD) {
            "I am not sure. $baseSummary Please scan again if that does not sound right."
        } else {
            baseSummary
        }
    }
}
