package com.beacon.domain.safety

import com.beacon.domain.guidance.GuidanceLanguage
import com.beacon.domain.vision.SceneLabel

/** Formats scene descriptions with safety-first wording (see SAFETY.md). */
object SceneSafety {

    const val LOW_CONFIDENCE_THRESHOLD = 0.65f

    fun spokenSummary(
        labels: List<SceneLabel>,
        baseSummary: String,
        language: GuidanceLanguage = GuidanceLanguage.English,
    ): String {
        if (labels.isEmpty()) {
            return when (language) {
                GuidanceLanguage.Hausa ->
                    "Ban tabbata abin da ke gaba ba. Da fatan za a sake dubawa."
                GuidanceLanguage.English ->
                    "I am not sure what is ahead. Please scan again."
            }
        }
        val topConfidence = labels.maxOfOrNull { it.confidence } ?: 0f
        return if (topConfidence < LOW_CONFIDENCE_THRESHOLD) {
            when (language) {
                GuidanceLanguage.Hausa ->
                    "Ban tabbata ba. $baseSummary Da fatan za a sake dubawa idan bai yi daidai ba."
                GuidanceLanguage.English ->
                    "I am not sure. $baseSummary Please scan again if that does not sound right."
            }
        } else {
            baseSummary
        }
    }
}
