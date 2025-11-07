package com.pevplugshare.domain.repository

import com.pevplugshare.data.api.OsmandCategory
import com.pevplugshare.data.model.Location

interface AuthRepository {
    suspend fun getAccessTokenFromCode(
        code: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String
    ): Result<String>

    suspend fun logout()

    suspend fun isLoggedIn(): Boolean

    suspend fun getUserInfo(): Result<Pair<String, String>>
}

interface LocationRepository {
    suspend fun getCategories(): Result<List<OsmandCategory>>

    suspend fun getLocationsByCategory(categoryName: String): Result<List<Location>>

    suspend fun addLocation(location: Location, categoryName: String): Result<Location>

    suspend fun updateLocation(location: Location, categoryName: String): Result<Location>

    suspend fun deleteLocation(locationId: String): Result<Unit>

    suspend fun syncLocations(categoryName: String): Result<List<Location>>
}
