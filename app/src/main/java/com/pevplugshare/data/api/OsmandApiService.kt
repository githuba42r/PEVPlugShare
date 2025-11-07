package com.pevplugshare.data.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface OsmandApiService {
    companion object {
        const val BASE_URL = "https://api.osmand.net/api/"
        const val OAUTH_URL = "https://osmand.net/api/"
    }

    // OAuth2 Authentication
    @FormUrlEncoded
    @POST("token")
    suspend fun getAccessToken(
        @retrofit2.http.Field("grant_type") grantType: String = "authorization_code",
        @retrofit2.http.Field("code") code: String,
        @retrofit2.http.Field("client_id") clientId: String,
        @retrofit2.http.Field("client_secret") clientSecret: String,
        @retrofit2.http.Field("redirect_uri") redirectUri: String
    ): OsmandAuthResponse

    @FormUrlEncoded
    @POST("token")
    suspend fun refreshAccessToken(
        @retrofit2.http.Field("grant_type") grantType: String = "refresh_token",
        @retrofit2.http.Field("refresh_token") refreshToken: String,
        @retrofit2.http.Field("client_id") clientId: String,
        @retrofit2.http.Field("client_secret") clientSecret: String
    ): OsmandAuthResponse

    // User Info
    @GET("me")
    suspend fun getUserInfo(
        @Header("Authorization") authorization: String
    ): OsmandUserInfo

    // Get all categories
    @GET("v1/favorites/categories")
    suspend fun getCategories(
        @Header("Authorization") authorization: String
    ): OsmandCategoriesResponse

    // Get markers/locations from a category
    @GET("v1/favorites/category/{category}")
    suspend fun getMarkersByCategory(
        @Path("category") categoryName: String,
        @Header("Authorization") authorization: String
    ): OsmandMarkersResponse

    // Create a new marker/location
    @POST("v1/favorites/marker")
    suspend fun createMarker(
        @Body marker: OsmandMarker,
        @Query("category") category: String,
        @Header("Authorization") authorization: String
    ): OsmandMarker

    // Update an existing marker
    @PUT("v1/favorites/marker/{id}")
    suspend fun updateMarker(
        @Path("id") markerId: String,
        @Body marker: OsmandMarker,
        @Query("category") category: String,
        @Header("Authorization") authorization: String
    ): OsmandMarker

    // Delete a marker
    @DELETE("v1/favorites/marker/{id}")
    suspend fun deleteMarker(
        @Path("id") markerId: String,
        @Header("Authorization") authorization: String
    ): Unit

    // Batch sync - get all locations
    @GET("v1/favorites")
    suspend fun getAllFavorites(
        @Header("Authorization") authorization: String
    ): OsmandMarkersResponse
}
