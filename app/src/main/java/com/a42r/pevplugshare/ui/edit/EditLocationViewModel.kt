package com.a42r.pevplugshare.ui.edit

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.data.model.Location
import com.a42r.pevplugshare.data.repository.LocationRepositoryImpl
import com.a42r.pevplugshare.util.LocationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EditLocationState {
    object Loading : EditLocationState()
    data class Loaded(val location: Location) : EditLocationState()
    object Ready : EditLocationState()  // For add mode - form ready for input
    object Updating : EditLocationState()
    object UpdateSuccess : EditLocationState()
    object DeleteSuccess : EditLocationState()
    data class Error(val message: String) : EditLocationState()
}

data class EditLocationFormData(
    val name: String = "",
    val socketTypes: String = "",
    val accessType: String = "public",
    val hasFee: Boolean = false,
    val providerName: String = "",
    val providerWebsite: String = "",
    val chargingCosts: String = "",
    val description: String = "",
    val openingHours: String = "",
    val additionalInfo: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val gpoCount: Int = 1,
    val isFavorite: Boolean = false,
    val photoPaths: List<String> = emptyList()
)

@HiltViewModel
class EditLocationViewModel @Inject constructor(
    private val locationRepository: com.a42r.pevplugshare.domain.repository.LocationRepository,
    private val osmandSyncService: com.a42r.pevplugshare.data.repository.OsmandLocationSyncService,
    private val tokenManager: com.a42r.pevplugshare.data.remote.TokenManager,
    private val descriptionParser: com.a42r.pevplugshare.ai.LocationDescriptionParser
) : ViewModel() {

    private val _editState = MutableStateFlow<EditLocationState>(EditLocationState.Loading)
    val editState: StateFlow<EditLocationState> = _editState

    private val _formData = MutableStateFlow(EditLocationFormData())
    val formData: StateFlow<EditLocationFormData> = _formData
    
    private var originalLocation: Location? = null

    fun initializeForAddMode() {
        _editState.value = EditLocationState.Ready
        // Set default values for new location
        _formData.value = EditLocationFormData(
            socketTypes = "AS3112",
            accessType = "public",
            openingHours = "24/7",
            gpoCount = 1
        )
    }

    fun loadLocation(locationId: String) {
        viewModelScope.launch {
            _editState.value = EditLocationState.Loading
            try {
                // Get selected category
                val category = tokenManager.getSelectedCategory() ?: "Charging Locations"
                
                // Load locations from repository
                val result = locationRepository.getLocationsByCategory(category)
                result.onSuccess { locations ->
                    val location = locations.find { it.id == locationId }
                    if (location != null) {
                        originalLocation = location
                        _formData.value = EditLocationFormData(
                            name = location.name,
                            socketTypes = location.socketTypes.firstOrNull()?.name ?: "AS3112",
                            accessType = location.publicPrivate.name.lowercase(),
                            hasFee = location.hasFee,
                            providerName = location.providerName ?: "",
                            providerWebsite = location.providerWebsite ?: "",
                            chargingCosts = location.chargingCosts ?: "",
                            description = location.description ?: "",
                            openingHours = location.openingHours ?: "",
                            additionalInfo = location.additionalInfo ?: "",
                            latitude = location.latitude,
                            longitude = location.longitude,
                            gpoCount = location.gpoCount,
                            isFavorite = location.isFavourite
                        )
                        _editState.value = EditLocationState.Loaded(location)
                    } else {
                        _editState.value = EditLocationState.Error("Location not found")
                    }
                }.onFailure { e ->
                    _editState.value = EditLocationState.Error(e.message ?: "Failed to load location")
                }
            } catch (e: Exception) {
                _editState.value = EditLocationState.Error(e.message ?: "Failed to load location")
            }
        }
    }

    fun updateName(name: String) {
        _formData.value = _formData.value.copy(name = name)
    }

    fun updateGpoCount(count: Int) {
        if (count in 1..10) {
            _formData.value = _formData.value.copy(gpoCount = count)
        }
    }

    fun updateSocketTypes(types: String) {
        _formData.value = _formData.value.copy(socketTypes = types)
    }

    fun updateAccessType(accessType: String) {
        _formData.value = _formData.value.copy(accessType = accessType)
    }

    fun updateHasFee(hasFee: Boolean) {
        _formData.value = _formData.value.copy(hasFee = hasFee)
    }

    fun updateProviderName(name: String) {
        _formData.value = _formData.value.copy(providerName = name)
    }

    fun updateProviderWebsite(website: String) {
        _formData.value = _formData.value.copy(providerWebsite = website)
    }

    fun updateChargingCosts(costs: String) {
        _formData.value = _formData.value.copy(chargingCosts = costs)
    }

    fun updateDescription(description: String) {
        _formData.value = _formData.value.copy(description = description)
    }

    fun updateOpeningHours(hours: String) {
        _formData.value = _formData.value.copy(openingHours = hours)
    }

    fun updateAdditionalInfo(info: String) {
        _formData.value = _formData.value.copy(additionalInfo = info)
    }

    fun updateLocation(latitude: Double, longitude: Double) {
        _formData.value = _formData.value.copy(latitude = latitude, longitude = longitude)
    }

    fun updateFavorite(isFavorite: Boolean) {
        _formData.value = _formData.value.copy(isFavorite = isFavorite)
    }

    fun updatePhotoPaths(paths: List<String>) {
        _formData.value = _formData.value.copy(photoPaths = paths)
    }

    fun addLocation(locationId: String? = null) {
        val data = _formData.value

        if (data.name.isBlank()) {
            _editState.value = EditLocationState.Error("Location name is required")
            return
        }

        if (data.gpoCount < 1 || data.gpoCount > 11) {
            _editState.value = EditLocationState.Error("GPO count must be between 1 and 10+")
            return
        }

        if (data.latitude == 0.0 && data.longitude == 0.0) {
            _editState.value = EditLocationState.Error("Location coordinates required")
            return
        }

        viewModelScope.launch {
            _editState.value = EditLocationState.Updating

            try {
                // Use provided locationId (if photos were pre-processed) or generate new one
                val finalLocationId = locationId ?: java.util.UUID.randomUUID().toString()
                Log.d("EditLocationViewModel", "addLocation: Using locationId: $finalLocationId (provided: ${locationId != null})")

                // Create new location
                val newLocation = Location(
                    id = finalLocationId,
                    name = data.name,
                    socketTypes = listOf(com.a42r.pevplugshare.data.model.SocketType.valueOf(data.socketTypes)),
                    publicPrivate = com.a42r.pevplugshare.data.model.LocationAccessType.valueOf(data.accessType.uppercase()),
                    hasFee = data.hasFee,
                    providerName = data.providerName.ifBlank { null },
                    providerWebsite = data.providerWebsite.ifBlank { null },
                    chargingCosts = data.chargingCosts.ifBlank { null },
                    description = data.description.ifBlank { null },
                    openingHours = data.openingHours.ifBlank { null },
                    additionalInfo = data.additionalInfo.ifBlank { null },
                    latitude = data.latitude,
                    longitude = data.longitude,
                    gpoCount = data.gpoCount,
                    isFavourite = data.isFavorite,
                    color = "#1f6b2a",
                    icon = "place",
                    lastModified = System.currentTimeMillis(),
                    syncStatus = com.a42r.pevplugshare.data.model.SyncStatus.PENDING,
                    imagePaths = data.photoPaths
                )
                
                val category = tokenManager.getSelectedCategory() ?: "Charging Locations"
                
                // Add to local database
                locationRepository.addLocation(newLocation, category)
                
                // Mark as pending sync
                osmandSyncService.markLocationPendingSync(newLocation, category)
                
                _editState.value = EditLocationState.UpdateSuccess
            } catch (e: Exception) {
                _editState.value = EditLocationState.Error(e.message ?: "Add failed")
            }
        }
    }

    fun submitUpdates(locationId: String) {
        val data = _formData.value

        if (data.name.isBlank()) {
            _editState.value = EditLocationState.Error("Location name is required")
            return
        }

        if (data.gpoCount < 1 || data.gpoCount > 10) {
            _editState.value = EditLocationState.Error("GPO count must be between 1 and 10")
            return
        }

        viewModelScope.launch {
            _editState.value = EditLocationState.Updating

            try {
                val original = originalLocation ?: run {
                    _editState.value = EditLocationState.Error("Original location not found")
                    return@launch
                }
                
                // Create updated location
                val updatedLocation = original.copy(
                    name = data.name,
                    socketTypes = listOf(com.a42r.pevplugshare.data.model.SocketType.valueOf(data.socketTypes)),
                    publicPrivate = com.a42r.pevplugshare.data.model.LocationAccessType.valueOf(data.accessType.uppercase()),
                    hasFee = data.hasFee,
                    providerName = data.providerName.ifBlank { null },
                    providerWebsite = data.providerWebsite.ifBlank { null },
                    chargingCosts = data.chargingCosts.ifBlank { null },
                    description = data.description.ifBlank { null },
                    openingHours = data.openingHours.ifBlank { null },
                    additionalInfo = data.additionalInfo.ifBlank { null },
                    latitude = data.latitude,
                    longitude = data.longitude,
                    gpoCount = data.gpoCount,
                    isFavourite = data.isFavorite,
                    lastModified = System.currentTimeMillis(),
                    syncStatus = com.a42r.pevplugshare.data.model.SyncStatus.PENDING,
                    imagePaths = data.photoPaths
                )
                
                val category = tokenManager.getSelectedCategory() ?: "Charging Locations"
                
                // Update in local database
                locationRepository.updateLocation(updatedLocation, category)
                
                // Mark as pending sync
                osmandSyncService.markLocationPendingSync(updatedLocation, category)
                
                _editState.value = EditLocationState.UpdateSuccess
            } catch (e: Exception) {
                _editState.value = EditLocationState.Error(e.message ?: "Update failed")
            }
        }
    }
    
    fun syncToOsmand(locationId: String) {
        viewModelScope.launch {
            try {
                val category = tokenManager.getSelectedCategory() ?: "Charging Locations"
                val result = locationRepository.getLocationsByCategory(category)
                
                result.onSuccess { locations ->
                    val location = locations.find { it.id == locationId }
                    if (location != null) {
                        osmandSyncService.updateLocationToOsmand(location, category, 0)
                            .onSuccess {
                                _editState.value = EditLocationState.UpdateSuccess
                            }
                            .onFailure { e ->
                                _editState.value = EditLocationState.Error("Sync failed: ${e.message}")
                            }
                    }
                }
            } catch (e: Exception) {
                _editState.value = EditLocationState.Error("Sync failed: ${e.message}")
            }
        }
    }

    fun parseDescription(description: String, onResult: (com.a42r.pevplugshare.ai.ParsedLocationData?, String?) -> Unit) {
        if (description.isBlank()) {
            onResult(null, "Description is empty")
            return
        }
        
        viewModelScope.launch {
            try {
                val result = descriptionParser.parseDescription(description)
                result.onSuccess { parsed ->
                    // Update form data with parsed values, defaulting opening hours to 24/7 if not found
                    val hours = parsed.openingHours ?: "24/7"
                    updateOpeningHours(hours)
                    
                    if (parsed.chargingPoints != null) {
                        updateGpoCount(parsed.chargingPoints)
                    }
                    if (parsed.capacity != null) {
                        updateChargingCosts(parsed.capacity)
                    }
                    if (parsed.additionalInfo != null) {
                        updateAdditionalInfo(parsed.additionalInfo)
                    }
                    updateHasFee(parsed.hasFee)
                    
                    onResult(parsed, null)
                }.onFailure { e ->
                    onResult(null, e.message ?: "Failed to parse description")
                }
            } catch (e: Exception) {
                onResult(null, e.message ?: "Unexpected error during parsing")
            }
        }
    }

    fun resetForm() {
        _formData.value = EditLocationFormData()
        _editState.value = EditLocationState.Loading
    }

    fun deleteLocation(locationId: String) {
        viewModelScope.launch {
            _editState.value = EditLocationState.Updating
            try {
                val category = tokenManager.getSelectedCategory() ?: "Charging Locations"
                val location = locationRepository.getLocationsByCategory(category).getOrNull()?.find { it.id == locationId }

                if (location != null) {
                    // Delete physical image files
                    location.imagePaths.forEach { imagePath ->
                        com.a42r.pevplugshare.util.ImageUtils.deleteImageFile(imagePath)
                    }
                    // Delete location from repository
                    locationRepository.deleteLocation(locationId)
                    _editState.value = EditLocationState.DeleteSuccess
                } else {
                    _editState.value = EditLocationState.Error("Location not found for deletion")
                }
            } catch (e: Exception) {
                _editState.value = EditLocationState.Error(e.message ?: "Failed to delete location")
            }
        }
    }
}
