package com.beacon.domain.text

/**
 * Text extracted from a captured image. [fullText] is shown on screen;
 * [spokenText] is what we pass to TTS (may be shortened for very long blocks).
 */
data class RecognizedText(
    val fullText: String,
    val spokenText: String,
)
