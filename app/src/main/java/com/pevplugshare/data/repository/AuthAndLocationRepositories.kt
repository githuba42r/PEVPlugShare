package com.pevplugshare.data.repository

import com.pevplugshare.data.api.OsmandApiService
import com.pevplugshare.data.api.OsmandCategory
import com.pevplugshare.data.api.OsmandMarker
import com.pevplugshare.data.model.Location
import com.pevplugshare.data.model.LocationAccessType
import com.pevplugshare.data.model.SocketType
import com.pevplugshare.data.remote.OsmandApiClient
import com.pevplugshare.data.remote.TokenManager
import com.pevplugshare.data.remote.safeApiCall
import com.pevplugshare.domain.repository.AuthRepository
import com.pevplugshare.domain.repository.LocationRepository

class AuthRepositoryImpl(
    private val apiClient: OsmandApiClient,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun getAccessTokenFromCode(
        code: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String
    ): Result<String> {
        return safeApiCall {
            val oauthService = apiClient.getOAuthRetrofit()
                .create(OsmandApiService::class.java)
            val response = oauthService.getAccessToken(
                code = code,
                clientId = clientId,
                clientSecret = clientSecret,
                redirectUri = redirectUri
            )
            tokenManager.saveAccessToken(response.access_token, response.expires_in)
            response.refresh_token?.let { tokenManager.saveRefreshToken(it) }
            response.access_token
        }.mapToResult()
    }

    override suspend fun logout() {
        tokenManager.logout()
    }

    override suspend fun isLoggedIn(): Boolean {
        return tokenManager.isLoggedIn()
    }

    override suspend fun getUserInfo(): Result<Pair<String, String>> {
        val authHeader = tokenManager.getAuthHeader() ?: return Result.failure(
            Exception("Not authenticated")
        )

        return safeApiCall {
            val userInfo = apiClient.api.getUserInfo(authHeader)
            Pair(userInfo.userid, userInfo.nickname ?: userInfo.login ?: "Unknown")
        }.mapToResult()
    }
}

class LocationRepositoryImpl(
    private val apiClient: OsmandApiClient,
    private val tokenManager: TokenManager
) : LocationRepository {

    override suspend fun getCategories(): Result<List<OsmandCategory>> {
        val authHeader = tokenManager.getAuthHeader() ?: return Result.failure(
            Exception("Not authenticated")
        )

        return safeApiCall {
            val response = apiClient.api.getCategories(authHeader)
            response.categories ?: emptyList()
        }.mapToResult()
    }

    override suspend fun getLocationsByCategory(categoryName: String): Result<List<Location>> {
        val authHeader = tokenManager.getAuthHeader() ?: return Result.failure(
            Exception("Not authenticated")
        )

        return safeApiCall {
            val response = apiClient.api.getMarkersByCategory(categoryName, authHeader)
            (response.markers ?: emptyList()).map { it.toLocation() }
        }.mapToResult()
    }

    override suspend fun addLocation(location: Location, categoryName: String): Result<Location> {
        val authHeader = tokenManager.getAuthHeader() ?: return Result.failure(
            Exception("Not authenticated")
        )

        return safeApiCall {
            val marker = location.toOsmandMarker()
            val response = apiClient.api.createMarker(marker, categoryName, authHeader)
            response.toLocation()
        }.mapToResult()
    }

    override suspend fun updateLocation(
        location: Location,
        categoryName: String
    ): Result<Location> {
        val authHeader = tokenManager.getAuthHeader() ?: return Result.failure(
            Exception("Not authenticated")
        )

        return safeApiCall {
            val marker = location.toOsmandMarker()
            val response = apiClient.api.updateMarker(
                location.id,
                marker,
                categoryName,
                authHeader
            )
            response.toLocation()
        }.mapToResult()
    }

    override suspend fun deleteLocation(locationId: String): Result<Unit> {
        val authHeader = tokenManager.getAuthHeader() ?: return Result.failure(
            Exception("Not authenticated")
        )

        return safeApiCall {
            apiClient.api.deleteMarker(locationId, authHeader)
        }.mapToResult()
    }

    override suspend fun syncLocations(categoryName: String): Result<List<Location>> {
        return getLocationsByCategory(categoryName)
    }
}

// Extension functions for data conversion
fun OsmandMarker.toLocation(): Location {
    return Location(
        id = id ?: name,
        name = name,
        description = description,
        latitude = latitude,
        longitude = longitude,
        socketTypes = listOf(SocketType.AS3112),
        publicPrivate = LocationAccessType.PUBLIC,
        gpoCount = 1,
        color = color ?: "#1f6b2a",
        icon = icon ?: "place",
        lastModified = timestamp ?: System.currentTimeMillis()
    )
}

fun Location.toOsmandMarker(): OsmandMarker {
    return OsmandMarker(
        name = name,
        description = description,
        latitude = latitude,
        longitude = longitude,
        color = color,
        icon = icon
    )
}

// Extension function for ApiResult to Result conversion
fun <T> com.pevplugshare.data.remote.ApiResult<T>.mapToResult(): Result<T> {
    return when (this) {
        is com.pevplugshare.data.remote.ApiResult.Success -> Result.success(data)
        is com.pevplugshare.data.remote.ApiResult.Error -> Result.failure(
            Exception("API Error: $message")
        )
        is com.pevplugshare.data.remote.ApiResult.NetworkError -> Result.failure(
            Exception("Network error")
        )
    }
}
