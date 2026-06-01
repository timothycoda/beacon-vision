package com.beacon.data.vision

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import com.beacon.core.concurrency.DispatcherProvider
import com.beacon.core.log.BeaconLog
import com.beacon.domain.vision.DetectedObject
import com.beacon.domain.vision.NormalizedRect
import com.beacon.domain.vision.ObjectDetector
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min

@Singleton
class MlKitObjectDetector @Inject constructor(
    private val dispatchers: DispatcherProvider,
) : ObjectDetector {

    private val objectClient by lazy {
        ObjectDetection.getClient(
            ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build(),
        )
    }

    private val labelClient by lazy {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.45f)
                .build(),
        )
    }

    private val poseClient by lazy {
        PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build(),
        )
    }

    private val faceClient by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(0.08f)
                .enableTracking()
                .build(),
        )
    }

    override suspend fun detect(bitmap: Bitmap): List<DetectedObject> = withContext(dispatchers.default) {
        runCatching {
            coroutineScope {
                val input = InputImage.fromBitmap(bitmap, 0)
                val width = bitmap.width.toFloat().coerceAtLeast(1f)
                val height = bitmap.height.toFloat().coerceAtLeast(1f)

                val objectsDeferred = async { detectObjects(input, width, height) }
                val labelsDeferred = async { detectImageLabels(input) }
                val posesDeferred = async { detectPeopleFromPose(input, width, height) }
                val facesDeferred = async { detectPeopleFromFaces(input, width, height) }

                val imageLabels = labelsDeferred.await()
                val merged = mutableListOf<DetectedObject>()
                merged.addAll(
                    objectsDeferred.await().map { obj ->
                        obj.copy(label = DetectionDisplayLabel.refineWithImageLabels(obj.label, imageLabels))
                    },
                )
                merged.addAll(posesDeferred.await())
                merged.addAll(facesDeferred.await())

                val hasPerson = merged.any { it.label.equals("person", ignoreCase = true) }
                personFromSceneLabels(imageLabels)?.takeIf { !hasPerson }?.let { merged.add(it) }

                dedupeAndLimit(merged, width, height)
            }
        }.getOrElse {
            BeaconLog.e(TAG, "object detection failed", it)
            emptyList()
        }
    }

    private suspend fun detectObjects(
        input: InputImage,
        width: Float,
        height: Float,
    ): List<DetectedObject> = suspendCancellableCoroutine { cont ->
        objectClient.process(input)
            .addOnSuccessListener { results ->
                val objects = results.mapIndexed { index, obj ->
                    val best = obj.labels.maxByOrNull { it.confidence }
                    val label = best?.text?.takeIf { it.isNotBlank() } ?: "object"
                    val confidence = best?.confidence ?: 0f
                    DetectedObject(
                        label = label,
                        confidence = confidence,
                        bounds = obj.boundingBox.normalize(width, height),
                        colorIndex = index % COLOR_SLOTS,
                    )
                }
                if (cont.isActive) cont.resume(objects)
            }
            .addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
    }

    private suspend fun detectImageLabels(input: InputImage): List<Pair<String, Float>> =
        suspendCancellableCoroutine { cont ->
            labelClient.process(input)
                .addOnSuccessListener { labels ->
                    val list = labels
                        .sortedByDescending { it.confidence }
                        .map { it.text to it.confidence }
                    if (cont.isActive) cont.resume(list)
                }
                .addOnFailureListener { if (cont.isActive) cont.resume(emptyList()) }
        }

    private suspend fun detectPeopleFromPose(
        input: InputImage,
        width: Float,
        height: Float,
    ): List<DetectedObject> = suspendCancellableCoroutine { cont ->
        poseClient.process(input)
            .addOnSuccessListener { pose ->
                val bounds = poseBounds(pose.allPoseLandmarks, width, height)
                val people = if (bounds != null) {
                    listOf(
                        DetectedObject(
                            label = "person",
                            confidence = 0.85f,
                            bounds = bounds,
                            colorIndex = 2,
                        ),
                    )
                } else {
                    emptyList()
                }
                if (cont.isActive) cont.resume(people)
            }
            .addOnFailureListener { if (cont.isActive) cont.resume(emptyList()) }
    }

    private suspend fun detectPeopleFromFaces(
        input: InputImage,
        width: Float,
        height: Float,
    ): List<DetectedObject> = suspendCancellableCoroutine { cont ->
        faceClient.process(input)
            .addOnSuccessListener { faces ->
                val people = faces.mapIndexed { index, face ->
                    val expanded = face.boundingBox.expandForPerson(width, height)
                    DetectedObject(
                        label = "person",
                        confidence = 0.8f,
                        bounds = expanded.normalize(width, height),
                        colorIndex = (index + 4) % COLOR_SLOTS,
                    )
                }
                if (cont.isActive) cont.resume(people)
            }
            .addOnFailureListener { if (cont.isActive) cont.resume(emptyList()) }
    }

    /** When pose/face miss a distant person, scene labels like "person" still get a guide box. */
    private fun personFromSceneLabels(imageLabels: List<Pair<String, Float>>): DetectedObject? {
        val hit = imageLabels.firstOrNull { (text, conf) ->
            conf >= 0.48f && PERSON_SCENE_LABELS.any { keyword ->
                text.contains(keyword, ignoreCase = true)
            }
        } ?: return null
        return DetectedObject(
            label = "person",
            confidence = hit.second,
            bounds = NormalizedRect(left = 0.15f, top = 0.08f, right = 0.85f, bottom = 0.92f),
            colorIndex = 0,
        )
    }

    private fun poseBounds(
        landmarks: List<PoseLandmark>,
        width: Float,
        height: Float,
    ): NormalizedRect? {
        val visible = landmarks.filter { it.inFrameLikelihood >= 0.3f }
        if (visible.size < 3) return null
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        visible.forEach { lm ->
            val x = lm.position.x
            val y = lm.position.y
            minX = min(minX, x)
            minY = min(minY, y)
            maxX = max(maxX, x)
            maxY = max(maxY, y)
        }
        val padX = (maxX - minX) * 0.15f
        val padY = (maxY - minY) * 0.15f
        return RectF(
            (minX - padX).coerceAtLeast(0f),
            (minY - padY).coerceAtLeast(0f),
            (maxX + padX).coerceAtMost(width),
            (maxY + padY).coerceAtMost(height),
        ).normalize(width, height)
    }

    private fun Rect.expandForPerson(imageWidth: Float, imageHeight: Float): Rect {
        val padX = (width() * 1.4f).toInt()
        val padTop = (height() * 2.2f).toInt()
        val padBottom = (height() * 0.5f).toInt()
        return Rect(
            (left - padX).coerceAtLeast(0),
            (top - padTop).coerceAtLeast(0),
            (right + padX).coerceAtMost(imageWidth.toInt()),
            (bottom + padBottom).coerceAtMost(imageHeight.toInt()),
        )
    }

    private fun dedupeAndLimit(
        objects: List<DetectedObject>,
        width: Float,
        height: Float,
    ): List<DetectedObject> {
        val sorted = objects.sortedByDescending { it.confidence }
        val kept = mutableListOf<DetectedObject>()
        for (candidate in sorted) {
            val overlapsPerson = candidate.label.equals("person", ignoreCase = true) &&
                kept.any { it.label.equals("person", ignoreCase = true) && iou(it.bounds, candidate.bounds) > 0.4f }
            if (overlapsPerson) continue
            val duplicate = kept.any { iou(it.bounds, candidate.bounds) > 0.5f }
            if (!duplicate) kept.add(candidate)
            if (kept.size >= MAX_OBJECTS) break
        }
        return kept.mapIndexed { index, obj -> obj.copy(colorIndex = index % COLOR_SLOTS) }
    }

    private fun iou(a: NormalizedRect, b: NormalizedRect): Float {
        val xLeft = max(a.left, b.left)
        val yTop = max(a.top, b.top)
        val xRight = min(a.right, b.right)
        val yBottom = min(a.bottom, b.bottom)
        if (xRight <= xLeft || yBottom <= yTop) return 0f
        val inter = (xRight - xLeft) * (yBottom - yTop)
        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)
        return inter / (areaA + areaB - inter).coerceAtLeast(1e-6f)
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

    private fun RectF.normalize(imageWidth: Float, imageHeight: Float): NormalizedRect =
        Rect(
            left.toInt(),
            top.toInt(),
            right.toInt(),
            bottom.toInt(),
        ).normalize(imageWidth, imageHeight)

    private companion object {
        const val TAG = "MlKitObjectDetector"
        const val COLOR_SLOTS = 6
        const val MAX_OBJECTS = 10

        val PERSON_SCENE_LABELS = setOf(
            "person",
            "people",
            "man",
            "woman",
            "boy",
            "girl",
            "human",
            "adult",
            "child",
        )
    }
}
