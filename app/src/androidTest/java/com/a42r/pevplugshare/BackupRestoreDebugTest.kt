package com.a42r.pevplugshare

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.a42r.pevplugshare.util.BackupRestoreHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupRestoreDebugTest {
    private lateinit var context: Context
    private lateinit var testBackupFile: File

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        // Find the backup file
        testBackupFile = findTestBackup()
        println("\n" + "=".repeat(80))
        println("TEST SETUP")
        println("=".repeat(80))
        println("Backup file: ${testBackupFile.path}")
        println("Backup size: ${testBackupFile.length()} bytes")
        println("Backup exists: ${testBackupFile.exists()}")
        println("Backup readable: ${testBackupFile.canRead()}")
    }

    private fun findTestBackup(): File {
        val locations = listOf(
            "/data/local/tmp/backup_full.zip",
            "/sdcard/Downloads/backup_full.zip",
            "/sdcard/Download/backup_full.zip"
        )

        for (path in locations) {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                return file
            }
        }
        throw AssertionError("Test backup not found")
    }

    @Test
    fun debugRestoreProcess() = runBlocking {
        println("\n" + "=".repeat(80))
        println("RESTORE DEBUG TEST")
        println("=".repeat(80))

        // Clear database
        println("\n1. Clearing existing database...")
        val dbPath = context.getDatabasePath("pevplugshare.db").absolutePath
        println("   Database path: $dbPath")
        File(dbPath).delete()
        println("   ✓ Deleted existing database file")

        // Create helper
        val helper = BackupRestoreHelper(context)

        // Restore
        println("\n2. Starting restore...")
        var lastMessage = ""
        var lastProgress = 0
        val callback = BackupRestoreHelper.ProgressCallback { message, progress ->
            lastMessage = message
            lastProgress = progress
            println("   [$progress%] $message")
        }

        val result = helper.restoreFromZip(testBackupFile, callback)

        // Check result
        println("\n3. Restore result: $result")
        when (result) {
            is BackupRestoreHelper.RestoreResult.Success -> println("   ✓ SUCCESS")
            is BackupRestoreHelper.RestoreResult.Error -> println("   ✗ ERROR: ${result.message}")
            is BackupRestoreHelper.RestoreResult.Progress -> println("   Progress: ${result.message}")
        }

        // Check if database file exists
        println("\n4. Checking restored database file...")
        val dbFile = File(dbPath)
        println("   Database file exists: ${dbFile.exists()}")
        println("   Database file size: ${dbFile.length()} bytes")
        println("   Database file readable: ${dbFile.canRead()}")

        if (!dbFile.exists()) {
            println("   ✗ ERROR: Database file was not created!")
            throw AssertionError("Database file not created after restore")
        }

        // Try to open and query
        println("\n5. Opening restored database...")
        var locationCount = 0
        var savedCount = 0
        try {
            val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)

            println("   ✓ Database opened successfully")
            println("   Database version: ${db.version}")

            // Get location count
            val locationCursor = db.rawQuery("SELECT COUNT(*) as count FROM locations", null)
            locationCount = if (locationCursor.moveToFirst()) {
                locationCursor.getInt(0)
            } else {
                0
            }
            locationCursor.close()
            println("   Locations in database: $locationCount")

            if (locationCount == 0) {
                println("   ✗ ERROR: Database has 0 locations!")

                // Debug: Check what tables exist
                val tablesCursor = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table'",
                    null
                )
                val tables = mutableListOf<String>()
                while (tablesCursor.moveToNext()) {
                    tables.add(tablesCursor.getString(0))
                }
                tablesCursor.close()
                println("   Tables in database: $tables")

                // Check locations table schema
                if (tables.contains("locations")) {
                    val schemaCursor = db.rawQuery("PRAGMA table_info(locations)", null)
                    println("   Locations table schema:")
                    while (schemaCursor.moveToNext()) {
                        val name = schemaCursor.getString(1)
                        val type = schemaCursor.getString(2)
                        println("     - $name: $type")
                    }
                    schemaCursor.close()
                }

                throw AssertionError("Database is empty after restore")
            } else {
                println("   ✓ Database has $locationCount locations")
            }

            // Get saved items count
            val savedCursor = db.rawQuery("SELECT COUNT(*) as count FROM saved_for_later", null)
            savedCount = if (savedCursor.moveToFirst()) {
                savedCursor.getInt(0)
            } else {
                0
            }
            savedCursor.close()
            println("   Saved items in database: $savedCount")

            db.close()

        } catch (e: Exception) {
            println("   ✗ ERROR opening database: ${e.message}")
            e.printStackTrace()
            throw AssertionError("Could not open database: ${e.message}")
        }

        // Check images
        println("\n6. Checking restored images...")
        val imagesDir = File(context.filesDir, "location_images")
        val imageCount = imagesDir.walk().count { it.isFile && it.extension == "jpg" }
        println("   Images directory: ${imagesDir.path}")
        println("   Images directory exists: ${imagesDir.exists()}")
        println("   Image count: $imageCount")

        if (imageCount == 0) {
            println("   ⚠ WARNING: No images were restored")
        } else {
            println("   ✓ $imageCount images restored")
        }

        println("\n" + "=".repeat(80))
        println("TEST COMPLETE")
        println("=".repeat(80) + "\n")

        // Final assertions
        assertTrue("Restore should succeed", result is BackupRestoreHelper.RestoreResult.Success)
        assertTrue("Database should exist", dbFile.exists())
        assertTrue("Database should have locations", locationCount > 0)
    }
}
