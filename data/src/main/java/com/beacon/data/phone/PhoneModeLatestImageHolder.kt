package com.beacon.data.phone

import com.beacon.domain.vision.LatestImageProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneModeLatestImageHolder @Inject constructor() : LatestImageProvider {

    @Volatile
    private var jpeg: ByteArray? = null

    @Volatile
    private var sceneSummary: String? = null

    fun updateFrame(jpegBytes: ByteArray?) {
        jpeg = jpegBytes
    }

    fun updateSceneSummary(summary: String?) {
        sceneSummary = summary?.trim()?.takeIf { it.isNotBlank() }
    }

    override suspend fun latestJpegBytes(): ByteArray? = jpeg

    override fun latestSceneSummary(): String? = sceneSummary
}
