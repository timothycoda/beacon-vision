package com.beacon.data.vision

import com.beacon.core.concurrency.DispatcherProvider
import com.beacon.core.result.OperationResult
import com.beacon.domain.safety.SceneSafety
import com.beacon.domain.vision.CapturedImage
import com.beacon.domain.vision.SceneDescriber
import com.beacon.domain.vision.SceneDescription
import com.beacon.domain.vision.SceneLabel
import com.beacon.data.guidance.GuidanceLanguagePreferences
import com.beacon.domain.guidance.GuidanceLanguage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ML Kit labels first; when a Gemma pack is installed and loaded, paraphrase into
 * richer spoken guidance (English or Hausa). Falls back to ML Kit if Gemma is
 * unavailable or replies in the wrong language.
 */
@Singleton
class CompositeSceneDescriber @Inject constructor(
    private val mlKit: MlKitSceneDescriber,
    private val narrationEnhancer: SceneNarrationEnhancer,
    private val languagePrefs: GuidanceLanguagePreferences,
    private val dispatchers: DispatcherProvider,
) : SceneDescriber {

    @Volatile
    private var lastGemmaEnhanceMs = 0L

    override suspend fun describe(image: CapturedImage): OperationResult<SceneDescription> =
        withContext(dispatchers.default) {
            when (val base = mlKit.describe(image)) {
                is OperationResult.Failure -> base
                is OperationResult.Success -> {
                    val language = languagePrefs.language.first()
                    val labels = base.value.labels
                    val gemmaLine = maybeEnhanceWithGemma(language, labels)
                    val spoken = pickSpokenSummary(
                        language = language,
                        labels = labels,
                        mlKitSummary = base.value.spokenSummary,
                        gemmaLine = gemmaLine,
                    )
                    OperationResult.Success(
                        SceneDescription(
                            spokenSummary = spoken,
                            labels = labels,
                        ),
                    )
                }
            }
        }

    private suspend fun maybeEnhanceWithGemma(
        language: GuidanceLanguage,
        labels: List<SceneLabel>,
    ): String? {
        if (language == GuidanceLanguage.Hausa) return null
        val now = System.currentTimeMillis()
        if (now - lastGemmaEnhanceMs < GEMMA_MIN_INTERVAL_MS) return null
        lastGemmaEnhanceMs = now
        return narrationEnhancer.enhance(labels)
    }

    private fun pickSpokenSummary(
        language: GuidanceLanguage,
        labels: List<SceneLabel>,
        mlKitSummary: String,
        gemmaLine: String?,
    ): String {
        val gemma = gemmaLine?.trim().orEmpty()
        if (gemma.isBlank()) {
            return mlKitSummary
        }
        val useGemma = when (language) {
            GuidanceLanguage.English -> true
            GuidanceLanguage.Hausa -> GemmaNarrationGuard.isLikelyHausa(gemma)
        }
        val base = if (useGemma) gemma else mlKitSummary
        return SceneSafety.spokenSummary(labels, base, language)
    }

    private companion object {
        /** Keeps phone/walking loops responsive; ML Kit fills gaps between Gemma runs. */
        const val GEMMA_MIN_INTERVAL_MS = 8_000L
    }
}
