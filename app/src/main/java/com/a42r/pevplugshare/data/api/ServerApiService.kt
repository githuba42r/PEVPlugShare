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

    // Friends
    @POST("api/friends/request")
    suspend fun sendFriendRequest(@Body body: FriendRequestBody): Response<ServerFriendship>

    @GET("api/friends")
    suspend fun getFriends(@Query("cursor") cursor: String? = null, @Query("limit") limit: Int? = null): Response<PaginatedResponse<ServerUser>>

    @GET("api/friends/requests/incoming")
    suspend fun getIncomingRequests(@Query("cursor") cursor: String? = null): Response<PaginatedResponse<ServerFriendship>>

    @GET("api/friends/requests/outgoing")
    suspend fun getOutgoingRequests(@Query("cursor") cursor: String? = null): Response<PaginatedResponse<ServerFriendship>>

    @PUT("api/friends/requests/{id}/accept")
    suspend fun acceptFriendRequest(@Path("id") id: String): Response<ServerFriendship>

    @PUT("api/friends/requests/{id}/decline")
    suspend fun declineFriendRequest(@Path("id") id: String): Response<ServerFriendship>

    @DELETE("api/friends/{userId}")
    suspend fun removeFriend(@Path("userId") userId: String): Response<Unit>

    // Groups
    @POST("api/groups")
    suspend fun createGroup(@Body body: CreateGroupBody): Response<ServerFriendGroup>

    @GET("api/groups")
    suspend fun getGroups(@Query("cursor") cursor: String? = null): Response<PaginatedResponse<ServerFriendGroup>>

    @GET("api/groups/{id}")
    suspend fun getGroup(@Path("id") id: String): Response<ServerFriendGroup>

    @PUT("api/groups/{id}")
    suspend fun updateGroup(@Path("id") id: String, @Body body: CreateGroupBody): Response<ServerFriendGroup>

    @DELETE("api/groups/{id}")
    suspend fun deleteGroup(@Path("id") id: String): Response<Unit>

    @GET("api/groups/{id}/members")
    suspend fun getGroupMembers(@Path("id") id: String): Response<List<ServerGroupMember>>

    @POST("api/groups/{id}/members")
    suspend fun addGroupMembers(@Path("id") id: String, @Body body: AddMembersBody): Response<Unit>

    @DELETE("api/groups/{id}/members/{userId}")
    suspend fun removeGroupMember(@Path("id") id: String, @Path("userId") userId: String): Response<Unit>

    @PUT("api/groups/{id}/members/{userId}/role")
    suspend fun changeGroupMemberRole(@Path("id") id: String, @Path("userId") userId: String, @Body body: ChangeRoleBody): Response<Unit>

    @POST("api/groups/{id}/invite")
    suspend fun generateGroupInvite(@Path("id") id: String): Response<InviteResponse>

    @GET("api/groups/invite/{token}")
    suspend fun getGroupInviteDetails(@Path("token") token: String): Response<ServerGroupInvitation>

    @POST("api/groups/invite/{token}/accept")
    suspend fun acceptGroupInvite(@Path("token") token: String): Response<Unit>

    // Sharing
    @POST("api/locations/{id}/share")
    suspend fun shareLocation(@Path("id") id: String, @Body body: ShareWithBody): Response<ServerLocationShare>

    @DELETE("api/locations/{id}/share/{shareId}")
    suspend fun revokeShare(@Path("id") id: String, @Path("shareId") shareId: String): Response<Unit>

    @GET("api/locations/{id}/shares")
    suspend fun getLocationShares(@Path("id") id: String, @Query("cursor") cursor: String? = null): Response<PaginatedResponse<ServerLocationShare>>

    @POST("api/locations/{id}/share/direct")
    suspend fun generateDirectShare(@Path("id") id: String, @Body body: DirectShareBody): Response<DirectShareResponse>

    @GET("api/share/{token}")
    suspend fun resolveDirectShare(@Path("token") token: String): Response<ServerLocation>

    @POST("api/locations/{shareId}/hide")
    suspend fun hideShare(@Path("shareId") shareId: String): Response<Unit>

    @DELETE("api/locations/{shareId}/hide")
    suspend fun unhideShare(@Path("shareId") shareId: String): Response<Unit>

    // Suggestions
    @POST("api/locations/{id}/suggestions")
    suspend fun submitSuggestion(@Path("id") id: String, @Body body: SubmitSuggestionBody): Response<ServerChangeSuggestion>

    @GET("api/suggestions/incoming")
    suspend fun getIncomingSuggestions(@Query("cursor") cursor: String? = null): Response<PaginatedResponse<ServerChangeSuggestion>>

    @GET("api/suggestions/outgoing")
    suspend fun getOutgoingSuggestions(@Query("cursor") cursor: String? = null): Response<PaginatedResponse<ServerChangeSuggestion>>

    @PUT("api/suggestions/{id}/accept")
    suspend fun acceptSuggestion(@Path("id") id: String, @Body body: SuggestionResponseBody): Response<ServerChangeSuggestion>

    @PUT("api/suggestions/{id}/reject")
    suspend fun rejectSuggestion(@Path("id") id: String, @Body body: SuggestionResponseBody): Response<ServerChangeSuggestion>

    @PUT("api/suggestions/{id}/withdraw")
    suspend fun withdrawSuggestion(@Path("id") id: String): Response<ServerChangeSuggestion>
}
