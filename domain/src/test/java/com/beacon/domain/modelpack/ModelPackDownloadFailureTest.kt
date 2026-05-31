package com.beacon.domain.modelpack

import org.junit.Assert.assertTrue
import org.junit.Test

class ModelPackDownloadFailureTest {

    @Test
    fun userMessage_wifiRequired_mentionsWifi() {
        val message = ModelPackDownloadFailure.WIFI_REQUIRED.userMessage()
        assertTrue(message.contains("Wi", ignoreCase = true))
    }

    @Test
    fun userMessage_insufficientStorage_mentionsStorage() {
        val message = ModelPackDownloadFailure.INSUFFICIENT_STORAGE.userMessage()
        assertTrue(message.contains("storage", ignoreCase = true))
    }
}
