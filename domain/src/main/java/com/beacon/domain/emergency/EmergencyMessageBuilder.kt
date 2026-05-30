package com.beacon.domain.emergency

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Builds a short, factual emergency SMS. No invented medical or identity claims.
 */
object EmergencyMessageBuilder {

    private val timeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z").withZone(ZoneId.systemDefault())

    fun build(
        contactName: String?,
        location: EmergencyLocation?,
        sceneSummary: String?,
        sentAt: Instant = Instant.now(),
    ): String {
        val lines = mutableListOf<String>()
        lines += "EMERGENCY — Beacon alert. I need help."
        contactName?.takeIf { it.isNotBlank() }?.let {
            lines += "Contact: $it."
        }
        if (location != null) {
            lines += "Location: ${location.mapsUrl}"
        } else {
            lines += "Location: unavailable on this phone."
        }
        val scene = sceneSummary?.trim().orEmpty()
        if (scene.isNotBlank()) {
            lines += "Around me: $scene"
        }
        lines += "Sent ${timeFormatter.format(sentAt)}."
        lines += "Please call or come if you can."
        return lines.joinToString("\n")
    }
}
