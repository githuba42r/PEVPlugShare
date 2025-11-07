package com.pevplugshare.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoStorageManager(private val context: Context) {

    private val photosDirectory: File
        get() {
            val dir = File(context.filesDir, "location_photos")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    private val thumbnailsDirectory: File
        get() {
            val dir = File(context.filesDir, "photo_thumbnails")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    fun savePhotoFromBitmap(
        bitmap: Bitmap,
        locationId: String,
        quality: Int = 85
    ): Pair<String, String>? {
        return try {
            val fileName = generatePhotoFileName(locationId)
            val photoFile = File(photosDirectory, fileName)

            // Save full resolution photo
            FileOutputStream(photoFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.flush()
            }

            // Create and save thumbnail
            val thumbnail = createThumbnail(bitmap, 200)
            val thumbnailFileName = "thumb_$fileName"
            val thumbnailFile = File(thumbnailsDirectory, thumbnailFileName)

            FileOutputStream(thumbnailFile).use { out ->
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 75, out)
                out.flush()
            }

            Log.d(TAG, "Photo saved: ${photoFile.absolutePath}")
            Log.d(TAG, "Thumbnail saved: ${thumbnailFile.absolutePath}")

            Pair(photoFile.absolutePath, thumbnailFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving photo", e)
            null
        }
    }

    fun deletePhoto(photoPath: String, thumbnailPath: String?): Boolean {
        return try {
            val photoFile = File(photoPath)
            val thumbnailFile = thumbnailPath?.let { File(it) }

            val photoDeleted = photoFile.delete()
            val thumbnailDeleted = thumbnailFile?.delete() ?: true

            Log.d(TAG, "Photo deleted: $photoDeleted, Thumbnail deleted: $thumbnailDeleted")
            photoDeleted && thumbnailDeleted
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting photo", e)
            false
        }
    }

    fun getPhotoFile(photoPath: String): File? {
        return try {
            val file = File(photoPath)
            if (file.exists()) file else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting photo file", e)
            null
        }
    }

    fun getPhotoSize(photoPath: String): Long {
        return try {
            File(photoPath).length()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting photo size", e)
            0L
        }
    }

    fun getLocationPhotos(locationId: String): List<File> {
        return try {
            photosDirectory.listFiles { file ->
                file.isFile && file.name.contains(locationId)
            }?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error listing photos", e)
            emptyList()
        }
    }

    fun clearLocationPhotos(locationId: String): Boolean {
        return try {
            var allDeleted = true
            val photos = getLocationPhotos(locationId)
            for (photo in photos) {
                if (!photo.delete()) {
                    allDeleted = false
                }
            }
            Log.d(TAG, "Location photos cleared for $locationId: $allDeleted")
            allDeleted
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing photos", e)
            false
        }
    }

    fun getStorageInfo(): StorageInfo {
        return try {
            val photoDir = photosDirectory
            val totalSize = calculateDirSize(photoDir)
            val photoCount = photoDir.listFiles()?.size ?: 0

            StorageInfo(
                totalPhotos = photoCount,
                totalSizeBytes = totalSize,
                availableSpaceBytes = photoDir.freeSpace
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting storage info", e)
            StorageInfo(0, 0, 0)
        }
    }

    private fun generatePhotoFileName(locationId: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        return "photo_${locationId}_$timestamp.jpg"
    }

    private fun createThumbnail(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val ratio = if (width > height) {
            maxSize.toFloat() / width
        } else {
            maxSize.toFloat() / height
        }

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) calculateDirSize(file) else file.length()
        }
        return size
    }

    data class StorageInfo(
        val totalPhotos: Int,
        val totalSizeBytes: Long,
        val availableSpaceBytes: Long
    ) {
        fun formatSize(): String {
            return formatBytes(totalSizeBytes)
        }

        fun formatAvailableSpace(): String {
            return formatBytes(availableSpaceBytes)
        }

        private fun formatBytes(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> "${bytes / (1024 * 1024 * 1024)} GB"
            }
        }
    }

    companion object {
        private const val TAG = "PhotoStorageManager"
    }
}
