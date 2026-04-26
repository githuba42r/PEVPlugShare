package com.a42r.pevplugshare

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.database.sqlite.SQLiteDatabase
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CompleteSavedLocationsTest {

    private lateinit var context: Context
    private lateinit var dbFile: File
    private lateinit var imagesDir: File
    private lateinit var db: SQLiteDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Use a private temp dir so we don't pollute the app's real data
        val testRoot = File(context.cacheDir, "csl_test_${System.currentTimeMillis()}")
        testRoot.mkdirs()

        imagesDir = File(testRoot, "location_images").also { it.mkdirs() }

        dbFile = File(testRoot, "test.db")
        db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        db.execSQL("""
            CREATE TABLE saved_for_later (
                id TEXT PRIMARY KEY,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                bearing REAL,
                photoPaths TEXT NOT NULL,
                photoMetadata TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """)
    }

    @After
    fun tearDown() {
        db.close()
        dbFile.parentFile?.deleteRecursively()
    }

    /** Create a minimal JPEG file with GPS EXIF at the given coordinates */
    private fun createGpsImage(name: String, lat: Double, lng: Double): File {
        // Start from a tiny valid 1×1 white JPEG (smallest possible)
        val jpegBytes = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
            0x00.toByte(), 0x10.toByte(), 0x4A.toByte(), 0x46.toByte(), 0x49.toByte(), 0x46.toByte(), 0x00.toByte(), 0x01.toByte(),
            0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(),
            0xFF.toByte(), 0xDB.toByte(), 0x00.toByte(), 0x43.toByte(), 0x00.toByte(),
            *ByteArray(64) { 0x08.toByte() },
            0xFF.toByte(), 0xC0.toByte(), 0x00.toByte(), 0x0B.toByte(), 0x08.toByte(),
            0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x01.toByte(), 0x01.toByte(), 0x01.toByte(), 0x11.toByte(), 0x00.toByte(),
            0xFF.toByte(), 0xC4.toByte(), 0x00.toByte(), 0x1F.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x01.toByte(), 0x05.toByte(), 0x01.toByte(), 0x01.toByte(), 0x01.toByte(), 0x01.toByte(), 0x01.toByte(),
            0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte(), 0x05.toByte(), 0x06.toByte(), 0x07.toByte(),
            0x08.toByte(), 0x09.toByte(), 0x0A.toByte(), 0x0B.toByte(),
            0xFF.toByte(), 0xDA.toByte(), 0x00.toByte(), 0x08.toByte(), 0x01.toByte(), 0x01.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x3F.toByte(), 0x00.toByte(), 0xF5.toByte(), 0x28.toByte(), 0xA1.toByte(), 0x4F.toByte(),
            0xFF.toByte(), 0xD9.toByte()
        )
        val file = File(imagesDir, name)
        file.writeBytes(jpegBytes)
        // Write GPS EXIF
        val exif = ExifInterface(file.absolutePath)
        exif.setLatLong(lat, lng)
        exif.saveAttributes()
        return file
    }

    /** Create a minimal JPEG file with NO GPS EXIF */
    private fun createNoGpsImage(name: String): File {
        val jpegBytes = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()
        )
        val file = File(imagesDir, name)
        file.writeBytes(jpegBytes)
        return file
    }

    private fun insertSavedLocation(id: String, lat: Double, lng: Double, photoCount: Int) {
        val fakeOldPaths = (1..photoCount).joinToString(",") { "/old/path_$it.jpg" }
        db.execSQL(
            "INSERT INTO saved_for_later VALUES (?, ?, ?, NULL, ?, '{}', 0)",
            arrayOf(id, lat, lng, fakeOldPaths)
        )
    }

    private fun getPhotoPaths(id: String): List<String> {
        val c = db.rawQuery("SELECT photoPaths FROM saved_for_later WHERE id = ?", arrayOf(id))
        val paths = if (c.moveToFirst()) c.getString(0).split(",").filter { it.isNotBlank() } else emptyList()
        c.close()
        return paths
    }

    @Test
    fun gpsImages_areAssignedToCorrectLocation() {
        // Location A: two images nearby at (37.7749, -122.4194)
        val imgA1 = createGpsImage("imgA1.jpg", 37.7749, -122.4194)
        val imgA2 = createGpsImage("imgA2.jpg", 37.7750, -122.4195)
        // Location B: one image nearby at (37.3382, -121.8863)
        val imgB1 = createGpsImage("imgB1.jpg", 37.3382, -121.8863)

        insertSavedLocation("saved_A", 37.7749, -122.4194, 2)
        insertSavedLocation("saved_B", 37.3382, -121.8863, 1)

        // Execute the logic being tested (extracted as a testable function -- see implementation task)
        com.a42r.pevplugshare.util.GpsImageMatcher.matchAndUpdate(db, imagesDir)

        val pathsA = getPhotoPaths("saved_A")
        val pathsB = getPhotoPaths("saved_B")

        assertEquals("Location A should have 2 images", 2, pathsA.size)
        assertEquals("Location B should have 1 image", 1, pathsB.size)

        assertTrue("imgA1 should be in location A", pathsA.any { it.contains("imgA1") })
        assertTrue("imgA2 should be in location A", pathsA.any { it.contains("imgA2") })
        assertTrue("imgB1 should be in location B", pathsB.any { it.contains("imgB1") })
    }

    @Test
    fun noGpsImages_fallBackToSequentialAssignment() {
        val noGps1 = createNoGpsImage("noGps1.jpg")
        val noGps2 = createNoGpsImage("noGps2.jpg")

        insertSavedLocation("saved_X", 37.7749, -122.4194, 2)

        com.a42r.pevplugshare.util.GpsImageMatcher.matchAndUpdate(db, imagesDir)

        val paths = getPhotoPaths("saved_X")
        assertEquals("Should fall back to 2 images from pool", 2, paths.size)
    }

    @Test
    fun emptyPhotoPaths_skipsAssignment() {
        // saved location with photoPaths = "" has 0 photos, should not be assigned images
        db.execSQL(
            "INSERT INTO saved_for_later VALUES (?, ?, ?, NULL, '', '{}', 0)",
            arrayOf("saved_empty", 37.7749, -122.4194)
        )
        createGpsImage("somewhere.jpg", 37.7749, -122.4194)

        com.a42r.pevplugshare.util.GpsImageMatcher.matchAndUpdate(db, imagesDir)

        val paths = getPhotoPaths("saved_empty")
        assertTrue("Empty photoPaths entry should remain empty", paths.isEmpty())
    }
}
