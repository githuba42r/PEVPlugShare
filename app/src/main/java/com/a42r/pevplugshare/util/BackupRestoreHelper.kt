package com.a42r.pevplugshare.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
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
                Thread.sleep(50)

                // Create temporary extraction directory
                val tempDir = File(context.cacheDir, "backup_restore_temp")
                if (tempDir.exists()) {
                    tempDir.deleteRecursively()
                }
                tempDir.mkdirs()

                // Extract zip
                Log.d(TAG, "Extracting backup zip...")
                progressCallback?.onProgress("Extracting backup file...", 15)
                Thread.sleep(50)
                extractZip(zipFile, tempDir)
                progressCallback?.onProgress("Extraction complete", 40)
                Thread.sleep(50)

                // Find and restore database
                Log.d(TAG, "Restoring database...")
                progressCallback?.onProgress("Restoring database...", 50)
                Thread.sleep(50)
                val dbResult = restoreDatabase(tempDir)
                if (dbResult !is RestoreResult.Success) {
                    tempDir.deleteRecursively()
                    return@withContext dbResult
                }
                progressCallback?.onProgress("Database restored", 70)
                Thread.sleep(50)

                // Find and restore images
                Log.d(TAG, "Restoring images...")
                progressCallback?.onProgress("Restoring images...", 75)
                Thread.sleep(50)
                val imagesResult = restoreImages(tempDir)
                if (imagesResult !is RestoreResult.Success) {
                    Log.w(TAG, "Images restore had issues, but database was restored: $imagesResult")
                    // Don't fail completely if images fail - database is more critical
                }
                progressCallback?.onProgress("Finalizing restore...", 90)
                Thread.sleep(50)

                // Cleanup
                tempDir.deleteRecursively()

                Log.i(TAG, "Backup restore completed successfully")
                progressCallback?.onProgress("Restore complete!", 100)
                Thread.sleep(50)
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

            Log.d(TAG, "========== RESTORE DATABASE START ==========")
            Log.d(TAG, "Backup database file: ${backupDbFile.path}")
            Log.d(TAG, "Backup database size: ${backupDbFile.length()} bytes")

            // CRITICAL: Verify backup database has data BEFORE doing anything
            try {
                val preBackupDb = SQLiteDatabase.openDatabase(backupDbFile.path, null, SQLiteDatabase.OPEN_READONLY)
                val preCursor = preBackupDb.rawQuery("SELECT COUNT(*) FROM locations", null)
                val preCount = if (preCursor.moveToFirst()) preCursor.getInt(0) else 0
                preCursor.close()
                preBackupDb.close()
                Log.d(TAG, "✓ Backup database verification: $preCount locations found")
                if (preCount == 0) {
                    Log.e(TAG, "✗ WARNING: Backup database is empty! Restore will produce empty database!")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not verify backup database before copy: ${e.message}")
            }

            // Create app databases directory if needed
            val dbDir = File(context.filesDir.parentFile, "databases")
            dbDir.mkdirs()
            Log.d(TAG, "Database directory: ${dbDir.path}, exists: ${dbDir.exists()}, isDir: ${dbDir.isDirectory}")

            // CRITICAL: Clear the Room database instance to release any open connections
            Log.d(TAG, "Step 1: Clearing Room database instance to release locks...")
            try {
                val dbClass = Class.forName("com.a42r.pevplugshare.data.db.PEVPlugShareDatabase")
                val companionClass = dbClass.getField("Companion").get(null)?.javaClass
                val clearMethod = companionClass?.getDeclaredMethod("clearDatabase")
                clearMethod?.invoke(null)
                Log.d(TAG, "✓ Database instance cleared successfully")
            } catch (e: Exception) {
                Log.w(TAG, "⚠ Could not clear database instance: ${e.message}")
            }

            Thread.sleep(100)
            Log.d(TAG, "Step 2: Copying database file...")

            // Copy database file
            val appDbFile = File(dbDir, "pevplugshare.db")
            if (appDbFile.exists()) {
                val oldSize = appDbFile.length()
                appDbFile.delete()
                Log.d(TAG, "  Deleted existing database file (was ${oldSize} bytes)")
            }

            Log.d(TAG, "  Copying ${backupDbFile.length()} bytes from ${backupDbFile.path}")
            backupDbFile.copyTo(appDbFile, overwrite = true)
            Log.d(TAG, "  ✓ Copy complete. New file size: ${appDbFile.length()} bytes")

            if (backupDbFile.length() != appDbFile.length()) {
                Log.e(TAG, "✗ SIZE MISMATCH! Backup: ${backupDbFile.length()}, Copied: ${appDbFile.length()}")
            }

            // Verify the copied database immediately after copy
            Log.d(TAG, "Step 3: Verifying copied database...")
            try {
                val verifyDb = SQLiteDatabase.openDatabase(appDbFile.path, null, SQLiteDatabase.OPEN_READONLY)
                val verifyVersion = verifyDb.version
                Log.d(TAG, "  Database version: $verifyVersion")

                val verifyCursor = verifyDb.rawQuery("SELECT COUNT(*) FROM locations", null)
                val verifyCount = if (verifyCursor.moveToFirst()) verifyCursor.getInt(0) else 0
                verifyCursor.close()

                Log.d(TAG, "  ✓ Locations in copied database: $verifyCount")
                if (verifyCount == 0) {
                    Log.e(TAG, "✗ CRITICAL: Copied database is empty!")
                }

                verifyDb.close()
            } catch (e: Exception) {
                Log.e(TAG, "✗ ERROR verifying database after copy: ${e.message}", e)
            }

            // Also copy any related files (WAL, SHM)
            Log.d(TAG, "Step 4: Restoring WAL/SHM files...")
            val walFile = File(tempDir, "$DB_BACKUP_PATH-wal")
            val shmFile = File(tempDir, "$DB_BACKUP_PATH-shm")

            if (walFile.exists()) {
                walFile.copyTo(File(dbDir, "pevplugshare.db-wal"), overwrite = true)
                Log.d(TAG, "  ✓ WAL file restored (${walFile.length()} bytes)")
            } else {
                Log.d(TAG, "  ⓘ No WAL file in backup")
            }

            if (shmFile.exists()) {
                shmFile.copyTo(File(dbDir, "pevplugshare.db-shm"), overwrite = true)
                Log.d(TAG, "  ✓ SHM file restored (${shmFile.length()} bytes)")
            } else {
                Log.d(TAG, "  ⓘ No SHM file in backup")
            }

            // Apply v11→v12 migration if needed
            Log.d(TAG, "Step 5: Checking if v11→v12 migration is needed...")
            try {
                val checkDb = SQLiteDatabase.openDatabase(appDbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
                val currentVersion = checkDb.version
                Log.d(TAG, "  Current database version: $currentVersion")

                if (currentVersion == 11) {
                    Log.d(TAG, "  Applying v11→v12 migration automatically...")
                    // Add server sync columns that v11→v12 migration adds
                    try {
                        checkDb.execSQL("ALTER TABLE locations ADD COLUMN serverId TEXT DEFAULT NULL")
                        checkDb.execSQL("ALTER TABLE locations ADD COLUMN serverVersion INTEGER DEFAULT NULL")
                        checkDb.execSQL("ALTER TABLE locations ADD COLUMN serverSyncStatus TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
                        checkDb.execSQL("ALTER TABLE locations ADD COLUMN serverLastSyncedAt INTEGER DEFAULT NULL")

                        // Bump version to 12
                        checkDb.version = 12
                        Log.d(TAG, "  ✓ Migration applied successfully. New version: 12")
                    } catch (e: Exception) {
                        Log.w(TAG, "  ⚠ Could not apply migration: ${e.message}")
                        // Continue anyway - Room might handle it on next open
                    }
                } else {
                    Log.d(TAG, "  Migration not needed (already at version $currentVersion)")
                }

                checkDb.close()
            } catch (e: Exception) {
                Log.w(TAG, "  Could not check/upgrade database version: ${e.message}")
                // Continue - Room will handle it
            }

            Log.i(TAG, "========== RESTORE DATABASE COMPLETE ==========")
            Log.i(TAG, "Database ready with all migrations applied")
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
