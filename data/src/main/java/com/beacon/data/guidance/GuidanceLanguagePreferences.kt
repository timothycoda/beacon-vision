package com.beacon.data.guidance

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.beacon.domain.guidance.GuidanceLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.guidanceLanguageStore by preferencesDataStore("guidance_language_prefs")

@Singleton
class GuidanceLanguagePreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val store = context.guidanceLanguageStore

    val language: Flow<GuidanceLanguage> =
        store.data.map { prefs ->
            GuidanceLanguage.fromStorageKey(prefs[KEY_LANGUAGE])
        }

    suspend fun setLanguage(language: GuidanceLanguage) {
        store.edit { it[KEY_LANGUAGE] = language.storageKey }
    }

    private companion object {
        val KEY_LANGUAGE = stringPreferencesKey("guidance_language")
    }
}
