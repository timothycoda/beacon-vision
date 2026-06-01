package com.beacon.data.device

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.beacon.domain.device.DevicePreferences
import com.beacon.domain.device.GuidanceInputMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.devicePrefsStore by preferencesDataStore(name = "device_prefs")

@Singleton
class DevicePreferencesImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : DevicePreferences {

    private val store = context.devicePrefsStore

    override fun guidanceInputMode(): Flow<GuidanceInputMode> =
        store.data.map { prefs -> readPreferredMode(prefs) }

    override suspend fun setGuidanceInputMode(mode: GuidanceInputMode) {
        store.edit {
            it[KEY_PREFERRED_INPUT] = mode.storageValue
            it.remove(KEY_PHONE_ONLY)
        }
    }

    override suspend fun setPhoneOnlyMode(enabled: Boolean) {
        setGuidanceInputMode(
            if (enabled) GuidanceInputMode.PhoneCamera else GuidanceInputMode.Glasses,
        )
    }

    private fun readPreferredMode(prefs: Preferences): GuidanceInputMode {
        prefs[KEY_PREFERRED_INPUT]?.let { stored ->
            return GuidanceInputMode.entries.firstOrNull { it.storageValue == stored }
                ?: GuidanceInputMode.Glasses
        }
        if (prefs[KEY_PHONE_ONLY] == true) return GuidanceInputMode.PhoneCamera
        return GuidanceInputMode.Glasses
    }

    private companion object {
        val KEY_PREFERRED_INPUT = stringPreferencesKey("preferred_guidance_input")
        val KEY_PHONE_ONLY = booleanPreferencesKey("phone_only_mode")
    }

    private val GuidanceInputMode.storageValue: String
        get() = when (this) {
            GuidanceInputMode.Glasses -> "glasses"
            GuidanceInputMode.PhoneCamera -> "phone"
        }
}
