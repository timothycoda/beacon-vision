package com.beacon.domain.emergency

data class TrustedContact(
    val name: String,
    val phoneNumber: String,
)

data class EmergencyLocation(
    val latitude: Double,
    val longitude: Double,
) {
    val mapsUrl: String get() = "https://maps.google.com/?q=$latitude,$longitude"
}

/** Ready to hand off to the system SMS app — user taps Send there. */
data class EmergencyAlertDraft(
    val phoneNumber: String,
    val message: String,
)
