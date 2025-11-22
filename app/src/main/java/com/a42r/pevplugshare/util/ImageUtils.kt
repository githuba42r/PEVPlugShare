package com.a42r.pevplugshare.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ImageUtils {
    private const val MAX_IMAGE_DIMENSION = 1024 // Max width or height
    private const val JPEG_QUALITY = 85

    fun getAppImagesDirectory(context: Context): File {
        val imagesDir = File(context.filesDir, "location_images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        return imagesDir
    }

    fun getLocationImageDirectory(context: Context, locationId: String): File {
        val locationDir = File(getAppImagesDirectory(context), locationId)
        if (!locationDir.exists()) {
            locationDir.mkdirs()
        }
        return locationDir
    }

    fun getTempImageDirectory(context: Context, sessionId: String): File {
        val tempDir = File(context.cacheDir, "temp_images/$sessionId")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        return tempDir
    }

    fun resizeAndSaveImage(context: Context, sourceFile: File, id: String, imageIndex: Int, isTemporary: Boolean = false): File? {
        try {
            android.util.Log.d("ImageUtils", "resizeAndSaveImage: id=$id, imageIndex=$imageIndex, sourceFile=${sourceFile.name}, isTemporary=$isTemporary")

            // Read the image
            val bitmap = decodeSampledBitmap(sourceFile.absolutePath, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
                ?: return null

            // Rotate if needed based on EXIF data
            val rotatedBitmap = rotateImageIfRequired(sourceFile.absolutePath, bitmap)

            // Determine output directory
            val outputDir = if (isTemporary) {
                getTempImageDirectory(context, id)
            } else {
                getLocationImageDirectory(context, id)
            }
            val outputFile = File(outputDir, "image_$imageIndex.jpg")

            android.util.Log.d("ImageUtils", "  - outputDir: ${outputDir.absolutePath}")
            android.util.Log.d("ImageUtils", "  - outputFile: ${outputFile.absolutePath}")

            FileOutputStream(outputFile).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }

            android.util.Log.d("ImageUtils", "  - File saved successfully, size: ${outputFile.length()} bytes")

            if (rotatedBitmap != bitmap) {
                rotatedBitmap.recycle()
            }
            bitmap.recycle()

            return outputFile
        } catch (e: Exception) {
            android.util.Log.e("ImageUtils", "Error saving image", e)
            e.printStackTrace()
            return null
        }
    }

    private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, this)

            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

            inJustDecodeBounds = false
            BitmapFactory.decodeFile(path, this)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun rotateImageIfRequired(path: String, bitmap: Bitmap): Bitmap {
        try {
            val exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: IOException) {
            return bitmap
        }
    }

    private fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun getLocationImages(context: Context, locationId: String): List<File> {
        val locationDir = getLocationImageDirectory(context, locationId)
        val result = locationDir.listFiles()?.filter { it.extension == "jpg" }?.sortedBy { it.name } ?: emptyList()
        android.util.Log.d("ImageUtils", "getLocationImages: locationId=$locationId, found ${result.size} images")
        result.forEach { file ->
            android.util.Log.d("ImageUtils", "  - ${file.name} (${file.length()} bytes)")
        }
        return result
    }

    fun deleteLocationImage(context: Context, locationId: String, imageIndex: Int): Boolean {
        val locationDir = getLocationImageDirectory(context, locationId)
        val imageFile = File(locationDir, "image_$imageIndex.jpg")
        return imageFile.delete()
    }

    fun deleteAllLocationImages(context: Context, locationId: String) {
        val locationDir = getLocationImageDirectory(context, locationId)
        locationDir.listFiles()?.forEach { it.delete() }
    }

    fun deleteImageFile(imagePath: String): Boolean {
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageUtils", "Error deleting image file: $imagePath", e)
            false
        }
    }

    fun moveTempImagesToPermanent(context: Context, sessionId: String, locationId: String): List<String> {
        val tempDir = getTempImageDirectory(context, sessionId)
        val permanentDir = getLocationImageDirectory(context, locationId)
        val newImagePaths = mutableListOf<String>()

        if (tempDir.exists() && tempDir.isDirectory) {
            // Get existing images in the permanent directory to find the next available index
            val existingPermanentImages = getLocationImages(context, locationId)
            var nextIndex = if (existingPermanentImages.isEmpty()) {
                0
            } else {
                existingPermanentImages.mapNotNull { file ->
                    file.name.removePrefix("image_").removeSuffix(".jpg").toIntOrNull()
                }.maxOrNull()?.plus(1) ?: 0
            }

            tempDir.listFiles()?.filter { it.extension == "jpg" }?.forEach { tempFile ->
                val newFileName = "image_$nextIndex.jpg"
                val newPermanentFile = File(permanentDir, newFileName)
                try {
                    tempFile.renameTo(newPermanentFile)
                    newImagePaths.add(newPermanentFile.absolutePath)
                    android.util.Log.d("ImageUtils", "Moved temp image ${tempFile.name} to ${newPermanentFile.absolutePath}")
                    nextIndex++
                } catch (e: Exception) {
                    android.util.Log.e("ImageUtils", "Error moving temp image ${tempFile.name}", e)
                }
            }
            // Delete the temporary directory after moving files
            tempDir.deleteRecursively()
            android.util.Log.d("ImageUtils", "Deleted temporary directory: ${tempDir.absolutePath}")
        }
        return newImagePaths
    }

    fun deleteTempImages(context: Context, sessionId: String) {
        val tempDir = getTempImageDirectory(context, sessionId)
        if (tempDir.exists() && tempDir.isDirectory) {
            tempDir.deleteRecursively()
            android.util.Log.d("ImageUtils", "Deleted temporary directory: ${tempDir.absolutePath}")
        }
    }

    fun renameLocationImageDirectory(context: Context, oldLocationId: String, newLocationId: String): List<String> {
        val oldDir = getLocationImageDirectory(context, oldLocationId)
        val newDir = getLocationImageDirectory(context, newLocationId)

        if (oldDir.exists()) {
            if (newDir.exists()) {
                android.util.Log.w("ImageUtils", "Target directory $newLocationId already exists. Attempting to delete if empty.")
                if (newDir.isDirectory && newDir.list()?.isEmpty() == true) {
                    if (newDir.delete()) {
                        android.util.Log.d("ImageUtils", "Empty target directory $newLocationId deleted.")
                    } else {
                        android.util.Log.e("ImageUtils", "Failed to delete empty target directory $newLocationId.")
                        return emptyList()
                    }
                } else if (newDir.isDirectory && newDir.list()?.isNotEmpty() == true) {
                    android.util.Log.e("ImageUtils", "Target directory $newLocationId exists and is NOT empty. Cannot rename.")
                    // If the new directory exists and is not empty, we cannot rename.
                    // This might indicate a previous failed operation or a logic error.
                    // For now, we'll return emptyList() to prevent data corruption.
                    return emptyList()
                }
            }

            if (oldDir.renameTo(newDir)) {
                android.util.Log.d("ImageUtils", "Renamed image directory from $oldLocationId to $newLocationId")
                // Return updated paths
                return newDir.listFiles()?.filter { it.extension == "jpg" }?.map { it.absolutePath } ?: emptyList()
            } else {
                android.util.Log.e("ImageUtils", "Failed to rename image directory from $oldLocationId to $newLocationId. oldDir exists: ${oldDir.exists()}, newDir exists: ${newDir.exists()}")
            }
        } else {
            android.util.Log.w("ImageUtils", "Source directory $oldLocationId does not exist. No renaming needed.")
        }
        return emptyList()
    }

    /**
     * Add GPS location data to an image file's EXIF metadata.
     * If the image already has GPS data, it will not be overwritten.
     *
     * @param imageFile The image file to add GPS data to
     * @param latitude The latitude in decimal degrees
     * @param longitude The longitude in decimal degrees
     * @return true if GPS data was added or already present, false if there was an error
     */
    fun addGpsDataToImage(imageFile: File, latitude: Double, longitude: Double): Boolean {
        if (!imageFile.exists()) {
            android.util.Log.e("ImageUtils", "Image file does not exist: ${imageFile.absolutePath}")
            return false
        }

        try {
            val exif = ExifInterface(imageFile.absolutePath)

            // Check if GPS data already exists
            val existingLat = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
            if (existingLat != null) {
                android.util.Log.d("ImageUtils", "Image already has GPS data, skipping: ${imageFile.name}")
                return true
            }

            // Convert decimal degrees to DMS (Degrees, Minutes, Seconds) format for EXIF
            val latDMS = convertDecimalToDMS(Math.abs(latitude))
            val lonDMS = convertDecimalToDMS(Math.abs(longitude))

            // Set GPS latitude
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latDMS)
            exif.setAttribute(
                ExifInterface.TAG_GPS_LATITUDE_REF,
                if (latitude >= 0) "N" else "S"
            )

            // Set GPS longitude
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, lonDMS)
            exif.setAttribute(
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                if (longitude >= 0) "E" else "W"
            )

            // Save the EXIF data
            exif.saveAttributes()

            android.util.Log.d("ImageUtils", "Added GPS data to image: ${imageFile.name} (lat: $latitude, lon: $longitude)")
            return true
        } catch (e: Exception) {
            android.util.Log.e("ImageUtils", "Error adding GPS data to image: ${imageFile.absolutePath}", e)
            return false
        }
    }

    /**
     * Convert decimal degrees to DMS (Degrees, Minutes, Seconds) format for EXIF
     * Format: "degrees/1,minutes/1,seconds/1000"
     */
    private fun convertDecimalToDMS(decimalDegrees: Double): String {
        val degrees = decimalDegrees.toInt()
        val minutesDecimal = (decimalDegrees - degrees) * 60
        val minutes = minutesDecimal.toInt()
        val seconds = ((minutesDecimal - minutes) * 60 * 1000).toInt()

        return "$degrees/1,$minutes/1,$seconds/1000"
    }
}
