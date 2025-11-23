package com.a42r.pevplugshare.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.util.Log
import com.a42r.pevplugshare.data.model.ArLocalizationMethod
import com.a42r.pevplugshare.data.model.ArSocketData
import com.a42r.pevplugshare.data.model.PhotoMetadata
import com.a42r.pevplugshare.util.PhotoUtils
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Analyzes existing photos to generate AR socket positioning data
 * Uses focus rectangles or AI detection to locate sockets in images
 */
object ArPhotoAnalyzer {

    private const val TAG = "ArPhotoAnalyzer"

    /**
     * Strategy for estimating socket position from photo
     */
    enum class AnalysisMethod {
        FOCUS_RECT,      // Use user-marked focus rectangle
        AI_DETECTION,    // Use ML Kit object detection
        EXIF_ONLY        // Use only EXIF GPS data (lowest accuracy)
    }

    data class AnalysisResult(
        val arSocketData: ArSocketData?,
        val method: AnalysisMethod,
        val confidence: Float,
        val errorMessage: String? = null
    )

    /**
     * Analyze a photo and generate AR socket data
     * Tries methods in order: focus rect -> AI detection -> EXIF only
     */
    suspend fun analyzePhoto(
        context: Context,
        photoPath: String,
        userDistanceEstimate: Float? = null
    ): AnalysisResult {
        try {
            val metadata = PhotoUtils.getPhotoMetadata(photoPath)

            // Method 1: Use focus rectangle if available
            if (metadata.focusRect != null && metadata.latitude != null &&
                metadata.longitude != null && metadata.bearing != null) {
                Log.d(TAG, "Using focus rectangle method for $photoPath")
                return analyzeWithFocusRect(photoPath, metadata, userDistanceEstimate)
            }

            // Method 2: Try AI detection if photo has GPS and bearing
            if (metadata.latitude != null && metadata.longitude != null &&
                metadata.bearing != null) {
                Log.d(TAG, "Attempting AI detection for $photoPath")
                val aiResult = analyzeWithAI(context, photoPath, metadata, userDistanceEstimate)
                if (aiResult.arSocketData != null) {
                    return aiResult
                }
            }

            // Method 3: Fallback to EXIF-only (just use GPS coords, no AR precision)
            if (metadata.latitude != null && metadata.longitude != null) {
                Log.d(TAG, "Using EXIF-only fallback for $photoPath")
                return AnalysisResult(
                    arSocketData = null,  // No precise AR data available
                    method = AnalysisMethod.EXIF_ONLY,
                    confidence = 0.3f,
                    errorMessage = "Photo lacks focus data and bearing for AR positioning"
                )
            }

            return AnalysisResult(
                arSocketData = null,
                method = AnalysisMethod.EXIF_ONLY,
                confidence = 0.0f,
                errorMessage = "Photo missing GPS data"
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing photo: $photoPath", e)
            return AnalysisResult(
                arSocketData = null,
                method = AnalysisMethod.EXIF_ONLY,
                confidence = 0.0f,
                errorMessage = "Analysis failed: ${e.message}"
            )
        }
    }

    /**
     * Analyze using user-marked focus rectangle
     * This is the most accurate method when available
     */
    private fun analyzeWithFocusRect(
        photoPath: String,
        metadata: PhotoMetadata,
        userDistanceEstimate: Float?
    ): AnalysisResult {
        val focusRect = metadata.focusRect!!
        val latitude = metadata.latitude!!
        val longitude = metadata.longitude!!
        val bearing = metadata.bearing!!
        val altitude = metadata.altitude ?: 0.0
        val pitch = metadata.pitch ?: 0f

        // Calculate distance estimate from focus rect and camera parameters
        val distance = userDistanceEstimate ?: estimateDistanceFromFocusRect(
            focusRect = focusRect,
            pitch = pitch,
            photoPath = photoPath
        )

        // Calculate horizontal and vertical angles to socket from focus rect center
        val (horizontalAngle, verticalAngle) = calculateAnglesFromFocusRect(
            focusRect = focusRect,
            photoPath = photoPath,
            cameraPitch = pitch
        )

        // Adjust bearing to point toward socket
        val socketBearing = bearing + horizontalAngle

        // Calculate 3D offset in ENU coordinates
        val (offsetE, offsetN, offsetU) = ArCoordinateUtils.calculateENU(
            cameraLat = latitude,
            cameraLon = longitude,
            cameraAlt = altitude,
            bearing = socketBearing,
            pitch = pitch + verticalAngle,
            distance = distance
        )

        // Calculate absolute socket position for verification
        val (socketLat, socketLon, socketAlt) = ArCoordinateUtils.enuToGeographic(
            latitude, longitude, altitude,
            offsetE, offsetN, offsetU
        )

        val arData = ArSocketData(
            offsetEast = offsetE,
            offsetNorth = offsetN,
            offsetUp = offsetU,
            cameraLatitude = latitude,
            cameraLongitude = longitude,
            cameraAltitude = altitude,
            cameraBearing = bearing,
            cameraPitch = pitch,
            socketLatitude = socketLat,
            socketLongitude = socketLon,
            socketAltitude = socketAlt,
            confidence = 0.85f,  // High confidence with user-marked focus
            method = ArLocalizationMethod.PHOTO_MATCHING
        )

        return AnalysisResult(
            arSocketData = arData,
            method = AnalysisMethod.FOCUS_RECT,
            confidence = 0.85f
        )
    }

    /**
     * Analyze using ML Kit object detection
     * Detects electrical outlets/sockets in the image
     */
    private suspend fun analyzeWithAI(
        context: Context,
        photoPath: String,
        metadata: PhotoMetadata,
        userDistanceEstimate: Float?
    ): AnalysisResult {
        return try {
            // Use ML Kit to detect sockets
            val detections = SocketDetector.detectSockets(context, photoPath)

            if (detections.isEmpty()) {
                Log.d(TAG, "No sockets detected in photo")
                return AnalysisResult(
                    arSocketData = null,
                    method = AnalysisMethod.AI_DETECTION,
                    confidence = 0.0f,
                    errorMessage = "No sockets detected in image"
                )
            }

            // Use the most confident detection
            val bestDetection = SocketDetector.getBestDetection(detections)!!
            Log.d(TAG, "Best socket detection: confidence=${bestDetection.confidence}, label=${bestDetection.label}")

            // Convert detection to focus rect and analyze
            val syntheticMetadata = metadata.copy(
                focusRect = bestDetection.boundingBox
            )

            val result = analyzeWithFocusRect(photoPath, syntheticMetadata, userDistanceEstimate)

            // Adjust confidence based on AI detection confidence
            val adjustedConfidence = result.confidence * bestDetection.confidence

            return result.copy(
                arSocketData = result.arSocketData?.copy(
                    confidence = adjustedConfidence,
                    method = ArLocalizationMethod.PHOTO_MATCHING
                ),
                method = AnalysisMethod.AI_DETECTION,
                confidence = adjustedConfidence
            )

        } catch (e: Exception) {
            Log.e(TAG, "AI detection failed", e)
            return AnalysisResult(
                arSocketData = null,
                method = AnalysisMethod.AI_DETECTION,
                confidence = 0.0f,
                errorMessage = "AI detection error: ${e.message}"
            )
        }
    }

    /**
     * Estimate distance to socket based on focus rect size and camera pitch
     * Uses simple heuristics based on typical socket sizes
     */
    private fun estimateDistanceFromFocusRect(
        focusRect: RectF,
        pitch: Float,
        photoPath: String
    ): Float {
        // Get image dimensions
        val dimensions = PhotoUtils.getImageDimensions(photoPath)
        if (dimensions == null) {
            return 3.0f // Default 3 meters
        }

        val (imageWidth, imageHeight) = dimensions

        // Calculate focus rect size as fraction of image
        val focusWidth = focusRect.width()
        val focusHeight = focusRect.height()

        // Normalize to image size (assuming focusRect is in pixel coordinates)
        val normalizedWidth = focusWidth / imageWidth
        val normalizedHeight = focusHeight / imageHeight

        // Average socket is about 10cm (0.1m) wide
        // Typical smartphone camera has ~70 degree horizontal FOV
        val typicalSocketSize = 0.1f  // meters
        val cameraFOV = 70f  // degrees (horizontal)

        // Estimate distance using similar triangles
        // distance = (object_size * image_width) / (focus_width * 2 * tan(FOV/2))
        val fovRadians = Math.toRadians(cameraFOV.toDouble())
        val distance = (typicalSocketSize * imageWidth) /
                      (focusWidth * 2 * tan(fovRadians / 2))

        // Clamp to reasonable range (0.5m to 20m)
        return distance.toFloat().coerceIn(0.5f, 20f)
    }

    /**
     * Calculate horizontal and vertical angles from camera center to focus rect center
     * Returns: Pair(horizontalAngle, verticalAngle) in degrees
     */
    private fun calculateAnglesFromFocusRect(
        focusRect: RectF,
        photoPath: String,
        cameraPitch: Float
    ): Pair<Float, Float> {
        val dimensions = PhotoUtils.getImageDimensions(photoPath)
        if (dimensions == null) {
            return Pair(0f, 0f)
        }

        val (imageWidth, imageHeight) = dimensions

        // Calculate center of focus rect
        val focusCenterX = focusRect.centerX()
        val focusCenterY = focusRect.centerY()

        // Calculate image center
        val imageCenterX = imageWidth / 2f
        val imageCenterY = imageHeight / 2f

        // Calculate offset from center (normalized to -1 to 1)
        val normalizedOffsetX = (focusCenterX - imageCenterX) / (imageWidth / 2f)
        val normalizedOffsetY = (focusCenterY - imageCenterY) / (imageHeight / 2f)

        // Typical smartphone camera FOV
        val horizontalFOV = 70f  // degrees
        val verticalFOV = 55f    // degrees (approximate, depends on aspect ratio)

        // Calculate angles
        val horizontalAngle = normalizedOffsetX * (horizontalFOV / 2f)
        val verticalAngle = -normalizedOffsetY * (verticalFOV / 2f)  // Negative because Y increases downward

        return Pair(horizontalAngle, verticalAngle)
    }

    /**
     * Batch analyze multiple photos for a location
     * Returns the best AR data from all photos
     */
    suspend fun analyzeBestPhoto(
        context: Context,
        photoPaths: List<String>
    ): AnalysisResult? {
        if (photoPaths.isEmpty()) return null

        val results = photoPaths.map { analyzePhoto(context, it) }

        // Return the result with highest confidence
        return results.maxByOrNull { it.confidence }
    }
}
