package com.a42r.pevplugshare.data.repository

import com.a42r.pevplugshare.data.api.CreateServerLocationRequest
import com.a42r.pevplugshare.data.api.ServerLocation
import com.a42r.pevplugshare.data.api.ServerNotification
import com.a42r.pevplugshare.data.api.ServerPhoto
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

    suspend fun uploadLocation(locationEntity: LocationEntity): Result<ServerLocation> {
        return try {
            val service = serverApiClient.getService()
            val now = System.currentTimeMillis()

            val response = if (locationEntity.serverId != null) {
                service.updateLocation(locationEntity.serverId, locationEntityToUpdateMap(locationEntity))
            } else {
                service.createLocation(locationEntityToCreateRequest(locationEntity))
            }

            if (response.isSuccessful && response.body() != null) {
                val serverLoc = response.body()!!
                updateLocalWithServerInfo(locationEntity.id, serverLoc)
                Result.success(serverLoc)
            } else {
                val error = response.errorBody()?.string() ?: "Upload failed: ${response.code()}"
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadPhoto(serverLocationId: String, photoFile: File): Result<ServerPhoto> {
        return try {
            val mediaType = "image/*".toMediaTypeOrNull()
            val requestBody = photoFile.asRequestBody(mediaType)
            val part = MultipartBody.Part.createFormData("photo", photoFile.name, requestBody)

            val response = serverApiClient.getService().uploadPhoto(serverLocationId, part)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val error = response.errorBody()?.string() ?: "Photo upload failed: ${response.code()}"
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyServerLocations(): Result<List<ServerLocation>> {
        return try {
            val service = serverApiClient.getService()
            val allLocations = mutableListOf<ServerLocation>()
            var cursor: String? = null

            do {
                val response = service.getMyLocations(cursor = cursor, limit = 100)
                if (!response.isSuccessful || response.body() == null) {
                    val error = response.errorBody()?.string() ?: "Fetch failed: ${response.code()}"
                    return Result.failure(Exception(error))
                }
                val page = response.body()!!
                allLocations.addAll(page.data)
                cursor = if (page.pagination.hasMore) page.pagination.cursor else null
            } while (cursor != null)

            Result.success(allLocations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotifications(): Result<List<ServerNotification>> {
        return try {
            val response = serverApiClient.getService().getNotifications(limit = 50)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                val error = response.errorBody()?.string() ?: "Failed to get notifications: ${response.code()}"
                Result.failure(Exception(error))
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
            id = localId,
            serverId = serverLoc.id,
            serverVersion = serverLoc.version,
            syncStatus = "SYNCED",
            syncedAt = System.currentTimeMillis()
        )
    }
}
