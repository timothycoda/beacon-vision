package com.beacon.data.vision

import com.beacon.domain.guidance.GuidanceLanguage
import com.beacon.domain.vision.DetectedObject
import java.util.Locale

/**
 * Merges live object detection (person, bike, cup, …) into ML Kit scene narration.
 * Image labeling often misses people that pose/face/object detectors already found.
 */
object PhoneObjectNarration {

    private const val MIN_CONFIDENCE = 0.35f

    fun enrichSpokenSummary(
        sceneSummary: String,
        objects: List<DetectedObject>,
        language: GuidanceLanguage,
    ): String {
        val labels = objects
            .filter { it.confidence >= MIN_CONFIDENCE }
            .sortedByDescending { it.confidence }
            .map { DetectionDisplayLabel.format(it.label, language).lowercase(Locale.getDefault()) }
            .distinct()
            .take(4)
        if (labels.isEmpty()) return sceneSummary

        val sceneLower = sceneSummary.lowercase(Locale.getDefault())
        val missing = labels.filter { term -> !sceneLower.contains(term) }
        if (missing.isEmpty()) return sceneSummary

        val addition = formatLabelList(missing, language)
        return when (language) {
            GuidanceLanguage.English -> insertEnglish(sceneSummary, addition)
            GuidanceLanguage.Hausa -> insertHausa(sceneSummary, addition)
        }
    }

    private fun insertEnglish(sceneSummary: String, addition: String): String {
        val marker = "i can see "
        val idx = sceneSummary.lowercase(Locale.getDefault()).indexOf(marker)
        return if (idx >= 0) {
            val start = idx + marker.length
            sceneSummary.substring(0, start) + "$addition, " + sceneSummary.substring(start)
        } else {
            "Ahead of you I can see $addition. $sceneSummary"
        }
    }

    private fun insertHausa(sceneSummary: String, addition: String): String {
        val marker = "na iya ganin "
        val idx = sceneSummary.lowercase(Locale.getDefault()).indexOf(marker)
        return if (idx >= 0) {
            val start = idx + marker.length
            sceneSummary.substring(0, start) + "$addition, " + sceneSummary.substring(start)
        } else {
            "A gaban ka na iya ganin $addition. $sceneSummary"
        }
    }

    private fun formatLabelList(labels: List<String>, language: GuidanceLanguage): String =
        when (labels.size) {
            1 -> labels[0]
            2 -> if (language == GuidanceLanguage.Hausa) "${labels[0]} da ${labels[1]}"
            else "${labels[0]} and ${labels[1]}"
            else -> if (language == GuidanceLanguage.Hausa) {
                labels.dropLast(1).joinToString(", ") + ", da " + labels.last()
            } else {
                labels.dropLast(1).joinToString(", ") + ", and " + labels.last()
            }
        }
}
