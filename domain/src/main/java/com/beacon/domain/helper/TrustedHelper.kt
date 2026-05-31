package com.beacon.domain.helper

import com.beacon.domain.emergency.TrustedContact
import java.util.UUID

data class TrustedHelper(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val phoneNumber: String,
    val whatsappEnabled: Boolean = true,
    val receiveEmergencyWhatsAppAlerts: Boolean = true,
    val preferredLanguage: String? = null,
    val relationship: String? = null,
    val isPrimaryHelper: Boolean = false,
    val isPrimaryEmergencyContact: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    fun toTrustedContact(): TrustedContact = TrustedContact(
        name = displayName,
        phoneNumber = phoneNumber,
    )
}
