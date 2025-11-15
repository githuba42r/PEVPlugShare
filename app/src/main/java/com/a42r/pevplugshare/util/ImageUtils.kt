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
    
    fun resizeAndSaveImage(context: Context, sourceFile: File, locationId: String, imageIndex: Int): File? {
        try {
            android.util.Log.d("ImageUtils", "resizeAndSaveImage: locationId=$locationId, imageIndex=$imageIndex, sourceFile=${sourceFile.name}")
            
            // Read the image
            val bitmap = decodeSampledBitmap(sourceFile.absolutePath, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
                ?: return null
            
            // Rotate if needed based on EXIF data
            val rotatedBitmap = rotateImageIfRequired(sourceFile.absolutePath, bitmap)
            
            // Save to app directory
            val outputDir = getLocationImageDirectory(context, locationId)
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
}
