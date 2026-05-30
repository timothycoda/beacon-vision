package com.beacon.domain.text

import com.beacon.core.result.OperationResult
import com.beacon.domain.vision.CapturedImage

/**
 * Extracts readable text from a captured image. Phase 3 uses ML Kit's bundled
 * Latin recognizer (fully on-device). Later phases can swap in richer OCR packs.
 */
interface TextRecognizer {
    suspend fun recognize(image: CapturedImage): OperationResult<RecognizedText>
}
