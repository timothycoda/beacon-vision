package com.beacon.data.vision

import com.beacon.domain.vision.SceneLabel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpSceneNarrationEnhancer @Inject constructor() : SceneNarrationEnhancer {
    override suspend fun enhance(labels: List<SceneLabel>): String? = null
}
