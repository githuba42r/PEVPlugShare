package com.a42r.pevplugshare.util

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Handles migration of backed-up files from /sdcard to app data directory.
 * Used for restoring images from device backups in emulator/clean install scenarios.
 */
object FileMigrationHelper {
    private const val TAG = "FileMigrationHelper"
    private const val SOURCE_DIR = "/sdcard/location_images"
    private const val DEST_DIR_NAME = "location_images"

    /**
     * Migrate backed-up location images from sdcard to app data directory.
     * Safe to call multiple times - skips if files already exist.
     * Runs asynchronously to not block app startup.
     */
    fun migrateBackupFilesAsync(context: Context) {
        Thread {
            try {
                migrateBackupFiles(context)
            } catch (e: Exception) {
                Log.e(TAG, "Unhandled error in migration", e)
            }
        }.start()
    }

    /**
     * Perform the actual file migration.
     * Handles missing directories and permission errors gracefully.
     */
    private fun migrateBackupFiles(context: Context) {
        val sourceDir = File(SOURCE_DIR)

        Log.d(TAG, "Starting migration check from $SOURCE_DIR")

        // Check if source directory exists
        if (!sourceDir.exists()) {
            Log.d(TAG, "Source directory does not exist: $SOURCE_DIR")
            return
        }

        if (!sourceDir.isDirectory) {
            Log.w(TAG, "Source path exists but is not a directory: $SOURCE_DIR")
            return
        }

        // List files with error handling
        val files: Array<File> = try {
            sourceDir.listFiles() ?: run {
                Log.w(TAG, "listFiles() returned null for $SOURCE_DIR")
                emptyArray()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied reading $SOURCE_DIR", e)
            emptyArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error listing source directory: ${e.message}", e)
            emptyArray()
        }

        if (files.isEmpty()) {
            Log.d(TAG, "No files found in $SOURCE_DIR")
            return
        }

        Log.d(TAG, "Found ${files.size} items in source directory")

        // Create destination directory
        val destDir = File(context.filesDir, DEST_DIR_NAME)
        Log.d(TAG, "Destination: ${destDir.absolutePath}")

        if (!destDir.exists()) {
            try {
                if (destDir.mkdirs()) {
                    Log.d(TAG, "Created destination directory")
                } else {
                    Log.w(TAG, "Failed to create destination directory")
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating destination directory: ${e.message}", e)
                return
            }
        }

        // Skip if destination already has files
        val existingFiles = destDir.listFiles() ?: emptyArray()
        if (existingFiles.isNotEmpty()) {
            Log.d(TAG, "Destination already contains ${existingFiles.size} items - skipping migration")
            return
        }

        // Copy files
        var copiedCount = 0
        var failedCount = 0

        for (sourceFile in files) {
            try {
                if (sourceFile.isDirectory) {
                    val subDir = File(destDir, sourceFile.name)
                    val (copied, failed) = copyDirectoryRecursively(sourceFile, subDir)
                    copiedCount += copied
                    failedCount += failed
                } else if (sourceFile.isFile) {
                    try {
                        sourceFile.copyTo(File(destDir, sourceFile.name), overwrite = true)
                        copiedCount++
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to copy file ${sourceFile.name}: ${e.message}")
                        failedCount++
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error processing ${sourceFile.name}: ${e.message}")
                failedCount++
            }
        }

        Log.i(TAG, "Migration complete: $copiedCount files copied, $failedCount failed")
    }

    /**
     * Recursively copy a directory and all its contents.
     * Returns pair of (copiedCount, failedCount)
     */
    private fun copyDirectoryRecursively(sourceDir: File, destDir: File): Pair<Int, Int> {
        return try {
            if (!destDir.exists()) {
                destDir.mkdirs()
            }

            var copiedCount = 0
            var failedCount = 0

            val files = sourceDir.listFiles() ?: emptyArray()
            for (sourceFile in files) {
                try {
                    val destFile = File(destDir, sourceFile.name)
                    if (sourceFile.isDirectory) {
                        val (copied, failed) = copyDirectoryRecursively(sourceFile, destFile)
                        copiedCount += copied
                        failedCount += failed
                    } else if (sourceFile.isFile) {
                        sourceFile.copyTo(destFile, overwrite = true)
                        copiedCount++
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error copying ${sourceFile.name}: ${e.message}")
                    failedCount++
                }
            }

            Pair(copiedCount, failedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error copying directory ${sourceDir.path}: ${e.message}", e)
            Pair(0, 1)
        }
    }
}
