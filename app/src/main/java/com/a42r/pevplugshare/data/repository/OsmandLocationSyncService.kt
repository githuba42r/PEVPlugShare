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
}
