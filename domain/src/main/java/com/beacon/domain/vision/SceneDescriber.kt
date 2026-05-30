package com.beacon.domain.vision

import com.beacon.core.result.OperationResult

/**
 * Turns a captured image into a short, speakable description. The Phase 2
 * implementation runs fully on-device (ML Kit bundled labeler); later phases can
 * swap in richer offline model packs behind this same contract.
 */
interface SceneDescriber {
    suspend fun describe(image: CapturedImage): OperationResult<SceneDescription>
}
