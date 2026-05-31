package com.beacon.data.modelpack

import com.beacon.core.concurrency.DispatcherProvider
import com.beacon.core.log.BeaconLog
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class ModelPackDownloader @Inject constructor(
    private val dispatchers: DispatcherProvider,
) {
    private val activeCall = AtomicReference<Call?>(null)

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        // Large HF files can stall briefly on slow Wi‑Fi; do not time out mid-stream.
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun download(
        url: String,
        destination: File,
        onProgress: suspend (bytesRead: Long, totalBytes: Long?) -> Unit,
    ) = withContext(dispatchers.io) {
        val temp = File(destination.parentFile, "${destination.name}.part")
        temp.parentFile?.mkdirs()

        var existingBytes = if (temp.isFile) temp.length() else 0L
        var attempt = 0
        while (attempt < MAX_DOWNLOAD_ATTEMPTS) {
            attempt++
            if (existingBytes > 0) {
                BeaconLog.i(TAG, "Resuming download at $existingBytes bytes for ${destination.name}")
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", "Beacon/1.0 (Android; model-pack-download)")
            if (existingBytes > 0) {
                requestBuilder.header("Range", "bytes=$existingBytes-")
            }

            val call = client.newCall(requestBuilder.build())
            activeCall.set(call)
            val response = try {
                call.execute()
            } finally {
                activeCall.compareAndSet(call, null)
            }

            var restartFromBeginning = false
            response.use { httpResponse ->
                when {
                    httpResponse.code == 416 -> {
                        temp.delete()
                        existingBytes = 0L
                        restartFromBeginning = true
                    }
                    httpResponse.code == 200 && existingBytes > 0 -> {
                        BeaconLog.w(TAG, "Server did not honor Range; restarting ${destination.name}")
                        temp.delete()
                        existingBytes = 0L
                        restartFromBeginning = true
                    }
                    !httpResponse.isSuccessful -> {
                        throw IOException("Download failed: HTTP ${httpResponse.code}")
                    }
                    else -> {
                        val body = httpResponse.body ?: throw IOException("Empty response body")
                        val contentRangeTotal = parseContentRangeTotal(httpResponse.header("Content-Range"))
                        val total = contentRangeTotal
                            ?: body.contentLength().takeIf { it > 0L }?.let { it + existingBytes }
                            ?: parseContentLength(httpResponse.header("Content-Length"))?.let { len ->
                                if (httpResponse.code == 206) len + existingBytes else len
                            }

                        body.byteStream().use { input ->
                            RandomAccessFile(temp, "rw").use { raf ->
                                if (existingBytes > 0) {
                                    raf.seek(existingBytes)
                                } else {
                                    raf.setLength(0)
                                }
                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                var downloaded = existingBytes
                                while (coroutineContext.isActive) {
                                    coroutineContext.ensureActive()
                                    val read = input.read(buffer)
                                    if (read <= 0) break
                                    raf.write(buffer, 0, read)
                                    downloaded += read
                                    onProgress(downloaded, total)
                                }
                            }
                        }

                        if (!temp.renameTo(destination)) {
                            temp.copyTo(destination, overwrite = true)
                            temp.delete()
                        }
                    }
                }
            }
            if (!restartFromBeginning) return@withContext
        }
        error("Download failed after $MAX_DOWNLOAD_ATTEMPTS attempts")
    }

    private fun parseContentRangeTotal(header: String?): Long? {
        if (header.isNullOrBlank()) return null
        // bytes 100-999/1000
        val slash = header.lastIndexOf('/')
        if (slash < 0) return null
        val total = header.substring(slash + 1)
        if (total == "*") return null
        return total.toLongOrNull()
    }

    private fun parseContentLength(header: String?): Long? =
        header?.toLongOrNull()

    fun cancel() {
        activeCall.getAndSet(null)?.cancel()
    }

    private companion object {
        const val TAG = "ModelPackDownloader"
        const val MAX_DOWNLOAD_ATTEMPTS = 3
    }
}
