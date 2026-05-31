package com.beacon.data.device

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
        store.data.map { prefs ->
            if (prefs[KEY_PHONE_ONLY] == true) GuidanceInputMode.PhoneCamera
            else GuidanceInputMode.Glasses
        }

    override suspend fun setPhoneOnlyMode(enabled: Boolean) {
        store.edit { it[KEY_PHONE_ONLY] = enabled }
    }

    private companion object {
        val KEY_PHONE_ONLY = booleanPreferencesKey("phone_only_mode")
    }
}
