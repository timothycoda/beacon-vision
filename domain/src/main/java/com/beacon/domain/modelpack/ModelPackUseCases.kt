package com.beacon.domain.modelpack

import com.beacon.domain.guidance.GuidanceLanguageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveModelPacksUseCase @Inject constructor(
    private val repository: ModelPackRepository,
) {
    operator fun invoke(): Flow<List<ModelPackStatus>> = repository.observeAllPacks()
}

class DownloadModelPackUseCase @Inject constructor(
    private val repository: ModelPackRepository,
) {
    suspend operator fun invoke(id: ModelPackId) = repository.startDownload(id)
}

class DeleteModelPackUseCase @Inject constructor(
    private val repository: ModelPackRepository,
) {
    suspend operator fun invoke(id: ModelPackId) = repository.deletePack(id)
}

class CancelModelPackDownloadUseCase @Inject constructor(
    private val repository: ModelPackRepository,
) {
    suspend operator fun invoke(id: ModelPackId) = repository.cancelDownload(id)
}

class PauseModelPackDownloadUseCase @Inject constructor(
    private val repository: ModelPackRepository,
) {
    suspend operator fun invoke(id: ModelPackId) = repository.pauseDownload(id)
}

class ReconcileModelPackDownloadsUseCase @Inject constructor(
    private val repository: ModelPackRepository,
) {
    suspend operator fun invoke() = repository.reconcileInterruptedDownloads()
}

class SetModelPackWifiOnlyUseCase @Inject constructor(
    private val repository: ModelPackRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setWifiOnlyDownload(enabled)
    fun observe(): Flow<Boolean> = repository.wifiOnlyDownload()
}

class SetDefaultNarrationPackUseCase @Inject constructor(
    private val repository: ModelPackRepository,
) {
    suspend operator fun invoke(id: ModelPackId) = repository.setDefaultNarrationPack(id)
}

class SetDefaultVoicePackUseCase @Inject constructor(
    private val repository: ModelPackRepository,
) {
    suspend operator fun invoke(id: ModelPackId) = repository.setDefaultVoicePack(id)
}

class ObserveActiveModelPacksSummaryUseCase @Inject constructor(
    private val modelPacks: ModelPackRepository,
    private val guidanceLanguage: GuidanceLanguageRepository,
) {
    operator fun invoke(): Flow<ActiveModelPacksSummary> =
        combine(
            modelPacks.observeAllPacks(),
            guidanceLanguage.language,
        ) { packs, language ->
            ActiveModelPacksSummary(
                intelligenceName = packs
                    .firstOrNull { it.isDefaultNarration }
                    ?.displayName
                    ?: BUILTIN_INTELLIGENCE,
                voiceName = packs.firstOrNull { it.isDefaultVoice }?.displayName,
                guidanceLanguage = language,
            )
        }

    private companion object {
        const val BUILTIN_INTELLIGENCE = "Built-in vision"
    }
}
