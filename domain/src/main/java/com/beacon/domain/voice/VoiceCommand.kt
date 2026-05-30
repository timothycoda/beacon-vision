package com.beacon.domain.voice

/**
 * A spoken instruction from the user, resolved from a transcript by
 * [VoiceCommandMatcher]. Each command (except [Unknown]) maps to a single
 * Beacon action so the UI layer can navigate or trigger a feature.
 */
sealed interface VoiceCommand {
    /** Describe what is in front of the user ("what is ahead", "describe"). */
    data object WhatIsAhead : VoiceCommand

    /** Read text aloud ("read this", "read the sign"). */
    data object ReadText : VoiceCommand

    /** Start continuous walking guidance ("walking mode", "start walking"). */
    data object WalkingMode : VoiceCommand

    /** Open the emergency flow ("emergency", "get help"). */
    data object Emergency : VoiceCommand

    /** Open glasses status / battery ("battery", "glasses status"). */
    data object GlassesStatus : VoiceCommand

    /** Open voice settings ("voice settings", "change voice"). */
    data object VoiceSettings : VoiceCommand

    /** Stop any current speech ("stop", "be quiet"). */
    data object StopSpeaking : VoiceCommand

    /** Go back / home ("home", "cancel", "go back"). */
    data object GoHome : VoiceCommand

    /** Nothing recognised; carries the raw transcript for feedback. */
    data class Unknown(val transcript: String) : VoiceCommand
}
