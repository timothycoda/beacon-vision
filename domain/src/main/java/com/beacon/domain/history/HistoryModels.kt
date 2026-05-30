package com.beacon.domain.history

enum class HistoryType {
    AHEAD,
    READ,
    EMERGENCY,
}

data class HistoryEntry(
    val type: HistoryType,
    val summary: String,
    val timestampMs: Long = System.currentTimeMillis(),
)
