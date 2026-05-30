package com.beacon.domain.voice

/**
 * Pure, offline intent matcher: turns a free-form transcript into a
 * [VoiceCommand]. Deterministic and side-effect free so it is fully unit
 * testable. Order matters — safety-critical phrases (emergency, stop) win over
 * everything else.
 */
object VoiceCommandMatcher {

    fun match(transcript: String): VoiceCommand {
        val text = normalize(transcript)
        if (text.isBlank()) return VoiceCommand.Unknown(transcript.trim())

        // Safety first: emergency and stop must never be shadowed.
        if (containsAny(text, EMERGENCY)) return VoiceCommand.Emergency
        if (containsAny(text, STOP)) return VoiceCommand.StopSpeaking

        if (containsAny(text, READ)) return VoiceCommand.ReadText
        if (containsAny(text, AHEAD)) return VoiceCommand.WhatIsAhead
        if (containsAny(text, WALKING)) return VoiceCommand.WalkingMode
        if (containsAny(text, VOICE_SETTINGS)) return VoiceCommand.VoiceSettings
        if (containsAny(text, STATUS)) return VoiceCommand.GlassesStatus
        if (containsAny(text, HOME)) return VoiceCommand.GoHome

        return VoiceCommand.Unknown(transcript.trim())
    }

    private fun normalize(raw: String): String =
        raw.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun containsAny(text: String, phrases: List<String>): Boolean =
        phrases.any { phrase ->
            text == phrase || text.contains(" $phrase ") ||
                text.startsWith("$phrase ") || text.endsWith(" $phrase") ||
                text.contains(phrase)
        }

    private val EMERGENCY = listOf(
        "emergency", "help me", "get help", "i need help", "call for help",
        "send help", "sos", "danger",
    )
    private val STOP = listOf(
        "stop", "be quiet", "quiet", "shush", "silence", "stop talking",
    )
    private val READ = listOf(
        "read", "read this", "read it", "read text", "read the", "scan text",
        "what does it say", "what does this say",
    )
    private val AHEAD = listOf(
        "what is ahead", "what's ahead", "whats ahead", "look ahead", "ahead",
        "describe", "what is in front", "what's in front", "what do you see",
        "what is around", "look around", "scene",
    )
    private val WALKING = listOf(
        "walking mode", "walk mode", "start walking", "walking", "walk", "guide me",
    )
    private val VOICE_SETTINGS = listOf(
        "voice settings", "change voice", "voice options", "settings voice",
    )
    private val STATUS = listOf(
        "battery", "glasses status", "status", "connection", "are you connected",
        "glasses",
    )
    private val HOME = listOf(
        "home", "go home", "go back", "back", "cancel", "main menu", "never mind",
    )
}
