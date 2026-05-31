package com.beacon.domain.whatsapp

/**
 * Builds wa.me URLs for text-only WhatsApp handoff. Phone digits only (no +).
 */
object WhatsAppHelperIntentBuilder {

    fun digitsForWaMe(phoneNumber: String): String? {
        val digits = phoneNumber.filter { it.isDigit() }
        return digits.takeIf { it.length >= 7 }
    }

    fun buildWaMeUrl(phoneNumber: String, message: String): String? {
        val digits = digitsForWaMe(phoneNumber) ?: return null
        val encoded = urlEncode(message)
        return "https://wa.me/$digits?text=$encoded"
    }

    fun urlEncode(text: String): String =
        java.net.URLEncoder.encode(text, Charsets.UTF_8.name())

    fun buildHelperMessage(
        template: String,
        lastSceneSummary: String?,
        locationMapsUrl: String?,
    ): String {
        val scene = lastSceneSummary?.trim()?.takeIf { it.isNotBlank() }?.let {
            "Last Beacon summary: $it"
        }.orEmpty()
        val location = locationMapsUrl?.trim()?.takeIf { it.isNotBlank() }?.let {
            "📍 Location: $it"
        }.orEmpty()
        return template
            .replace("{lastSceneSummaryText}", scene)
            .replace("{locationText}", location)
            .replace("  ", " ")
            .trim()
    }
}
