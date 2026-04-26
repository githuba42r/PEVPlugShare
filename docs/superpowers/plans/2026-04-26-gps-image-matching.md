# GPS-Based Image Matching for Saved Locations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace sequential image assignment in `completeSavedLocations()` with GPS-based EXIF matching so restored images are correctly linked to their saved locations regardless of sort order.

**Architecture:** Read GPS EXIF from every restored image using `PhotoUtils.extractLocationFromExif()`, then for each `saved_for_later` row compute distance to every GPS-tagged image using `LocationUtils.haversineDistance()` and assign all images within 150 m to that location. Images without GPS data fall back to sequential assignment for any locations that received zero GPS-matched images.

**Tech Stack:** Kotlin, AndroidX ExifInterface (already imported in PhotoUtils), Room/SQLiteDatabase (already used in BackupRestoreHelper), existing `LocationUtils` and `PhotoUtils` utilities.

---

## File Map

| Action | File | What changes |
|--------|------|-------------|
| Modify | `app/src/main/java/com/a42r/pevplugshare/util/BackupRestoreHelper.kt` | Replace sequential loop in `completeSavedLocations()` with GPS-distance matching |

No new files; no new dependencies.

---

### Task 1: Understand existing test coverage

**Files:**
- Read: `app/src/androidTest/java/com/a42r/pevplugshare/BackupRestoreTest.kt`

- [ ] **Step 1: Read the existing test class to see what it covers**

Open `app/src/androidTest/java/com/a42r/pevplugshare/BackupRestoreTest.kt` and note what assertions already exist around `saved_for_later` photo paths.

- [ ] **Step 2: Record whether any test exercises `completeSavedLocations()` path-mapping specifically**

If yes, note the test name — it will need updating. If no existing test covers it, Task 2 adds one.

---

### Task 2: Write a unit test for GPS-matched image assignment

This is an instrumented/unit test that does NOT require a real device — it mocks the file system and database.

**Files:**
- Create: `app/src/androidTest/java/com/a42r/pevplugshare/CompleteSavedLocationsTest.kt`

> **Note:** Android instrumented tests run on-device/emulator. This test creates real temp JPEG files with EXIF GPS data using `androidx.exifinterface`, writes a small SQLite DB, calls the (refactored) helper logic, and asserts the expected paths.

- [ ] **Step 1: Create the test file**

```kotlin
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
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
            0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
            0xFF.toByte(), 0xDB.toByte(), 0x00, 0x43, 0x00,
            *ByteArray(64) { 0x08 },
            0xFF.toByte(), 0xC0.toByte(), 0x00, 0x0B, 0x08,
            0x00, 0x01, 0x00, 0x01, 0x01, 0x01, 0x11, 0x00,
            0xFF.toByte(), 0xC4.toByte(), 0x00, 0x1F, 0x00,
            0x00, 0x01, 0x05, 0x01, 0x01, 0x01, 0x01, 0x01,
            0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
            0x08, 0x09, 0x0A, 0x0B,
            0xFF.toByte(), 0xDA.toByte(), 0x00, 0x08, 0x01, 0x01,
            0x00, 0x00, 0x3F, 0x00, 0xF5, 0x28, 0xA1, 0x4F,
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
```

- [ ] **Step 2: Try to build to confirm compile errors (expected — `GpsImageMatcher` doesn't exist yet)**

```bash
cd /home/philg/src/AndroidStudioProjects/PEVPlugShare
./gradlew :app:compileDebugAndroidTestKotlin 2>&1 | tail -30
```

Expected: compilation error mentioning `GpsImageMatcher`.

---

### Task 3: Extract the matching logic into `GpsImageMatcher`

The test references `com.a42r.pevplugshare.util.GpsImageMatcher.matchAndUpdate(db, imagesDir)`. Create that object so:
- It can be unit-tested without a full `Context`
- `completeSavedLocations()` delegates to it

**Files:**
- Create: `app/src/main/java/com/a42r/pevplugshare/util/GpsImageMatcher.kt`
- Modify: `app/src/main/java/com/a42r/pevplugshare/util/BackupRestoreHelper.kt`

- [ ] **Step 1: Create `GpsImageMatcher.kt`**

```kotlin
package com.a42r.pevplugshare.util

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File
import kotlin.math.abs

/**
 * Matches restored images to saved_for_later entries using GPS EXIF data.
 *
 * Strategy:
 *  1. Extract GPS coordinates from each image's EXIF metadata.
 *  2. For each saved_for_later row, assign all images whose GPS is within
 *     GPS_MATCH_RADIUS_METERS of the row's lat/lng.
 *  3. Images with no GPS (or locations that received zero GPS matches)
 *     fall back to sequential assignment from the unmatched pool.
 */
object GpsImageMatcher {

    private const val TAG = "GpsImageMatcher"
    private const val GPS_MATCH_RADIUS_METERS = 150.0

    data class SavedLocation(
        val id: String,
        val latitude: Double,
        val longitude: Double,
        val expectedPhotoCount: Int
    )

    data class ImageWithGps(val file: File, val latitude: Double, val longitude: Double)

    /**
     * Read all saved_for_later rows, match images to them by GPS, and write updated
     * photoPaths back to [db].
     *
     * @param db       Open read-write SQLiteDatabase for the restored database file.
     * @param imagesDir Directory where restored images now live.
     */
    fun matchAndUpdate(db: SQLiteDatabase, imagesDir: File) {
        val savedLocations = readSavedLocations(db)
        if (savedLocations.isEmpty()) {
            Log.d(TAG, "No saved_for_later entries — nothing to match")
            return
        }

        val allImages = imagesDir.listFiles()
            ?.filter { it.isFile && (it.extension == "jpg" || it.extension == "png") }
            ?: emptyList()

        Log.d(TAG, "Matching ${allImages.size} images to ${savedLocations.size} saved locations")

        // Partition images by GPS availability
        val gpsImages = mutableListOf<ImageWithGps>()
        val noGpsImages = mutableListOf<File>()

        for (image in allImages) {
            val coords = PhotoUtils.extractLocationFromExif(image.absolutePath)
            if (coords != null) {
                gpsImages.add(ImageWithGps(image, coords.first, coords.second))
                Log.d(TAG, "  GPS found in ${image.name}: (${coords.first}, ${coords.second})")
            } else {
                noGpsImages.add(image)
                Log.d(TAG, "  No GPS in ${image.name}")
            }
        }

        Log.d(TAG, "${gpsImages.size} images with GPS, ${noGpsImages.size} without")

        // Track which GPS images have been consumed so one image isn't assigned twice
        val usedGpsImages = mutableSetOf<File>()

        val pathUpdates = mutableListOf<Pair<String, String>>() // id -> newPaths

        val locationsNeedingFallback = mutableListOf<SavedLocation>()

        for (location in savedLocations) {
            if (location.expectedPhotoCount == 0) {
                Log.d(TAG, "  ${location.id}: 0 expected photos, skipping")
                continue
            }

            // Find GPS images within radius, sorted by distance ascending
            val nearby = gpsImages
                .filter { it.file !in usedGpsImages }
                .map { img ->
                    val dist = LocationUtils.haversineDistance(
                        location.latitude, location.longitude,
                        img.latitude, img.longitude
                    )
                    img to dist
                }
                .filter { (_, dist) -> dist <= GPS_MATCH_RADIUS_METERS }
                .sortedBy { (_, dist) -> dist }
                .map { (img, dist) ->
                    Log.d(TAG, "    ${img.file.name} is ${dist.toInt()}m from ${location.id}")
                    img
                }

            if (nearby.isNotEmpty()) {
                // Take up to expectedPhotoCount closest matches
                val assigned = nearby.take(location.expectedPhotoCount)
                usedGpsImages += assigned.map { it.file }
                val paths = assigned.map { it.file.absolutePath }.joinToString(",")
                pathUpdates.add(location.id to paths)
                Log.d(TAG, "  ${location.id}: GPS-matched ${assigned.size} image(s)")
            } else {
                locationsNeedingFallback.add(location)
                Log.d(TAG, "  ${location.id}: no GPS matches, queued for fallback")
            }
        }

        // Sequential fallback for locations that had no GPS-matched images
        if (locationsNeedingFallback.isNotEmpty()) {
            val fallbackPool = (noGpsImages + gpsImages
                .filter { it.file !in usedGpsImages }
                .map { it.file })
                .toMutableList()

            Log.d(TAG, "Fallback pool: ${fallbackPool.size} images for ${locationsNeedingFallback.size} location(s)")

            for (location in locationsNeedingFallback) {
                if (fallbackPool.isEmpty()) break
                val count = minOf(location.expectedPhotoCount, fallbackPool.size)
                val assigned = fallbackPool.subList(0, count).toList()
                fallbackPool.subList(0, count).clear()
                val paths = assigned.map { it.absolutePath }.joinToString(",")
                pathUpdates.add(location.id to paths)
                Log.d(TAG, "  ${location.id}: fallback-assigned ${assigned.size} image(s)")
            }
        }

        // Write all path updates to the database
        for ((id, newPaths) in pathUpdates) {
            db.execSQL("UPDATE saved_for_later SET photoPaths = ? WHERE id = ?", arrayOf(newPaths, id))
            Log.d(TAG, "Updated photoPaths for $id")
        }

        Log.i(TAG, "matchAndUpdate complete: ${pathUpdates.size}/${savedLocations.size} locations updated")
    }

    private fun readSavedLocations(db: SQLiteDatabase): List<SavedLocation> {
        val cursor = db.rawQuery(
            "SELECT id, latitude, longitude, photoPaths FROM saved_for_later ORDER BY id",
            null
        )
        val result = mutableListOf<SavedLocation>()
        while (cursor.moveToNext()) {
            val id = cursor.getString(0)
            val lat = cursor.getDouble(1)
            val lng = cursor.getDouble(2)
            val rawPaths = cursor.getString(3) ?: ""
            // Guard against the empty-string case: "".split(",") yields [""], not []
            val expectedCount = if (rawPaths.isBlank()) 0 else rawPaths.split(",").size
            result.add(SavedLocation(id, lat, lng, expectedCount))
        }
        cursor.close()
        return result
    }
}
```

- [ ] **Step 2: Update `completeSavedLocations()` in `BackupRestoreHelper.kt` to delegate to `GpsImageMatcher`**

Replace the entire method body of `completeSavedLocations()` (lines 247-334 of BackupRestoreHelper.kt) with:

```kotlin
private fun completeSavedLocations(tempDir: File) {
    val dbFile = File(context.filesDir.parentFile, "databases/pevplugshare.db")
    if (!dbFile.exists()) return

    val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
    try {
        val appImagesDir = File(context.filesDir, "location_images")
        if (!appImagesDir.exists()) {
            Log.d(TAG, "No images directory — skipping image matching")
        } else {
            GpsImageMatcher.matchAndUpdate(db, appImagesDir)
        }

        // Restore location names by looking up lat/lng in locations table
        Log.d(TAG, "Restoring location names for saved items...")
        val savedWithoutNames = db.rawQuery(
            "SELECT id, latitude, longitude FROM saved_for_later WHERE id LIKE 'saved_%'",
            null
        )

        val nameUpdates = mutableListOf<Pair<String, String>>()
        while (savedWithoutNames.moveToNext()) {
            val id = savedWithoutNames.getString(0)
            val lat = savedWithoutNames.getDouble(1)
            val lng = savedWithoutNames.getDouble(2)

            val locationCursor = db.rawQuery(
                """SELECT name FROM locations
                   WHERE ABS(latitude - ?) < 0.001 AND ABS(longitude - ?) < 0.001
                   LIMIT 1""",
                arrayOf(lat.toString(), lng.toString())
            )

            if (locationCursor.moveToFirst()) {
                val locationName = locationCursor.getString(0)
                nameUpdates.add(id to locationName)
                Log.d(TAG, "Found location name for $id: $locationName")
            }
            locationCursor.close()
        }
        savedWithoutNames.close()

        for ((id, name) in nameUpdates) {
            db.execSQL("UPDATE saved_for_later SET locationName = ? WHERE id = ?", arrayOf(name, id))
        }

        Log.i(TAG, "completeSavedLocations done: ${nameUpdates.size} names resolved")
    } finally {
        db.close()
    }
}
```

> **Note on `locationName` column:** The existing code calls `updateDB(saved_location.id, closestLocation.name)` but the handover doc only shows the SELECT — double-check whether `saved_for_later` has a `locationName` column. If it does not exist in the schema, remove the name-update loop above (image matching is the critical fix; name lookup is secondary).

- [ ] **Step 3: Build to confirm no compile errors**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL with no errors.

---

### Task 4: Verify `saved_for_later` schema has a `locationName` column (or remove the name update)

**Files:**
- Read: `app/src/main/java/com/a42r/pevplugshare/data/db/entity/Entities.kt`

- [ ] **Step 1: Check the entity definition**

Open `Entities.kt` and look at `SavedForLaterEntity`. If it has no `locationName` field, the `UPDATE saved_for_later SET locationName = ?` line in Task 3 will fail at runtime.

- [ ] **Step 2a: If `locationName` does NOT exist** — remove the name-update block from `completeSavedLocations()` (lines from "Restoring location names" through the closing `for` loop). Name display relies on the locations table anyway.

- [ ] **Step 2b: If `locationName` DOES exist** — leave the block as-is, confirm the column name matches exactly.

---

### Task 5: Run the instrumented tests on emulator

- [ ] **Step 1: Start the emulator (if not running)**

```bash
emulator -avd <your_avd_name> &
adb wait-for-device
```

- [ ] **Step 2: Run just the new test class**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.a42r.pevplugshare.CompleteSavedLocationsTest \
  2>&1 | tail -40
```

Expected: 3 tests pass — `gpsImages_areAssignedToCorrectLocation`, `noGpsImages_fallBackToSequentialAssignment`, `emptyPhotoPaths_skipsAssignment`.

- [ ] **Step 3: Run the existing backup-restore tests to check for regressions**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.a42r.pevplugshare.BackupRestoreTest \
  2>&1 | tail -40
```

Expected: all existing tests still pass.

---

### Task 6: End-to-end test on emulator with the real backup

- [ ] **Step 1: Install fresh debug APK**

```bash
./gradlew assembleDebug
adb uninstall com.a42r.pevplugshare
adb install app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 2: Push backup file**

```bash
adb push ~/backup_full.zip /sdcard/Download/
```

- [ ] **Step 3: Run the restore flow**

1. Launch app → skip login
2. Hamburger menu → `[DEBUG] Restore Backup`
3. Select `/sdcard/Download/backup_full.zip`
4. Wait for "Restore complete!"

- [ ] **Step 4: Verify in logcat**

```bash
adb logcat -d | grep -E "GpsImageMatcher|BackupRestore"
```

Expected log lines:
```
GpsImageMatcher: 49 images with GPS, 0 without        (or mix)
GpsImageMatcher: saved_1773226196895: GPS-matched N image(s)
GpsImageMatcher: matchAndUpdate complete: 7/7 locations updated
```

If any locations show "no GPS matches, queued for fallback", the images for that location lack GPS EXIF — that is expected and the fallback handles it.

- [ ] **Step 5: Verify saved locations display correctly**

1. Restart app
2. Open Saved tab
3. Confirm 7 saved locations show non-grey thumbnails
4. Tap each location — verify the correct photo is shown (not a photo from a different location)

- [ ] **Step 6: Commit**

```bash
git add \
  app/src/main/java/com/a42r/pevplugshare/util/GpsImageMatcher.kt \
  app/src/main/java/com/a42r/pevplugshare/util/BackupRestoreHelper.kt \
  app/src/androidTest/java/com/a42r/pevplugshare/CompleteSavedLocationsTest.kt
git commit -m "fix: match restored images to saved locations using GPS EXIF instead of sequential order"
```

---

## Verification Summary

| Check | Command | Expected |
|-------|---------|----------|
| Compile | `./gradlew :app:compileDebugKotlin` | BUILD SUCCESSFUL |
| New unit tests | `connectedDebugAndroidTest` targeting `CompleteSavedLocationsTest` | 3 PASS |
| Regression tests | `connectedDebugAndroidTest` targeting `BackupRestoreTest` | All PASS |
| Real restore | Logcat after restore | 7/7 locations GPS-matched (or fallback for no-GPS images) |
| Visual | Saved tab in app | Correct thumbnails per location |
