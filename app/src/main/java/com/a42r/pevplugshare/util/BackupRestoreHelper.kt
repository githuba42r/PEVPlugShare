package com.a42r.pevplugshare.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Helper for restoring app backups from zip files.
 * Supports extracting and installing database and image files.
 */
class BackupRestoreHelper(private val context: Context) {
    companion object {
        private const val TAG = "BackupRestoreHelper"
        private const val DB_BACKUP_PATH = "apps/com.a42r.pevplugshare/db/pevplugshare.db"
        private const val IMAGES_BACKUP_PATH = "apps/com.a42r.pevplugshare/f/location_images"
    }

    fun interface ProgressCallback {
        fun onProgress(message: String, progress: Int)
    }

    sealed class RestoreResult {
        object Success : RestoreResult()
        data class Error(val message: String) : RestoreResult()
        data class Progress(val message: String, val progress: Int) : RestoreResult()
    }

    /**
     * Restore backup from zip file.
     * Extracts database and images, then installs them.
     */
    suspend fun restoreFromZip(zipFile: File, progressCallback: ProgressCallback? = null): RestoreResult {
        return withContext(Dispatchers.IO) {
            try {
                if (!zipFile.exists()) {
                    return@withContext RestoreResult.Error("Backup file not found: ${zipFile.path}")
                }

                if (!zipFile.canRead()) {
                    return@withContext RestoreResult.Error("Cannot read backup file")
                }

                Log.d(TAG, "Starting restore from: ${zipFile.path}")
                progressCallback?.onProgress("Starting restore...", 5)

                // Create temporary extraction directory
                val tempDir = File(context.cacheDir, "backup_restore_temp")
                if (tempDir.exists()) {
                    tempDir.deleteRecursively()
                }
                tempDir.mkdirs()

                // Extract zip
                Log.d(TAG, "Extracting backup zip...")
                progressCallback?.onProgress("Extracting backup file...", 15)
                extractZip(zipFile, tempDir)
                progressCallback?.onProgress("Extraction complete", 40)

                // Find and restore database
                Log.d(TAG, "Restoring database...")
                progressCallback?.onProgress("Restoring database...", 50)
                val dbResult = restoreDatabase(tempDir)
                if (dbResult !is RestoreResult.Success) {
                    tempDir.deleteRecursively()
                    return@withContext dbResult
                }
                progressCallback?.onProgress("Database restored", 70)

                // Find and restore images
                Log.d(TAG, "Restoring images...")
                progressCallback?.onProgress("Restoring images...", 75)
                val imagesResult = restoreImages(tempDir)
                if (imagesResult !is RestoreResult.Success) {
                    Log.w(TAG, "Images restore had issues, but database was restored: $imagesResult")
                    // Don't fail completely if images fail - database is more critical
                }
                progressCallback?.onProgress("Finalizing restore...", 90)

                // Cleanup
                tempDir.deleteRecursively()

                Log.i(TAG, "Backup restore completed successfully")
                progressCallback?.onProgress("Restore complete!", 100)
                RestoreResult.Success
            } catch (e: Exception) {
                Log.e(TAG, "Error during backup restore", e)
                RestoreResult.Error("Restore failed: ${e.message}")
            }
        }
    }

    private fun extractZip(zipFile: File, outputDir: File) {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(outputDir, entry.name)

                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    file.outputStream().use { output ->
                        zis.copyTo(output)
                    }
                }

                entry = zis.nextEntry
            }
        }
    }

    private fun restoreDatabase(tempDir: File): RestoreResult {
        return try {
            val backupDbFile = File(tempDir, DB_BACKUP_PATH)

            if (!backupDbFile.exists()) {
                Log.w(TAG, "Database file not found in backup at: $DB_BACKUP_PATH")
                return RestoreResult.Error("Database file not found in backup")
            }

            if (!backupDbFile.canRead()) {
                return RestoreResult.Error("Cannot read database file from backup")
            }

            Log.d(TAG, "Database file found: ${backupDbFile.path}, size: ${backupDbFile.length()} bytes")

            // Create app databases directory if needed
            val dbDir = File(context.filesDir.parentFile, "databases")
            dbDir.mkdirs()
            Log.d(TAG, "Database directory: ${dbDir.path}, exists: ${dbDir.exists()}, isDir: ${dbDir.isDirectory}")

            // Copy database file
            val appDbFile = File(dbDir, "pevplugshare.db")
            if (appDbFile.exists()) {
                Log.d(TAG, "Existing database file: size before delete: ${appDbFile.length()} bytes")
                appDbFile.delete()
            }

            backupDbFile.copyTo(appDbFile, overwrite = true)
            Log.i(TAG, "Database file copied to: ${appDbFile.path}, size after copy: ${appDbFile.length()} bytes")

            if (appDbFile.length() != backupDbFile.length()) {
                Log.w(TAG, "WARNING: Size mismatch! Backup: ${backupDbFile.length()}, Copied: ${appDbFile.length()}")
            }

            // Also copy any related files (WAL, SHM)
            val walFile = File(tempDir, "$DB_BACKUP_PATH-wal")
            val shmFile = File(tempDir, "$DB_BACKUP_PATH-shm")

            if (walFile.exists()) {
                walFile.copyTo(File(dbDir, "pevplugshare.db-wal"), overwrite = true)
                Log.d(TAG, "WAL file restored")
            }
            if (shmFile.exists()) {
                shmFile.copyTo(File(dbDir, "pevplugshare.db-shm"), overwrite = true)
                Log.d(TAG, "SHM file restored")
            }

            Log.i(TAG, "Database restored successfully - Room will handle migrations on app restart")
            RestoreResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring database", e)
            RestoreResult.Error("Database restore failed: ${e.message}")
        }
    }


    private fun restoreImages(tempDir: File): RestoreResult {
        return try {
            val backupImagesDir = File(tempDir, IMAGES_BACKUP_PATH)

            if (!backupImagesDir.exists()) {
                Log.w(TAG, "Images directory not found in backup")
                return RestoreResult.Success // Not critical
            }

            val appImagesDir = File(context.filesDir, "location_images")
            appImagesDir.mkdirs()

            // Copy all image files recursively
            var copiedCount = 0
            backupImagesDir.walk().forEach { sourceFile ->
                if (sourceFile.isFile) {
                    val relativePath = sourceFile.relativeTo(backupImagesDir)
                    val destFile = File(appImagesDir, relativePath.path)
                    destFile.parentFile?.mkdirs()

                    try {
                        sourceFile.copyTo(destFile, overwrite = true)
                        copiedCount++
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to copy image file: ${sourceFile.name}", e)
                    }
                }
            }

            Log.i(TAG, "Restored $copiedCount image files")
            RestoreResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring images", e)
            RestoreResult.Error("Images restore failed: ${e.message}")
        }
    }

    /**
     * Get information about a backup zip file without extracting.
     */
    fun getBackupInfo(zipFile: File): BackupInfo? {
        return try {
            var hasDatabase = false
            var imageCount = 0

            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    when {
                        entry.name == DB_BACKUP_PATH -> hasDatabase = true
                        entry.name.startsWith(IMAGES_BACKUP_PATH) && entry.name.endsWith(".jpg") -> imageCount++
                    }
                    entry = zis.nextEntry
                }
            }

            BackupInfo(
                fileName = zipFile.name,
                hasDatabase = hasDatabase,
                imageCount = imageCount,
                fileSize = zipFile.length()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading backup info", e)
            null
        }
    }

    data class BackupInfo(
        val fileName: String,
        val hasDatabase: Boolean,
        val imageCount: Int,
        val fileSize: Long
    )
}
