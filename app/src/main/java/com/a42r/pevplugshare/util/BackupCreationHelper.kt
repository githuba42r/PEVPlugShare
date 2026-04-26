package com.a42r.pevplugshare.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Helper for creating backup ZIP files containing database and optional images.
 * Generates backup files with structure: backup_YYYYMMDD_HHMMSS.zip
 */
class BackupCreationHelper(private val context: Context) {
    companion object {
        private const val TAG = "BackupCreationHelper"
        private const val DB_NAME = "pevplugshare.db"
        private const val DB_BACKUP_PATH = "apps/com.a42r.pevplugshare/db/"
        private const val IMAGES_BACKUP_PATH = "apps/com.a42r.pevplugshare/f/location_images/"
        private const val MANIFEST_NAME = "manifest.json"
    }

    fun interface ProgressCallback {
        fun onProgress(current: Int, total: Int, message: String)
    }

    data class BackupCreationResult(
        val file: File,
        val itemCount: Int,
        val fileSize: Long,
        val timestamp: Long,
        val databaseSize: Long,
        val photosCount: Int
    )

    /**
     * Create a backup ZIP file with database and optional images.
     *
     * @param includePhotos Whether to include location_images directory
     * @param description Optional description for the backup
     * @param progressCallback Callback for progress updates (current, total, message)
     * @return BackupCreationResult with file info
     */
    suspend fun createBackup(
        includePhotos: Boolean = true,
        description: String = "",
        progressCallback: ProgressCallback = ProgressCallback { _, _, _ -> }
    ): BackupCreationResult = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val dateStr = dateFormat.format(Date(timestamp))
            val backupFileName = "backup_$dateStr.zip"

            val backupFile = File(context.getExternalFilesDir(null), backupFileName)
            Log.d(TAG, "Creating backup: ${backupFile.path}")

            val dbDir = File(context.filesDir.parentFile, "databases")
            val dbFile = File(dbDir, DB_NAME)
            val dbWalFile = File(dbDir, "$DB_NAME-wal")
            val dbShmFile = File(dbDir, "$DB_NAME-shm")
            val imagesDir = File(context.filesDir, "location_images")

            var totalItems = 0
            var photosCount = 0
            var databaseSize = 0L

            // Count items for progress tracking
            if (dbFile.exists()) {
                databaseSize = dbFile.length()
                totalItems++
            }
            if (dbWalFile.exists()) totalItems++
            if (dbShmFile.exists()) totalItems++

            if (includePhotos && imagesDir.exists()) {
                val photoFiles = imagesDir.listFiles() ?: emptyArray()
                photosCount = photoFiles.size
                totalItems += photosCount
            }

            // Create manifest
            val manifest = createManifest(timestamp, databaseSize, photosCount, description, includePhotos)

            Log.d(TAG, "Backup manifest: $manifest")
            progressCallback.onProgress(0, 100, "Starting backup creation...")

            // Create ZIP file
            ZipOutputStream(backupFile.outputStream()).use { zos ->
                var currentItem = 0

                // Write manifest
                Log.d(TAG, "Adding manifest...")
                progressCallback.onProgress(currentItem, totalItems, "Writing manifest...")
                zos.putNextEntry(ZipEntry(MANIFEST_NAME))
                zos.write(manifest.toByteArray())
                zos.closeEntry()
                currentItem++

                // Copy database file
                if (dbFile.exists()) {
                    Log.d(TAG, "Adding database file (${dbFile.length()} bytes)...")
                    progressCallback.onProgress(currentItem, totalItems, "Copying database...")
                    copyFileToZip(zos, dbFile, DB_BACKUP_PATH + DB_NAME)
                    currentItem++
                }

                // Copy WAL file
                if (dbWalFile.exists()) {
                    Log.d(TAG, "Adding WAL file (${dbWalFile.length()} bytes)...")
                    progressCallback.onProgress(currentItem, totalItems, "Copying WAL file...")
                    copyFileToZip(zos, dbWalFile, DB_BACKUP_PATH + "$DB_NAME-wal")
                    currentItem++
                }

                // Copy SHM file
                if (dbShmFile.exists()) {
                    Log.d(TAG, "Adding SHM file (${dbShmFile.length()} bytes)...")
                    progressCallback.onProgress(currentItem, totalItems, "Copying SHM file...")
                    copyFileToZip(zos, dbShmFile, DB_BACKUP_PATH + "$DB_NAME-shm")
                    currentItem++
                }

                // Copy images directory
                if (includePhotos && imagesDir.exists()) {
                    val photoFiles = imagesDir.listFiles() ?: emptyArray()
                    for (photoFile in photoFiles) {
                        if (photoFile.isFile) {
                            Log.d(TAG, "Adding image: ${photoFile.name}")
                            progressCallback.onProgress(
                                currentItem,
                                totalItems,
                                "Copying images (${currentItem}/${photosCount})..."
                            )
                            copyFileToZip(zos, photoFile, IMAGES_BACKUP_PATH + photoFile.name)
                            currentItem++
                        }
                    }
                }

                progressCallback.onProgress(100, 100, "Backup complete!")
            }

            val fileSize = backupFile.length()
            Log.i(TAG, "Backup created successfully: ${backupFile.name} (${formatFileSize(fileSize)})")

            BackupCreationResult(
                file = backupFile,
                itemCount = totalItems,
                fileSize = fileSize,
                timestamp = timestamp,
                databaseSize = databaseSize,
                photosCount = photosCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup", e)
            throw Exception("Backup creation failed: ${e.message}", e)
        }
    }

    private fun copyFileToZip(
        zos: ZipOutputStream,
        sourceFile: File,
        zipEntryPath: String
    ) {
        try {
            val entry = ZipEntry(zipEntryPath)
            zos.putNextEntry(entry)

            sourceFile.inputStream().use { input ->
                input.copyTo(zos, bufferSize = 8192)
            }

            zos.closeEntry()
            Log.d(TAG, "Added to ZIP: $zipEntryPath (${sourceFile.length()} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Error copying file to ZIP: $zipEntryPath", e)
            throw e
        }
    }

    private fun createManifest(
        timestamp: Long,
        databaseSize: Long,
        photosCount: Int,
        description: String,
        includePhotos: Boolean
    ): String {
        val manifest = JSONObject()
        manifest.put("version", 1)
        manifest.put("appVersion", getAppVersion())
        manifest.put("timestamp", timestamp)
        manifest.put("description", description)
        manifest.put("databaseSize", databaseSize)
        manifest.put("photosCount", photosCount)
        manifest.put("includePhotos", includePhotos)

        // Add counts from database if available
        try {
            val dbDir = File(context.filesDir.parentFile, "databases")
            val dbFile = File(dbDir, DB_NAME)

            if (dbFile.exists()) {
                val dbCounts = getDbItemCounts(dbFile)
                manifest.put("itemCounts", dbCounts)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not retrieve item counts: ${e.message}")
        }

        return manifest.toString(2)
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun getDbItemCounts(dbFile: File): JSONObject {
        val counts = JSONObject()
        return try {
            val db = SQLiteDatabase.openDatabase(
                dbFile.path,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            try {
                // Count locations
                val locationsCursor = db.rawQuery("SELECT COUNT(*) FROM locations", null)
                val locationsCount = if (locationsCursor.moveToFirst()) locationsCursor.getInt(0) else 0
                locationsCursor.close()
                counts.put("locations", locationsCount)

                // Count photos
                val photosCursor = db.rawQuery("SELECT COUNT(*) FROM location_photos", null)
                val photosCount = if (photosCursor.moveToFirst()) photosCursor.getInt(0) else 0
                photosCursor.close()
                counts.put("photos", photosCount)

                // Count saved_for_later
                val savedCursor = db.rawQuery("SELECT COUNT(*) FROM saved_for_later", null)
                val savedCount = if (savedCursor.moveToFirst()) savedCursor.getInt(0) else 0
                savedCursor.close()
                counts.put("savedForLater", savedCount)

                Log.d(TAG, "Database item counts: $counts")
            } finally {
                db.close()
            }

            counts
        } catch (e: Exception) {
            Log.w(TAG, "Error getting item counts: ${e.message}")
            counts
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
        }
    }
}
