package com.beacon.data.modelpack

import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelPackVerifier @Inject constructor() {

    fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun matchesExpected(file: File, expectedSha256Hex: String, expectedSizeBytes: Long, verifySizeOnly: Boolean): Boolean {
        if (!file.isFile || file.length() <= 0L) return false
        if (verifySizeOnly || expectedSha256Hex.all { it == '0' }) {
            return file.length() == expectedSizeBytes
        }
        return sha256Hex(file).equals(expectedSha256Hex, ignoreCase = true)
    }

    fun isPlaceholderChecksum(expectedSha256Hex: String, verifySizeOnly: Boolean): Boolean =
        !verifySizeOnly && expectedSha256Hex.all { it == '0' }
}
