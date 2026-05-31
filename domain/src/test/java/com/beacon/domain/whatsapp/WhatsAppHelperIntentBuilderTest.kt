package com.beacon.domain.whatsapp

import com.beacon.domain.emergency.EmergencyLocation
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppHelperIntentBuilderTest {

    @Test
    fun buildWaMeUrl_stripsPlusAndEncodes() {
        val url = WhatsAppHelperIntentBuilder.buildWaMeUrl("+2348012345678", "Hello there")
        assertNotNull(url)
        assertTrue(url!!.contains("wa.me/2348012345678"))
        assertTrue(url.contains("text="))
    }

    @Test
    fun rejectsShortPhone() {
        assertNull(WhatsAppHelperIntentBuilder.digitsForWaMe("123"))
    }

    @Test
    fun emergencyMessage_includesMapLink() {
        val msg = WhatsAppEmergencyMessageBuilder.build(
            location = EmergencyLocation(6.45, 3.39),
            sceneSummary = "Chair ahead",
        )
        assertTrue(msg.contains("maps.google.com"))
        assertTrue(msg.contains("Chair ahead"))
    }
}
