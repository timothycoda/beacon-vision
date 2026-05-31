package com.beacon.domain.guidance

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveGuidanceLanguageUseCase @Inject constructor(
    private val repository: GuidanceLanguageRepository,
) {
    operator fun invoke(): Flow<GuidanceLanguage> = repository.language
}

class SetGuidanceLanguageUseCase @Inject constructor(
    private val repository: GuidanceLanguageRepository,
) {
    suspend operator fun invoke(language: GuidanceLanguage) = repository.setLanguage(language)
}
