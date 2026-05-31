package com.beacon.domain.vision

import android.graphics.Bitmap

/**
 * On-device object detection with bounding boxes (phone camera mode).
 */
interface ObjectDetector {
    suspend fun detect(bitmap: Bitmap): List<DetectedObject>
}
