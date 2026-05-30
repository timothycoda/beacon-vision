package com.beacon.domain.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandMatcherTest {

    @Test
    fun matches_whatIsAhead_variants() {
        assertEquals(VoiceCommand.WhatIsAhead, VoiceCommandMatcher.match("what is ahead"))
        assertEquals(VoiceCommand.WhatIsAhead, VoiceCommandMatcher.match("What's ahead?"))
        assertEquals(VoiceCommand.WhatIsAhead, VoiceCommandMatcher.match("describe what you see"))
        assertEquals(VoiceCommand.WhatIsAhead, VoiceCommandMatcher.match("look around"))
    }

    @Test
    fun matches_readText_variants() {
        assertEquals(VoiceCommand.ReadText, VoiceCommandMatcher.match("read this"))
        assertEquals(VoiceCommand.ReadText, VoiceCommandMatcher.match("Read the sign"))
        assertEquals(VoiceCommand.ReadText, VoiceCommandMatcher.match("what does it say"))
    }

    @Test
    fun matches_walkingMode() {
        assertEquals(VoiceCommand.WalkingMode, VoiceCommandMatcher.match("start walking"))
        assertEquals(VoiceCommand.WalkingMode, VoiceCommandMatcher.match("walking mode please"))
    }

    @Test
    fun emergency_winsOverOtherKeywords() {
        // Even with other words present, emergency must take priority.
        assertEquals(VoiceCommand.Emergency, VoiceCommandMatcher.match("help me read this"))
        assertEquals(VoiceCommand.Emergency, VoiceCommandMatcher.match("emergency"))
        assertEquals(VoiceCommand.Emergency, VoiceCommandMatcher.match("SOS"))
    }

    @Test
    fun stop_isRecognised() {
        assertEquals(VoiceCommand.StopSpeaking, VoiceCommandMatcher.match("stop"))
        assertEquals(VoiceCommand.StopSpeaking, VoiceCommandMatcher.match("be quiet"))
    }

    @Test
    fun status_andSettings() {
        assertEquals(VoiceCommand.GlassesStatus, VoiceCommandMatcher.match("what's my battery"))
        assertEquals(VoiceCommand.VoiceSettings, VoiceCommandMatcher.match("change voice"))
    }

    @Test
    fun home_andBack() {
        assertEquals(VoiceCommand.GoHome, VoiceCommandMatcher.match("go home"))
        assertEquals(VoiceCommand.GoHome, VoiceCommandMatcher.match("never mind"))
    }

    @Test
    fun unknown_keepsTrimmedTranscript() {
        val command = VoiceCommandMatcher.match("  what's the weather  ")
        assertTrue(command is VoiceCommand.Unknown)
        assertEquals("what's the weather", (command as VoiceCommand.Unknown).transcript)
    }

    @Test
    fun blank_isUnknown() {
        assertTrue(VoiceCommandMatcher.match("   ") is VoiceCommand.Unknown)
    }
}
