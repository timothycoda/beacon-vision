package com.beacon.domain.text.usecase

import com.beacon.core.result.OperationResult
import com.beacon.domain.text.RecognizedText
import com.beacon.domain.text.TextRecognizer
import com.beacon.domain.vision.CapturedImage
import javax.inject.Inject

/** Read all text visible in an already-captured image. */
class ReadTextUseCase @Inject constructor(
    private val recognizer: TextRecognizer,
) {
    suspend operator fun invoke(image: CapturedImage): OperationResult<RecognizedText> =
        recognizer.recognize(image)
}
