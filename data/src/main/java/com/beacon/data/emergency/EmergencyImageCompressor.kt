package com.beacon.data.emergency

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

@Singleton
class EmergencyImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun compressToShareFile(jpegBytes: ByteArray): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "emergency_images").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val decoded = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size) ?: return@runCatching null
            val scaled = scaleDown(decoded)
            if (scaled !== decoded) decoded.recycle()
            val out = File(dir, "emergency_${System.currentTimeMillis()}.jpg")
            FileOutputStream(out).use { stream ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            }
            scaled.recycle()
            out.takeIf { it.isFile && it.length() > 0L }
        }.getOrNull()
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val maxDim = max(bitmap.width, bitmap.height)
        if (maxDim <= MAX_DIMENSION) return bitmap
        val scale = MAX_DIMENSION.toFloat() / maxDim
        val w = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    private companion object {
        const val MAX_DIMENSION = 1280
        const val JPEG_QUALITY = 82
    }
}
