package com.beacon.data.vision

import com.beacon.core.concurrency.DispatcherProvider
import com.beacon.core.result.OperationResult
import com.beacon.domain.safety.SceneSafety
import com.beacon.domain.vision.CapturedImage
import com.beacon.domain.vision.SceneDescriber
import com.beacon.domain.vision.SceneDescription
import com.beacon.data.guidance.GuidanceLanguagePreferences
import com.beacon.data.vision.HausaLabelTranslator
import com.beacon.domain.guidance.GuidanceLanguage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ML Kit labels first; when a Gemma pack is installed and loaded, paraphrase into
 * richer spoken guidance.
 */
@Singleton
class CompositeSceneDescriber @Inject constructor(
    private val mlKit: MlKitSceneDescriber,
    private val narrationEnhancer: SceneNarrationEnhancer,
    private val languagePrefs: GuidanceLanguagePreferences,
    private val dispatchers: DispatcherProvider,
) : SceneDescriber {

    override suspend fun describe(image: CapturedImage): OperationResult<SceneDescription> =
        withContext(dispatchers.default) {
            when (val base = mlKit.describe(image)) {
                is OperationResult.Failure -> base
                is OperationResult.Success -> {
                    val language = languagePrefs.language.first()
                    val labels = base.value.labels
                    val labelsForGemma = if (language == GuidanceLanguage.Hausa) {
                        labels.map { it.copy(text = HausaLabelTranslator.translate(it.text)) }
                    } else {
                        labels
                    }
                    val gemmaLine = narrationEnhancer.enhance(labelsForGemma)
                    if (!gemmaLine.isNullOrBlank()) {
                        OperationResult.Success(
                            SceneDescription(
                                spokenSummary = SceneSafety.spokenSummary(labels, gemmaLine),
                                labels = labels,
                            ),
                        )
                    } else {
                        base
                    }
                }
            }
        }
}
