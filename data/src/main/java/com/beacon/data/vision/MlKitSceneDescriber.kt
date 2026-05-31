package com.beacon.data.vision

import android.graphics.BitmapFactory
import com.beacon.core.concurrency.DispatcherProvider
import com.beacon.core.log.BeaconLog
import com.beacon.core.result.OperationResult
import com.beacon.domain.vision.CapturedImage
import com.beacon.domain.safety.SceneSafety
import com.beacon.domain.vision.SceneDescriber
import com.beacon.domain.vision.SceneDescription
import com.beacon.data.guidance.GuidanceLanguagePreferences
import com.beacon.domain.guidance.GuidanceLanguage
import com.beacon.domain.vision.SceneLabel
import kotlinx.coroutines.flow.first
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * On-device scene description using ML Kit's bundled image labeler. Runs fully
 * offline — no network, no Play Services dependency for the bundled model.
 * Produces a short, natural sentence for text-to-speech.
 */
@Singleton
class MlKitSceneDescriber @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val languagePrefs: GuidanceLanguagePreferences,
) : SceneDescriber {

    private val labeler by lazy {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(CONFIDENCE_THRESHOLD)
                .build(),
        )
    }

    override suspend fun describe(image: CapturedImage): OperationResult<SceneDescription> =
        withContext(dispatchers.default) {
            val bitmap = runCatching {
                BitmapFactory.decodeByteArray(image.jpegBytes, 0, image.jpegBytes.size)
            }.getOrNull()
                ?: return@withContext OperationResult.Failure("The photo could not be read")

            val labels = runCatching { label(bitmap) }
                .getOrElse {
                    BeaconLog.e(TAG, "labeling failed", it)
                    return@withContext OperationResult.Failure("Could not analyse the scene")
                }

            val language = languagePrefs.language.first()
            val displayLabels = if (language == GuidanceLanguage.Hausa) {
                labels.map { label ->
                    label.copy(text = HausaLabelTranslator.translate(label.text))
                }
            } else {
                labels
            }
            val base = summarize(displayLabels, language)
            OperationResult.Success(
                SceneDescription(
                    spokenSummary = SceneSafety.spokenSummary(displayLabels, base, language),
                    labels = displayLabels,
                ),
            )
        }

    private suspend fun label(bitmap: android.graphics.Bitmap): List<SceneLabel> =
        suspendCancellableCoroutine { cont ->
            val input = InputImage.fromBitmap(bitmap, 0)
            labeler.process(input)
                .addOnSuccessListener { result ->
                    val labels = result
                        .sortedByDescending { it.confidence }
                        .take(MAX_LABELS)
                        .map { SceneLabel(text = it.text, confidence = it.confidence) }
                    if (cont.isActive) cont.resume(labels)
                }
                .addOnFailureListener { e ->
                    if (cont.isActive) cont.resumeWith(Result.failure(e))
                }
        }

    private fun summarize(labels: List<SceneLabel>, language: GuidanceLanguage): String {
        if (labels.isEmpty()) {
            return when (language) {
                GuidanceLanguage.Hausa -> "Ban tabbata abin da ke gaba ba."
                GuidanceLanguage.English -> "I am not sure what is ahead."
            }
        }
        val names = labels.map { it.text.lowercase() }
        val list = when (names.size) {
            1 -> names[0]
            2 -> if (language == GuidanceLanguage.Hausa) "${names[0]} da ${names[1]}"
            else "${names[0]} and ${names[1]}"
            else -> if (language == GuidanceLanguage.Hausa) {
                names.dropLast(1).joinToString(", ") + ", da " + names.last()
            } else {
                names.dropLast(1).joinToString(", ") + ", and " + names.last()
            }
        }
        return when (language) {
            GuidanceLanguage.Hausa -> "A gaban ka na iya ganin $list."
            GuidanceLanguage.English -> "Ahead of you I can see $list."
        }
    }

    private companion object {
        const val TAG = "MlKitSceneDescriber"
        // Slightly lower threshold + more labels = better recall; still capped for speed/TTS.
        const val CONFIDENCE_THRESHOLD = 0.55f
        const val MAX_LABELS = 5
    }
}
