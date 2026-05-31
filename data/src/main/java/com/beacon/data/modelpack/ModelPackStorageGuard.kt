package com.beacon.data.modelpack

import android.content.Context
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ensures there is enough free space before starting a large model download.
 */
@Singleton
class ModelPackStorageGuard @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun availableBytes(): Long = runCatching {
        StatFs(context.filesDir.absolutePath).availableBytes
    }.getOrDefault(0L)

    fun hasSpaceFor(requiredBytes: Long): Boolean {
        if (requiredBytes <= 0L) return true
        return availableBytes() >= requiredBytes + SAFETY_MARGIN_BYTES
    }

    private companion object {
        /** Headroom for app cache, verification temp files, and OS. */
        const val SAFETY_MARGIN_BYTES = 128L * 1024L * 1024L
    }
}
