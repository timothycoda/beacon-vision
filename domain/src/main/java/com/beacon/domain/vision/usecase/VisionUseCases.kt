package com.beacon.domain.vision.usecase

import com.beacon.core.result.OperationResult
import com.beacon.domain.glasses.GlassesRepository
import com.beacon.domain.vision.CapturedImage
import com.beacon.domain.vision.SceneDescriber
import com.beacon.domain.vision.SceneDescription
import javax.inject.Inject

/** Capture a single still image from the glasses camera. */
class CapturePhotoUseCase @Inject constructor(
    private val repository: GlassesRepository,
) {
    suspend operator fun invoke(): OperationResult<CapturedImage> = repository.capturePhoto()
}

/** Describe an already-captured image as a short, speakable summary. */
class DescribeSceneUseCase @Inject constructor(
    private val describer: SceneDescriber,
) {
    suspend operator fun invoke(image: CapturedImage): OperationResult<SceneDescription> =
        describer.describe(image)
}
