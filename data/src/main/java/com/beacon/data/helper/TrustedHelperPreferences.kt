package com.beacon.data.helper

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.beacon.data.emergency.emergencyPrefs
import com.beacon.domain.helper.TrustedHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrustedHelperPreferences @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson,
) {
    private val store = context.emergencyPrefs

    val helpers: Flow<List<TrustedHelper>> = store.data.map { prefs ->
        decode(prefs[KEY_HELPERS_JSON])
    }

    suspend fun saveAll(helpers: List<TrustedHelper>) {
        store.edit { it[KEY_HELPERS_JSON] = encode(helpers) }
    }

    suspend fun readAll(): List<TrustedHelper> = helpers.first()

    private fun encode(list: List<TrustedHelper>): String = gson.toJson(list)

    private fun decode(json: String?): List<TrustedHelper> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val type = object : TypeToken<List<TrustedHelper>>() {}.type
            gson.fromJson<List<TrustedHelper>>(json, type)
        }.getOrDefault(emptyList())
    }

    private companion object {
        val KEY_HELPERS_JSON = stringPreferencesKey("trusted_helpers_json")
    }
}
