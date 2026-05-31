package com.beacon.data.vision

import android.graphics.Bitmap
import android.graphics.Rect
import com.beacon.core.concurrency.DispatcherProvider
import com.beacon.core.log.BeaconLog
import com.beacon.domain.vision.DetectedObject
import com.beacon.domain.vision.NormalizedRect
import com.beacon.domain.vision.ObjectDetector
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class MlKitObjectDetector @Inject constructor(
    private val dispatchers: DispatcherProvider,
) : ObjectDetector {

    private val client by lazy {
        ObjectDetection.getClient(
            ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build(),
        )
    }

    override suspend fun detect(bitmap: Bitmap): List<DetectedObject> = withContext(dispatchers.default) {
        runCatching {
            val input = InputImage.fromBitmap(bitmap, 0)
            suspendCancellableCoroutine { cont ->
                client.process(input)
                    .addOnSuccessListener { results ->
                        val width = bitmap.width.toFloat().coerceAtLeast(1f)
                        val height = bitmap.height.toFloat().coerceAtLeast(1f)
                        val objects = results.mapIndexed { index, obj ->
                            val label = obj.labels.maxByOrNull { it.confidence }?.text ?: "object"
                            val box = obj.boundingBox.normalize(width, height)
                            DetectedObject(
                                label = label,
                                confidence = obj.labels.maxByOrNull { it.confidence }?.confidence ?: 0f,
                                bounds = box,
                                colorIndex = index % COLOR_SLOTS,
                            )
                        }
                        if (cont.isActive) cont.resume(objects)
                    }
                    .addOnFailureListener { e ->
                        if (cont.isActive) cont.resumeWithException(e)
                    }
            }
        }.getOrElse {
            BeaconLog.e(TAG, "object detection failed", it)
            emptyList()
        }
    }

    private fun Rect?.normalize(imageWidth: Float, imageHeight: Float): NormalizedRect {
        if (this == null) return NormalizedRect(0f, 0f, 0f, 0f)
        return NormalizedRect(
            left = (left / imageWidth).coerceIn(0f, 1f),
            top = (top / imageHeight).coerceIn(0f, 1f),
            right = (right / imageWidth).coerceIn(0f, 1f),
            bottom = (bottom / imageHeight).coerceIn(0f, 1f),
        )
    }

    private companion object {
        const val TAG = "MlKitObjectDetector"
        const val COLOR_SLOTS = 6
    }
}
