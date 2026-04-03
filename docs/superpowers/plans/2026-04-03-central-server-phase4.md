# Central Location Server — Phase 4 Implementation Plan (Android Server Integration)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Connect the PEVPlugShare Android app to the central location server — Google Sign-In, server API client, location sync (upload/download), photo upload, and notification polling.

**Architecture:** Google Identity Services for sign-in → exchange Google ID token with server for JWT → store JWT in TokenManager. New `ServerApiService` (Retrofit interface) talks to the server. `ServerSyncRepository` manages upload/download of locations and photos. LocationEntity gets new server fields (serverId, serverVersion, serverSyncStatus). All sync is user-initiated — explicit upload/download buttons, no background sync.

**Tech Stack:** Google Identity Services (`play-services-auth`), Retrofit 2 (existing), Room (migration 11→12), Hilt DI, existing TokenManager/OkHttp patterns

**Spec:** `docs/features/CENTRAL_LOCATION_SERVER_SPEC.md` — Android App Changes section

**Prerequisite:** Server (Phases 1-3) running and accessible from the device/emulator.

---

## Scope

Phase 4 builds:

1. **Google Sign-In dependency** + credential helper
2. **Server API models** — Kotlin data classes for server JSON
3. **Server API service** — Retrofit interface for all server endpoints
4. **Server API client** — OkHttp + Retrofit setup with JWT auth interceptor
5. **TokenManager extensions** — Server JWT/user storage
6. **Room migration 11→12** — Add server fields to LocationEntity
7. **Server sync repository** — Upload/download locations, photos
8. **DI wiring** — Hilt providers for server components
9. **Server account UI** — Google Sign-In screen, connection status
10. **Location sync toggle** — Per-location sync-to-server in edit screen
11. **Notification polling** — Check for notifications on app open
12. **Settings integration** — Server account section in settings

## File Structure

### New Files

```
app/src/main/java/com/a42r/pevplugshare/
  data/
    api/
      ServerApiModels.kt           # Data classes for server API request/response
      ServerApiService.kt          # Retrofit interface
    remote/
      ServerApiClient.kt           # OkHttp + Retrofit setup with JWT interceptor
    repository/
      ServerAuthRepository.kt      # Google Sign-In → server JWT exchange
      ServerSyncRepository.kt      # Location and photo sync with server

  ui/
    server/
      ServerAccountActivity.kt     # Google Sign-In, connection status, disconnect
      ServerAccountViewModel.kt    # State management for server connection
    sync/
      SyncLocationActivity.kt      # Select locations to upload/sync
      SyncLocationViewModel.kt     # Manages sync operations

  res/
    layout/
      activity_server_account.xml
      activity_sync_location.xml
      item_sync_location.xml       # List item for sync screen
```

### Modified Files

```
app/build.gradle.kts                                # Add play-services-auth
app/src/main/java/com/a42r/pevplugshare/
  data/db/entity/Entities.kt                        # Add server fields to LocationEntity
  data/db/PEVPlugShareDatabase.kt                   # Migration 11→12, bump version
  data/db/entity/PendingOperationEntity.kt          # Add SERVER_ operation types
  data/remote/TokenManager.kt                       # Add server JWT/user methods
  di/NetworkModule.kt                               # Add server API providers
  ui/settings/SettingsActivity.kt                   # Add server account section
  ui/edit/EditLocationActivity.kt                   # Add sync toggle
  ui/main/MainActivity.kt                           # Add notification polling on resume
  res/values/strings.xml                            # Server-related strings
  AndroidManifest.xml                               # Register new activities
```

---

### Task 1: Add Google Sign-In Dependency

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add dependency**

Add to the dependencies block in `app/build.gradle.kts`, after the play-services-location line:

```kotlin
implementation("com.google.android.gms:play-services-auth:21.0.0")
```

- [ ] **Step 2: Verify build**

Run: `cd /home/philg/src/AndroidStudioProjects/PEVPlugShare && ./gradlew compileDebugKotlin 2>&1 | tail -3`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore: add Google Sign-In dependency for server integration"
```

---

### Task 2: Server API Models

**Files:**
- Create: `app/src/main/java/com/a42r/pevplugshare/data/api/ServerApiModels.kt`

- [ ] **Step 1: Create all request/response models**

```kotlin
package com.a42r.pevplugshare.data.api

import com.google.gson.annotations.SerializedName

// --- Auth ---

data class GoogleAuthRequest(
    @SerializedName("idToken") val idToken: String
)

data class GoogleAuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: ServerUser
)

data class ServerUser(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("avatarUrl") val avatarUrl: String?,
    @SerializedName("isAdmin") val isAdmin: Boolean
)

// --- Locations ---

data class ServerLocation(
    @SerializedName("id") val id: String,
    @SerializedName("ownerId") val ownerId: String,
    @SerializedName("name") val name: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("description") val description: String?,
    @SerializedName("additionalInfo") val additionalInfo: String?,
    @SerializedName("openingHours") val openingHours: String?,
    @SerializedName("socketTypes") val socketTypes: String?,
    @SerializedName("gpoCount") val gpoCount: Int?,
    @SerializedName("hasFee") val hasFee: Boolean?,
    @SerializedName("chargingCosts") val chargingCosts: String?,
    @SerializedName("publicPrivate") val publicPrivate: String?,
    @SerializedName("providerName") val providerName: String?,
    @SerializedName("providerWebsite") val providerWebsite: String?,
    @SerializedName("businessName") val businessName: String?,
    @SerializedName("streetAddress") val streetAddress: String?,
    @SerializedName("suburb") val suburb: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("postcode") val postcode: String?,
    @SerializedName("color") val color: String?,
    @SerializedName("icon") val icon: String?,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("version") val version: Int,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
)

data class CreateServerLocationRequest(
    @SerializedName("name") val name: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("description") val description: String? = null,
    @SerializedName("additionalInfo") val additionalInfo: String? = null,
    @SerializedName("openingHours") val openingHours: String? = null,
    @SerializedName("socketTypes") val socketTypes: String? = null,
    @SerializedName("gpoCount") val gpoCount: Int? = null,
    @SerializedName("hasFee") val hasFee: Boolean? = null,
    @SerializedName("chargingCosts") val chargingCosts: String? = null,
    @SerializedName("publicPrivate") val publicPrivate: String? = null,
    @SerializedName("providerName") val providerName: String? = null,
    @SerializedName("providerWebsite") val providerWebsite: String? = null,
    @SerializedName("businessName") val businessName: String? = null,
    @SerializedName("streetAddress") val streetAddress: String? = null,
    @SerializedName("suburb") val suburb: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("postcode") val postcode: String? = null,
    @SerializedName("color") val color: String? = null,
    @SerializedName("icon") val icon: String? = null
)

// --- Photos ---

data class ServerPhoto(
    @SerializedName("id") val id: String,
    @SerializedName("locationId") val locationId: String,
    @SerializedName("filePath") val filePath: String,
    @SerializedName("thumbnailPath") val thumbnailPath: String?,
    @SerializedName("caption") val caption: String?,
    @SerializedName("mimeType") val mimeType: String?,
    @SerializedName("uploadedAt") val uploadedAt: String?
)

// --- Notifications ---

data class ServerNotification(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("type") val type: String,
    @SerializedName("payloadJson") val payloadJson: String,
    @SerializedName("isRead") val isRead: Boolean,
    @SerializedName("createdAt") val createdAt: String?
)

// --- Pagination ---

data class PaginationInfo(
    @SerializedName("cursor") val cursor: String?,
    @SerializedName("hasMore") val hasMore: Boolean
)

data class PaginatedResponse<T>(
    @SerializedName("data") val data: List<T>,
    @SerializedName("pagination") val pagination: PaginationInfo
)
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -3`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/a42r/pevplugshare/data/api/ServerApiModels.kt
git commit -m "feat: add server API request/response models"
```

---

### Task 3: Server API Service (Retrofit Interface)

**Files:**
- Create: `app/src/main/java/com/a42r/pevplugshare/data/api/ServerApiService.kt`

- [ ] **Step 1: Create Retrofit interface**

```kotlin
package com.a42r.pevplugshare.data.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ServerApiService {
    // Auth
    @POST("auth/google")
    suspend fun authenticateWithGoogle(@Body request: GoogleAuthRequest): Response<GoogleAuthResponse>

    // Users
    @GET("api/users/me")
    suspend fun getCurrentUser(): Response<ServerUser>

    @GET("api/users/search")
    suspend fun searchUsers(@Query("q") query: String, @Query("cursor") cursor: String? = null, @Query("limit") limit: Int? = null): Response<PaginatedResponse<ServerUser>>

    // Locations
    @POST("api/locations")
    suspend fun createLocation(@Body location: CreateServerLocationRequest): Response<ServerLocation>

    @GET("api/locations")
    suspend fun getMyLocations(@Query("cursor") cursor: String? = null, @Query("limit") limit: Int? = null): Response<PaginatedResponse<ServerLocation>>

    @GET("api/locations/{id}")
    suspend fun getLocation(@Path("id") id: String): Response<ServerLocation>

    @PUT("api/locations/{id}")
    suspend fun updateLocation(@Path("id") id: String, @Body fields: Map<String, @JvmSuppressWildcards Any?>): Response<ServerLocation>

    @HTTP(method = "DELETE", path = "api/locations/{id}", hasBody = true)
    suspend fun deleteLocation(@Path("id") id: String, @Body body: Map<String, String>): Response<Map<String, String>>

    @GET("api/locations/shared-with-me")
    suspend fun getSharedLocations(@Query("cursor") cursor: String? = null, @Query("limit") limit: Int? = null): Response<PaginatedResponse<ServerLocation>>

    @GET("api/locations/nearby")
    suspend fun getNearbyLocations(@Query("lat") lat: Double, @Query("lon") lon: Double, @Query("radius") radiusKm: Double, @Query("cursor") cursor: String? = null): Response<PaginatedResponse<ServerLocation>>

    // Photos
    @Multipart
    @POST("api/locations/{locationId}/photos")
    suspend fun uploadPhoto(@Path("locationId") locationId: String, @Part photo: MultipartBody.Part): Response<ServerPhoto>

    @DELETE("api/locations/{locationId}/photos/{photoId}")
    suspend fun deletePhoto(@Path("locationId") locationId: String, @Path("photoId") photoId: String): Response<Unit>

    // Notifications
    @GET("api/notifications")
    suspend fun getNotifications(@Query("cursor") cursor: String? = null, @Query("limit") limit: Int? = null): Response<PaginatedResponse<ServerNotification>>

    @PUT("api/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<Map<String, String>>

    @PUT("api/notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<Map<String, String>>
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/a42r/pevplugshare/data/api/ServerApiService.kt
git commit -m "feat: add server API Retrofit interface"
```

---

### Task 4: Server API Client & TokenManager Extensions

**Files:**
- Create: `app/src/main/java/com/a42r/pevplugshare/data/remote/ServerApiClient.kt`
- Modify: `app/src/main/java/com/a42r/pevplugshare/data/remote/TokenManager.kt`

- [ ] **Step 1: Add server JWT methods to TokenManager**

Add these constants and methods to TokenManager:

```kotlin
// New constants in companion object
private const val KEY_SERVER_JWT = "server_jwt"
private const val KEY_SERVER_USER_ID = "server_user_id"
private const val KEY_SERVER_EMAIL = "server_email"
private const val KEY_SERVER_DISPLAY_NAME = "server_display_name"
private const val KEY_SERVER_CONNECTED = "server_connected"
private const val KEY_SERVER_URL = "server_url"

// New methods
fun saveServerJwt(token: String) {
    encryptedPrefs.edit().putString(KEY_SERVER_JWT, token).apply()
}

fun getServerJwt(): String? = encryptedPrefs.getString(KEY_SERVER_JWT, null)

fun saveServerUserInfo(userId: String, email: String, displayName: String) {
    encryptedPrefs.edit()
        .putString(KEY_SERVER_USER_ID, userId)
        .putString(KEY_SERVER_EMAIL, email)
        .putString(KEY_SERVER_DISPLAY_NAME, displayName)
        .putBoolean(KEY_SERVER_CONNECTED, true)
        .apply()
}

fun getServerUserId(): String? = encryptedPrefs.getString(KEY_SERVER_USER_ID, null)
fun getServerEmail(): String? = encryptedPrefs.getString(KEY_SERVER_EMAIL, null)
fun getServerDisplayName(): String? = encryptedPrefs.getString(KEY_SERVER_DISPLAY_NAME, null)
fun isServerConnected(): Boolean = encryptedPrefs.getBoolean(KEY_SERVER_CONNECTED, false)

fun saveServerUrl(url: String) {
    encryptedPrefs.edit().putString(KEY_SERVER_URL, url).apply()
}

fun getServerUrl(): String? = encryptedPrefs.getString(KEY_SERVER_URL, null)

fun disconnectServer() {
    encryptedPrefs.edit()
        .remove(KEY_SERVER_JWT)
        .remove(KEY_SERVER_USER_ID)
        .remove(KEY_SERVER_EMAIL)
        .remove(KEY_SERVER_DISPLAY_NAME)
        .remove(KEY_SERVER_URL)
        .putBoolean(KEY_SERVER_CONNECTED, false)
        .apply()
}
```

- [ ] **Step 2: Create ServerApiClient**

```kotlin
package com.a42r.pevplugshare.data.remote

import com.a42r.pevplugshare.data.api.ServerApiService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ServerApiClient(
    private val tokenManager: TokenManager
) {
    private var retrofit: Retrofit? = null
    private var service: ServerApiService? = null

    private fun buildClient(): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val request = chain.request()
            val jwt = tokenManager.getServerJwt()
            val newRequest = if (jwt != null && request.url.encodedPath.startsWith("/api/")) {
                request.newBuilder()
                    .addHeader("Authorization", "Bearer $jwt")
                    .build()
            } else {
                request
            }
            chain.proceed(newRequest)
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun getService(): ServerApiService {
        val baseUrl = tokenManager.getServerUrl() ?: throw IllegalStateException("Server URL not configured")
        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        if (service == null || retrofit?.baseUrl()?.toString() != url) {
            retrofit = Retrofit.Builder()
                .baseUrl(url)
                .client(buildClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            service = retrofit!!.create(ServerApiService::class.java)
        }

        return service!!
    }

    fun clearService() {
        service = null
        retrofit = null
    }
}
```

- [ ] **Step 3: Verify compilation, commit**

```bash
git add app/src/main/java/com/a42r/pevplugshare/data/remote/ServerApiClient.kt \
       app/src/main/java/com/a42r/pevplugshare/data/remote/TokenManager.kt
git commit -m "feat: server API client with JWT interceptor and TokenManager extensions"
```

---

### Task 5: Room Migration — Add Server Fields to LocationEntity

**Files:**
- Modify: `app/src/main/java/com/a42r/pevplugshare/data/db/entity/Entities.kt`
- Modify: `app/src/main/java/com/a42r/pevplugshare/data/db/PEVPlugShareDatabase.kt`

- [ ] **Step 1: Add server fields to LocationEntity**

Add after the existing OsmAnd sync fields (line 45):

```kotlin
    // Central server sync tracking fields
    val serverId: String? = null,                      // Server-side UUID
    val serverVersion: Int? = null,                    // Last known server version
    val serverSyncStatus: String = "LOCAL_ONLY",       // LOCAL_ONLY | SYNCED | MODIFIED | CONFLICT
    val serverLastSyncedAt: Long? = null               // Timestamp of last successful server sync
```

- [ ] **Step 2: Add migration 11→12**

In PEVPlugShareDatabase.kt, add migration:

```kotlin
private val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE locations ADD COLUMN serverId TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE locations ADD COLUMN serverVersion INTEGER DEFAULT NULL")
        database.execSQL("ALTER TABLE locations ADD COLUMN serverSyncStatus TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
        database.execSQL("ALTER TABLE locations ADD COLUMN serverLastSyncedAt INTEGER DEFAULT NULL")
    }
}
```

Bump database version from 11 to 12. Add `MIGRATION_11_12` to the `addMigrations()` call.

- [ ] **Step 3: Verify compilation, commit**

```bash
git add app/src/main/java/com/a42r/pevplugshare/data/db/
git commit -m "feat: Room migration 11→12 — add server sync fields to LocationEntity"
```

---

### Task 6: Server Auth Repository

**Files:**
- Create: `app/src/main/java/com/a42r/pevplugshare/data/repository/ServerAuthRepository.kt`

- [ ] **Step 1: Create server auth repository**

```kotlin
package com.a42r.pevplugshare.data.repository

import com.a42r.pevplugshare.data.api.GoogleAuthRequest
import com.a42r.pevplugshare.data.api.ServerUser
import com.a42r.pevplugshare.data.remote.ServerApiClient
import com.a42r.pevplugshare.data.remote.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerAuthRepository @Inject constructor(
    private val serverApiClient: ServerApiClient,
    private val tokenManager: TokenManager
) {
    suspend fun authenticateWithGoogle(googleIdToken: String): Result<ServerUser> {
        return try {
            val response = serverApiClient.getService()
                .authenticateWithGoogle(GoogleAuthRequest(googleIdToken))

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                tokenManager.saveServerJwt(body.token)
                tokenManager.saveServerUserInfo(body.user.id, body.user.email, body.user.displayName)
                Result.success(body.user)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Authentication failed"
                Result.failure(Exception(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): Result<ServerUser> {
        return try {
            val response = serverApiClient.getService().getCurrentUser()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isConnected(): Boolean = tokenManager.isServerConnected()

    fun disconnect() {
        tokenManager.disconnectServer()
        serverApiClient.clearService()
    }

    fun getServerUrl(): String? = tokenManager.getServerUrl()
    fun getServerEmail(): String? = tokenManager.getServerEmail()
    fun getServerDisplayName(): String? = tokenManager.getServerDisplayName()
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/a42r/pevplugshare/data/repository/ServerAuthRepository.kt
git commit -m "feat: server auth repository — Google Sign-In to server JWT exchange"
```

---

### Task 7: Server Sync Repository

**Files:**
- Create: `app/src/main/java/com/a42r/pevplugshare/data/repository/ServerSyncRepository.kt`

- [ ] **Step 1: Create sync repository**

This is the core class for uploading/downloading locations and photos to/from the server. Key methods:

```kotlin
package com.a42r.pevplugshare.data.repository

import com.a42r.pevplugshare.data.api.*
import com.a42r.pevplugshare.data.db.dao.LocationDao
import com.a42r.pevplugshare.data.db.dao.PhotoDao
import com.a42r.pevplugshare.data.db.entity.LocationEntity
import com.a42r.pevplugshare.data.remote.ServerApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerSyncRepository @Inject constructor(
    private val serverApiClient: ServerApiClient,
    private val locationDao: LocationDao,
    private val photoDao: PhotoDao
) {
    /**
     * Upload a local location to the server.
     * Creates on server if no serverId, updates if serverId exists.
     */
    suspend fun uploadLocation(locationEntity: LocationEntity): Result<ServerLocation> {
        return try {
            val service = serverApiClient.getService()

            if (locationEntity.serverId != null) {
                // Update existing
                val fields = locationEntityToUpdateMap(locationEntity)
                val response = service.updateLocation(locationEntity.serverId, fields)
                if (response.isSuccessful && response.body() != null) {
                    val serverLoc = response.body()!!
                    updateLocalWithServerInfo(locationEntity.id, serverLoc)
                    Result.success(serverLoc)
                } else {
                    Result.failure(Exception("Update failed: ${response.code()}"))
                }
            } else {
                // Create new
                val request = locationEntityToCreateRequest(locationEntity)
                val response = service.createLocation(request)
                if (response.isSuccessful && response.body() != null) {
                    val serverLoc = response.body()!!
                    updateLocalWithServerInfo(locationEntity.id, serverLoc)
                    Result.success(serverLoc)
                } else {
                    Result.failure(Exception("Create failed: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload a photo for a server location.
     */
    suspend fun uploadPhoto(serverLocationId: String, photoFile: File): Result<ServerPhoto> {
        return try {
            val requestBody = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("photo", photoFile.name, requestBody)
            val response = serverApiClient.getService().uploadPhoto(serverLocationId, part)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Photo upload failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download server locations owned by the current user.
     */
    suspend fun getMyServerLocations(): Result<List<ServerLocation>> {
        return try {
            val allLocations = mutableListOf<ServerLocation>()
            var cursor: String? = null
            do {
                val response = serverApiClient.getService().getMyLocations(cursor, 100)
                if (response.isSuccessful && response.body() != null) {
                    val page = response.body()!!
                    allLocations.addAll(page.data)
                    cursor = page.pagination.cursor
                    if (!page.pagination.hasMore) break
                } else {
                    return Result.failure(Exception("Failed to fetch locations"))
                }
            } while (cursor != null)
            Result.success(allLocations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get unread notifications from server.
     */
    suspend fun getNotifications(): Result<List<ServerNotification>> {
        return try {
            val response = serverApiClient.getService().getNotifications(limit = 50)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to fetch notifications"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun locationEntityToCreateRequest(entity: LocationEntity): CreateServerLocationRequest {
        return CreateServerLocationRequest(
            name = entity.name,
            latitude = entity.latitude,
            longitude = entity.longitude,
            description = entity.description,
            additionalInfo = entity.additionalInfo,
            openingHours = entity.openingHours,
            socketTypes = entity.socketTypes,
            gpoCount = entity.gpoCount,
            hasFee = entity.hasFee,
            chargingCosts = entity.chargingCosts,
            publicPrivate = entity.publicPrivate,
            providerName = entity.providerName,
            providerWebsite = entity.providerWebsite,
            businessName = entity.businessName,
            streetAddress = entity.streetAddress,
            suburb = entity.suburb,
            state = entity.state,
            postcode = entity.postcode,
            color = entity.color,
            icon = entity.icon
        )
    }

    private fun locationEntityToUpdateMap(entity: LocationEntity): Map<String, Any?> {
        return mapOf(
            "name" to entity.name,
            "latitude" to entity.latitude,
            "longitude" to entity.longitude,
            "description" to entity.description,
            "additionalInfo" to entity.additionalInfo,
            "openingHours" to entity.openingHours,
            "socketTypes" to entity.socketTypes,
            "gpoCount" to entity.gpoCount,
            "hasFee" to entity.hasFee,
            "chargingCosts" to entity.chargingCosts,
            "publicPrivate" to entity.publicPrivate,
            "providerName" to entity.providerName,
            "providerWebsite" to entity.providerWebsite,
            "businessName" to entity.businessName,
            "streetAddress" to entity.streetAddress,
            "suburb" to entity.suburb,
            "state" to entity.state,
            "postcode" to entity.postcode,
            "color" to entity.color,
            "icon" to entity.icon
        )
    }

    private suspend fun updateLocalWithServerInfo(localId: String, serverLoc: ServerLocation) {
        locationDao.updateServerSyncInfo(
            localId,
            serverLoc.id,
            serverLoc.version,
            "SYNCED",
            System.currentTimeMillis()
        )
    }
}
```

The implementer needs to add a `@Query` method to `LocationDao`:

```kotlin
@Query("UPDATE locations SET serverId = :serverId, serverVersion = :serverVersion, serverSyncStatus = :syncStatus, serverLastSyncedAt = :syncedAt WHERE id = :id")
suspend fun updateServerSyncInfo(id: String, serverId: String, serverVersion: Int, syncStatus: String, syncedAt: Long)
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/a42r/pevplugshare/data/repository/ServerSyncRepository.kt \
       app/src/main/java/com/a42r/pevplugshare/data/db/dao/
git commit -m "feat: server sync repository — upload/download locations and photos"
```

---

### Task 8: Hilt DI Wiring

**Files:**
- Modify: `app/src/main/java/com/a42r/pevplugshare/di/NetworkModule.kt`

- [ ] **Step 1: Add server providers**

Add to NetworkModule:

```kotlin
@Singleton
@Provides
fun provideServerApiClient(tokenManager: TokenManager): ServerApiClient {
    return ServerApiClient(tokenManager)
}

@Singleton
@Provides
fun provideServerAuthRepository(
    serverApiClient: ServerApiClient,
    tokenManager: TokenManager
): ServerAuthRepository {
    return ServerAuthRepository(serverApiClient, tokenManager)
}

@Singleton
@Provides
fun provideServerSyncRepository(
    serverApiClient: ServerApiClient,
    locationDao: LocationDao,
    photoDao: PhotoDao
): ServerSyncRepository {
    return ServerSyncRepository(serverApiClient, locationDao, photoDao)
}
```

Add the necessary imports.

- [ ] **Step 2: Verify compilation, commit**

```bash
git add app/src/main/java/com/a42r/pevplugshare/di/NetworkModule.kt
git commit -m "feat: Hilt providers for server API client and repositories"
```

---

### Task 9: Server Account Activity

**Files:**
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/server/ServerAccountActivity.kt`
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/server/ServerAccountViewModel.kt`
- Create: `app/src/main/res/layout/activity_server_account.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Create layout**

Layout with:
- Server URL input field (TextInputLayout)
- Google Sign-In button (com.google.android.gms.common.SignInButton or Material button)
- Connected state: show email, display name, disconnect button
- Status text

- [ ] **Step 2: Create ViewModel**

```kotlin
@HiltViewModel
class ServerAccountViewModel @Inject constructor(
    private val serverAuthRepository: ServerAuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    // State flows for connection status, loading, error
    // Methods: connect(serverUrl, googleIdToken), disconnect()
}
```

- [ ] **Step 3: Create Activity**

Google Sign-In flow:
1. User enters server URL
2. User taps "Sign In with Google"
3. Launch Google Sign-In intent
4. On result, get Google ID token
5. Send to server via `serverAuthRepository.authenticateWithGoogle(idToken)`
6. On success, show connected state

Use `GoogleSignInClient` / `GoogleSignIn.getClient()` or the newer `CredentialManager` API.

- [ ] **Step 4: Register activity in manifest, add strings**

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/a42r/pevplugshare/ui/server/ \
       app/src/main/res/layout/activity_server_account.xml \
       app/src/main/AndroidManifest.xml \
       app/src/main/res/values/strings.xml
git commit -m "feat: server account screen — Google Sign-In and connection management"
```

---

### Task 10: Sync Location Screen

**Files:**
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/sync/SyncLocationActivity.kt`
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/sync/SyncLocationViewModel.kt`
- Create: `app/src/main/res/layout/activity_sync_location.xml`
- Create: `app/src/main/res/layout/item_sync_location.xml`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create list item layout**

Each item shows: location name, sync status icon (local/synced/modified), checkbox for selection.

- [ ] **Step 2: Create activity layout**

RecyclerView of locations with checkboxes. "Select All" toggle. "Upload Selected" button. Progress bar.

- [ ] **Step 3: Create ViewModel**

Loads all local locations from `LocationDao`. For each, shows current `serverSyncStatus`. User selects locations and taps "Upload". Calls `serverSyncRepository.uploadLocation()` for each, then uploads photos.

- [ ] **Step 4: Create Activity with RecyclerView adapter**

- [ ] **Step 5: Register in manifest, commit**

```bash
git add app/src/main/java/com/a42r/pevplugshare/ui/sync/ \
       app/src/main/res/layout/activity_sync_location.xml \
       app/src/main/res/layout/item_sync_location.xml \
       app/src/main/AndroidManifest.xml
git commit -m "feat: sync location screen — select and upload locations to server"
```

---

### Task 11: Settings Integration & Navigation

**Files:**
- Modify: `app/src/main/java/com/a42r/pevplugshare/ui/settings/SettingsActivity.kt`
- Modify: `app/src/main/res/layout/activity_settings.xml`

- [ ] **Step 1: Add server section to settings**

Add a "Central Server" section to SettingsActivity:
- Show connection status (connected/disconnected)
- Show server email if connected
- "Manage Server Account" button → opens ServerAccountActivity
- "Sync Locations" button → opens SyncLocationActivity (only if connected)

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/a42r/pevplugshare/ui/settings/ \
       app/src/main/res/layout/activity_settings.xml
git commit -m "feat: add server account section to settings"
```

---

### Task 12: Final Verification

- [ ] **Step 1: Verify full Android build**

```bash
./gradlew assembleDebug 2>&1 | tail -5
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Install on emulator and smoke test**

```bash
./gradlew installDebug
adb shell am start -n com.a42r.pevplugshare/.ui.auth.SplashActivity
```

Verify: app launches, settings shows server section, server account screen opens.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: Phase 4 complete — Android server integration with Google Sign-In and sync"
```
