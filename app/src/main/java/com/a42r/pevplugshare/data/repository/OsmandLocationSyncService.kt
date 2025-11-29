package com.a42r.pevplugshare.data.repository

import android.util.Log
import com.a42r.pevplugshare.data.model.Location
import com.a42r.pevplugshare.data.model.SyncStatus
import com.a42r.pevplugshare.data.remote.OsmandApiClient
import com.a42r.pevplugshare.data.remote.TokenManager
import com.a42r.pevplugshare.data.remote.safeApiCall
import com.a42r.pevplugshare.domain.repository.LocationRepository
import com.google.gson.GsonBuilder
import okhttp3.Cookie
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OsmandLocationSyncService @Inject constructor(
    private val apiClient: OsmandApiClient,
    private val tokenManager: TokenManager,
    private val localRepository: LocationRepository
) {
    companion object {
        private const val TAG = "OsmandLocationSync"
        private val gson = GsonBuilder().setPrettyPrinting().create()

        private fun formatJsonIfPossible(content: String?): String {
            if (content.isNullOrBlank()) return content ?: "null"
            return try {
                // Try to parse and pretty-print as JSON
                val jsonElement = gson.fromJson(content, com.google.gson.JsonElement::class.java)
                gson.toJson(jsonElement)
            } catch (e: Exception) {
                // If not JSON, return as-is
                content
            }
        }
    }

    // Cache for category defaults (icon and color)
    private var categoryDefaultsCache: Map<String, Pair<String?, String?>>? = null
    private var categoryDefaultsCacheTime: Long = 0
    private val CATEGORY_CACHE_DURATION = 5 * 60 * 1000L // 5 minutes

    /**
     * Get the default icon and color for a category from OsmAnd by downloading the GPX file.
     * Returns Pair(color, icon) or Pair(null, null) if not found.
     * Uses caching to avoid excessive API calls.
     */
    private suspend fun getCategoryDefaults(categoryName: String): Pair<String?, String?> {
        val now = System.currentTimeMillis()

        // Check if cache is valid
        if (categoryDefaultsCache != null && (now - categoryDefaultsCacheTime) < CATEGORY_CACHE_DURATION) {
            Log.d(TAG, "Using cached category defaults")
            categoryDefaultsCache?.get(categoryName)?.let {
                Log.d(TAG, "  Found '$categoryName' in cache: color=${it.first}, icon=${it.second}")
                return it
            }
            Log.d(TAG, "  '$categoryName' NOT in cache")
        }

        // Fetch category defaults by downloading and parsing GPX files
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "Fetching category defaults by downloading GPX files")

        try {
            val webApiService = apiClient.getWebApiRetrofit()
                .create(com.a42r.pevplugshare.data.api.OsmandApiService::class.java)

            // Get list of all favorites files
            val listResponse = webApiService.listFiles()
            val favoriteFiles = listResponse.uniqueFiles?.filter {
                it.type == "FAVOURITES" && it.name.startsWith("favorites-")
            } ?: emptyList()

            Log.d(TAG, "Found ${favoriteFiles.size} favorite files")

            // Download and parse each GPX file to extract group styles
            val groupStyles = mutableMapOf<String, Pair<String?, String?>>()

            for (file in favoriteFiles) {
                try {
                    // Extract group name from filename: "favorites-Charging Locations.gpx" -> "Charging Locations"
                    val groupName = file.name
                        .removePrefix("favorites-")
                        .removeSuffix(".gpx")

                    Log.d(TAG, "  Downloading ${file.name} to extract style...")

                    val response = webApiService.downloadFile(
                        type = "FAVOURITES",
                        name = file.name,
                        shared = false
                    )

                    val gpxContent = response.string()

                    // Parse color and icon from GPX metadata
                    // Look for first waypoint's color and icon as group defaults
                    val colorMatch = Regex("""color['"]\s*:\s*['"]([^'"]+)['"]""").find(gpxContent)
                    val iconMatch = Regex("""icon['"]\s*:\s*['"]([^'"]+)['"]""").find(gpxContent)

                    val color = colorMatch?.groupValues?.get(1)
                    val icon = iconMatch?.groupValues?.get(1)

                    groupStyles[groupName] = Pair(color, icon)
                    Log.d(TAG, "    ✓ $groupName: color=$color, icon=$icon")

                } catch (e: Exception) {
                    Log.e(TAG, "    ✗ Failed to download ${file.name}: ${e.message}")
                }
            }

            // Build cache
            categoryDefaultsCache = groupStyles
            categoryDefaultsCacheTime = now

            Log.d(TAG, "Cached ${groupStyles.size} group defaults from GPX files")

            // Return the requested category's defaults
            val result = categoryDefaultsCache?.get(categoryName) ?: Pair(null, null)
            Log.d(TAG, "Category '$categoryName' defaults: color=${result.first}, icon=${result.second}")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch category defaults from GPX files: ${e.message}", e)
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }

        return Pair(null, null)
    }

    /**
     * Build a formatted address string from location data
     */
    private fun buildAddress(location: Location): String {
        val addressParts = mutableListOf<String>()

        location.businessName?.let { addressParts.add(it) }
        location.streetAddress?.let { addressParts.add(it) }

        val suburbStatePostcode = listOfNotNull(
            location.suburb,
            location.state,
            location.postcode
        ).joinToString(", ")

        if (suburbStatePostcode.isNotEmpty()) {
            addressParts.add(suburbStatePostcode)
        }

        return if (addressParts.isNotEmpty()) {
            addressParts.joinToString(", ")
        } else {
            "Near ${location.latitude}, ${location.longitude}"
        }
    }

    /**
     * Download a GPX file from OsmAnd and return its content.
     * Returns null if download fails.
     */
    private suspend fun downloadGpxFile(fileName: String): String? {
        return try {
            val webApiService = apiClient.getWebApiRetrofit()
                .create(com.a42r.pevplugshare.data.api.OsmandApiService::class.java)

            val response = webApiService.downloadFile(
                type = "FAVOURITES",
                name = fileName,
                shared = false
            )

            response.string()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download GPX file $fileName: ${e.message}", e)
            null
        }
    }

    /**
     * Update a location in OSMAnd favorites
     *
     * @deprecated This function does NOT follow the exact OsmAnd algorithm and will likely
     * fail with error 106. Use updateFavorite() instead which implements the correct algorithm.
     *
     * The problems with this function:
     * - Does not fetch current updatetime from /fav/list before each operation
     * - Uses current system time instead of server updatetime (guaranteed error 106)
     * - No retry logic for error 106
     * - Missing proper headers (Origin, Referer)
     */
    @Deprecated(
        message = "Use updateFavorite() instead - this function uses incorrect updatetime and will fail",
        replaceWith = ReplaceWith("updateFavorite(location, categoryName, currentName, index)")
    )
    suspend fun updateLocationToOsmand(
        location: Location,
        categoryName: String,
        index: Int = 0
    ): Result<Unit> {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "Updating location to OSMAnd: ${location.name}")
        Log.d(TAG, "Category: $categoryName")

        val apiResult = safeApiCall {
            val webApiService = apiClient.getWebApiRetrofit()
                .create(com.a42r.pevplugshare.data.api.OsmandApiService::class.java)

            val groupName = "favorites-${categoryName}.gpx".replace(" ", "+")
            val currentTime = System.currentTimeMillis()

            // Build the waypoint name for the URL (prefix + location name)
            val wptName = "PEV Charging - ${location.name}".replace(" ", "+")

            // Log cookies being sent with the request
            val cookies = apiClient.getCookiesForUrl("https://osmand.net/mapapi/fav/update")
            Log.d(TAG, "Cookies being sent: ${cookies.joinToString("; ") { "${it.name}=${it.value}" }}")

            // Build the update request payload
            val updateRequest = com.a42r.pevplugshare.data.api.OsmandUpdateFavoriteRequest(
                name = location.name,
                desc = location.description ?: "",
                address = buildAddress(location),
                color = location.color,
                background = "octagon",
                hidden = "true",
                icon = "charging_station", // Use charging_station icon for charging locations
                lat = location.latitude,
                lon = location.longitude,
                category = categoryName,
                ext = com.a42r.pevplugshare.data.api.OsmandFavoriteExtension(
                    time = location.lastModified,
                    ele = 6.0
                )
            )

            // Log the request details
            Log.d(TAG, "OSMAnd Update Request:")
            Log.d(TAG, "URL: https://osmand.net/mapapi/fav/update?wptName=${wptName}&oldGroupName=${groupName}&newGroupName=${groupName}&oldGroupUpdatetime=${currentTime}&newGroupUpdatetime=${currentTime}&ind=${index}")
            Log.d(TAG, "Payload JSON:\n${gson.toJson(updateRequest)}")

            Log.d(TAG, "Calling fav/update endpoint")
            val response = webApiService.updateFavorite(
                wptName = wptName,
                oldGroupName = groupName,
                newGroupName = groupName,
                oldGroupUpdatetime = currentTime,
                newGroupUpdatetime = currentTime,
                ind = index,
                updateRequest = updateRequest
            )

            // Log the response
            Log.d(TAG, "OSMAnd Update Response:")
            Log.d(TAG, "Status: ${response.code()}")
            Log.d(TAG, "Message: ${response.message()}")

            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                Log.d(TAG, "Response Body:\n${formatJsonIfPossible(responseBody)}")
            } else {
                // For error responses, try to read the error body
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Error Body:\n${formatJsonIfPossible(errorBody)}")
                throw Exception("HTTP ${response.code()}: ${response.message()}${if (errorBody != null) " - $errorBody" else ""}")
            }

            Log.d(TAG, "Location updated successfully")

            Unit
        }

        return when (apiResult) {
            is com.a42r.pevplugshare.data.remote.ApiResult.Success -> {
                // Update local database to mark as synced
                val updatedLocation = location.copy(
                    syncStatus = SyncStatus.SYNCED,
                    osmandSyncStatus = com.a42r.pevplugshare.data.model.OsmandSyncStatus.SYNCED
                )
                localRepository.updateLocation(updatedLocation, categoryName)
                Result.success(Unit)
            }
            is com.a42r.pevplugshare.data.remote.ApiResult.Error -> {
                Log.e(TAG, "OSMAnd update failed: ${apiResult.message}")
                Result.failure(Exception(apiResult.message))
            }
            is com.a42r.pevplugshare.data.remote.ApiResult.NetworkError -> {
                Log.e(TAG, "OSMAnd update network error")
                Result.failure(Exception("Network error"))
            }
        }
    }

    /**
     * Mark a location as pending sync
     * If location was previously synced to OsmAnd, mark as MODIFIED
     */
    suspend fun markLocationPendingSync(location: Location, categoryName: String) {
        // If location was previously synced to OsmAnd, mark it as MODIFIED
        // Otherwise, leave it as LOCAL_ONLY (never been synced)
        val newOsmandSyncStatus = if (location.osmandSyncedToServer) {
            com.a42r.pevplugshare.data.model.OsmandSyncStatus.MODIFIED
        } else {
            location.osmandSyncStatus
        }
        
        val updatedLocation = location.copy(
            syncStatus = SyncStatus.PENDING,
            osmandSyncStatus = newOsmandSyncStatus
        )
        localRepository.updateLocation(updatedLocation, categoryName)
    }

    /**
     * Smart sync function that automatically determines whether to ADD or UPDATE.
     * Downloads GPX file and uses intelligent matching to find if location exists in OsmAnd.
     *
     * Matching strategy:
     * 1. Name match with GPS verification (name matches AND within match distance)
     * 2. GPS proximity match (closest location within match distance)
     *
     * @param location The location to sync
     * @param categoryName The category/group name
     * @return Result indicating success or failure
     */
    suspend fun smartSync(
        location: Location,
        categoryName: String
    ): Result<Unit> {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "SMART SYNC - Auto-detecting ADD vs UPDATE")
        Log.d(TAG, "Location: ${location.name}")
        Log.d(TAG, "Category: $categoryName")
        Log.d(TAG, "Coordinates: ${location.latitude}, ${location.longitude}")

        val groupName = "favorites-${categoryName}.gpx"

        // Download current GPX to check if location exists
        val gpxContent = downloadGpxFile(groupName)

        if (gpxContent == null) {
            Log.w(TAG, "GPX file doesn't exist yet - will ADD as new location")
            return addFavorite(location, categoryName)
        }

        // Parse all waypoints from GPX
        val wptPattern = "<wpt\\s+lat=\"([^\"]+)\"\\s+lon=\"([^\"]+)\">.*?<name>([^<]+)</name>".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matches = wptPattern.findAll(gpxContent).toList()

        Log.d(TAG, "Found ${matches.size} existing locations in OsmAnd")

        val matchDistanceMeters = tokenManager.getOsmandLocationMatchDistance()
        val matchDistanceDegrees = matchDistanceMeters / 111000.0

        Log.d(TAG, "Using match distance: ${matchDistanceMeters}m (~${String.format("%.6f", matchDistanceDegrees)}°)")

        // Strategy 1: Try name match with GPS verification
        var matchedIndex: Int? = null
        var matchedName: String? = null

        matches.forEachIndexed { idx, match ->
            val wptLat = match.groupValues[1].toDoubleOrNull() ?: return@forEachIndexed
            val wptLon = match.groupValues[2].toDoubleOrNull() ?: return@forEachIndexed
            val wptName = match.groupValues[3]

            // Check name match
            if (wptName == location.name) {
                // Verify GPS proximity
                val latDiff = Math.abs(wptLat - location.latitude)
                val lonDiff = Math.abs(wptLon - location.longitude)

                if (latDiff < matchDistanceDegrees && lonDiff < matchDistanceDegrees) {
                    matchedIndex = idx
                    matchedName = wptName
                    Log.d(TAG, "✓ MATCHED by name with GPS verification at index $idx")
                    Log.d(TAG, "  Name: '$wptName'")
                    Log.d(TAG, "  OsmAnd: $wptLat, $wptLon")
                    Log.d(TAG, "  App:    ${location.latitude}, ${location.longitude}")
                    return@forEachIndexed
                } else {
                    Log.d(TAG, "⚠️ Name matches but GPS too far: '$wptName' (lat diff: ${latDiff}, lon diff: ${lonDiff})")
                }
            }
        }

        // Strategy 2: If no name match, find closest location within match distance
        if (matchedIndex == null) {
            Log.d(TAG, "No name match found, searching by GPS proximity...")

            var closestDistance = Double.MAX_VALUE
            var closestIndex: Int? = null
            var closestName: String? = null
            var closestLat: Double? = null
            var closestLon: Double? = null

            matches.forEachIndexed { idx, match ->
                val wptLat = match.groupValues[1].toDoubleOrNull() ?: return@forEachIndexed
                val wptLon = match.groupValues[2].toDoubleOrNull() ?: return@forEachIndexed
                val wptName = match.groupValues[3]

                val latDiff = Math.abs(wptLat - location.latitude)
                val lonDiff = Math.abs(wptLon - location.longitude)

                // Simple Euclidean distance in degrees
                val distance = Math.sqrt(latDiff * latDiff + lonDiff * lonDiff)

                if (distance < closestDistance && latDiff < matchDistanceDegrees && lonDiff < matchDistanceDegrees) {
                    closestDistance = distance
                    closestIndex = idx
                    closestName = wptName
                    closestLat = wptLat
                    closestLon = wptLon
                }
            }

            if (closestIndex != null) {
                matchedIndex = closestIndex
                matchedName = closestName
                Log.d(TAG, "✓ MATCHED by GPS proximity at index $closestIndex")
                Log.d(TAG, "  Name: '$closestName'")
                Log.d(TAG, "  OsmAnd: $closestLat, $closestLon")
                Log.d(TAG, "  App:    ${location.latitude}, ${location.longitude}")
                Log.d(TAG, "  Distance: ~${String.format("%.6f", closestDistance)}° (~${String.format("%.1f", closestDistance * 111000)}m)")
            }
        }

        // Decide: ADD or UPDATE
        val finalMatchedIndex = matchedIndex
        val finalMatchedName = matchedName

        return if (finalMatchedIndex != null && finalMatchedName != null) {
            Log.d(TAG, "→ Location EXISTS in OsmAnd - using UPDATE")
            updateFavorite(location, categoryName, finalMatchedName, finalMatchedIndex)
        } else {
            Log.d(TAG, "→ Location NOT FOUND in OsmAnd - using ADD")
            addFavorite(location, categoryName)
        }
    }

    /**
     * Get the current updatetime for a favorites group from the server.
     * This MUST be called immediately before every update/add operation.
     *
     * @param groupName The group name (e.g., "favorites-Charging Locations.gpx")
     * @return The current updatetime in milliseconds, or null if group doesn't exist
     */
    private suspend fun getCurrentGroupUpdatetime(groupName: String): Long? {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "Getting current updatetime for group: $groupName")

        val apiResult = safeApiCall {
            val webApiService = apiClient.getWebApiRetrofit()
                .create(com.a42r.pevplugshare.data.api.OsmandApiService::class.java)

            Log.d(TAG, "Calling /mapapi/list-files endpoint")
            val response = webApiService.listFiles()

            Log.d(TAG, "Received ${response.uniqueFiles?.size ?: 0} files")

            // Find the target FAVOURITES file
            val targetFile = response.uniqueFiles?.find {
                it.name == groupName && it.type == "FAVOURITES"
            }

            if (targetFile != null) {
                Log.d(TAG, "✅ Found group '$groupName' with updatetimems: ${targetFile.updatetimems}")
                targetFile.updatetimems
            } else {
                Log.w(TAG, "⚠️ Group '$groupName' not found in list-files response")
                null
            }
        }

        return when (apiResult) {
            is com.a42r.pevplugshare.data.remote.ApiResult.Success -> apiResult.data
            is com.a42r.pevplugshare.data.remote.ApiResult.Error -> {
                Log.e(TAG, "Failed to get updatetime: ${apiResult.message}")
                null
            }
            is com.a42r.pevplugshare.data.remote.ApiResult.NetworkError -> {
                Log.e(TAG, "Network error getting updatetime")
                null
            }
        }
    }

    /**
     * Update an existing favorite on osmand.net using the EXACT required algorithm.
     *
     * CRITICAL: This follows the exact 7-step algorithm required by OsmAnd's web API.
     * Any deviation will result in error 106 "File was changed!"
     *
     * @param location The location with updated data
     * @param categoryName The category/group name (e.g., "Charging Locations")
     * @param currentName The EXACT current <name> from the GPX file (URL-encoded when sent)
     * @param index The zero-based index of the waypoint in the current GPX file
     * @return Result indicating success or failure
     */
    suspend fun updateFavorite(
        location: Location,
        categoryName: String,
        currentName: String,
        index: Int
    ): Result<Unit> {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "UPDATE FAVORITE - EXACT ALGORITHM")
        Log.d(TAG, "Current name: $currentName")
        Log.d(TAG, "New name: ${location.name}")
        Log.d(TAG, "Category: $categoryName")
        Log.d(TAG, "Index: $index")

        val groupName = "favorites-${categoryName}.gpx"
        var attempt = 0
        val maxAttempts = 2

        while (attempt < maxAttempts) {
            attempt++
            Log.d(TAG, "────────────────────────────────────────────────────")
            Log.d(TAG, "Attempt $attempt of $maxAttempts")

            try {
                // ═══════════════════════════════════════════════════════════
                // STEP 1: Call GET https://osmand.net/mapapi/fav/list
                // → Extract the exact updatetime for the target group file
                // ═══════════════════════════════════════════════════════════
                Log.d(TAG, "STEP 1: Getting current updatetime from /fav/list")

                val updatetime = getCurrentGroupUpdatetime(groupName)
                if (updatetime == null) {
                    Log.e(TAG, "❌ FAILED: Could not get updatetime for group: $groupName")
                    return Result.failure(Exception("Could not get current updatetime for group $groupName"))
                }

                Log.d(TAG, "✅ Current updatetime: $updatetime")

                // ═══════════════════════════════════════════════════════════
                // STEP 1.5: Use hardcoded style attributes
                // → Always use specific color, icon for consistency
                // ═══════════════════════════════════════════════════════════
                Log.d(TAG, "STEP 1.5: Using hardcoded style attributes")

                val useColor = "#FF2C2C"
                val useIcon = "charging_station"

                Log.d(TAG, "  Style values to use in request:")
                Log.d(TAG, "    - color: $useColor")
                Log.d(TAG, "    - icon: $useIcon")

                // ═══════════════════════════════════════════════════════════
                // STEP 2: Re-download GPX file to verify current state
                // → Ensures we have the EXACT current <wpt> order and <name>
                // → Critical when location was modified in OsmAnd app/web
                // ═══════════════════════════════════════════════════════════
                Log.d(TAG, "STEP 2: Re-downloading GPX to verify current name and index")

                val gpxContent = downloadGpxFile(groupName)
                if (gpxContent == null) {
                    Log.e(TAG, "❌ FAILED: Could not download GPX file: $groupName")
                    return Result.failure(Exception("Could not download GPX file to verify current state"))
                }

                // Parse GPX to find all waypoints and verify our location
                val wptPattern = "<wpt\\s+lat=\"([^\"]+)\"\\s+lon=\"([^\"]+)\">.*?<name>([^<]+)</name>".toRegex(RegexOption.DOT_MATCHES_ALL)
                val matches = wptPattern.findAll(gpxContent).toList()

                Log.d(TAG, "Found ${matches.size} waypoints in GPX:")
                matches.forEachIndexed { idx, match ->
                    val wptName = match.groupValues[3]
                    Log.d(TAG, "  [$idx] $wptName")
                }

                // Try to find the location by name or by lat/lon proximity
                var verifiedIndex: Int? = null
                var verifiedName: String? = null

                // First try: exact name match
                matches.forEachIndexed { idx, match ->
                    val wptName = match.groupValues[3]
                    if (wptName == currentName || wptName == location.name) {
                        verifiedIndex = idx
                        verifiedName = wptName
                        Log.d(TAG, "✓ Found by name match at index $idx: '$wptName'")
                        return@forEachIndexed
                    }
                }

                // Second try: lat/lon proximity using configured match distance
                if (verifiedIndex == null) {
                    val matchDistanceMeters = tokenManager.getOsmandLocationMatchDistance()
                    Log.d(TAG, "Using match distance: ${matchDistanceMeters}m")

                    // Convert meters to approximate degrees
                    // At equator: 1 degree latitude ≈ 111km, 1 degree longitude ≈ 111km * cos(latitude)
                    // For simplicity, use 111,000 meters per degree as approximation
                    val matchDistanceDegrees = matchDistanceMeters / 111000.0

                    matches.forEachIndexed { idx, match ->
                        val wptLat = match.groupValues[1].toDoubleOrNull() ?: return@forEachIndexed
                        val wptLon = match.groupValues[2].toDoubleOrNull() ?: return@forEachIndexed
                        val wptName = match.groupValues[3]

                        val latDiff = Math.abs(wptLat - location.latitude)
                        val lonDiff = Math.abs(wptLon - location.longitude)

                        if (latDiff < matchDistanceDegrees && lonDiff < matchDistanceDegrees) {
                            verifiedIndex = idx
                            verifiedName = wptName
                            Log.d(TAG, "✓ Found by lat/lon proximity at index $idx: '$wptName'")
                            Log.d(TAG, "  GPX: $wptLat, $wptLon")
                            Log.d(TAG, "  App: ${location.latitude}, ${location.longitude}")
                            Log.d(TAG, "  Distance threshold: ${matchDistanceMeters}m (~${String.format("%.6f", matchDistanceDegrees)}°)")
                            return@forEachIndexed
                        }
                    }
                }

                if (verifiedIndex == null || verifiedName == null) {
                    Log.e(TAG, "❌ FAILED: Could not find location in GPX")
                    Log.e(TAG, "   Searched for name: '$currentName' or '${location.name}'")
                    Log.e(TAG, "   Searched near: ${location.latitude}, ${location.longitude}")
                    return Result.failure(Exception("Location not found in GPX - may have been deleted or moved"))
                }

                // Use verified values instead of caller-provided ones
                val actualCurrentName = verifiedName!!
                val actualIndex = verifiedIndex!!

                if (actualCurrentName != currentName || actualIndex != index) {
                    Log.w(TAG, "⚠️ CORRECTED name/index from GPX verification:")
                    Log.w(TAG, "   Caller provided: name='$currentName', index=$index")
                    Log.w(TAG, "   GPX verified:    name='$actualCurrentName', index=$actualIndex")
                } else {
                    Log.d(TAG, "✓ Verified: name and index match caller's values")
                }

                // ═══════════════════════════════════════════════════════════
                // STEP 3: Build request with EXACT query parameters
                // ═══════════════════════════════════════════════════════════
                Log.d(TAG, "STEP 3: Building request parameters")
                Log.d(TAG, "  oldGroupName: $groupName")
                Log.d(TAG, "  newGroupName: $groupName")
                Log.d(TAG, "  oldGroupUpdatetime: $updatetime")
                Log.d(TAG, "  newGroupUpdatetime: $updatetime")
                Log.d(TAG, "  wptName: $actualCurrentName (will be URL-encoded)")
                Log.d(TAG, "  ind: $actualIndex")

                // ═══════════════════════════════════════════════════════════
                // STEP 5: Build JSON payload matching exact structure
                // ═══════════════════════════════════════════════════════════
                Log.d(TAG, "STEP 5: Building JSON payload")

                val updateRequest = com.a42r.pevplugshare.data.api.OsmandUpdateFavoriteRequest(
                    name = location.name,  // Can be new/different
                    desc = location.description ?: "",
                    address = buildAddress(location),
                    color = useColor,      // Use category color
                    background = "octagon",
                    icon = useIcon,        // Use category icon
                    hidden = "true",  // Note: "true" means hidden
                    lat = location.latitude,
                    lon = location.longitude,
                    category = categoryName,  // Must match group without "favorites-" and ".gpx"
                    ext = com.a42r.pevplugshare.data.api.OsmandFavoriteExtension(
                        firstPoint = false,
                        lastPoint = false,
                        deleted = false,
                        time = location.lastModified,
                        ele = 6.0,
                        speed = 0.0,
                        hdop = null,
                        heading = null,
                        bearing = null,
                        speedColor = 0,
                        altitudeColor = 0,
                        slopeColor = 0,
                        colourARGB = 0,
                        distance = 0.0,
                        extensions = emptyMap()
                    )
                )

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "UPDATE REQUEST PAYLOAD:")
                Log.d(TAG, "  name: ${updateRequest.name}")
                Log.d(TAG, "  desc: ${updateRequest.desc}")
                Log.d(TAG, "  address: ${updateRequest.address}")
                Log.d(TAG, "  ✨ color: ${updateRequest.color}")
                Log.d(TAG, "  background: ${updateRequest.background}")
                Log.d(TAG, "  ✨ icon: ${updateRequest.icon}")
                Log.d(TAG, "  hidden: ${updateRequest.hidden}")
                Log.d(TAG, "  lat: ${updateRequest.lat}")
                Log.d(TAG, "  lon: ${updateRequest.lon}")
                Log.d(TAG, "  category: ${updateRequest.category}")
                Log.d(TAG, "Full JSON:")
                Log.d(TAG, gson.toJson(updateRequest))
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                // ═══════════════════════════════════════════════════════════
                // STEP 6: Send request as GET + JSON body with proper headers
                // ═══════════════════════════════════════════════════════════
                Log.d(TAG, "STEP 6: Sending GET request with JSON body")

                val webApiService = apiClient.getWebApiRetrofit()
                    .create(com.a42r.pevplugshare.data.api.OsmandApiService::class.java)

                // Log cookies (SESSION)
                val cookies = apiClient.getCookiesForUrl("https://osmand.net/mapapi/fav/update")
                Log.d(TAG, "Cookies: ${cookies.joinToString("; ") { "${it.name}=${it.value.take(10)}..." }}")

                val response = webApiService.updateFavorite(
                    wptName = actualCurrentName,  // EXACT current name from verified GPX
                    oldGroupName = groupName,
                    newGroupName = groupName,
                    oldGroupUpdatetime = updatetime,
                    newGroupUpdatetime = updatetime,
                    ind = actualIndex,  // EXACT zero-based index from verified GPX
                    // Origin and Referer headers are set in the service interface
                    updateRequest = updateRequest
                )

                Log.d(TAG, "Response: HTTP ${response.code()} ${response.message()}")

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    Log.d(TAG, "✅ UPDATE SUCCESSFUL!")
                    Log.d(TAG, "Response body: ${formatJsonIfPossible(responseBody)}")

                    // ═══════════════════════════════════════════════════════════
                    // STEP 7: After success, invalidate cache and update local DB
                    // ═══════════════════════════════════════════════════════════
                    Log.d(TAG, "STEP 7: Updating local database")

                    val syncedLocation = location.copy(
                        syncStatus = SyncStatus.SYNCED,
                        osmandSyncStatus = com.a42r.pevplugshare.data.model.OsmandSyncStatus.SYNCED,
                        osmandOriginalName = location.name,  // Update to new server name
                        osmandGpxIndex = actualIndex,        // Update to verified index
                        osmandLastSyncedAt = System.currentTimeMillis()
                    )
                    localRepository.updateLocation(syncedLocation, categoryName)

                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    return Result.success(Unit)

                } else {
                    // ═══════════════════════════════════════════════════════════
                    // ERROR HANDLING: Check for error 106
                    // ═══════════════════════════════════════════════════════════
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "❌ UPDATE FAILED: HTTP ${response.code()}")
                    Log.e(TAG, "Error body: ${formatJsonIfPossible(errorBody)}")

                    // Try to parse error code 106 specifically
                    try {
                        val errorResponse = gson.fromJson(
                            errorBody,
                            com.a42r.pevplugshare.data.api.OsmandFavUpdateErrorResponse::class.java
                        )
                        if (errorResponse?.error?.errorCode == 106) {
                            Log.w(TAG, "⚠️ ERROR 106: File was changed!")
                            Log.w(TAG, "Timestamp mismatch - will retry with fresh updatetime")

                            if (attempt < maxAttempts) {
                                Log.d(TAG, "Retrying...")
                                continue  // Retry loop will fetch fresh updatetime
                            }
                        }
                    } catch (e: Exception) {
                        // Not a parseable error response
                        Log.e(TAG, "Could not parse error response: ${e.message}")
                    }

                    throw Exception("HTTP ${response.code()}: ${response.message()} - $errorBody")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception during update: ${e.message}", e)

                if (attempt >= maxAttempts) {
                    Log.e(TAG, "❌ FAILED after $maxAttempts attempts")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    return Result.failure(e)
                }

                // Continue to next attempt
                Log.w(TAG, "Will retry...")
            }
        }

        Log.e(TAG, "❌ FAILED after $maxAttempts attempts")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        return Result.failure(Exception("Update failed after $maxAttempts attempts"))
    }

    /**
     * Add a new favorite to osmand.net using the EXACT required algorithm.
     *
     * CRITICAL: This follows the exact algorithm for ADD operations.
     * Key differences from UPDATE:
     * - DO NOT include the 'ind' parameter at all
     * - DO NOT include 'wptName' parameter
     * - The point will be appended to the group
     *
     * @param location The new location to add
     * @param categoryName The category/group name (e.g., "Charging Locations")
     * @return Result indicating success or failure
     */
    suspend fun addFavorite(
        location: Location,
        categoryName: String
    ): Result<Unit> {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "ADD FAVORITE - EXACT ALGORITHM (using /fav/add)")
        Log.d(TAG, "📍 LOCATION DETAILS:")
        Log.d(TAG, "  Name: ${location.name}")
        Log.d(TAG, "  Category: $categoryName")
        Log.d(TAG, "  Coordinates: ${location.latitude}, ${location.longitude}")
        Log.d(TAG, "📋 CURRENT OSMAND METADATA:")
        Log.d(TAG, "  osmandSyncedToServer: ${location.osmandSyncedToServer}")
        Log.d(TAG, "  osmandOriginalName: ${location.osmandOriginalName}")
        Log.d(TAG, "  osmandGpxIndex: ${location.osmandGpxIndex}")
        Log.d(TAG, "  syncStatus: ${location.syncStatus}")
        Log.d(TAG, "🔑 VALIDATION:")
        Log.d(TAG, "  Has ID: ${location.id != null}")
        Log.d(TAG, "  Has name: ${location.name.isNotBlank()}")
        Log.d(TAG, "  Has coordinates: ${location.latitude != 0.0 && location.longitude != 0.0}")

        if (location.osmandSyncedToServer == true && location.osmandGpxIndex != null) {
            Log.w(TAG, "⚠️⚠️⚠️ WARNING: This location appears to already be synced!")
            Log.w(TAG, "         Should use UPDATE instead of ADD to avoid duplicates!")
        }
        
        Log.d(TAG, "🔍 CHECKING AUTHENTICATION:")
        val token = tokenManager.getAccessToken()
        Log.d(TAG, "  Has token: ${!token.isNullOrBlank()}")
        Log.d(TAG, "  Token length: ${token?.length ?: 0}")

        val groupName = "favorites-${categoryName}.gpx"
        var attempt = 0
        val maxAttempts = 2

        while (attempt < maxAttempts) {
            attempt++
            Log.d(TAG, "────────────────────────────────────────────────────")
            Log.d(TAG, "Attempt $attempt of $maxAttempts")

            try {
                // ═══════════════════════════════════════════════════════════
                // STEP 1: Call GET https://osmand.net/mapapi/fav/list
                // → Extract the exact updatetime for the target group file
                // → If group doesn't exist, we can still proceed (will be created)
                // ═══════════════════════════════════════════════════════════
                Log.d(TAG, "STEP 1: Getting current updatetime from /fav/list")
                Log.d(TAG, "  📡 Calling /mapapi/list-files endpoint...")

                val updatetime: Long = getCurrentGroupUpdatetime(groupName) ?: run {
                    Log.w(TAG, "⚠️ Group doesn't exist yet - will be created")
                    Log.w(TAG, "Using current time as updatetime")
                    System.currentTimeMillis()
                }

                Log.d(TAG, "✅ Using updatetime: $updatetime (${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(updatetime))})")

                // ═══════════════════════════════════════════════════════════
                // STEP 1.5: Use hardcoded style attributes
                // → Always use specific color, icon for consistency
                // ═══════════════════════════════════════════════════════════
                Log.d(TAG, "STEP 1.5: Using hardcoded style attributes")

                val useColor = "#FF2C2C"
                val useIcon = "charging_station"

                Log.d(TAG, "  Style values to use in request:")
                Log.d(TAG, "    - color: $useColor")
                Log.d(TAG, "    - icon: $useIcon")

                // ═══════════════════════════════════════════════════════════
                // STEP 2: Build request with correct parameters for /fav/add
                // Parameters: fileName and updatetime (simpler than /fav/update)
                // ═══════════════════════════════════════════════════════════
                Log.d(TAG, "STEP 2: Building request parameters for /fav/add")
                Log.d(TAG, "  fileName: $groupName")
                Log.d(TAG, "  updatetime: $updatetime")

                // ═══════════════════════════════════════════════════════════
                // STEP 3: Build JSON payload matching web app structure
                // Note: category can be null, ext can be null, lat/lon are strings
                // ═══════════════════════════════════════════════════════════
                Log.d(TAG, "STEP 3: Building JSON payload")

                val addRequest = com.a42r.pevplugshare.data.api.OsmandAddFavoriteRequest(
                    name = location.name,
                    address = buildAddress(location).takeIf { it.isNotBlank() },
                    desc = location.description?.takeIf { it.isNotBlank() },
                    color = useColor,      // Use category color
                    background = "octagon",  // Use octagon shape
                    icon = useIcon,        // Use category icon
                    category = null,  // Web app sends null, not category name
                    lat = location.latitude.toString(),   // String, not Double
                    lon = location.longitude.toString(),  // String, not Double
                    ext = null  // Web app sends null for ADD
                )

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "ADD REQUEST PAYLOAD:")
                Log.d(TAG, "  name: ${addRequest.name}")
                Log.d(TAG, "  desc: ${addRequest.desc}")
                Log.d(TAG, "  address: ${addRequest.address}")
                Log.d(TAG, "  ✨ color: ${addRequest.color}")
                Log.d(TAG, "  background: ${addRequest.background}")
                Log.d(TAG, "  ✨ icon: ${addRequest.icon}")
                Log.d(TAG, "  category: ${addRequest.category}")
                Log.d(TAG, "  lat: ${addRequest.lat}")
                Log.d(TAG, "  lon: ${addRequest.lon}")
                Log.d(TAG, "  ext: ${addRequest.ext}")
                Log.d(TAG, "Full JSON:")
                Log.d(TAG, gson.toJson(addRequest))
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                // ═══════════════════════════════════════════════════════════
                // STEP 4: Send POST request to /fav/add
                // ═══════════════════════════════════════════════════════════
                Log.d(TAG, "STEP 4: Sending POST request to /fav/add")
                Log.d(TAG, "📤 REQUEST DETAILS:")
                Log.d(TAG, "  Endpoint: POST https://osmand.net/mapapi/fav/add")
                Log.d(TAG, "  fileName: $groupName")
                Log.d(TAG, "  updatetime: $updatetime")

                val webApiService = apiClient.getWebApiRetrofit()
                    .create(com.a42r.pevplugshare.data.api.OsmandApiService::class.java)

                // Log cookies (SESSION)
                val cookies = apiClient.getCookiesForUrl("https://osmand.net/mapapi/fav/add")
                Log.d(TAG, "  Cookies (count): ${cookies.size}")
                cookies.forEach { cookie ->
                    Log.d(TAG, "    - ${cookie.name}=${cookie.value.take(20)}...")
                }

                Log.d(TAG, "  Sending request...")
                val response = webApiService.addFavorite(
                    fileName = groupName,
                    updatetime = updatetime,
                    // Origin and Referer headers are set in the service interface
                    addRequest = addRequest
                )

                Log.d(TAG, "📥 RESPONSE RECEIVED:")
                Log.d(TAG, "  HTTP Status: ${response.code()} ${response.message()}")
                Log.d(TAG, "  Is Successful: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    Log.d(TAG, "✅ ADD SUCCESSFUL!")
                    Log.d(TAG, "Response body: ${formatJsonIfPossible(responseBody)}")

                    // ═══════════════════════════════════════════════════════════
                    // STEP 5: Download GPX file to get the new location's index
                    // → This is required so future syncs can use UPDATE instead of ADD
                    // ═══════════════════════════════════════════════════════════
                    Log.d(TAG, "STEP 5: Downloading GPX to find new location's index")
                    Log.d(TAG, "  ⬇️ Downloading $groupName from OsmAnd...")

                    var gpxIndex: Int? = null
                    try {
                        val gpxContent = downloadGpxFile(groupName)
                        if (gpxContent != null) {
                            Log.d(TAG, "  ✓ Downloaded ${gpxContent.length} bytes")
                            // Parse GPX to find our location's index
                            val wptPattern = "<wpt\\s+lat=\"([^\"]+)\"\\s+lon=\"([^\"]+)\">.*?<name>([^<]+)</name>".toRegex(RegexOption.DOT_MATCHES_ALL)
                            val matches = wptPattern.findAll(gpxContent)

                            matches.forEachIndexed { index, match ->
                                val wptName = match.groupValues[3]
                                Log.d(TAG, "  Checking [index=$index]: '$wptName'")
                                if (wptName == location.name) {
                                    gpxIndex = index
                                    Log.d(TAG, "  ✅ Found location at index $index in GPX")
                                    return@forEachIndexed
                                }
                            }

                            if (gpxIndex == null) {
                                Log.w(TAG, "  ⚠️ Could not find location in GPX - index will remain null")
                                Log.w(TAG, "    Searched for: '${location.name}'")
                            }
                        } else {
                            Log.w(TAG, "  ⚠️ Could not download GPX - index will remain null")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "  ❌ Error finding index in GPX: ${e.message}", e)
                    }

                    // ═══════════════════════════════════════════════════════════
                    // STEP 6: Update local DB with OsmAnd metadata
                    // ═══════════════════════════════════════════════════════════
                    Log.d(TAG, "STEP 6: Updating local database")
                    Log.d(TAG, "  💾 Updating sync metadata...")

                    val syncedLocation = location.copy(
                        syncStatus = SyncStatus.SYNCED,
                        osmandSyncStatus = com.a42r.pevplugshare.data.model.OsmandSyncStatus.SYNCED,
                        osmandOriginalName = location.name,      // Server now knows this name
                        osmandSyncSource = "OSMAND",             // Now exists on OsmAnd
                        osmandSyncedToServer = true,             // Confirmed on server
                        osmandLastSyncedAt = System.currentTimeMillis(),
                        osmandGpxIndex = gpxIndex                // Index from GPX (may be null)
                    )
                    localRepository.updateLocation(syncedLocation, categoryName)

                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.d(TAG, "✅ COMPLETE: Location synced to OsmAnd")
                    Log.d(TAG, "LOCAL DB UPDATE COMPLETE:")
                    Log.d(TAG, "  osmandSyncedToServer: ${syncedLocation.osmandSyncedToServer}")
                    Log.d(TAG, "  osmandOriginalName: ${syncedLocation.osmandOriginalName}")
                    Log.d(TAG, "  osmandGpxIndex: ${syncedLocation.osmandGpxIndex}")
                    Log.d(TAG, "  syncStatus: ${syncedLocation.syncStatus}")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    return Result.success(Unit)

                } else {
                    // ═══════════════════════════════════════════════════════════
                    // ERROR HANDLING: Check for error 106
                    // ═══════════════════════════════════════════════════════════
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "❌ ADD FAILED: HTTP ${response.code()}")
                    Log.e(TAG, "📋 ERROR DETAILS:")
                    Log.e(TAG, "  Status: ${response.code()} ${response.message()}")
                    Log.e(TAG, "  Headers: ${response.headers()}")
                    Log.e(TAG, "  Body: ${formatJsonIfPossible(errorBody)}")

                    // Try to parse error code 106 specifically
                    try {
                        val errorResponse = gson.fromJson(
                            errorBody,
                            com.a42r.pevplugshare.data.api.OsmandFavUpdateErrorResponse::class.java
                        )
                        if (errorResponse?.error?.errorCode == 106) {
                            Log.w(TAG, "⚠️ ERROR 106: File was changed!")
                            Log.w(TAG, "Timestamp mismatch - will retry with fresh updatetime")

                            if (attempt < maxAttempts) {
                                Log.d(TAG, "Retrying...")
                                continue  // Retry loop will fetch fresh updatetime
                            }
                        }
                    } catch (e: Exception) {
                        // Not a parseable error response
                        Log.e(TAG, "Could not parse error response: ${e.message}")
                    }

                    throw Exception("HTTP ${response.code()}: ${response.message()} - $errorBody")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during add: ${e.message}", e)
                Log.e(TAG, "💥 STACK TRACE:")
                e.stackTrace.forEach { element ->
                    Log.e(TAG, "    at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})")
                }

                if (attempt >= maxAttempts) {
                    Log.e(TAG, "❌ FAILED after $maxAttempts attempts - giving up")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    return Result.failure(e)
                }

                // Continue to next attempt
                Log.w(TAG, "⏳ Will retry (attempt ${attempt + 1}/$maxAttempts)...")
            }
        }

        Log.e(TAG, "❌ FAILED after $maxAttempts attempts")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        return Result.failure(Exception("Add failed after $maxAttempts attempts"))
    }
}
