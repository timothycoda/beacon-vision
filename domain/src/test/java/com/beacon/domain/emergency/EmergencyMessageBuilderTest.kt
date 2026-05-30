package com.beacon.domain.emergency

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class EmergencyMessageBuilderTest {

    @Test
    fun build_includesEmergencyPrefixAndLocation() {
        val message = EmergencyMessageBuilder.build(
            contactName = "Alex",
            location = EmergencyLocation(6.5244, 3.3792),
            sceneSummary = "Ahead of you I can see a door and stairs.",
            sentAt = Instant.parse("2026-05-29T12:00:00Z"),
        )
        assertTrue(message.contains("EMERGENCY"))
        assertTrue(message.contains("maps.google.com"))
        assertTrue(message.contains("door and stairs"))
        assertTrue(message.contains("Alex"))
    }

    @Test
    fun build_withoutLocation_notesUnavailable() {
        val message = EmergencyMessageBuilder.build(
            contactName = null,
            location = null,
            sceneSummary = null,
            sentAt = Instant.parse("2026-05-29T12:00:00Z"),
        )
        assertTrue(message.contains("Location: unavailable"))
    }
}
