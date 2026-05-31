package com.beacon.data.vision

import com.beacon.domain.vision.SceneLabel

/**
 * Optional richer narration when a Gemma pack is installed (implemented in the app module).
 */
interface SceneNarrationEnhancer {
    suspend fun enhance(labels: List<SceneLabel>): String?
}
