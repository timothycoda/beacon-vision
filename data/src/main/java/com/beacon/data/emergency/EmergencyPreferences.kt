package com.beacon.data.emergency

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.beacon.domain.emergency.EmergencySettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.emergencyPrefs

    val settings: Flow<EmergencySettings> = store.data.map { prefs ->
        EmergencySettings(
            includeSceneFromGlasses = prefs[KEY_INCLUDE_SCENE] ?: true,
            autoSendSms = prefs[KEY_AUTO_SEND] ?: true,
            confirmBeforeSend = prefs[KEY_CONFIRM_BEFORE_SEND] ?: false,
        )
    }

    suspend fun setIncludeScene(enabled: Boolean) {
        store.edit { it[KEY_INCLUDE_SCENE] = enabled }
    }

    suspend fun setAutoSendSms(enabled: Boolean) {
        store.edit { it[KEY_AUTO_SEND] = enabled }
    }

    suspend fun setConfirmBeforeSend(enabled: Boolean) {
        store.edit { it[KEY_CONFIRM_BEFORE_SEND] = enabled }
    }

    private companion object {
        val KEY_INCLUDE_SCENE = booleanPreferencesKey("include_scene")
        val KEY_AUTO_SEND = booleanPreferencesKey("auto_send_sms")
        val KEY_CONFIRM_BEFORE_SEND = booleanPreferencesKey("confirm_before_send")
    }
}
