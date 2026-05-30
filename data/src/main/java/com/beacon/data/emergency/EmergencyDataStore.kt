package com.beacon.data.emergency

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** Shared emergency DataStore (trusted contact + emergency settings). */
internal val Context.emergencyPrefs: DataStore<Preferences> by preferencesDataStore(
    name = "emergency_prefs",
)
