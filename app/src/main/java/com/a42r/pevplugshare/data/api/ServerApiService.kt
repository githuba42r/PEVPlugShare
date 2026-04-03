package com.a42r.pevplugshare.data.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ServerApiService {
    @POST("auth/google")
    suspend fun authenticateWithGoogle(@Body request: GoogleAuthRequest): Response<GoogleAuthResponse>

    @GET("api/users/me")
    suspend fun getCurrentUser(): Response<ServerUser>

    @GET("api/users/search")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<PaginatedResponse<ServerUser>>

    @POST("api/locations")
    suspend fun createLocation(@Body location: CreateServerLocationRequest): Response<ServerLocation>

    @GET("api/locations")
    suspend fun getMyLocations(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<PaginatedResponse<ServerLocation>>

    @GET("api/locations/{id}")
    suspend fun getLocation(@Path("id") id: String): Response<ServerLocation>

    @PUT("api/locations/{id}")
    suspend fun updateLocation(
        @Path("id") id: String,
        @Body fields: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ServerLocation>

    @HTTP(method = "DELETE", path = "api/locations/{id}", hasBody = true)
    suspend fun deleteLocation(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<Map<String, String>>

    @GET("api/locations/shared-with-me")
    suspend fun getSharedLocations(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<PaginatedResponse<ServerLocation>>

    @GET("api/locations/nearby")
    suspend fun getNearbyLocations(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("radius") radiusKm: Double,
        @Query("cursor") cursor: String? = null
    ): Response<PaginatedResponse<ServerLocation>>

    @Multipart
    @POST("api/locations/{locationId}/photos")
    suspend fun uploadPhoto(
        @Path("locationId") locationId: String,
        @Part photo: MultipartBody.Part
    ): Response<ServerPhoto>

    @DELETE("api/locations/{locationId}/photos/{photoId}")
    suspend fun deletePhoto(
        @Path("locationId") locationId: String,
        @Path("photoId") photoId: String
    ): Response<Unit>

    @GET("api/notifications")
    suspend fun getNotifications(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<PaginatedResponse<ServerNotification>>

    @PUT("api/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<Map<String, String>>

    @PUT("api/notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<Map<String, String>>
}
