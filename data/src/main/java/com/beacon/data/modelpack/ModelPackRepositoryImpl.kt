package com.beacon.data.modelpack

import com.beacon.core.log.BeaconLog
import com.beacon.domain.modelpack.ModelPackId
import com.beacon.domain.modelpack.ModelPackInstallState
import com.beacon.domain.modelpack.ModelPackKind
import com.beacon.domain.modelpack.ModelPackRepository
import com.beacon.domain.modelpack.ModelPackStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelPackRepositoryImpl @Inject constructor(
    private val prefs: ModelPackPreferences,
    private val storage: ModelPackStorage,
    private val downloader: ModelPackDownloader,
    private val verifier: ModelPackVerifier,
    private val networkPolicy: NetworkPolicy,
    private val downloadLauncher: ModelPackDownloadLauncher,
    private val narrationCoordinator: ModelPackNarrationCoordinator,
    private val downloadProgressBus: ModelPackDownloadProgressBus,
) : ModelPackRepository {

    private val downloadMutex = Mutex()
    private var downloadInProgress = false

    override fun observeAllPacks(): Flow<List<ModelPackStatus>> {
        val packFlows = ModelPackCatalog.visibleInUi.map { entry ->
            combine(
                combine(
                    prefs.installState(entry.id),
                    prefs.bytesDownloaded(entry.id),
                    prefs.errorMessage(entry.id),
                ) { state, bytes, error -> Triple(state, bytes, error) },
                combine(
                    prefs.wifiOnlyDownload,
                    prefs.defaultNarrationPack,
                    prefs.defaultVoicePack,
                ) { wifiOnly, defaultNarration, defaultVoice ->
                    Triple(wifiOnly, defaultNarration, defaultVoice)
                },
            ) { (state, bytes, error), (wifiOnly, defaultNarration, defaultVoice) ->
                entry.toStatus(state, bytes, error, wifiOnly, defaultNarration, defaultVoice)
            }
        }
        return combine(packFlows) { statuses -> statuses.toList() }
    }

    override fun observePack(id: ModelPackId): Flow<ModelPackStatus?> =
        observeAllPacks().map { packs -> packs.firstOrNull { it.id == id } }

    override fun isInstalled(id: ModelPackId): Boolean {
        val entry = ModelPackCatalog.find(id) ?: return false
        return storage.isInstalled(id, entry.fileName)
    }

    override suspend fun startDownload(id: ModelPackId) {
        val entry = ModelPackCatalog.find(id) ?: return
        if (downloadInProgress) return

        if (verifier.isPlaceholderChecksum(entry.sha256Hex, entry.verifySizeOnly)) {
            prefs.setFailed(id, "This pack is not available to download yet.")
            return
        }

        val wifiOnly = prefs.wifiOnlyDownload.first()
        if (wifiOnly && !networkPolicy.isUnmetered()) {
            prefs.setFailed(
                id,
                "Connect to Wi‑Fi to download, or turn off “Download on Wi‑Fi only”.",
            )
            return
        }

        val partFile = storage.partialFile(id, entry.fileName)
        prefs.setDownloading(id, partFile.takeIf { it.isFile }?.length() ?: 0L)
        downloadProgressBus.update(
            ModelPackDownloadProgress(id, entry.displayName, partFile.length(), entry.sizeBytes),
        )
        downloadLauncher.startForegroundDownload(id)
    }

    suspend fun performDownload(id: ModelPackId) {
        downloadMutex.withLock {
            if (downloadInProgress) return
            downloadInProgress = true
        }
        val entry = ModelPackCatalog.find(id)
        try {
            if (entry != null) {
                runDownload(id, entry)
            }
        } finally {
            downloadMutex.withLock { downloadInProgress = false }
            downloadProgressBus.clear()
            downloadLauncher.stopForegroundDownload()
        }
    }

    private suspend fun runDownload(id: ModelPackId, entry: ModelPackEntry) {
        try {
            val dest = storage.packFile(id, entry.fileName)
            var lastProgressBytes = 0L
            var lastProgressAtMs = 0L

            downloader.download(entry.downloadUrl, dest) { bytes, _ ->
                currentCoroutineContext().ensureActive()
                val now = System.currentTimeMillis()
                if (bytes - lastProgressBytes >= PROGRESS_STEP_BYTES ||
                    now - lastProgressAtMs >= PROGRESS_INTERVAL_MS
                ) {
                    prefs.updateProgress(id, bytes)
                    downloadProgressBus.update(
                        ModelPackDownloadProgress(id, entry.displayName, bytes, entry.sizeBytes),
                    )
                    lastProgressBytes = bytes
                    lastProgressAtMs = now
                }
            }
            prefs.updateProgress(id, dest.length())

            if (!verifier.matchesExpected(dest, entry.sha256Hex, entry.sizeBytes, entry.verifySizeOnly)) {
                dest.delete()
                error("Download verification failed — file may be corrupt. Try again.")
            }
            prefs.setInstalled(id)
            maybeSetDefaultAfterInstall(id, entry.kind)
            narrationCoordinator.onPackInstalled()
        } catch (e: CancellationException) {
            prefs.clearPack(id)
            storage.deletePack(id)
            narrationCoordinator.onPackRemoved()
            throw e
        } catch (e: Exception) {
            BeaconLog.e(TAG, "download failed for $id", e)
            val hint = when {
                e.message?.contains("HTTP", ignoreCase = true) == true ->
                    "${e.message} Check your connection and try again."
                else -> e.message ?: "Download failed"
            }
            prefs.setFailed(id, hint)
        }
    }

    private suspend fun maybeSetDefaultAfterInstall(id: ModelPackId, kind: ModelPackKind) {
        when (kind) {
            ModelPackKind.Narration -> {
                if (prefs.defaultNarrationPack.first() == null) {
                    prefs.setDefaultNarrationPack(id)
                }
            }
            ModelPackKind.Voice -> {
                if (prefs.defaultVoicePack.first() == null) {
                    prefs.setDefaultVoicePack(id)
                }
            }
            ModelPackKind.Utility -> Unit
        }
    }

    override suspend fun cancelDownload(id: ModelPackId) {
        downloader.cancel()
        downloadMutex.withLock { downloadInProgress = false }
        downloadProgressBus.clear()
        downloadLauncher.stopForegroundDownload()
        prefs.clearPack(id)
        storage.deletePack(id)
        narrationCoordinator.onPackRemoved()
    }

    override suspend fun deletePack(id: ModelPackId) {
        cancelDownload(id)
        if (prefs.defaultNarrationPack.first() == id) prefs.setDefaultNarrationPack(null)
        if (prefs.defaultVoicePack.first() == id) prefs.setDefaultVoicePack(null)
        prefs.clearPack(id)
        storage.deletePack(id)
        narrationCoordinator.onPackRemoved()
    }

    override suspend fun setWifiOnlyDownload(enabled: Boolean) {
        prefs.setWifiOnlyDownload(enabled)
    }

    override fun wifiOnlyDownload(): Flow<Boolean> = prefs.wifiOnlyDownload

    override suspend fun setDefaultNarrationPack(id: ModelPackId) {
        requireNotNull(ModelPackCatalog.find(id)) { "Unknown pack" }
        require(isInstalled(id)) { "Pack must be installed first" }
        prefs.setDefaultNarrationPack(id)
        narrationCoordinator.onPackInstalled()
    }

    override suspend fun setDefaultVoicePack(id: ModelPackId) {
        requireNotNull(ModelPackCatalog.find(id)) { "Unknown pack" }
        require(isInstalled(id)) { "Pack must be installed first" }
        prefs.setDefaultVoicePack(id)
    }

    override fun defaultNarrationPack(): Flow<ModelPackId?> = prefs.defaultNarrationPack

    override fun defaultVoicePack(): Flow<ModelPackId?> = prefs.defaultVoicePack

    private fun ModelPackEntry.toStatus(
        state: ModelPackInstallState,
        bytesDownloaded: Long,
        errorMessage: String?,
        wifiOnly: Boolean,
        defaultNarration: ModelPackId?,
        defaultVoice: ModelPackId?,
    ): ModelPackStatus {
        val resolvedState = when {
            state == ModelPackInstallState.NotInstalled &&
                storage.isInstalled(id, fileName) -> ModelPackInstallState.Installed
            else -> state
        }
        return ModelPackStatus(
            id = id,
            displayName = displayName,
            capability = capability,
            ramHint = ramHint,
            kind = kind,
            sizeBytes = sizeBytes,
            state = resolvedState,
            bytesDownloaded = bytesDownloaded,
            errorMessage = errorMessage,
            wifiOnly = wifiOnly,
            isDefaultNarration = defaultNarration == id && kind == ModelPackKind.Narration,
            isDefaultVoice = defaultVoice == id && kind == ModelPackKind.Voice,
        )
    }

    private companion object {
        const val TAG = "ModelPackRepository"
        const val PROGRESS_STEP_BYTES = 512L * 1024L
        const val PROGRESS_INTERVAL_MS = 500L
    }
}
