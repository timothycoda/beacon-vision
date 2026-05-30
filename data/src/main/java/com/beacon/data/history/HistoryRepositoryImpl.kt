package com.beacon.data.history

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.beacon.domain.history.HistoryEntry
import com.beacon.domain.history.HistoryRepository
import com.beacon.domain.history.HistoryType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.historyDataStore by preferencesDataStore(name = "history_prefs")

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : HistoryRepository {

    private val store = context.historyDataStore

    override val recent: Flow<List<HistoryEntry>> =
        store.data.map { prefs ->
            decode(prefs[KEY_ENTRIES].orEmpty())
        }

    override suspend fun add(entry: HistoryEntry) {
        store.edit { prefs ->
            val current = decode(prefs[KEY_ENTRIES].orEmpty())
            val summary = entry.summary.take(MAX_SUMMARY_CHARS).replace('\n', ' ')
            val normalized = entry.copy(summary = summary)
            val updated = listOf(normalized) + current
            prefs[KEY_ENTRIES] = encode(updated.take(MAX_ENTRIES))
        }
    }

    override suspend fun clear() {
        store.edit { it.remove(KEY_ENTRIES) }
    }

    private fun encode(entries: List<HistoryEntry>): String =
        entries.joinToString(RECORD_SEP) { e ->
            "${e.type.name}$FIELD_SEP${e.timestampMs}$FIELD_SEP${e.summary}"
        }

    private fun decode(raw: String): List<HistoryEntry> {
        if (raw.isBlank()) return emptyList()
        return raw.split(RECORD_SEP).mapNotNull { line ->
            val parts = line.split(FIELD_SEP, limit = 3)
            if (parts.size < 3) return@mapNotNull null
            val type = runCatching { HistoryType.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
            val ts = parts[1].toLongOrNull() ?: return@mapNotNull null
            HistoryEntry(type = type, summary = parts[2], timestampMs = ts)
        }
    }

    private companion object {
        val KEY_ENTRIES = stringPreferencesKey("entries_v1")
        const val RECORD_SEP = "\u001E"
        const val FIELD_SEP = "\u001F"
        const val MAX_ENTRIES = 15
        const val MAX_SUMMARY_CHARS = 400
    }
}
