package com.a42r.pevplugshare.data.repository

import com.a42r.pevplugshare.data.api.OsmandCategory
import com.a42r.pevplugshare.data.db.dao.LocationDao
import com.a42r.pevplugshare.data.db.entity.LocationEntity
import com.a42r.pevplugshare.data.model.Location
import com.a42r.pevplugshare.data.model.LocationAccessType
import com.a42r.pevplugshare.data.model.SocketType
import com.a42r.pevplugshare.data.model.SyncStatus
import com.a42r.pevplugshare.data.remote.GeocodingService
import com.a42r.pevplugshare.domain.repository.LocationRepository
import com.a42r.pevplugshare.util.DistanceCalculator
import kotlinx.coroutines.flow.first

class LocalLocationRepository(
    private val locationDao: LocationDao,
    private val geocodingService: GeocodingService
) : LocationRepository {

    companion object {
        private const val TAG = "LocalLocationRepository"
    }

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
            // Perform reverse geocoding to get address information
            android.util.Log.d(TAG, "Adding location: ${location.name} at (${location.latitude}, ${location.longitude})")
            android.util.Log.d(TAG, "Performing reverse geocoding for new location")

            val geocodingResult = geocodingService.reverseGeocode(location.latitude, location.longitude)
            android.util.Log.d(TAG, "Geocoding result for new location: $geocodingResult")

            val locationWithAddress = when (geocodingResult) {
                is com.a42r.pevplugshare.data.remote.GeocodingResult.Success -> {
                    // Use displayName for businessName, and if location name is empty, use displayName for location name too
                    val businessName = geocodingResult.displayName ?: geocodingResult.businessName
                    val locationName = if (location.name.isBlank() && geocodingResult.displayName != null) {
                        geocodingResult.displayName
                    } else {
                        location.name
                    }

                    val updatedLocation = location.copy(
                        name = locationName,
                        businessName = businessName,
                        streetAddress = geocodingResult.streetAddress,
                        suburb = geocodingResult.suburb,
                        state = geocodingResult.state,
                        postcode = geocodingResult.postcode
                    )
                    android.util.Log.d(TAG, "Location updated with geocoded data: name='$locationName', businessName='$businessName', streetAddress=${geocodingResult.streetAddress}, suburb=${geocodingResult.suburb}, state=${geocodingResult.state}, postcode=${geocodingResult.postcode}")
                    updatedLocation
                }
                is com.a42r.pevplugshare.data.remote.GeocodingResult.Error -> {
                    // Log the error but continue with the location as-is
                    android.util.Log.w(TAG, "Geocoding failed for new location: ${geocodingResult.message}")
                    location
                }
            }

            val entity = locationWithAddress.toLocationEntity()
            locationDao.insertLocation(entity)
            Result.success(locationWithAddress)
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
            imagePaths = if (imagePaths.isNotBlank()) imagePaths.split(",") else emptyList(), // Map imagePaths
            distanceMeters = null,
            businessName = businessName,
            streetAddress = streetAddress,
            suburb = suburb,
            state = state,
            postcode = postcode
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
            imagePaths = imagePaths.joinToString(","), // Map imagePaths
            descriptionHash = null,
            aiParsed = false,
            aiParsedAt = null,
            businessName = businessName,
            streetAddress = streetAddress,
            suburb = suburb,
            state = state,
            postcode = postcode
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
