package com.pevplugshare.data.repository

import com.pevplugshare.data.api.OsmandCategory
import com.pevplugshare.data.db.dao.LocationDao
import com.pevplugshare.data.db.entity.LocationEntity
import com.pevplugshare.data.model.Location
import com.pevplugshare.data.model.LocationAccessType
import com.pevplugshare.data.model.SocketType
import com.pevplugshare.data.model.SyncStatus
import com.pevplugshare.domain.repository.LocationRepository
import com.pevplugshare.util.DistanceCalculator
import kotlinx.coroutines.flow.first

class LocalLocationRepository(
    private val locationDao: LocationDao
) : LocationRepository {

    override suspend fun getCategories(): Result<List<OsmandCategory>> {
        // OSMAnd doesn't provide a way to query categories via API
        // Return a default category
        return Result.success(listOf(
            OsmandCategory(name = "Charging Stations", color = "#1f6b2a")
        ))
    }

    override suspend fun getLocationsByCategory(categoryName: String): Result<List<Location>> {
        return try {
            val locationEntities = locationDao.getAllLocations().first()
            val locations = locationEntities.map { it.toLocation() }
            Result.success(locations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addLocation(location: Location, categoryName: String): Result<Location> {
        return try {
            val entity = location.toLocationEntity()
            locationDao.insertLocation(entity)
            Result.success(location)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLocation(
        location: Location,
        categoryName: String
    ): Result<Location> {
        return try {
            val entity = location.toLocationEntity()
            locationDao.updateLocation(entity)
            Result.success(location)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateLocationToOsmand(
        location: Location,
        categoryName: String,
        index: Int,
        updateTime: Long
    ): Result<Unit> {
        // This is handled by AuthRepositoryImpl which has access to the API
        return Result.failure(UnsupportedOperationException("LocalLocationRepository does not sync to OSMAnd"))
    }

    override suspend fun deleteLocation(locationId: String): Result<Unit> {
        return try {
            locationDao.deleteLocationById(locationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
        return try {
            val locations = getLocationsByCategory(categoryName).getOrThrow()
            val nearby = locations.mapNotNull { location ->
                val distance = DistanceCalculator.calculateDistance(
                    latitude, longitude,
                    location.latitude, location.longitude
                )
                if (distance <= radiusMeters) {
                    Pair(location, distance)
                } else {
                    null
                }
            }.sortedBy { it.second }
            Result.success(nearby)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Extension functions for entity conversion
    private fun LocationEntity.toLocation(): Location {
        return Location(
            id = id,
            name = name,
            description = description,
            latitude = latitude,
            longitude = longitude,
            socketTypes = parseSocketTypes(socketTypes),
            publicPrivate = LocationAccessType.valueOf(publicPrivate),
            hasFee = hasFee,
            gpoCount = gpoCount,
            openingHours = openingHours,
            additionalInfo = additionalInfo,
            providerName = providerName,
            providerWebsite = providerWebsite,
            chargingCosts = chargingCosts,
            color = color,
            icon = icon,
            lastModified = lastModified,
            isFavourite = isFavourite,
            syncStatus = SyncStatus.valueOf(syncStatus),
            photos = emptyList(),
            distanceMeters = null
        )
    }

    private fun Location.toLocationEntity(): LocationEntity {
        return LocationEntity(
            id = id,
            name = name,
            description = description,
            latitude = latitude,
            longitude = longitude,
            socketTypes = socketTypes.joinToString(",") { it.name },
            publicPrivate = publicPrivate.name,
            hasFee = hasFee,
            gpoCount = gpoCount,
            openingHours = openingHours,
            additionalInfo = additionalInfo,
            providerName = providerName,
            providerWebsite = providerWebsite,
            chargingCosts = chargingCosts,
            color = color,
            icon = icon,
            lastModified = lastModified,
            isFavourite = isFavourite,
            syncStatus = syncStatus.value,
            descriptionHash = null,
            aiParsed = false,
            aiParsedAt = null
        )
    }

    private fun parseSocketTypes(socketTypeString: String): List<SocketType> {
        return if (socketTypeString.isBlank()) {
            emptyList()
        } else {
            socketTypeString.split(",").mapNotNull { 
                try {
                    SocketType.valueOf(it.trim())
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}
