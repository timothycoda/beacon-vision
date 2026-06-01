package com.beacon.domain.device

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveGuidanceInputModeUseCase @Inject constructor(
    private val preferences: DevicePreferences,
) {
    operator fun invoke(): Flow<GuidanceInputMode> = preferences.guidanceInputMode()
}

class SetGuidanceInputModeUseCase @Inject constructor(
    private val preferences: DevicePreferences,
) {
    suspend operator fun invoke(mode: GuidanceInputMode) = preferences.setGuidanceInputMode(mode)
}

class SetPhoneOnlyModeUseCase @Inject constructor(
    private val preferences: DevicePreferences,
) {
    suspend operator fun invoke(enabled: Boolean) = preferences.setPhoneOnlyMode(enabled)
}
