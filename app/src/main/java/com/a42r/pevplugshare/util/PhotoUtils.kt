package com.a42r.pevplugshare.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.a42r.pevplugshare.data.model.PhotoMetadata
import com.a42r.pevplugshare.ui.edit.ImageEditorActivity
import java.io.File

object PhotoUtils {
    fun getPhotoMetadataFromUri(context: android.content.Context, uri: android.net.Uri): PhotoMetadata {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Could not open input stream for URI: $uri")
                return PhotoMetadata()
            }

            val exif = androidx.exifinterface.media.ExifInterface(inputStream)
            val latLong = exif.latLong
            val latitude = latLong?.get(0)
            val longitude = latLong?.get(1)
            val bearing = extractBearing(exif)
            val focusRectString = exif.getAttribute(ImageEditorActivity.EXIF_TAG_PEVPLUGSHARE_FOCUS_RECT)
            val focusRect = parseFocusRectString(focusRectString)

            PhotoMetadata(
                latitude = latitude,
                longitude = longitude,
                altitude = null,
                bearing = bearing,
                dateTime = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME),
                make = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKE),
                model = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL),
                hasGpsData = latitude != null && longitude != null,
                hasBearingData = bearing != null,
                focusRect = focusRect
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading photo metadata from URI", e)
            PhotoMetadata()
        }
    }

    fun getPhotoMetadata(photoPath: String): PhotoMetadata {
        return try {
            val exif = ExifInterface(photoPath)
            val latLong = exif.latLong
            val latitude = latLong?.get(0)
            val longitude = latLong?.get(1)

            // Extract bearing/direction if available (some devices record this)
            val bearing = extractBearing(exif)
            val focusRectString = exif.getAttribute(ImageEditorActivity.EXIF_TAG_PEVPLUGSHARE_FOCUS_RECT)
            val focusRect = parseFocusRectString(focusRectString)

            PhotoMetadata(
                latitude = latitude,
                longitude = longitude,
                altitude = null, // EXIF_TAG_ALTITUDE not available in simple way, skip for now
                bearing = bearing,
                dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME),
                make = exif.getAttribute(ExifInterface.TAG_MAKE),
                model = exif.getAttribute(ExifInterface.TAG_MODEL),
                hasGpsData = latitude != null && longitude != null,
                hasBearingData = bearing != null,
                focusRect = focusRect
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading photo metadata", e)
            PhotoMetadata()
        }
    }

    private fun extractBearing(exif: ExifInterface): Float? {
        return try {
            // Try to get bearing from various EXIF tags that might store it
            val bearing = exif.getAttribute("GPSImgDirection")?.toFloatOrNull()
                ?: exif.getAttribute("GPSDestBearing")?.toFloatOrNull()

            // If no bearing in EXIF, try to infer from device orientation
            // This is less accurate but better than nothing
            if (bearing == null) {
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                // Use orientation as a rough estimate (not accurate but better than nothing)
                return null // Better to return null than inaccurate data
            }

            bearing?.let { if (it < 0f) it + 360f else it }
        } catch (e: Exception) {
            Log.d(TAG, "Could not extract bearing from EXIF", e)
            null
        }
    }

    fun getExifData(photoPath: String): Map<String, String> {
        return try {
            val exif = ExifInterface(photoPath)
            mapOf(
                "latitude" to (exif.latLong?.get(0)?.toString() ?: "N/A"),
                "longitude" to (exif.latLong?.get(1)?.toString() ?: "N/A"),
                "altitude" to "N/A",
                "dateTime" to (exif.getAttribute(ExifInterface.TAG_DATETIME) ?: "N/A"),
                "make" to (exif.getAttribute(ExifInterface.TAG_MAKE) ?: "N/A"),
                "model" to (exif.getAttribute(ExifInterface.TAG_MODEL) ?: "N/A"),
                "orientation" to (exif.getAttribute(ExifInterface.TAG_ORIENTATION) ?: "0")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading EXIF data", e)
            emptyMap()
        }
    }

    fun rotateImageIfNeeded(photoPath: String): Bitmap? {
        return try {
            var bitmap = BitmapFactory.decodeFile(photoPath) ?: return null

            val exif = ExifInterface(photoPath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            bitmap = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipBitmap(bitmap, true, false)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> flipBitmap(bitmap, false, true)
                else -> bitmap
            }

            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error rotating image", e)
            null
        }
    }

    fun rotateImageIfNeeded(photoPath: String, options: BitmapFactory.Options): Bitmap? {
        return try {
            var bitmap = BitmapFactory.decodeFile(photoPath, options) ?: return null

            val exif = ExifInterface(photoPath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            bitmap = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipBitmap(bitmap, true, false)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> flipBitmap(bitmap, false, true)
                else -> bitmap
            }

            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error rotating image with options", e)
            null
        }
    }

    fun compressImage(inputPath: String, outputPath: String, quality: Int = 85): Boolean {
        return try {
            val bitmap = rotateImageIfNeeded(inputPath) ?: return false

            val file = File(outputPath)
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            Log.d(TAG, "Image compressed: $outputPath (quality: $quality)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image", e)
            false
        }
    }

    fun getImageDimensions(photoPath: String): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(photoPath, options)
            Pair(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting image dimensions", e)
            null
        }
    }

    fun validatePhotoFile(photoPath: String): Boolean {
        return try {
            val file = File(photoPath)
            if (!file.exists()) return false
            if (file.length() == 0L) return false

            // Try to decode to verify it's a valid image
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(photoPath, options)
            options.outWidth > 0 && options.outHeight > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error validating photo", e)
            false
        }
    }

    fun extractLocationFromExif(photoPath: String): Pair<Double, Double>? {
        return try {
            val exif = ExifInterface(photoPath)
            val latLong = exif.latLong
            if (latLong != null && latLong.size == 2) {
                Pair(latLong[0], latLong[1])
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting location from EXIF", e)
            null
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degree) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun flipBitmap(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) preScale(-1f, 1f, bitmap.width / 2f, 0f)
            if (vertical) preScale(1f, -1f, 0f, bitmap.height / 2f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    internal fun parseFocusRectString(focusRectString: String?): RectF? {
        if (focusRectString == null) return null
        return try {
            val parts = focusRectString.split(',').map { it.toFloat() }
            if (parts.size == 4) {
                RectF(parts[0], parts[1], parts[2], parts[3])
            } else {
                Log.w(TAG, "Invalid focusRectString format: $focusRectString")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing focusRectString: $focusRectString", e)
            null
        }
    }

    /**
     * Normalize focus rectangle to 0-1 coordinates relative to bitmap dimensions
     * This ensures the focus rectangle remains valid even if the image is scaled/resampled
     */
    fun normalizeFocusRect(focusRect: RectF, bitmapWidth: Int, bitmapHeight: Int): RectF {
        return RectF(
            focusRect.left / bitmapWidth,
            focusRect.top / bitmapHeight,
            focusRect.right / bitmapWidth,
            focusRect.bottom / bitmapHeight
        )
    }

    /**
     * Denormalize focus rectangle from 0-1 coordinates to actual pixel coordinates
     * based on provided bitmap dimensions
     */
    fun denormalizeFocusRect(normalizedRect: RectF, bitmapWidth: Int, bitmapHeight: Int): RectF {
        return RectF(
            normalizedRect.left * bitmapWidth,
            normalizedRect.top * bitmapHeight,
            normalizedRect.right * bitmapWidth,
            normalizedRect.bottom * bitmapHeight
        )
    }

    /**
     * Convert focus rectangle to normalized string format (0-1 coordinates)
     */
    fun focusRectToNormalizedString(focusRect: RectF, bitmapWidth: Int, bitmapHeight: Int): String {
        Log.d(TAG, "Normalizing focus rect: ${focusRect.left},${focusRect.top},${focusRect.right},${focusRect.bottom}")
        Log.d(TAG, "Using bitmap dimensions: $bitmapWidth x $bitmapHeight")
        val normalized = normalizeFocusRect(focusRect, bitmapWidth, bitmapHeight)
        val result = "${normalized.left},${normalized.top},${normalized.right},${normalized.bottom}"
        Log.d(TAG, "Normalized result: $result")
        return result
    }

    /**
     * Convert normalized string to focus rectangle in pixel coordinates
     */
    fun normalizedStringToFocusRect(normalizedString: String?, bitmapWidth: Int, bitmapHeight: Int): RectF? {
        if (normalizedString == null) return null
        return try {
            val parts = normalizedString.trim().split(',').map { it.trim().toFloat() }
            if (parts.size == 4) {
                val normalizedRect = RectF(parts[0], parts[1], parts[2], parts[3])
                Log.d(TAG, "Parsed normalized rect: ${normalizedRect.left},${normalizedRect.top},${normalizedRect.right},${normalizedRect.bottom}")
                Log.d(TAG, "Denormalizing with bitmap dimensions: $bitmapWidth x $bitmapHeight")
                val denormalized = denormalizeFocusRect(normalizedRect, bitmapWidth, bitmapHeight)
                Log.d(TAG, "Denormalized to: ${denormalized.left},${denormalized.top},${denormalized.right},${denormalized.bottom}")
                denormalized
            } else {
                Log.w(TAG, "Invalid normalized focusRectString format: $normalizedString (parts.size=${parts.size})")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing normalized focusRectString: $normalizedString", e)
            null
        }
    }

    private const val TAG = "PhotoUtils"
}
