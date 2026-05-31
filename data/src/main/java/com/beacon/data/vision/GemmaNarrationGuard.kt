package com.beacon.data.vision

/**
 * When Gemma is prompted for Hausa it sometimes still replies in English; keep ML Kit
 * Hausa as fallback instead of speaking English in Hausa guidance mode.
 */
internal object GemmaNarrationGuard {

  private val englishOpeners =
      listOf(
          "ahead of you",
          "ahead ",
          "there is",
          "there are",
          "i can see",
          "you can see",
          "i see",
          "in front of you",
          "looks like",
          "it appears",
          "this is a",
          "this looks",
      )

  fun isLikelyHausa(line: String): Boolean {
    val trimmed = line.trim()
    if (trimmed.isBlank()) return false
    val lower = trimmed.lowercase()
    if (lower.startsWith("a gaban") || lower.startsWith("ban tabbata")) return true
    if (lower.startsWith("da fatan")) return true
    return englishOpeners.none { lower.startsWith(it) }
  }
}
