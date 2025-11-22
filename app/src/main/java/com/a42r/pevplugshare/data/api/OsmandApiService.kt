package com.a42r.pevplugshare.data.api

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
    suspend fun updateFavorite(
        @Query("wptName") wptName: String,
        @Query("oldGroupName") oldGroupName: String,
        @Query("newGroupName") newGroupName: String,
        @Query("oldGroupUpdatetime") oldGroupUpdatetime: Long,
        @Query("newGroupUpdatetime") newGroupUpdatetime: Long,
        @Query("ind") ind: Int,
        @Header("Origin") origin: String = "https://osmand.net",
        @Header("Referer") referer: String = "https://osmand.net/map/mydata/favorites/",
        @Body updateRequest: OsmandUpdateFavoriteRequest
    ): retrofit2.Response<okhttp3.ResponseBody>

    // Web API - Add a new favorite
    // NOTE: For ADD, we do NOT include ind or wptName parameters at all
    @POST("fav/update")
    suspend fun addFavorite(
        @Query("oldGroupName") oldGroupName: String,
        @Query("newGroupName") newGroupName: String,
        @Query("oldGroupUpdatetime") oldGroupUpdatetime: Long,
        @Query("newGroupUpdatetime") newGroupUpdatetime: Long,
        @Header("Origin") origin: String = "https://osmand.net",
        @Header("Referer") referer: String = "https://osmand.net/map/mydata/favorites/",
        @Body updateRequest: OsmandUpdateFavoriteRequest
    ): retrofit2.Response<okhttp3.ResponseBody>
}
