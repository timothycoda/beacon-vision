package com.beacon.domain.vision.usecase

import android.graphics.Bitmap
import com.beacon.domain.vision.DetectedObject
import com.beacon.domain.vision.ObjectDetector
import javax.inject.Inject

class DetectObjectsUseCase @Inject constructor(
    private val detector: ObjectDetector,
) {
    suspend operator fun invoke(bitmap: Bitmap): List<DetectedObject> = detector.detect(bitmap)
}
