package com.beacon.domain.whatsapp

import com.beacon.domain.emergency.EmergencyLocation
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object WhatsAppEmergencyMessageBuilder {

    private val timeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z").withZone(ZoneId.systemDefault())

    fun build(
        location: EmergencyLocation?,
        sceneSummary: String?,
        sentAt: Instant = Instant.now(),
    ): String {
        val parts = mutableListOf(
            "🚨 Beacon Emergency Alert",
            "",
            "I may need urgent help. Please call me immediately or check on me.",
            "",
        )
        location?.mapsUrl?.let { parts += "📍 Location: $it" }
        sceneSummary?.trim()?.takeIf { it.isNotBlank() }?.let {
            parts += "Last Beacon summary: $it"
        }
        parts += "Time: ${timeFormatter.format(sentAt)}"
        parts += ""
        parts += "Sent from Beacon."
        return parts.joinToString("\n")
    }
}
