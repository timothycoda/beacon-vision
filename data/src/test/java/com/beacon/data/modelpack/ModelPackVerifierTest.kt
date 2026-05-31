package com.beacon.data.modelpack

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ModelPackVerifierTest {

    private val verifier = ModelPackVerifier()

    @Test
    fun matchesExpected_sizeOnly_acceptsMatchingLength() {
        val file = File.createTempFile("pack", ".bin").apply {
            writeBytes(ByteArray(1024) { 0 })
        }
        try {
            assertTrue(
                verifier.matchesExpected(
                    file = file,
                    expectedSha256Hex = "0".repeat(64),
                    expectedSizeBytes = 1024L,
                    verifySizeOnly = true,
                ),
            )
        } finally {
            file.delete()
        }
    }

    @Test
    fun matchesExpected_sizeOnly_rejectsWrongLength() {
        val file = File.createTempFile("pack", ".bin").apply {
            writeBytes(ByteArray(512) { 0 })
        }
        try {
            assertFalse(
                verifier.matchesExpected(
                    file = file,
                    expectedSha256Hex = "0".repeat(64),
                    expectedSizeBytes = 1024L,
                    verifySizeOnly = true,
                ),
            )
        } finally {
            file.delete()
        }
    }

    @Test
    fun isPlaceholderChecksum_trueForAllZeros() {
        assertTrue(verifier.isPlaceholderChecksum("0".repeat(64), verifySizeOnly = false))
    }

    @Test
    fun isPlaceholderChecksum_falseWhenSizeOnly() {
        assertFalse(verifier.isPlaceholderChecksum("0".repeat(64), verifySizeOnly = true))
    }

    @Test
    fun sha256Hex_isStableHexDigest() {
        val file = File.createTempFile("pack", ".txt").apply {
            writeText("beacon")
        }
        try {
            val first = verifier.sha256Hex(file)
            val second = verifier.sha256Hex(file)
            assertTrue(first == second)
            assertTrue(first.length == 64)
            assertTrue(first.all { it in '0'..'9' || it in 'a'..'f' })
        } finally {
            file.delete()
        }
    }
}
