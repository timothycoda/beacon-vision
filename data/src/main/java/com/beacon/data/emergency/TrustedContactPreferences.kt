package com.beacon.data.emergency

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.beacon.domain.emergency.TrustedContact
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrustedContactPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.emergencyPrefs

    val contact: Flow<TrustedContact?> = store.data.map { prefs ->
        val phone = prefs[KEY_PHONE]
        val name = prefs[KEY_NAME]
        if (phone.isNullOrBlank()) null else TrustedContact(name = name.orEmpty(), phoneNumber = phone)
    }

    suspend fun save(contact: TrustedContact) {
        store.edit {
            it[KEY_NAME] = contact.name.trim()
            it[KEY_PHONE] = contact.phoneNumber.trim()
        }
    }

    suspend fun clear() {
        store.edit {
            it.remove(KEY_NAME)
            it.remove(KEY_PHONE)
        }
    }

    private companion object {
        val KEY_NAME = stringPreferencesKey("trusted_contact_name")
        val KEY_PHONE = stringPreferencesKey("trusted_contact_phone")
    }
}
