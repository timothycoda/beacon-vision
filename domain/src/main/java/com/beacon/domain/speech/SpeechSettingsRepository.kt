package com.beacon.domain.speech

import kotlinx.coroutines.flow.Flow

interface SpeechSettingsRepository {
    val settings: Flow<SpeechSettings>
    suspend fun save(settings: SpeechSettings)
    suspend fun resetToSystemDefault()
}
