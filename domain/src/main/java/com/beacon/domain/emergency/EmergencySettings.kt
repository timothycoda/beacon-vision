package com.beacon.domain.emergency

/** User preferences for the emergency feature (persisted in DataStore). */
data class EmergencySettings(
    val includeSceneFromGlasses: Boolean = true,
    val autoSendSms: Boolean = true,
    /** When true, hands-free emergency waits a few seconds before sending SMS. */
    val confirmBeforeSend: Boolean = false,
)
