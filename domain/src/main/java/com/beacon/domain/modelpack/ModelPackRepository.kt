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

    suspend fun deletePack(id: ModelPackId)

    suspend fun setWifiOnlyDownload(enabled: Boolean)

    fun wifiOnlyDownload(): Flow<Boolean>

    suspend fun setDefaultNarrationPack(id: ModelPackId)

    suspend fun setDefaultVoicePack(id: ModelPackId)

    fun defaultNarrationPack(): Flow<ModelPackId?>

    fun defaultVoicePack(): Flow<ModelPackId?>
}
