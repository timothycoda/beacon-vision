package com.beacon.domain.modelpack

import kotlinx.coroutines.flow.Flow

/**
 * Optional offline AI packs — downloaded after the base app is installed.
 */
interface ModelPackRepository {
    fun observeAllPacks(): Flow<List<ModelPackStatus>>

    fun observePack(id: ModelPackId): Flow<ModelPackStatus?>

    fun isInstalled(id: ModelPackId): Boolean

    suspend fun startDownload(id: ModelPackId)

    suspend fun cancelDownload(id: ModelPackId)

    /** Stops the active download but keeps partial files so the user can resume later. */
    suspend fun pauseDownload(id: ModelPackId)

    /**
     * After a process kill or crash, moves orphan [ModelPackInstallState.Downloading]
     * rows to [ModelPackInstallState.Paused] so the UI can offer Resume.
     */
    suspend fun reconcileInterruptedDownloads()

    suspend fun deletePack(id: ModelPackId)

    suspend fun setWifiOnlyDownload(enabled: Boolean)

    fun wifiOnlyDownload(): Flow<Boolean>

    suspend fun setDefaultNarrationPack(id: ModelPackId)

    suspend fun setDefaultVoicePack(id: ModelPackId)

    fun defaultNarrationPack(): Flow<ModelPackId?>

    fun defaultVoicePack(): Flow<ModelPackId?>
}
