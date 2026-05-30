package com.beacon.domain.history

import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    val recent: Flow<List<HistoryEntry>>
    suspend fun add(entry: HistoryEntry)
    suspend fun clear()
}
