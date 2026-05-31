package com.beacon.app.vision

import com.beacon.data.vision.SceneNarrationEnhancer
import com.beacon.domain.vision.SceneLabel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GemmaSceneNarrationEnhancer @Inject constructor(
    private val engine: GemmaNarrationEngine,
) : SceneNarrationEnhancer {
    override suspend fun enhance(labels: List<SceneLabel>): String? =
        engine.narrate(labels.map { it.text })
}
