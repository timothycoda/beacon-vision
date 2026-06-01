package com.beacon.domain.accessibility

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAccessibilitySettingsUseCase @Inject constructor(
    private val repository: AccessibilitySettingsRepository,
) {
    operator fun invoke(): Flow<AccessibilitySettings> = repository.settings
}

class UpdateAccessibilitySettingsUseCase @Inject constructor(
    private val repository: AccessibilitySettingsRepository,
) {
    suspend operator fun invoke(transform: (AccessibilitySettings) -> AccessibilitySettings) =
        repository.update(transform)
}
