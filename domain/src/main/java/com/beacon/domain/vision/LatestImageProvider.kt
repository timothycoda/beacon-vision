package com.beacon.domain.vision

/** Latest JPEG frame for emergency sharing (phone mode or glasses). */
interface LatestImageProvider {
    suspend fun latestJpegBytes(): ByteArray?
    fun latestSceneSummary(): String?
}
