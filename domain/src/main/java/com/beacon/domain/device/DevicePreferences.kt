package com.beacon.domain.device

import kotlinx.coroutines.flow.Flow

/**
 * How Beacon captures images for guidance features.
 */
enum class GuidanceInputMode {
    /** Capture stills from paired HeyCyan / X01 glasses (default when paired). */
    Glasses,
    /** Full-screen phone camera with on-screen detection overlays. */
    PhoneCamera,
}

interface DevicePreferences {
    fun guidanceInputMode(): Flow<GuidanceInputMode>

    /** Persists the user's chosen default (onboarding); not toggled when leaving phone camera. */
    suspend fun setGuidanceInputMode(mode: GuidanceInputMode)

    suspend fun setPhoneOnlyMode(enabled: Boolean)
}
