package com.a42r.pevplugshare.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * ML Kit-based socket/outlet detector
 * Detects electrical sockets and outlets in photos
 */
object SocketDetector {

    private const val TAG = "SocketDetector"

    data class SocketDetection(
        val boundingBox: RectF,
        val confidence: Float,
        val label: String
    )

    /**
     * Detect sockets/outlets in an image
     * Returns list of detected sockets with bounding boxes
     */
    suspend fun detectSockets(context: Context, photoPath: String): List<SocketDetection> {
        return suspendCoroutine { continuation ->
            try {
                val bitmap = BitmapFactory.decodeFile(photoPath)
                if (bitmap == null) {
                    continuation.resumeWithException(Exception("Failed to load image: $photoPath"))
                    return@suspendCoroutine
                }

                val image = InputImage.fromBitmap(bitmap, 0)

                // Configure object detector
                // Using multiple detector mode to find small objects like sockets
                val options = ObjectDetectorOptions.Builder()
                    .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                    .enableMultipleObjects()
                    .enableClassification()
                    .build()

                val detector = ObjectDetection.getClient(options)

                detector.process(image)
                    .addOnSuccessListener { detectedObjects ->
                        Log.d(TAG, "Detected ${detectedObjects.size} objects in image")

                        // Filter for likely sockets/outlets
                        val socketDetections = detectedObjects
                            .filter { isLikelySocket(it) }
                            .map { obj ->
                                SocketDetection(
                                    boundingBox = RectF(
                                        obj.boundingBox.left.toFloat(),
                                        obj.boundingBox.top.toFloat(),
                                        obj.boundingBox.right.toFloat(),
                                        obj.boundingBox.bottom.toFloat()
                                    ),
                                    confidence = getObjectConfidence(obj),
                                    label = getObjectLabel(obj)
                                )
                            }
                            .sortedByDescending { it.confidence }

                        Log.d(TAG, "Found ${socketDetections.size} potential sockets")
                        continuation.resume(socketDetections)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Object detection failed", e)
                        continuation.resumeWithException(e)
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Error during socket detection", e)
                continuation.resumeWithException(e)
            }
        }
    }

    /**
     * Heuristic to determine if detected object is likely a socket/outlet
     */
    private fun isLikelySocket(obj: DetectedObject): Boolean {
        // Check aspect ratio (sockets are usually square or slightly rectangular)
        val width = obj.boundingBox.width()
        val height = obj.boundingBox.height()
        val aspectRatio = width.toFloat() / height.toFloat()

        // Sockets typically have aspect ratio between 0.5 and 2.0
        if (aspectRatio < 0.5f || aspectRatio > 2.0f) {
            return false
        }

        // Check size relative to image
        // Sockets are usually small to medium sized objects
        // (we'll refine this with image dimensions if needed)
        val area = width * height
        if (area < 100) {  // Too small (probably noise)
            return false
        }

        // Check if object has classification labels
        if (obj.labels.isNotEmpty()) {
            val labels = obj.labels.map { it.text.lowercase() }

            // Look for relevant labels
            val socketKeywords = listOf(
                "electronic", "device", "appliance", "outlet", "plug",
                "socket", "port", "connector", "switch", "box"
            )

            if (labels.any { label ->
                socketKeywords.any { keyword -> label.contains(keyword) }
            }) {
                Log.d(TAG, "Object matches socket keywords: $labels")
                return true
            }
        }

        // If no specific labels, accept objects with reasonable size and aspect ratio
        // (ML Kit's base detector may not have specific "socket" category)
        return true
    }

    /**
     * Get confidence score for detected object
     */
    private fun getObjectConfidence(obj: DetectedObject): Float {
        return if (obj.labels.isNotEmpty()) {
            obj.labels.maxOf { it.confidence }
        } else {
            0.5f  // Default confidence when no labels
        }
    }

    /**
     * Get best label for detected object
     */
    private fun getObjectLabel(obj: DetectedObject): String {
        return if (obj.labels.isNotEmpty()) {
            obj.labels.maxByOrNull { it.confidence }?.text ?: "Unknown"
        } else {
            "Object"
        }
    }

    /**
     * Get the most confident socket detection from a list
     */
    fun getBestDetection(detections: List<SocketDetection>): SocketDetection? {
        return detections.maxByOrNull { it.confidence }
    }

    /**
     * Convert detection to focus rectangle format
     */
    fun detectionToFocusRect(detection: SocketDetection): RectF {
        return detection.boundingBox
    }
}
