package com.beacon.data.guidance

import com.beacon.domain.guidance.GuidanceLanguage
import com.beacon.domain.guidance.GuidanceLanguageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GuidanceLanguageRepositoryImpl @Inject constructor(
    private val prefs: GuidanceLanguagePreferences,
) : GuidanceLanguageRepository {
    override val language: Flow<GuidanceLanguage> = prefs.language
    override suspend fun setLanguage(language: GuidanceLanguage) = prefs.setLanguage(language)
}
