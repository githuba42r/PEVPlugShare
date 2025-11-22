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
     */
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

            Log.d(TAG, "Calling /mapapi/fav/list endpoint")
            val response = webApiService.getFavList()

            Log.d(TAG, "Received ${response.favGroups?.size ?: 0} favorite groups")
            response.favGroups?.forEach { group ->
                Log.d(TAG, "  - ${group.name}: updatetime=${group.updatetime}")
            }

            val targetGroup = response.favGroups?.find { it.name == groupName }
            if (targetGroup != null) {
                Log.d(TAG, "✅ Found group '$groupName' with updatetime: ${targetGroup.updatetime}")
                targetGroup.updatetime
            } else {
                Log.w(TAG, "⚠️ Group '$groupName' not found in fav/list response")
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
     * Update an existing favorite on osmand.net.
     * Follows the exact flow required by the OSMAnd web API to avoid error 106.
     *
     * @param location The location with updated data
     * @param categoryName The category/group name (e.g., "Charging Locations")
     * @param currentName The EXACT current name of the waypoint on the server
     * @param index The zero-based index of the waypoint in the GPX file
     * @return Result indicating success or failure
     */
    suspend fun updateFavorite(
        location: Location,
        categoryName: String,
        currentName: String,
        index: Int
    ): Result<Unit> {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "Updating existing favorite on OSMAnd")
        Log.d(TAG, "Current name: $currentName")
        Log.d(TAG, "New name: ${location.name}")
        Log.d(TAG, "Category: $categoryName")
        Log.d(TAG, "Index: $index")

        val groupName = "favorites-${categoryName}.gpx"

        // Step 1: Get current updatetime from server
        val updatetime = getCurrentGroupUpdatetime(groupName)
        if (updatetime == null) {
            Log.e(TAG, "❌ Failed to get current updatetime for group: $groupName")
            return Result.failure(Exception("Could not get current updatetime for group $groupName"))
        }

        // Step 2: Attempt the update with retry logic for error 106
        var attempt = 0
        val maxAttempts = 2

        while (attempt < maxAttempts) {
            attempt++
            Log.d(TAG, "Update attempt $attempt/$maxAttempts with updatetime=$updatetime")

            val apiResult = safeApiCall {
                val webApiService = apiClient.getWebApiRetrofit()
                    .create(com.a42r.pevplugshare.data.api.OsmandApiService::class.java)

                // Log cookies
                val cookies = apiClient.getCookiesForUrl("https://osmand.net/mapapi/fav/update")
                Log.d(TAG, "Session cookies: ${cookies.joinToString("; ") { "${it.name}=${it.value.take(10)}..." }}")

                // Build the update request payload
                val updateRequest = com.a42r.pevplugshare.data.api.OsmandUpdateFavoriteRequest(
                    name = location.name,
                    desc = location.description ?: "",
                    address = buildAddress(location),
                    color = location.color,
                    background = "octagon",
                    hidden = "true",
                    icon = location.icon,
                    lat = location.latitude,
                    lon = location.longitude,
                    category = categoryName,
                    ext = com.a42r.pevplugshare.data.api.OsmandFavoriteExtension(
                        time = location.lastModified,
                        ele = 6.0
                    )
                )

                Log.d(TAG, "Request payload:\n${gson.toJson(updateRequest)}")

                // Call the update endpoint
                val response = webApiService.updateFavorite(
                    wptName = currentName,
                    oldGroupName = groupName,
                    newGroupName = groupName,
                    oldGroupUpdatetime = updatetime,
                    newGroupUpdatetime = updatetime,
                    ind = index,
                    updateRequest = updateRequest
                )

                Log.d(TAG, "Response: ${response.code()} ${response.message()}")

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    Log.d(TAG, "✅ Update successful!")
                    Log.d(TAG, "Response body: ${formatJsonIfPossible(responseBody)}")
                    Unit
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "❌ Update failed: HTTP ${response.code()}")
                    Log.e(TAG, "Error body: ${formatJsonIfPossible(errorBody)}")

                    // Try to parse error code 106 specifically
                    try {
                        val errorResponse = gson.fromJson(
                            errorBody,
                            com.a42r.pevplugshare.data.api.OsmandFavUpdateErrorResponse::class.java
                        )
                        if (errorResponse?.error?.errorCode == 106) {
                            Log.w(TAG, "Error 106: File was changed! Will retry with fresh updatetime")
                            throw Exception("ERROR_106_RETRY")
                        }
                    } catch (e: Exception) {
                        if (e.message == "ERROR_106_RETRY") throw e
                        // Not a parseable error, continue with generic error
                    }

                    throw Exception("HTTP ${response.code()}: ${response.message()} - $errorBody")
                }
            }

            when (apiResult) {
                is com.a42r.pevplugshare.data.remote.ApiResult.Success -> {
                    Log.d(TAG, "✅ Successfully updated favorite on OSMAnd")

                    // Update local database with new OSMAnd metadata
                    val syncedLocation = location.copy(
                        syncStatus = SyncStatus.SYNCED,
                        osmandOriginalName = location.name,  // Update to new server name
                        osmandGpxIndex = index,              // Confirm index
                        osmandLastSyncedAt = System.currentTimeMillis()
                    )
                    localRepository.updateLocation(syncedLocation, categoryName)

                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    return Result.success(Unit)
                }
                is com.a42r.pevplugshare.data.remote.ApiResult.Error -> {
                    if (apiResult.message.contains("ERROR_106_RETRY") && attempt < maxAttempts) {
                        Log.w(TAG, "Retrying after error 106...")
                        // Loop will retry with fresh updatetime
                        continue
                    }

                    Log.e(TAG, "❌ Update failed: ${apiResult.message}")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    return Result.failure(Exception(apiResult.message))
                }
                is com.a42r.pevplugshare.data.remote.ApiResult.NetworkError -> {
                    Log.e(TAG, "❌ Network error during update")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    return Result.failure(Exception("Network error"))
                }
            }
        }

        Log.e(TAG, "❌ Failed after $maxAttempts attempts")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        return Result.failure(Exception("Update failed after $maxAttempts attempts"))
    }

    /**
     * Add a new favorite to osmand.net.
     * Uses the same endpoint as update, but with ind=-1 and no wptName.
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
        Log.d(TAG, "Adding new favorite to OSMAnd")
        Log.d(TAG, "Name: ${location.name}")
        Log.d(TAG, "Category: $categoryName")
        Log.d(TAG, "Location: ${location.latitude}, ${location.longitude}")

        val groupName = "favorites-${categoryName}.gpx"

        // Step 1: Get current updatetime from server
        var updatetime: Long = getCurrentGroupUpdatetime(groupName) ?: run {
            Log.w(TAG, "⚠️ Group doesn't exist yet, using current time as updatetime")
            System.currentTimeMillis()
        }

        // Step 2: Attempt to add with retry logic for error 106
        var attempt = 0
        val maxAttempts = 2

        while (attempt < maxAttempts) {
            attempt++
            Log.d(TAG, "Add attempt $attempt/$maxAttempts with updatetime=$updatetime")

            val apiResult = safeApiCall {
                val webApiService = apiClient.getWebApiRetrofit()
                    .create(com.a42r.pevplugshare.data.api.OsmandApiService::class.java)

                // Log cookies
                val cookies = apiClient.getCookiesForUrl("https://osmand.net/mapapi/fav/update")
                Log.d(TAG, "Session cookies: ${cookies.joinToString("; ") { "${it.name}=${it.value.take(10)}..." }}")

                // Build the add request payload
                val addRequest = com.a42r.pevplugshare.data.api.OsmandUpdateFavoriteRequest(
                    name = location.name,
                    desc = location.description ?: "",
                    address = buildAddress(location),
                    color = location.color,
                    background = "octagon",
                    hidden = "true",
                    icon = location.icon,
                    lat = location.latitude,
                    lon = location.longitude,
                    category = categoryName,
                    ext = com.a42r.pevplugshare.data.api.OsmandFavoriteExtension(
                        time = location.lastModified,
                        ele = 6.0
                    )
                )

                Log.d(TAG, "Request payload:\n${gson.toJson(addRequest)}")

                // Call the add endpoint (using updateFavorite with ind=-1)
                val response = webApiService.addFavorite(
                    oldGroupName = groupName,
                    newGroupName = groupName,
                    oldGroupUpdatetime = updatetime,
                    newGroupUpdatetime = updatetime,
                    ind = -1,  // -1 indicates adding a new favorite
                    updateRequest = addRequest
                )

                Log.d(TAG, "Response: ${response.code()} ${response.message()}")

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    Log.d(TAG, "✅ Add successful!")
                    Log.d(TAG, "Response body: ${formatJsonIfPossible(responseBody)}")
                    Unit
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "❌ Add failed: HTTP ${response.code()}")
                    Log.e(TAG, "Error body: ${formatJsonIfPossible(errorBody)}")

                    // Try to parse error code 106 specifically
                    try {
                        val errorResponse = gson.fromJson(
                            errorBody,
                            com.a42r.pevplugshare.data.api.OsmandFavUpdateErrorResponse::class.java
                        )
                        if (errorResponse?.error?.errorCode == 106) {
                            Log.w(TAG, "Error 106: File was changed! Will retry with fresh updatetime")
                            throw Exception("ERROR_106_RETRY")
                        }
                    } catch (e: Exception) {
                        if (e.message == "ERROR_106_RETRY") throw e
                        // Not a parseable error, continue with generic error
                    }

                    throw Exception("HTTP ${response.code()}: ${response.message()} - $errorBody")
                }
            }

            when (apiResult) {
                is com.a42r.pevplugshare.data.remote.ApiResult.Success -> {
                    Log.d(TAG, "✅ Successfully added favorite to OSMAnd")

                    // Update local database with OSMAnd sync metadata
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
                }
                is com.a42r.pevplugshare.data.remote.ApiResult.Error -> {
                    if (apiResult.message.contains("ERROR_106_RETRY") && attempt < maxAttempts) {
                        Log.w(TAG, "Retrying after error 106...")
                        // Refresh updatetime for retry
                        val newUpdatetime = getCurrentGroupUpdatetime(groupName)
                        if (newUpdatetime != null) {
                            updatetime = newUpdatetime
                            continue
                        }
                    }

                    Log.e(TAG, "❌ Add failed: ${apiResult.message}")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    return Result.failure(Exception(apiResult.message))
                }
                is com.a42r.pevplugshare.data.remote.ApiResult.NetworkError -> {
                    Log.e(TAG, "❌ Network error during add")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    return Result.failure(Exception("Network error"))
                }
            }
        }

        Log.e(TAG, "❌ Failed after $maxAttempts attempts")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        return Result.failure(Exception("Add failed after $maxAttempts attempts"))
    }
}
