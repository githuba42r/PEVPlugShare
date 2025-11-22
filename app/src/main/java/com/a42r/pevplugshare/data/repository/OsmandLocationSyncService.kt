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
                val updatedLocation = location.copy(syncStatus = SyncStatus.SYNCED)
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
     */
    suspend fun markLocationPendingSync(location: Location, categoryName: String) {
        val updatedLocation = location.copy(syncStatus = SyncStatus.PENDING)
        localRepository.updateLocation(updatedLocation, categoryName)
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
                // STEP 1.5: Get category defaults (icon and color)
                // → Use category's default icon/color instead of location's
                // ═══════════════════════════════════════════════════════════
                Log.d(TAG, "STEP 1.5: Getting category defaults for icon and color")
                Log.d(TAG, "  Location's stored values:")
                Log.d(TAG, "    - color: ${location.color}")
                Log.d(TAG, "    - icon: ${location.icon}")

                val (categoryColor, categoryIcon) = getCategoryDefaults(categoryName)

                val useColor = categoryColor ?: location.color
                val useIcon = categoryIcon ?: location.icon

                Log.d(TAG, "  Category defaults fetched:")
                Log.d(TAG, "    - categoryColor: $categoryColor")
                Log.d(TAG, "    - categoryIcon: $categoryIcon")
                Log.d(TAG, "  Final values to use in request:")
                Log.d(TAG, "    - useColor: $useColor")
                Log.d(TAG, "    - useIcon: $useIcon")

                // ═══════════════════════════════════════════════════════════
                // STEP 2: (Optional but recommended) Re-download GPX file
                // → Ensures we have current <wpt> order and exact <name> values
                // NOTE: Skipping this for now as we're trusting the caller's
                // currentName and index values. In production, you should
                // re-download and verify.
                // ═══════════════════════════════════════════════════════════
                Log.d(TAG, "STEP 2: (Skipped - trusting provided currentName and index)")

                // ═══════════════════════════════════════════════════════════
                // STEP 3: Build request with EXACT query parameters
                // ═══════════════════════════════════════════════════════════
                Log.d(TAG, "STEP 3: Building request parameters")
                Log.d(TAG, "  oldGroupName: $groupName")
                Log.d(TAG, "  newGroupName: $groupName")
                Log.d(TAG, "  oldGroupUpdatetime: $updatetime")
                Log.d(TAG, "  newGroupUpdatetime: $updatetime")
                Log.d(TAG, "  wptName: $currentName (will be URL-encoded)")
                Log.d(TAG, "  ind: $index")

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
                    hidden = "false",  // Note: "false" means visible
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
                    wptName = currentName,  // EXACT current name from GPX
                    oldGroupName = groupName,
                    newGroupName = groupName,
                    oldGroupUpdatetime = updatetime,
                    newGroupUpdatetime = updatetime,
                    ind = index,  // EXACT zero-based index from GPX
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
                        osmandOriginalName = location.name,  // Update to new server name
                        osmandGpxIndex = index,              // Confirm index
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
        Log.d(TAG, "Name: ${location.name}")
        Log.d(TAG, "Category: $categoryName")
        Log.d(TAG, "Location: ${location.latitude}, ${location.longitude}")

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

                val updatetime: Long = getCurrentGroupUpdatetime(groupName) ?: run {
                    Log.w(TAG, "⚠️ Group doesn't exist yet - will be created")
                    Log.w(TAG, "Using current time as updatetime")
                    System.currentTimeMillis()
                }

                Log.d(TAG, "✅ Using updatetime: $updatetime")

                // ═══════════════════════════════════════════════════════════
                // STEP 1.5: Get category defaults (icon and color)
                // → Use category's default icon/color instead of location's
                // ═══════════════════════════════════════════════════════════
                Log.d(TAG, "STEP 1.5: Getting category defaults for icon and color")
                Log.d(TAG, "  Location's stored values:")
                Log.d(TAG, "    - color: ${location.color}")
                Log.d(TAG, "    - icon: ${location.icon}")

                val (categoryColor, categoryIcon) = getCategoryDefaults(categoryName)

                val useColor = categoryColor ?: location.color
                val useIcon = categoryIcon ?: location.icon

                Log.d(TAG, "  Category defaults fetched:")
                Log.d(TAG, "    - categoryColor: $categoryColor")
                Log.d(TAG, "    - categoryIcon: $categoryIcon")
                Log.d(TAG, "  Final values to use in request:")
                Log.d(TAG, "    - useColor: $useColor")
                Log.d(TAG, "    - useIcon: $useIcon")

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
                    background = "circle",  // Web app uses "circle"
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

                val webApiService = apiClient.getWebApiRetrofit()
                    .create(com.a42r.pevplugshare.data.api.OsmandApiService::class.java)

                // Log cookies (SESSION)
                val cookies = apiClient.getCookiesForUrl("https://osmand.net/mapapi/fav/add")
                Log.d(TAG, "Cookies: ${cookies.joinToString("; ") { "${it.name}=${it.value.take(10)}..." }}")

                val response = webApiService.addFavorite(
                    fileName = groupName,
                    updatetime = updatetime,
                    // Origin and Referer headers are set in the service interface
                    addRequest = addRequest
                )

                Log.d(TAG, "Response: HTTP ${response.code()} ${response.message()}")

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    Log.d(TAG, "✅ ADD SUCCESSFUL!")
                    Log.d(TAG, "Response body: ${formatJsonIfPossible(responseBody)}")

                    // ═══════════════════════════════════════════════════════════
                    // STEP 5: After success, update local DB
                    // ═══════════════════════════════════════════════════════════
                    Log.d(TAG, "STEP 5: Updating local database")

                    val syncedLocation = location.copy(
                        syncStatus = SyncStatus.SYNCED,
                        osmandOriginalName = location.name,      // Server now knows this name
                        osmandSyncSource = "OSMAND",             // Now exists on OSMAnd
                        osmandSyncedToServer = true,             // Confirmed on server
                        osmandLastSyncedAt = System.currentTimeMillis(),
                        osmandGpxIndex = null  // Index unknown until next GPX download
                    )
                    localRepository.updateLocation(syncedLocation, categoryName)

                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    return Result.success(Unit)

                } else {
                    // ═══════════════════════════════════════════════════════════
                    // ERROR HANDLING: Check for error 106
                    // ═══════════════════════════════════════════════════════════
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "❌ ADD FAILED: HTTP ${response.code()}")
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
                Log.e(TAG, "Exception during add: ${e.message}", e)

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
        return Result.failure(Exception("Add failed after $maxAttempts attempts"))
    }
}
