package com.beacon.data.modelpack

import com.beacon.core.log.BeaconLog
import com.beacon.domain.modelpack.ModelPackDownloadFailure
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
    private val storageGuard: ModelPackStorageGuard,
    private val downloader: ModelPackDownloader,
    private val verifier: ModelPackVerifier,
    private val networkPolicy: NetworkPolicy,
    private val downloadLauncher: ModelPackDownloadLauncher,
    private val narrationCoordinator: ModelPackNarrationCoordinator,
    private val downloadProgressBus: ModelPackDownloadProgressBus,
) : ModelPackRepository {

    private val downloadMutex = Mutex()
    private var downloadInProgress = false

    @Volatile
    private var stopRequested: DownloadStop? = null

    private enum class DownloadStop {
        Pause,
        Cancel,
    }

    override fun observeAllPacks(): Flow<List<ModelPackStatus>> {
        val packFlows = ModelPackCatalog.visibleInUi.map { entry ->
            combine(
                combine(
                    prefs.installState(entry.id),
                    prefs.bytesDownloaded(entry.id),
                    prefs.errorMessage(entry.id),
                    prefs.failureKind(entry.id),
                ) { state, bytes, error, failure -> Quad(state, bytes, error, failure) },
                combine(
                    prefs.wifiOnlyDownload,
                    prefs.defaultNarrationPack,
                    prefs.defaultVoicePack,
                ) { wifiOnly, defaultNarration, defaultVoice ->
                    Triple(wifiOnly, defaultNarration, defaultVoice)
                },
            ) { quad, (wifiOnly, defaultNarration, defaultVoice) ->
                entry.toStatus(
                    quad.a,
                    quad.b,
                    quad.c,
                    quad.d,
                    wifiOnly,
                    defaultNarration,
                    defaultVoice,
                )
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
            prefs.setFailed(
                id,
                ModelPackDownloadFailure.UNAVAILABLE.userMessage(),
                ModelPackDownloadFailure.UNAVAILABLE,
            )
            return
        }

        if (!storageGuard.hasSpaceFor(entry.sizeBytes)) {
            prefs.setFailed(
                id,
                ModelPackDownloadFailure.INSUFFICIENT_STORAGE.userMessage(),
                ModelPackDownloadFailure.INSUFFICIENT_STORAGE,
            )
            return
        }

        val wifiOnly = prefs.wifiOnlyDownload.first()
        if (wifiOnly && !networkPolicy.isUnmetered()) {
            prefs.setFailed(
                id,
                ModelPackDownloadFailure.WIFI_REQUIRED.userMessage(),
                ModelPackDownloadFailure.WIFI_REQUIRED,
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
            stopRequested = null
            downloadMutex.withLock { downloadInProgress = false }
            downloadProgressBus.clear()
            downloadLauncher.stopForegroundDownload()
        }
    }

    private suspend fun runDownload(id: ModelPackId, entry: ModelPackEntry) {
        try {
            if (!storageGuard.hasSpaceFor(entry.sizeBytes)) {
                prefs.setFailed(
                    id,
                    ModelPackDownloadFailure.INSUFFICIENT_STORAGE.userMessage(),
                    ModelPackDownloadFailure.INSUFFICIENT_STORAGE,
                )
                return
            }

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
                error("Download verification failed — file may be corrupt.")
            }
            prefs.setInstalled(id)
            maybeSetDefaultAfterInstall(id, entry.kind)
            narrationCoordinator.onPackInstalled()
        } catch (e: CancellationException) {
            when (stopRequested) {
                DownloadStop.Pause -> {
                    val bytes = storage.partialFile(id, entry.fileName)
                        .takeIf { it.isFile }
                        ?.length()
                        ?: prefs.bytesDownloaded(id).first()
                    prefs.setPaused(id, bytes)
                }
                DownloadStop.Cancel, null -> {
                    prefs.clearPack(id)
                    storage.deletePack(id)
                    narrationCoordinator.onPackRemoved()
                }
            }
            throw e
        } catch (e: Exception) {
            BeaconLog.e(TAG, "download failed for $id", e)
            val failure = ModelPackDownloadErrors.classify(e)
            val message = ModelPackDownloadErrors.userMessage(failure, e)
            prefs.setFailed(id, message, failure)
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

    override suspend fun pauseDownload(id: ModelPackId) {
        val entry = ModelPackCatalog.find(id) ?: return
        stopRequested = DownloadStop.Pause
        downloader.cancel()
        downloadMutex.withLock { downloadInProgress = false }
        downloadProgressBus.clear()
        downloadLauncher.stopForegroundDownload()
        val part = storage.partialFile(id, entry.fileName)
        val bytes = part.takeIf { it.isFile }?.length() ?: prefs.bytesDownloaded(id).first()
        prefs.setPaused(id, bytes)
    }

    override suspend fun cancelDownload(id: ModelPackId) {
        stopRequested = DownloadStop.Cancel
        downloader.cancel()
        downloadMutex.withLock { downloadInProgress = false }
        downloadProgressBus.clear()
        downloadLauncher.stopForegroundDownload()
        prefs.clearPack(id)
        storage.deletePack(id)
        narrationCoordinator.onPackRemoved()
    }

    override suspend fun reconcileInterruptedDownloads() {
        downloadMutex.withLock { downloadInProgress = false }
        downloadProgressBus.clear()
        downloadLauncher.stopForegroundDownload()
        downloader.cancel()

        ModelPackCatalog.visibleInUi.forEach { entry ->
            if (prefs.installState(entry.id).first() != ModelPackInstallState.Downloading) return@forEach
            val part = storage.partialFile(entry.id, entry.fileName)
            val bytes = part.takeIf { it.isFile }?.length() ?: 0L
            prefs.setPaused(entry.id, bytes)
            BeaconLog.i(TAG, "Reconciled interrupted download for ${entry.id} at $bytes bytes")
        }
    }

    override suspend fun deletePack(id: ModelPackId) {
        stopRequested = DownloadStop.Cancel
        downloader.cancel()
        downloadMutex.withLock { downloadInProgress = false }
        downloadProgressBus.clear()
        downloadLauncher.stopForegroundDownload()
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
        failureKind: ModelPackDownloadFailure?,
        wifiOnly: Boolean,
        defaultNarration: ModelPackId?,
        defaultVoice: ModelPackId?,
    ): ModelPackStatus {
        val resolvedState = when {
            state == ModelPackInstallState.NotInstalled &&
                storage.isInstalled(id, fileName) -> ModelPackInstallState.Installed
            else -> state
        }
        val pausedBytes = if (resolvedState == ModelPackInstallState.Paused) {
            val part = storage.partialFile(id, fileName)
            maxOf(bytesDownloaded, part.takeIf { it.isFile }?.length() ?: 0L)
        } else {
            bytesDownloaded
        }
        return ModelPackStatus(
            id = id,
            displayName = displayName,
            capability = capability,
            ramHint = ramHint,
            kind = kind,
            sizeBytes = sizeBytes,
            state = resolvedState,
            bytesDownloaded = pausedBytes,
            errorMessage = errorMessage,
            failureKind = failureKind,
            wifiOnly = wifiOnly,
            isDefaultNarration = defaultNarration == id && kind == ModelPackKind.Narration,
            isDefaultVoice = defaultVoice == id && kind == ModelPackKind.Voice,
        )
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    private companion object {
        const val TAG = "ModelPackRepository"
        const val PROGRESS_STEP_BYTES = 512L * 1024L
        const val PROGRESS_INTERVAL_MS = 500L
    }
}
