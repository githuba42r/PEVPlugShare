package com.a42r.pevplugshare.data.repository

import android.content.Context
import android.util.Log
import com.a42r.pevplugshare.data.api.OsmandCategory
import com.a42r.pevplugshare.data.api.OsmandMarker
import com.a42r.pevplugshare.data.model.Location
import com.a42r.pevplugshare.data.model.LocationAccessType
import com.a42r.pevplugshare.data.model.SocketType
import com.a42r.pevplugshare.data.remote.ApiResult
import com.a42r.pevplugshare.data.remote.CookieManager
import com.a42r.pevplugshare.data.remote.OsmandApiClient
import com.a42r.pevplugshare.data.remote.SessionRefreshWorker
import com.a42r.pevplugshare.data.remote.TokenManager
import com.a42r.pevplugshare.data.remote.safeApiCall
import com.a42r.pevplugshare.domain.repository.AuthRepository
import com.a42r.pevplugshare.domain.repository.LocationRepository

class AuthRepositoryImpl(
    private val apiClient: OsmandApiClient,
    private val tokenManager: TokenManager,
    private val context: Context,
    private val cookieManager: CookieManager
) : AuthRepository {

    companion object {
        private const val TAG = "OsmAndAuth"
    }

    override suspend fun loginWithWebApi(username: String, password: String, rememberMe: Boolean): Result<String> {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "Starting Web API Login")
        Log.d(TAG, "Username: $username")
        Log.d(TAG, "Remember Me: $rememberMe")
        Log.d(TAG, "Endpoint: https://osmand.net/mapapi/auth/login")

        return safeApiCall {
            apiClient.clearCookies()
            Log.d(TAG, "Cleared existing cookies")

            val webApiService = apiClient.getWebApiRetrofit()
                .create(com.a42r.pevplugshare.data.api.OsmandApiService::class.java)
            val loginRequest = com.a42r.pevplugshare.data.api.OsmandLoginRequest(
                username = username,
                password = password,
                lang = "en"
            )

            Log.d(TAG, "Sending login request...")
            val response = webApiService.webLogin(loginRequest)

            Log.d(TAG, "Login Response:")
            Log.d(TAG, "  Status: ${response.status}")
            Log.d(TAG, "  Message: ${response.message}")
            Log.d(TAG, "  User ID: ${response.userid}")

            // Get cookies after login
            val cookies = cookieManager.loadCookies("osmand.net")
            Log.d(TAG, "Session Cookies Received: ${cookies.size}")
            cookies.forEachIndexed { index, cookie ->
                Log.d(TAG, "  Cookie $index:")
                Log.d(TAG, "    Name: ${cookie.name}")
                Log.d(TAG, "    Value: ${cookie.value.take(20)}... (truncated)")
                Log.d(TAG, "    Domain: ${cookie.domain}")
                Log.d(TAG, "    Path: ${cookie.path}")
                Log.d(TAG, "    Expires: ${cookie.expiresAt}")
                Log.d(TAG, "    Secure: ${cookie.secure}")
                Log.d(TAG, "    HttpOnly: ${cookie.httpOnly}")
            }

            if (response.status == "ok" || response.userid != null) {
                Log.d(TAG, "✅ Login Successful!")
                tokenManager.saveWebApiCredentials(username, password, rememberMe)
                response.userid?.let { tokenManager.saveUserInfo(it, username) }
                SessionRefreshWorker.schedule(context)
                Log.d(TAG, "Credentials saved (rememberMe=$rememberMe), session refresh scheduled")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                response.userid ?: "success"
            } else {
                Log.e(TAG, "❌ Login Failed: ${response.message}")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                throw Exception(response.message ?: "Login failed")
            }
        }.mapToResult()
    }

    override suspend fun hasStoredCredentials(): Boolean {
        return tokenManager.hasStoredCredentials()
    }

    override suspend fun hasSessionToken(): Boolean {
        // Check if we have valid session cookies from OSMAnd
        val hasCookies = cookieManager.hasCookies("osmand.net")
        Log.d(TAG, "Checking session token - has cookies: $hasCookies")

        // Also check access token for backward compatibility with OAuth
        val hasAccessToken = tokenManager.isLoggedIn()
        Log.d(TAG, "Checking session token - has access token: $hasAccessToken")

        return hasCookies || hasAccessToken
    }

    override suspend fun autoLogin(): Result<String> {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "Attempting Auto-Login")

        val username = tokenManager.getUsername()
        val password = tokenManager.getPassword()

        if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
            Log.d(TAG, "❌ No stored credentials found")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            return Result.failure(Exception("No stored credentials"))
        }

        Log.d(TAG, "Found stored credentials for: $username")
        return loginWithWebApi(username, password, true)
    }

    override suspend fun refreshWebApiSession(): Result<Unit> {
        return safeApiCall {
            if (tokenManager.getAuthType() != TokenManager.AuthType.WEB_API) {
                throw Exception("Not using web API authentication")
            }

            val username = tokenManager.getUsername()
            val password = tokenManager.getPassword()

            if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
                throw Exception("Credentials not found")
            }

            val webApiService = apiClient.getWebApiRetrofit()
                .create(com.a42r.pevplugshare.data.api.OsmandApiService::class.java)
            val loginRequest = com.a42r.pevplugshare.data.api.OsmandLoginRequest(
                username = username,
                password = password,
                lang = "en"
            )
            val response = webApiService.webLogin(loginRequest)

            if (response.status != "ok" && response.userid == null) {
                throw Exception(response.message ?: "Session refresh failed")
            }
        }.mapToResult()
    }

    override suspend fun getFavoritesCategories(): Result<List<String>> {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "Fetching Favorites Categories")
        Log.d(TAG, "Endpoint: https://osmand.net/mapapi/list-files")

        // Check cookies before request
        val cookies = cookieManager.loadCookies("osmand.net")
        Log.d(TAG, "Using ${cookies.size} session cookie(s)")
        cookies.forEachIndexed { index, cookie ->
            Log.d(TAG, "  Cookie $index: ${cookie.name} = ${cookie.value.take(20)}...")
        }

        return try {
            val result = safeApiCall {
                val webApiService = apiClient.getWebApiRetrofit()
                    .create(com.a42r.pevplugshare.data.api.OsmandApiService::class.java)

                Log.d(TAG, "Sending list-files request...")
                Log.d(TAG, "Request will timeout after 5 seconds")
                val startTime = System.currentTimeMillis()

                val response = webApiService.listFiles()

                val elapsed = System.currentTimeMillis() - startTime
                Log.d(TAG, "Response received in ${elapsed}ms")

                Log.d(TAG, "List Files Response:")
                Log.d(TAG, "  Total Files: ${response.totalFiles}")
                Log.d(TAG, "  Total Unique Files: ${response.totalUniqueFiles}")
                Log.d(TAG, "  Unique Files Count: ${response.uniqueFiles?.size}")

                val favoriteFiles = response.uniqueFiles?.filter {
                    it.type == "FAVOURITES"
                } ?: emptyList()

                Log.d(TAG, "Favorites Files Found: ${favoriteFiles.size}")
                favoriteFiles.forEachIndexed { index, file ->
                    Log.d(TAG, "  File $index:")
                    Log.d(TAG, "    Name: ${file.name}")
                    Log.d(TAG, "    Type: ${file.type}")
                    Log.d(TAG, "    Size: ${file.filesize} bytes")
                    Log.d(TAG, "    Points: ${file.details?.analysis?.wptPoints}")
                    val categories = file.details?.analysis?.wptCategories
                    if (categories != null) {
                        Log.d(TAG, "    Categories: ${categories.keys.joinToString(", ")}")
                    }
                }

                val categoryNames = favoriteFiles.mapNotNull { file ->
                    file.name
                        .removePrefix("favourites-")
                        .removePrefix("favorites-")
                        .removeSuffix(".gpx")
                }

                Log.d(TAG, "Extracted Category Names: ${categoryNames.joinToString(", ")}")
                Log.d(TAG, "✅ Successfully retrieved ${categoryNames.size} categories")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                categoryNames
            }

            when (result) {
                is ApiResult.Success -> {
                    Result.success(result.data)
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "❌ API Error: ${result.code} - ${result.message}")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Result.failure(Exception("API Error (${result.code}): ${result.message}"))
                }
                is ApiResult.NetworkError -> {
                    Log.e(TAG, "❌ Network Error - check connection")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Result.failure(Exception("Network error - please check your connection"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception in getFavoritesCategories: ${e.message}")
            e.printStackTrace()
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Result.failure(e)
        }
    }

    override suspend fun downloadFavoritesGpx(categoryName: String): Result<List<Location>> {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "Downloading Favorites GPX for category: $categoryName")

        return try {
            // Use "favorites" (American spelling) to match OSMAnd's file naming
            val fileName = "favorites-${categoryName}.gpx"
            Log.d(TAG, "GPX File name: $fileName")

            val result = safeApiCall {
                val webApiService = apiClient.getWebApiRetrofit()
                    .create(com.a42r.pevplugshare.data.api.OsmandApiService::class.java)

                val response = webApiService.downloadFile(
                    type = "FAVOURITES",
                    name = fileName,
                    shared = false
                )

                // Get the full content as bytes first
                val gpxBytes = response.bytes()
                Log.d(TAG, "Downloaded ${gpxBytes.size} bytes")

                // Parse the GPX content
                val parser = com.a42r.pevplugshare.data.gpx.GpxParser()
                val locations = parser.parse(gpxBytes.inputStream())

                Log.d(TAG, "Parsed ${locations.size} waypoints from GPX")
                locations
            }

            when (result) {
                is ApiResult.Success -> {
                    Log.d(TAG, "✅ Successfully downloaded and parsed GPX")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Result.success(result.data)
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "❌ API Error: ${result.code} - ${result.message}")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Result.failure(Exception("Failed to download GPX: ${result.message}"))
                }
                is ApiResult.NetworkError -> {
                    Log.e(TAG, "❌ Network Error")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Result.failure(Exception("Network error"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception downloading GPX: ${e.message}", e)
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        apiClient.clearCookies()
        tokenManager.logout()
        SessionRefreshWorker.cancel(context)
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

    companion object {
        private const val TAG = "LocationRepository"
    }

    override suspend fun getCategories(): Result<List<OsmandCategory>> {
        val authHeader = tokenManager.getAuthHeader() ?: return Result.failure(
            Exception("Please log in to OSMAnd to access your favorites")
        )

        return safeApiCall {
            val response = apiClient.api.getCategories(authHeader)
            response.categories ?: emptyList()
        }.mapToResult()
    }

    override suspend fun getLocationsByCategory(categoryName: String): Result<List<Location>> {
        val authHeader = tokenManager.getAuthHeader() ?: return Result.failure(
            Exception("Please log in to OSMAnd to access your favorites")
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

    override suspend fun saveLocationsToLocalDb(locations: List<Location>, categoryName: String): Result<Int> {
        // This implementation is not used - LocalLocationRepository is injected via DI
        return Result.failure(UnsupportedOperationException("Use LocalLocationRepository"))
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

    override suspend fun updateLocationToOsmand(
        location: Location,
        categoryName: String,
        index: Int,
        updateTime: Long
    ): Result<Unit> {
        // This implementation uses the Web API
        return Result.failure(UnsupportedOperationException("Use OsmandLocationSyncService"))
    }

    override suspend fun syncLocations(categoryName: String): Result<List<Location>> {
        return getLocationsByCategory(categoryName)
    }

    override suspend fun findNearbyLocations(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        categoryName: String
    ): Result<List<Pair<Location, Double>>> {
        return getLocationsByCategory(categoryName).map { locations ->
            locations.mapNotNull { location ->
                val distance = com.a42r.pevplugshare.util.DistanceCalculator.calculateDistance(
                    latitude, longitude,
                    location.latitude, location.longitude
                )
                if (distance <= radiusMeters) {
                    Pair(location, distance)
                } else {
                    null
                }
            }.sortedBy { it.second }
        }
    }

    override suspend fun deleteAllOsmandLocations(): Result<Int> {
        // This implementation is for the OSMAnd API-based repository
        // Since we can't delete from OSMAnd server via API in bulk,
        // and this class isn't actually used (LocalLocationRepository is injected instead),
        // we return a not-supported error
        return Result.failure(UnsupportedOperationException("Bulk deletion of OSMAnd locations not supported via API"))
    }

    override suspend fun getAllLocationsCount(): Int {
        return 0
    }
}

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

fun <T> ApiResult<T>.mapToResult(): Result<T> {
    return when (this) {
        is ApiResult.Success -> Result.success(data)
        is ApiResult.Error -> Result.failure(
            Exception("API Error: $message")
        )
        is ApiResult.NetworkError -> Result.failure(
            Exception("Network error - please check your connection")
        )
    }
}
