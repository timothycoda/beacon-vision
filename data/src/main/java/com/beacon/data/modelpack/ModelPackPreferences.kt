package com.beacon.data.modelpack

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.beacon.domain.modelpack.ModelPackId
import com.beacon.domain.modelpack.ModelPackInstallState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.modelPackDataStore by preferencesDataStore(name = "model_pack_prefs")

@Singleton
class ModelPackPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.modelPackDataStore

    val wifiOnlyDownload: Flow<Boolean> =
        store.data.map { it[KEY_WIFI_ONLY] ?: true }

    val defaultNarrationPack: Flow<ModelPackId?> =
        store.data.map { prefs ->
            prefs[KEY_DEFAULT_NARRATION]?.let { ModelPackId.fromStorageKey(it) }
        }

    val defaultVoicePack: Flow<ModelPackId?> =
        store.data.map { prefs ->
            prefs[KEY_DEFAULT_VOICE]?.let { ModelPackId.fromStorageKey(it) }
        }

    suspend fun setWifiOnlyDownload(enabled: Boolean) {
        store.edit { it[KEY_WIFI_ONLY] = enabled }
    }

    fun installState(id: ModelPackId): Flow<ModelPackInstallState> =
        store.data.map { prefs ->
            when (prefs[stringKeyState(id)]) {
                STATE_INSTALLED -> ModelPackInstallState.Installed
                STATE_DOWNLOADING -> ModelPackInstallState.Downloading
                STATE_FAILED -> ModelPackInstallState.Failed
                else -> ModelPackInstallState.NotInstalled
            }
        }

    fun bytesDownloaded(id: ModelPackId): Flow<Long> =
        store.data.map { it[longKeyBytes(id)] ?: 0L }

    fun errorMessage(id: ModelPackId): Flow<String?> =
        store.data.map { it[stringKeyError(id)] }

    suspend fun setDownloading(id: ModelPackId, bytesAlreadyDownloaded: Long = 0L) {
        store.edit {
            it[stringKeyState(id)] = STATE_DOWNLOADING
            it.remove(stringKeyError(id))
            it[longKeyBytes(id)] = bytesAlreadyDownloaded.coerceAtLeast(0L)
        }
    }

    suspend fun updateProgress(id: ModelPackId, bytesDownloaded: Long) {
        store.edit { it[longKeyBytes(id)] = bytesDownloaded }
    }

    suspend fun setInstalled(id: ModelPackId) {
        store.edit {
            it[stringKeyState(id)] = STATE_INSTALLED
            it.remove(stringKeyError(id))
        }
    }

    suspend fun setFailed(id: ModelPackId, message: String) {
        store.edit {
            it[stringKeyState(id)] = STATE_FAILED
            it[stringKeyError(id)] = message
        }
    }

    suspend fun clearPack(id: ModelPackId) {
        store.edit {
            it.remove(stringKeyState(id))
            it.remove(longKeyBytes(id))
            it.remove(stringKeyError(id))
        }
    }

    suspend fun setDefaultNarrationPack(id: ModelPackId?) {
        store.edit {
            if (id != null) {
                it[KEY_DEFAULT_NARRATION] = id.storageKey
            } else {
                it.remove(KEY_DEFAULT_NARRATION)
            }
        }
    }

    suspend fun setDefaultVoicePack(id: ModelPackId?) {
        store.edit {
            if (id != null) {
                it[KEY_DEFAULT_VOICE] = id.storageKey
            } else {
                it.remove(KEY_DEFAULT_VOICE)
            }
        }
    }

    private companion object {
        val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only_download")
        val KEY_DEFAULT_NARRATION = stringPreferencesKey("default_narration_pack")
        val KEY_DEFAULT_VOICE = stringPreferencesKey("default_voice_pack")

        const val STATE_INSTALLED = "installed"
        const val STATE_DOWNLOADING = "downloading"
        const val STATE_FAILED = "failed"

        fun stringKeyState(id: ModelPackId) = stringPreferencesKey("pack_${id.storageKey}_state")
        fun longKeyBytes(id: ModelPackId) = longPreferencesKey("pack_${id.storageKey}_bytes")
        fun stringKeyError(id: ModelPackId) = stringPreferencesKey("pack_${id.storageKey}_error")
    }
}
