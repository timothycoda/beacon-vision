package com.beacon.domain.accessibility

import kotlinx.coroutines.flow.Flow

interface AccessibilitySettingsRepository {
    val settings: Flow<AccessibilitySettings>
    suspend fun update(transform: (AccessibilitySettings) -> AccessibilitySettings)
}
