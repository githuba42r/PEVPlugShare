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
