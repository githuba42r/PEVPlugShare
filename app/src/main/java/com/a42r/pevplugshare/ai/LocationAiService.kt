package com.a42r.pevplugshare.ai

import android.util.Log
import com.a42r.pevplugshare.data.db.dao.LocationDao
import com.a42r.pevplugshare.data.db.entity.LocationEntity
import com.a42r.pevplugshare.data.model.Location
import com.a42r.pevplugshare.data.model.LocationAccessType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationAiService @Inject constructor(
    private val parser: LocationDescriptionParser,
    private val locationDao: LocationDao
) {
    companion object {
        private const val TAG = "LocationAiService"
    }

    /**
     * Process a location and parse its description with AI if needed
     * Checks if description has changed before parsing
     */
    suspend fun processLocation(location: Location, entity: LocationEntity): LocationEntity {
        return withContext(Dispatchers.IO) {
            val description = location.description

            // Skip if no description
            if (description.isNullOrEmpty()) {
                Log.d(TAG, "Skipping ${location.name} - no description")
                return@withContext entity
            }

            // Calculate new hash
            val newHash = parser.calculateDescriptionHash(description)

            // Check if we need to parse
            val shouldParse = !entity.aiParsed ||
                              parser.hasDescriptionChanged(entity.descriptionHash, description)

            if (!shouldParse) {
                Log.d(TAG, "Skipping ${location.name} - description unchanged")
                return@withContext entity
            }

            Log.d(TAG, "Parsing description for ${location.name}")

            // Parse with AI
            val result = parser.parseDescription(description)

            result.onSuccess { parsed ->
                Log.d(TAG, "Successfully parsed ${location.name}: " +
                    "hours=${parsed.openingHours}, " +
                    "points=${parsed.chargingPoints}, " +
                    "capacity=${parsed.capacity}, " +
                    "free=${parsed.isFree}, " +
                    "public=${parsed.isPublic}")

                // Default to 24/7 if no opening hours found
                val hours = parsed.openingHours ?: "24/7"

                // Update entity with parsed data
                return@withContext entity.copy(
                    openingHours = hours,
                    gpoCount = parsed.chargingPoints ?: entity.gpoCount,
                    chargingCosts = parsed.capacity ?: entity.chargingCosts,
                    hasFee = parsed.hasFee,
                    publicPrivate = when (parsed.isPublic) {
                        true -> LocationAccessType.PUBLIC.name
                        false -> LocationAccessType.PRIVATE.name
                        null -> entity.publicPrivate
                    },
                    descriptionHash = newHash,
                    aiParsed = true,
                    aiParsedAt = System.currentTimeMillis()
                )
            }.onFailure { error ->
                Log.e(TAG, "Failed to parse ${location.name}", error)
            }

            // Return original entity with hash updated to avoid retry
            entity.copy(
                descriptionHash = newHash,
                aiParsed = false
            )
        }
    }

    /**
     * Process a batch of locations
     */
    suspend fun processLocations(locations: List<Pair<Location, LocationEntity>>): List<LocationEntity> {
        return withContext(Dispatchers.IO) {
            locations.map { (location, entity) ->
                processLocation(location, entity)
            }
        }
    }

    /**
     * Reprocess all locations with changed descriptions
     */
    suspend fun reprocessAllLocations() {
        withContext(Dispatchers.IO) {
            val entities = locationDao.getAllLocations().first()

            entities.forEach { entity ->
                val location = entity.toLocation()
                val updated = processLocation(location, entity)

                if (updated != entity) {
                    locationDao.updateLocation(updated)
                }
            }
        }
    }

    private fun LocationEntity.toLocation(): Location {
        return Location(
            id = id,
            name = name,
            description = description,
            latitude = latitude,
            longitude = longitude,
            socketTypes = emptyList(),
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
            syncStatus = com.a42r.pevplugshare.data.model.SyncStatus.valueOf(syncStatus)
        )
    }
}
