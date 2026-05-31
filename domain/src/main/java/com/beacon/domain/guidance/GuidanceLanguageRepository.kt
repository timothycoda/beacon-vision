package com.beacon.domain.guidance

import kotlinx.coroutines.flow.Flow

interface GuidanceLanguageRepository {
    val language: Flow<GuidanceLanguage>
    suspend fun setLanguage(language: GuidanceLanguage)
}
