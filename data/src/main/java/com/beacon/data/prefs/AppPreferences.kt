package com.beacon.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appDataStore by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.appDataStore

    /** True after the user has paired glasses at least once. */
    val onboardingComplete: Flow<Boolean> =
        store.data.map { it[KEY_ONBOARDING_COMPLETE] ?: false }

    /** True after the 3-screen feature introduction (Welcome → tour → permissions). */
    val featureTourComplete: Flow<Boolean> =
        store.data.map { it[KEY_FEATURE_TOUR_COMPLETE] ?: false }

    suspend fun setOnboardingComplete() {
        store.edit { it[KEY_ONBOARDING_COMPLETE] = true }
    }

    suspend fun setFeatureTourComplete() {
        store.edit { it[KEY_FEATURE_TOUR_COMPLETE] = true }
    }

    private companion object {
        val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val KEY_FEATURE_TOUR_COMPLETE = booleanPreferencesKey("feature_tour_complete")
    }
}
