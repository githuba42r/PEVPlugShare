package com.pevplugshare.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File

object PhotoUtils {

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

    private const val TAG = "PhotoUtils"
}
