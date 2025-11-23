package com.a42r.pevplugshare.domain.repository

import com.a42r.pevplugshare.data.api.OsmandCategory
import com.a42r.pevplugshare.data.model.Location

interface AuthRepository {
    suspend fun loginWithWebApi(username: String, password: String, rememberMe: Boolean = false): Result<String>

    suspend fun refreshWebApiSession(): Result<Unit>

    suspend fun getFavoritesCategories(): Result<List<String>>
    
    suspend fun downloadFavoritesGpx(categoryName: String): Result<List<com.a42r.pevplugshare.data.model.Location>>

    suspend fun logout()

    suspend fun isLoggedIn(): Boolean
    
    suspend fun hasStoredCredentials(): Boolean
    
    suspend fun hasSessionToken(): Boolean
    
    suspend fun autoLogin(): Result<String>

    suspend fun getUserInfo(): Result<Pair<String, String>>
}

interface LocationRepository {
    suspend fun getCategories(): Result<List<OsmandCategory>>

    suspend fun getLocationsByCategory(categoryName: String): Result<List<Location>>

    suspend fun addLocation(location: Location, categoryName: String): Result<Location>

    suspend fun updateLocation(location: Location, categoryName: String): Result<Location>
    
    suspend fun updateLocationToOsmand(location: Location, categoryName: String, index: Int, updateTime: Long): Result<Unit>

    suspend fun deleteLocation(locationId: String): Result<Unit>

    suspend fun syncLocations(categoryName: String): Result<List<Location>>
    
    /**
     * Find nearby locations within specified radius
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @param radiusMeters Search radius in meters (default 250m for duplicate detection)
     * @param categoryName Category to search in
     * @return List of nearby locations with distances
     */
    suspend fun findNearbyLocations(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double = 250.0,
        categoryName: String
    ): Result<List<Pair<Location, Double>>>

    /**
     * Delete all locations that were synced from OsmAnd
     * @return Number of locations deleted
     */
    suspend fun deleteAllOsmandLocations(): Result<Int>
}
