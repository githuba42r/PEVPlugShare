package com.a42r.pevplugshare.ui.savedforlater

import android.graphics.RectF
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.data.db.entity.SavedForLaterEntity
import com.a42r.pevplugshare.data.model.PhotoMetadata
import com.a42r.pevplugshare.data.remote.GeocodingService
import com.a42r.pevplugshare.domain.repository.SavedForLaterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

sealed class SavedForLaterState {
    object Loading : SavedForLaterState()
    data class Success(val items: List<SavedForLaterItem>) : SavedForLaterState()
    data class Error(val message: String) : SavedForLaterState()
}

data class SavedForLaterItem(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val bearing: Float?,
    val photoPaths: List<String>,
    val photoMetadata: Map<String, PhotoMetadata>,
    val createdAt: Long,
    val locationDisplayName: String,
    val coordinatesText: String,
    val createdAtText: String
)

@HiltViewModel
class SavedForLaterViewModel @Inject constructor(
    private val savedForLaterRepository: SavedForLaterRepository,
    private val geocodingService: GeocodingService
) : ViewModel() {

    private val _savedLocationsState = MutableStateFlow<SavedForLaterState>(SavedForLaterState.Loading)
    val savedLocationsState: StateFlow<SavedForLaterState> = _savedLocationsState

    fun loadSavedLocations() {
        viewModelScope.launch {
            // Check if we already have loaded data to avoid showing Loading spinner on return
            val alreadyLoaded = _savedLocationsState.value is SavedForLaterState.Success
            if (!alreadyLoaded) {
                _savedLocationsState.value = SavedForLaterState.Loading
            }

            try {
                // Load all entities from database (fast, no network calls)
                val entities = savedForLaterRepository.getAllSavedForLaterList()

                // Build items with coordinates as placeholder names (fast path)
                val items = entities.map { entity ->
                    buildItemWithCoordinates(entity)
                }

                // Emit Success immediately with coordinates as display names
                _savedLocationsState.value = SavedForLaterState.Success(items)

                // Launch parallel geocoding tasks for each item (background)
                items.forEach { item ->
                    launch(Dispatchers.IO) {
                        geocodeAndUpdateItem(item)
                    }
                }
            } catch (e: Exception) {
                Log.e("SavedForLaterViewModel", "Error loading saved locations", e)
                _savedLocationsState.value = SavedForLaterState.Error("Failed to load saved locations: ${e.message}")
            }
        }
    }

    private fun buildItemWithCoordinates(entity: SavedForLaterEntity): SavedForLaterItem {
        // Synchronous helper: builds item with coordinates as placeholder name
        val photoPaths = entity.photoPaths.split(",").filter { it.isNotBlank() }
        val photoMetadata = deserializePhotoMetadata(entity.photoMetadata)
        val coordinatesText = "Lat: %.4f, Lng: %.4f".format(entity.latitude, entity.longitude)
        val createdAtText = formatTimeAgo(entity.createdAt)

        return SavedForLaterItem(
            id = entity.id,
            latitude = entity.latitude,
            longitude = entity.longitude,
            bearing = entity.bearing,
            photoPaths = photoPaths,
            photoMetadata = photoMetadata,
            createdAt = entity.createdAt,
            locationDisplayName = coordinatesText,  // Use coordinates as placeholder
            coordinatesText = coordinatesText,
            createdAtText = createdAtText
        )
    }

    private suspend fun geocodeAndUpdateItem(item: SavedForLaterItem) {
        // Background geocoding: reverse geocode and update the item's display name
        try {
            val result = geocodingService.reverseGeocode(item.latitude, item.longitude)
            val displayName = when (result) {
                is com.a42r.pevplugshare.data.remote.GeocodingResult.Success -> {
                    buildString {
                        if (!result.suburb.isNullOrBlank()) {
                            append(result.suburb)
                        }
                        if (!result.state.isNullOrBlank()) {
                            if (isNotEmpty()) append(", ")
                            append(result.state)
                        }
                        if (!result.postcode.isNullOrBlank()) {
                            if (isNotEmpty()) append(" ")
                            append(result.postcode)
                        }
                        if (isEmpty()) {
                            append("Unknown location")
                        }
                    }
                }
                is com.a42r.pevplugshare.data.remote.GeocodingResult.Error -> {
                    "Unknown location"
                }
            }
            updateItemDisplayName(item.id, displayName)
        } catch (e: Exception) {
            Log.e("SavedForLaterViewModel", "Error geocoding item ${item.id}", e)
            updateItemDisplayName(item.id, "Unknown location")
        }
    }

    private fun updateItemDisplayName(id: String, displayName: String) {
        // Update the Success state with the new display name for this item
        val currentState = _savedLocationsState.value
        if (currentState is SavedForLaterState.Success) {
            val updatedItems = currentState.items.map { item ->
                if (item.id == id) {
                    item.copy(locationDisplayName = displayName)
                } else {
                    item
                }
            }
            _savedLocationsState.value = SavedForLaterState.Success(updatedItems)
        }
    }

    private fun deserializePhotoMetadata(json: String): Map<String, PhotoMetadata> {
        val result = mutableMapOf<String, PhotoMetadata>()
        try {
            val jsonObject = JSONObject(json)
            jsonObject.keys().forEach { key ->
                val metadataObj = jsonObject.getJSONObject(key)
                val metadata = PhotoMetadata(
                    hasGpsData = metadataObj.optBoolean("hasGpsData", false),
                    latitude = metadataObj.optDouble("latitude", 0.0),
                    longitude = metadataObj.optDouble("longitude", 0.0),
                    bearing = if (metadataObj.has("bearing") && !metadataObj.isNull("bearing")) {
                        metadataObj.getDouble("bearing").toFloat()
                    } else null
                )

                if (metadataObj.has("focusRect")) {
                    val focusRectStr = metadataObj.getString("focusRect")
                    metadata.focusRect = PhotoMetadata.deserializeRectF(focusRectStr)
                }

                result[key] = metadata
            }
        } catch (e: Exception) {
            Log.e("SavedForLaterViewModel", "Error deserializing metadata", e)
        }
        return result
    }

    private fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000} minute(s) ago"
            diff < 86400_000 -> "${diff / 3600_000} hour(s) ago"
            diff < 604800_000 -> "${diff / 86400_000} day(s) ago"
            else -> "${diff / 604800_000} week(s) ago"
        }
    }

    suspend fun deleteSavedForLater(id: String, deletePhotoFiles: Boolean = true) {
        withContext(Dispatchers.IO) {
            try {
                // Get the entity to delete associated photos if requested
                if (deletePhotoFiles) {
                    val entity = savedForLaterRepository.getSavedForLaterById(id)
                    if (entity != null) {
                        // Delete photo files
                        val photoPaths = entity.photoPaths.split(",").filter { it.isNotBlank() }
                        photoPaths.forEach { path ->
                            try {
                                val file = File(path)
                                if (file.exists()) {
                                    file.delete()
                                }
                            } catch (e: Exception) {
                                Log.e("SavedForLaterViewModel", "Error deleting photo file: $path", e)
                            }
                        }
                    }
                }

                // Delete from database
                savedForLaterRepository.deleteSavedForLater(id)

                // Reload the list
                loadSavedLocations()
            } catch (e: Exception) {
                Log.e("SavedForLaterViewModel", "Error deleting saved for later", e)
                _savedLocationsState.value = SavedForLaterState.Error("Failed to delete: ${e.message}")
            }
        }
    }
}
