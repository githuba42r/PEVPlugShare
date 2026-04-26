package com.a42r.pevplugshare

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.a42r.pevplugshare.data.db.PEVPlugShareDatabase
import com.a42r.pevplugshare.data.db.entity.LocationEntity
import com.a42r.pevplugshare.util.BackupRestoreHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class BackupRestoreTest {
    private lateinit var context: Context
    private lateinit var testDir: File
    private lateinit var db: PEVPlugShareDatabase

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        testDir = File(context.cacheDir, "backup_restore_test")
        testDir.mkdirs()

        // Get or create database
        db = PEVPlugShareDatabase.getDatabase(context)
    }

    @After
    fun tearDown() {
        // Cleanup
        testDir.deleteRecursively()
    }

    @Test
    fun testBackupFileStructure() {
        // Test 1: Verify backup.zip has correct structure
        // Search for backup file in multiple locations
        val possibleLocations = listOf(
            File(context.getExternalFilesDir(null), "backup_full.zip"),
            File(context.cacheDir, "backup_full.zip"),
            File(context.filesDir, "backup_full.zip"),
            File("/sdcard/Download/backup_full.zip"),
            File("/sdcard/Downloads/backup_full.zip")
        )

        val backupZip = possibleLocations.firstOrNull { it.exists() }
            ?: run {
                println("Backup file not found in any location:")
                possibleLocations.forEach { println("  - ${it.path} (exists: ${it.exists()})") }
                // Try to find any backup_full.zip
                File("/sdcard").walk()
                    .filter { it.name == "backup_full.zip" }
                    .forEach { println("  Found: ${it.path}") }
                possibleLocations[0]
            }

        assertTrue("Backup file should exist at ${backupZip.path}", backupZip.exists())
        assertTrue("Backup file should be readable", backupZip.canRead())
        assertTrue("Backup file should be non-empty (size: ${backupZip.length()})", backupZip.length() > 0)

        // Check zip contents
        val entries = mutableListOf<String>()
        val zis = java.util.zip.ZipInputStream(backupZip.inputStream())
        var entry = zis.nextEntry
        while (entry != null) {
            entries.add(entry.name)
            entry = zis.nextEntry
        }
        zis.close()

        println("=== Backup ZIP Contents ===")
        entries.forEach { println("  $it") }

        val dbPath = "apps/com.a42r.pevplugshare/db/pevplugshare.db"
        assertTrue("Backup should contain database at $dbPath", entries.contains(dbPath))
        assertTrue("Backup should contain images", entries.any { it.contains("location_images") })
    }

    @Test
    fun testBackupDatabaseHasData() {
        // Test 2: Extract and verify backup database contains data
        val backupZip = findBackupFile()

        val extractDir = File(testDir, "extract_test")
        extractDir.mkdirs()

        // Extract backup
        val zis = java.util.zip.ZipInputStream(backupZip.inputStream())
        var entry = zis.nextEntry
        while (entry != null) {
            val file = File(extractDir, entry.name)
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
        zis.close()

        // Verify extracted database
        val dbFile = File(extractDir, "apps/com.a42r.pevplugshare/db/pevplugshare.db")
        assertTrue("Database should exist in extracted backup", dbFile.exists())
        assertTrue("Database should be non-empty", dbFile.length() > 0)

        // Query extracted database
        val backupDb = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
        try {
            val cursor = backupDb.rawQuery("SELECT COUNT(*) FROM locations", null)
            val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
            cursor.close()

            println("Locations in backup database: $count")
            assertTrue("Backup database should have locations", count > 0)
        } finally {
            backupDb.close()
        }
    }

    @Test
    fun testRestoreProcess() = runBlocking {
        // Test 3: Full restore process
        val backupZip = findBackupFile()

        // Clear database before restore
        PEVPlugShareDatabase.clearDatabase()
        Thread.sleep(500)

        val helper = BackupRestoreHelper(context)

        var lastProgress = 0
        val progressUpdates = mutableListOf<String>()
        val progressCallback = BackupRestoreHelper.ProgressCallback { message, progress ->
            lastProgress = progress
            progressUpdates.add("$progress%: $message")
            println("Progress: $progress% - $message")
        }

        val result = helper.restoreFromZip(backupZip, progressCallback)

        println("=== Progress Updates ===")
        progressUpdates.forEach { println("  $it") }
        println("=== Restore Result ===")
        println("Result: $result")

        when (result) {
            is BackupRestoreHelper.RestoreResult.Success -> {
                println("✓ Restore succeeded")
            }
            is BackupRestoreHelper.RestoreResult.Error -> {
                println("✗ Restore failed: ${result.message}")
                fail("Restore failed: ${result.message}")
            }
            is BackupRestoreHelper.RestoreResult.Progress -> {
                println("Progress: ${result.message}")
            }
        }

        assertEquals("Should complete at 100%", 100, lastProgress)
        assertTrue("Should have multiple progress updates", progressUpdates.size > 3)
    }

    private fun findBackupFile(): File {
        val searchLocations = listOf(
            "/data/local/tmp/backup_full.zip",
            "/sdcard/Downloads/backup_full.zip",
            "/sdcard/Download/backup_full.zip",
            "${context.cacheDir.path}/backup_full.zip"
        )

        // Check each location
        for (path in searchLocations) {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                println("✓ Found backup at: $path (size: ${file.length()} bytes)")
                return file
            } else {
                println("✗ Not found or unreadable: $path")
            }
        }

        throw AssertionError("Backup file not found in any location")
    }

    @Test
    fun testRestoredDatabaseContent() = runBlocking {
        // Test 4: Verify restored database has all data
        val backupZip = findBackupFile()

        // Clear and restore
        PEVPlugShareDatabase.clearDatabase()
        Thread.sleep(500) // Ensure database is closed

        val helper = BackupRestoreHelper(context)
        helper.restoreFromZip(backupZip)

        Thread.sleep(500) // Wait for restore to complete

        // Reopen database
        val dbPath = context.getDatabasePath("pevplugshare.db").absolutePath
        val restoredDb = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)

        try {
            // Count locations
            val locationCursor = restoredDb.rawQuery("SELECT COUNT(*) FROM locations", null)
            val locationCount = if (locationCursor.moveToFirst()) locationCursor.getInt(0) else 0
            locationCursor.close()

            println("Locations after restore: $locationCount")
            assertTrue("Restored database should have locations", locationCount > 0)

            // Count saved items
            val savedCursor = restoredDb.rawQuery("SELECT COUNT(*) FROM saved_for_later", null)
            val savedCount = if (savedCursor.moveToFirst()) savedCursor.getInt(0) else 0
            savedCursor.close()

            println("Saved items after restore: $savedCount")
            assertTrue("Restored database should have saved items", savedCount > 0)

            // Check database version
            println("Database version: ${restoredDb.version}")

        } finally {
            restoredDb.close()
        }
    }

    @Test
    fun testImagesRestored() = runBlocking {
        // Test 5: Verify images are restored
        val backupZip = findBackupFile()

        PEVPlugShareDatabase.clearDatabase()

        val helper = BackupRestoreHelper(context)
        helper.restoreFromZip(backupZip)

        Thread.sleep(500)

        // Count restored images
        val imagesDir = File(context.filesDir, "location_images")
        val imageCount = imagesDir.walk().count { it.isFile && it.extension == "jpg" }

        println("Images restored: $imageCount")
        assertTrue("Should restore images", imageCount > 0)
    }

    @Test
    fun testDatabaseLockedDuringRestore() = runBlocking {
        // Test 6: Ensure database isn't locked during restore
        val backupZip = findBackupFile()

        PEVPlugShareDatabase.clearDatabase()

        val helper = BackupRestoreHelper(context)
        val result = helper.restoreFromZip(backupZip)

        // Try to access database after restore
        try {
            val restoredDb = SQLiteDatabase.openDatabase(
                context.getDatabasePath("pevplugshare.db").absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            val cursor = restoredDb.rawQuery("SELECT COUNT(*) FROM locations", null)
            cursor.close()
            restoredDb.close()
            assertTrue("Database should be accessible after restore", true)
        } catch (e: Exception) {
            fail("Database should not be locked: ${e.message}")
        }
    }
}
