package com.beacon.data.glasses

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.beacon.domain.glasses.model.GlassesDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.glassesDataStore by preferencesDataStore(name = "glasses_prefs")

/** Persists the last connected device so it can be auto-reconnected / shown. */
@Singleton
class GlassesPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.glassesDataStore

    val lastAddress: Flow<String?> = store.data.map { it[KEY_ADDRESS] }
    val lastName: Flow<String?> = store.data.map { it[KEY_NAME] }

    val lastDevice: Flow<GlassesDevice?> = store.data.map { prefs ->
        val address = prefs[KEY_ADDRESS] ?: return@map null
        val name = prefs[KEY_NAME].orEmpty().ifBlank { "Glasses" }
        GlassesDevice(name = name, address = address, rssi = 0)
    }

    suspend fun save(device: GlassesDevice) {
        store.edit {
            it[KEY_ADDRESS] = device.address
            it[KEY_NAME] = device.name
        }
    }

    suspend fun clear() {
        store.edit {
            it.remove(KEY_ADDRESS)
            it.remove(KEY_NAME)
        }
    }

    private companion object {
        val KEY_ADDRESS = stringPreferencesKey("last_device_address")
        val KEY_NAME = stringPreferencesKey("last_device_name")
    }
}
