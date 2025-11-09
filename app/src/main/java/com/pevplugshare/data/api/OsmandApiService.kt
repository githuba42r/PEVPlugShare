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
        const val WEB_API_URL = "https://osmand.net/mapapi/"
    }

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

    // Web API - Login with username/password
    @POST("auth/login")
    suspend fun webLogin(
        @Body loginRequest: OsmandLoginRequest
    ): OsmandLoginResponse

    // Web API - Get list of files including favorites
    @GET("list-files")
    suspend fun listFiles(): OsmandListFilesResponse

    // Web API - Download favorites GPX file
    @GET("download-file")
    suspend fun downloadFile(
        @Query("type") type: String,
        @Query("name") name: String,
        @Query("shared") shared: Boolean = false
    ): okhttp3.ResponseBody
    
    // Web API - Update favorite waypoint
    @POST("fav/update")
    @FormUrlEncoded
    suspend fun updateFavorite(
        @retrofit2.http.Field("wptName") wptName: String,
        @retrofit2.http.Field("oldGroupName") oldGroupName: String,
        @retrofit2.http.Field("newGroupName") newGroupName: String,
        @retrofit2.http.Field("oldGroupUpdatetime") oldGroupUpdatetime: Long,
        @retrofit2.http.Field("newGroupUpdatetime") newGroupUpdatetime: Long,
        @retrofit2.http.Field("ind") ind: Int,
        @retrofit2.http.Field("lat") lat: Double? = null,
        @retrofit2.http.Field("lon") lon: Double? = null,
        @retrofit2.http.Field("desc") desc: String? = null,
        @retrofit2.http.Field("color") color: String? = null
    ): okhttp3.ResponseBody
}
