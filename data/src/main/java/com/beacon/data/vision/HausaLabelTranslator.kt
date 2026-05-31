package com.beacon.data.vision

/**
 * Maps common ML Kit English labels to Hausa for offline guidance when Hausa is selected.
 */
object HausaLabelTranslator {
    private val map = mapOf(
        "chair" to "kujera",
        "table" to "tebur",
        "door" to "ƙofa",
        "person" to "mutum",
        "people" to "mutane",
        "car" to "mota",
        "vehicle" to "abun hawa",
        "road" to "hanya",
        "street" to "titi",
        "stairs" to "matakala",
        "stair" to "mataki",
        "wall" to "bango",
        "window" to "taga",
        "tree" to "itace",
        "dog" to "kare",
        "cat" to "cat",
        "bicycle" to "keke",
        "bus" to "bas",
        "truck" to "babbar mota",
        "food" to "abinci",
        "cup" to "kofi",
        "bottle" to "kwalba",
        "phone" to "waya",
        "computer" to "kwamfuta",
        "book" to "littafi",
        "bag" to "jaket",
        "hand" to "hannu",
        "face" to "fuska",
        "building" to "gini",
        "sky" to "sama",
        "water" to "ruwa",
        "grass" to "ciyawa",
        "flower" to "fure",
        "sign" to "alama",
        "pole" to "sandar katako",
        "bench" to "bencin",
        "sidewalk" to "titi na matakan kafa",
        "crosswalk" to "wurin tsallaka",
    )

    fun translate(label: String): String {
        val key = label.trim().lowercase()
        return map[key] ?: label
    }

    fun translateAll(labels: List<String>): List<String> =
        labels.map { translate(it) }
}
