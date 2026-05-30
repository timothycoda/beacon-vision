package com.beacon.data.text

import android.graphics.BitmapFactory
import com.beacon.core.concurrency.DispatcherProvider
import com.beacon.core.log.BeaconLog
import com.beacon.core.result.OperationResult
import com.beacon.domain.text.RecognizedText
import com.beacon.domain.text.TextRecognizer
import com.beacon.domain.vision.CapturedImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * On-device OCR using ML Kit's bundled Latin text recognizer. No network required.
 */
@Singleton
class MlKitTextRecognizer @Inject constructor(
    private val dispatchers: DispatcherProvider,
) : TextRecognizer {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun recognize(image: CapturedImage): OperationResult<RecognizedText> =
        withContext(dispatchers.default) {
            val bitmap = runCatching {
                BitmapFactory.decodeByteArray(image.jpegBytes, 0, image.jpegBytes.size)
            }.getOrNull()
                ?: return@withContext OperationResult.Failure("The photo could not be read")

            val raw = runCatching { extractText(bitmap) }
                .getOrElse {
                    BeaconLog.e(TAG, "OCR failed", it)
                    return@withContext OperationResult.Failure("Could not read the text")
                }

            val normalized = normalize(raw)
            if (normalized.isEmpty()) {
                return@withContext OperationResult.Success(
                    RecognizedText(
                        fullText = "",
                        spokenText = NO_TEXT_MESSAGE,
                    ),
                )
            }

            OperationResult.Success(
                RecognizedText(
                    fullText = normalized,
                    spokenText = toSpokenText(normalized),
                ),
            )
        }

    private suspend fun extractText(bitmap: android.graphics.Bitmap): String =
        suspendCancellableCoroutine { cont ->
            val input = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(input)
                .addOnSuccessListener { result ->
                    if (cont.isActive) cont.resume(result.text.orEmpty())
                }
                .addOnFailureListener { e ->
                    if (cont.isActive) cont.resumeWith(Result.failure(e))
                }
        }

    private fun normalize(text: String): String =
        text.trim().replace(Regex("\\s+"), " ")

    private fun toSpokenText(full: String): String {
        if (full.length <= MAX_SPOKEN_CHARS) return full
        val cut = full.take(MAX_SPOKEN_CHARS)
        val lastSpace = cut.lastIndexOf(' ')
        val head = if (lastSpace > MAX_SPOKEN_CHARS / 2) cut.take(lastSpace) else cut
        return "$head. There is more text on screen."
    }

    private companion object {
        const val TAG = "MlKitTextRecognizer"
        const val MAX_SPOKEN_CHARS = 450
        const val NO_TEXT_MESSAGE =
            "I couldn't find any readable text. Try moving closer, hold steady, and capture again."
    }
}
