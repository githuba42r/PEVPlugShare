package com.a42r.pevplugshare.data.api

import com.google.gson.annotations.SerializedName

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
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("isAdmin") val isAdmin: Boolean
)

data class ServerLocation(
    @SerializedName("id") val id: String,
    @SerializedName("ownerId") val ownerId: String,
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
    @SerializedName("icon") val icon: String? = null,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("version") val version: Int,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
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

data class ServerPhoto(
    @SerializedName("id") val id: String,
    @SerializedName("locationId") val locationId: String,
    @SerializedName("filePath") val filePath: String,
    @SerializedName("thumbnailPath") val thumbnailPath: String? = null,
    @SerializedName("caption") val caption: String? = null,
    @SerializedName("mimeType") val mimeType: String? = null,
    @SerializedName("uploadedAt") val uploadedAt: String? = null
)

data class ServerNotification(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("type") val type: String,
    @SerializedName("payloadJson") val payloadJson: String,
    @SerializedName("isRead") val isRead: Boolean,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class PaginationInfo(
    @SerializedName("cursor") val cursor: String? = null,
    @SerializedName("hasMore") val hasMore: Boolean
)

data class PaginatedResponse<T>(
    @SerializedName("data") val data: List<T>,
    @SerializedName("pagination") val pagination: PaginationInfo
)
