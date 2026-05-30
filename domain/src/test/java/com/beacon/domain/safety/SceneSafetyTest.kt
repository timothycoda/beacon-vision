package com.beacon.domain.safety

import com.beacon.domain.vision.SceneLabel
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneSafetyTest {

    @Test
    fun lowConfidence_addsNotSureWording() {
        val summary = SceneSafety.spokenSummary(
            labels = listOf(SceneLabel("door", 0.4f)),
            baseSummary = "Ahead of you I can see a door.",
        )
        assertTrue(summary.contains("not sure", ignoreCase = true))
    }

    @Test
    fun highConfidence_keepsBaseSummary() {
        val base = "Ahead of you I can see a door."
        val summary = SceneSafety.spokenSummary(
            labels = listOf(SceneLabel("door", 0.9f)),
            baseSummary = base,
        )
        assertTrue(summary == base)
    }
}
