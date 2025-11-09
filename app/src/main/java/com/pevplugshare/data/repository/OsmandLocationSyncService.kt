package com.pevplugshare.data.repository

import android.util.Log
import com.pevplugshare.data.model.Location
import com.pevplugshare.data.model.SyncStatus
import com.pevplugshare.data.remote.OsmandApiClient
import com.pevplugshare.data.remote.TokenManager
import com.pevplugshare.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OsmandLocationSyncService @Inject constructor(
    private val apiClient: OsmandApiClient,
    private val tokenManager: TokenManager,
    private val localRepository: LocalLocationRepository
) {
    companion object {
        private const val TAG = "OsmandLocationSync"
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
                .create(com.pevplugshare.data.api.OsmandApiService::class.java)
            
            val groupName = "favorites-${categoryName}.gpx"
            val currentTime = System.currentTimeMillis()
            
            Log.d(TAG, "Calling fav/update endpoint")
            Log.d(TAG, "wptName: ${location.name}")
            Log.d(TAG, "groupName: $groupName")
            Log.d(TAG, "lat: ${location.latitude}, lon: ${location.longitude}")
            
            webApiService.updateFavorite(
                wptName = location.name,
                oldGroupName = groupName,
                newGroupName = groupName,
                oldGroupUpdatetime = currentTime,
                newGroupUpdatetime = currentTime,
                ind = index,
                lat = location.latitude,
                lon = location.longitude,
                desc = location.description,
                color = location.color
            )
            
            Log.d(TAG, "Location updated successfully")
            
            Unit
        }
        
        return when (apiResult) {
            is com.pevplugshare.data.remote.ApiResult.Success -> {
                // Update local database to mark as synced
                val updatedLocation = location.copy(syncStatus = SyncStatus.SYNCED)
                localRepository.updateLocation(updatedLocation, categoryName)
                Result.success(Unit)
            }
            is com.pevplugshare.data.remote.ApiResult.Error -> {
                Result.failure(Exception(apiResult.message))
            }
            is com.pevplugshare.data.remote.ApiResult.NetworkError -> {
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
