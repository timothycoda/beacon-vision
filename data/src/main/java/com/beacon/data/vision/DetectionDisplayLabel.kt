package com.beacon.data.vision

import com.beacon.domain.guidance.GuidanceLanguage
import java.util.Locale

/**
 * Maps ML Kit coarse object classes and refines labels for on-screen display.
 */
object DetectionDisplayLabel {

    private val vagueObjectClasses = setOf(
        "fashion good",
        "home good",
        "place",
        "unknown",
        "object",
    )

    private val objectClassDisplay = mapOf(
        "fashion good" to "clothing",
        "home good" to "item",
        "food" to "food",
        "plant" to "plant",
        "place" to "furniture",
        "object" to "object",
    )

    fun format(raw: String, language: GuidanceLanguage): String {
        val english = refineEnglish(raw)
        val text = if (language == GuidanceLanguage.Hausa) {
            HausaLabelTranslator.translate(english)
        } else {
            english
        }
        return text.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }

    fun refineWithImageLabels(
        objectLabel: String,
        imageLabels: List<Pair<String, Float>>,
    ): String {
        val lower = objectLabel.lowercase(Locale.getDefault())
        if (lower !in vagueObjectClasses) return objectLabel
        val concrete = imageLabels
            .filter { (_, conf) -> conf >= 0.45f }
            .map { (text, _) -> text.lowercase(Locale.getDefault()) }
            .firstOrNull { label ->
                label !in SKIP_IMAGE_LABELS && label !in vagueObjectClasses
            }
        return concrete ?: objectClassDisplay[lower] ?: objectLabel
    }

    private fun refineEnglish(raw: String): String {
        val lower = raw.lowercase(Locale.getDefault()).trim()
        return objectClassDisplay[lower] ?: lower
    }

    private val SKIP_IMAGE_LABELS = setOf(
        "room",
        "indoor",
        "outdoor",
        "sky",
        "building",
        "infrastructure",
        "furniture",
        "text",
        "font",
        "pattern",
        "material",
        "still life",
        "snapshot",
        "monochrome",
        "darkness",
        "lighting",
    )
}
