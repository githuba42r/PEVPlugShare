package com.a42r.pevplugshare.ui.edit

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.data.model.Location
import com.a42r.pevplugshare.data.remote.GeocodingResult
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
    val photoPaths: List<String> = emptyList(),
    val businessName: String = "",
    val streetAddress: String = "",
    val suburb: String = "",
    val state: String = "",
    val postcode: String = ""
)

@HiltViewModel
class EditLocationViewModel @Inject constructor(
    private val locationRepository: com.a42r.pevplugshare.domain.repository.LocationRepository,
    private val authRepository: com.a42r.pevplugshare.domain.repository.AuthRepository,
    private val osmandSyncService: com.a42r.pevplugshare.data.repository.OsmandLocationSyncService,
    private val tokenManager: com.a42r.pevplugshare.data.remote.TokenManager,
    private val descriptionParser: com.a42r.pevplugshare.ai.LocationDescriptionParser,
    private val descriptionGenerator: com.a42r.pevplugshare.ai.LocationDescriptionGenerator,
    private val geocodingService: com.a42r.pevplugshare.data.remote.GeocodingService
) : ViewModel() {

    companion object {
        private const val TAG = "EditLocationViewModel"
    }

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
                Log.d(TAG, "━━━ LOADING LOCATION ━━━")
                Log.d(TAG, "Location ID: $locationId")

                // Get selected category
                val category = tokenManager.getSelectedCategory() ?: "Charging Locations"

                // Load locations from repository
                val result = locationRepository.getLocationsByCategory(category)
                result.onSuccess { locations ->
                    val location = locations.find { it.id == locationId }
                    if (location != null) {
                        Log.d(TAG, "✓ Found location in repository")
                        Log.d(TAG, "  Description from DB: '${location.description}'")
                        Log.d(TAG, "  Additional info from DB: '${location.additionalInfo}'")

                        originalLocation = location
                        val initialFormData = EditLocationFormData(
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
                            isFavorite = location.isFavourite,
                            businessName = location.businessName ?: "",
                            streetAddress = location.streetAddress ?: "",
                            suburb = location.suburb ?: "",
                            state = location.state ?: "",
                            postcode = location.postcode ?: ""
                        )
                        _formData.value = initialFormData
                        _editState.value = EditLocationState.Loaded(location)

                        // Check if address fields are empty and perform reverse geocoding if needed
                        val hasAddressData = location.businessName?.isNotBlank() == true ||
                                           location.streetAddress?.isNotBlank() == true ||
                                           location.suburb?.isNotBlank() == true ||
                                           location.state?.isNotBlank() == true ||
                                           location.postcode?.isNotBlank() == true

                        Log.d(TAG, "Location ${location.id} has address data: $hasAddressData")

                        if (!hasAddressData) {
                            Log.d(TAG, "Performing reverse geocoding for location ${location.id} at (${location.latitude}, ${location.longitude})")
                            // Perform reverse geocoding to populate address fields
                            viewModelScope.launch {
                                try {
                                    val geocodingResult = geocodingService.reverseGeocode(location.latitude, location.longitude)
                                    Log.d(TAG, "Geocoding result for location ${location.id}: $geocodingResult")

                                    if (geocodingResult is com.a42r.pevplugshare.data.remote.GeocodingResult.Success) {
                                        Log.d(TAG, "Updating form data with geocoded address for location ${location.id}")

                                        // Use displayName for businessName, and update location name if it's empty
                                        val businessName = geocodingResult.displayName ?: geocodingResult.businessName ?: ""
                                        val locationName = if (_formData.value.name.isBlank() && geocodingResult.displayName != null) {
                                            geocodingResult.displayName
                                        } else {
                                            _formData.value.name
                                        }

                                        _formData.value = _formData.value.copy(
                                            name = locationName,
                                            businessName = businessName,
                                            streetAddress = geocodingResult.streetAddress ?: "",
                                            suburb = geocodingResult.suburb ?: "",
                                            state = geocodingResult.state ?: "",
                                            postcode = geocodingResult.postcode ?: ""
                                        )

                                        Log.d(TAG, "Form data updated: name='$locationName', businessName='$businessName'")
                                    } else {
                                        Log.w(TAG, "Geocoding failed for location ${location.id}: ${geocodingResult}")
                                    }
                                } catch (e: Exception) {
                                    // Silently fail geocoding - user can still edit manually
                                    Log.w(TAG, "Failed to reverse geocode location ${location.id}: ${e.message}")
                                }
                            }
                        }
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
        Log.d(TAG, "updateGpoCount() called with count=$count")
        if (count in 1..11) {
            val oldValue = _formData.value.gpoCount
            _formData.value = _formData.value.copy(gpoCount = count)
            Log.d(TAG, "  ✓ gpoCount updated: $oldValue → $count")
            Log.d(TAG, "  formData.gpoCount is now: ${_formData.value.gpoCount}")
        } else {
            Log.d(TAG, "  ✗ Count $count is out of valid range 1..11")
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

    fun updateBusinessName(businessName: String) {
        _formData.value = _formData.value.copy(businessName = businessName)
    }

    fun updateStreetAddress(streetAddress: String) {
        _formData.value = _formData.value.copy(streetAddress = streetAddress)
    }

    fun updateSuburb(suburb: String) {
        _formData.value = _formData.value.copy(suburb = suburb)
    }

    fun updateState(state: String) {
        _formData.value = _formData.value.copy(state = state)
    }

    fun updatePostcode(postcode: String) {
        _formData.value = _formData.value.copy(postcode = postcode)
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
                    imagePaths = data.photoPaths,
                    businessName = data.businessName.ifBlank { null },
                    streetAddress = data.streetAddress.ifBlank { null },
                    suburb = data.suburb.ifBlank { null },
                    state = data.state.ifBlank { null },
                    postcode = data.postcode.ifBlank { null }
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

    fun submitUpdates() {
        val data = _formData.value

        if (data.name.isBlank()) {
            _editState.value = EditLocationState.Error("Location name is required")
            return
        }

        if (data.gpoCount < 1 || data.gpoCount > 11) {
            _editState.value = EditLocationState.Error("GPO count must be between 1 and 10+")
            return
        }

        viewModelScope.launch {
            _editState.value = EditLocationState.Updating

            try {
                val original = originalLocation ?: run {
                    _editState.value = EditLocationState.Error("Original location not found")
                    return@launch
                }

                Log.d(TAG, "━━━ SUBMITTING LOCATION UPDATE ━━━")
                Log.d(TAG, "Original gpoCount: ${original.gpoCount}")
                Log.d(TAG, "New gpoCount from form: ${data.gpoCount}")
                Log.d(TAG, "Original description: '${original.description}'")
                Log.d(TAG, "New description from form: '${data.description}'")
                Log.d(TAG, "Original additional info: '${original.additionalInfo}'")
                Log.d(TAG, "New additional info from form: '${data.additionalInfo}'")

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
                    imagePaths = data.photoPaths,
                    businessName = data.businessName.ifBlank { null },
                    streetAddress = data.streetAddress.ifBlank { null },
                    suburb = data.suburb.ifBlank { null },
                    state = data.state.ifBlank { null },
                    postcode = data.postcode.ifBlank { null }
                )

                Log.d(TAG, "Updated location gpoCount after copy: ${updatedLocation.gpoCount}")
                Log.d(TAG, "Updated location description after copy: '${updatedLocation.description}'")
                Log.d(TAG, "Updated location additional info after copy: '${updatedLocation.additionalInfo}'")

                val category = tokenManager.getSelectedCategory() ?: "Charging Locations"

                // Update in local database
                Log.d(TAG, "Calling locationRepository.updateLocation()...")
                locationRepository.updateLocation(updatedLocation, category)
                Log.d(TAG, "✓ locationRepository.updateLocation() completed")

                // Mark as pending sync
                Log.d(TAG, "Calling osmandSyncService.markLocationPendingSync()...")
                osmandSyncService.markLocationPendingSync(updatedLocation, category)
                Log.d(TAG, "✓ osmandSyncService.markLocationPendingSync() completed")

                _editState.value = EditLocationState.UpdateSuccess
                Log.d(TAG, "✓ Set state to UpdateSuccess")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during submitUpdates: ${e.message}", e)
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
                        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        Log.d(TAG, "SYNC TO OSMAND - Starting smart sync")
                        Log.d(TAG, "  Location: ${location.name}")
                        Log.d(TAG, "  Category: $category")
                        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                        // Use smart sync which automatically determines ADD vs UPDATE
                        osmandSyncService.smartSync(location, category)
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

    fun syncToOsmandWithDescription(locationId: String, description: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "━━━ syncToOsmandWithDescription START ━━━")
                Log.d(TAG, "Location ID: $locationId")
                Log.d(TAG, "Description to sync: $description")

                val category = tokenManager.getSelectedCategory() ?: "Charging Locations"
                val result = locationRepository.getLocationsByCategory(category)

                result.onSuccess { locations ->
                    Log.d(TAG, "Found ${locations.size} locations in category")
                    val location = locations.find { it.id == locationId }
                    if (location != null) {
                        Log.d(TAG, "Found location: ${location.name}")
                        Log.d(TAG, "Location from DB - gpoCount: ${location.gpoCount}")
                        Log.d(TAG, "Location from DB - description: '${location.description}'")
                        Log.d(TAG, "Location from DB - socketTypes: ${location.socketTypes}")
                        Log.d(TAG, "Location from DB - hasFee: ${location.hasFee}")

                        // Create a copy of the location with the generated description
                        val locationWithGeneratedDescription = location.copy(description = description)

                        Log.d(TAG, "Calling smartSync with generated description: $description")

                        // Use smart sync which automatically determines ADD vs UPDATE
                        osmandSyncService.smartSync(locationWithGeneratedDescription, category)
                            .onSuccess {
                                Log.d(TAG, "✓ Smart sync SUCCESS")
                                _editState.value = EditLocationState.UpdateSuccess
                            }
                            .onFailure { e ->
                                Log.e(TAG, "✗ Smart sync FAILED: ${e.message}", e)
                                _editState.value = EditLocationState.Error("Sync failed: ${e.message}")
                            }
                    } else {
                        Log.e(TAG, "✗ Location not found in DB with ID: $locationId")
                        _editState.value = EditLocationState.Error("Location not found")
                    }
                }.onFailure { e ->
                    Log.e(TAG, "✗ Failed to get locations: ${e.message}", e)
                    _editState.value = EditLocationState.Error("Failed to get locations: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "✗ Exception in syncToOsmandWithDescription: ${e.message}", e)
                _editState.value = EditLocationState.Error("Sync failed: ${e.message}")
            }
        }
    }

    suspend fun generateDescriptionFromLocation(locationId: String): Result<String> {
        return try {
            val category = tokenManager.getSelectedCategory() ?: "Charging Locations"
            val result = locationRepository.getLocationsByCategory(category)

            result.fold(
                onSuccess = { locations ->
                    val location = locations.find { it.id == locationId }
                    if (location != null) {
                        // Use location details from DB for AI generation
                        val socketType = location.socketTypes.firstOrNull()?.name ?: "AS3112"
                        val accessType = location.publicPrivate
                        val hasFee = location.hasFee
                        val gpoCount = location.gpoCount
                        val openingHours = location.openingHours
                        val capacity = location.chargingCosts
                        val additionalInfo = location.additionalInfo
                        val currentDescription = location.description ?: ""

                        descriptionGenerator.generateDescription(
                            locationName = "",
                            accessType = accessType,
                            hasFee = hasFee,
                            chargingPoints = gpoCount,
                            socketTypes = listOf(socketType),
                            openingHours = openingHours,
                            capacity = capacity,
                            additionalInfo = additionalInfo,
                            description = currentDescription,
                            conversationHistory = emptyList()
                        )
                    } else {
                        Result.failure(Exception("Location not found in database"))
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
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

    suspend fun performReverseGeocoding(latitude: Double, longitude: Double): com.a42r.pevplugshare.data.remote.GeocodingResult {
        return geocodingService.reverseGeocode(latitude, longitude)
    }

    fun hasChanges(): Boolean {
        val original = originalLocation ?: return true // If no original location, consider it as having changes (add mode)

        val current = _formData.value

        // Compare all relevant fields
        return original.name != current.name ||
               original.socketTypes.firstOrNull()?.name != current.socketTypes ||
               original.publicPrivate.name.lowercase() != current.accessType ||
               original.hasFee != current.hasFee ||
               (original.providerName ?: "") != current.providerName ||
               (original.providerWebsite ?: "") != current.providerWebsite ||
               (original.chargingCosts ?: "") != current.chargingCosts ||
               (original.description ?: "") != current.description ||
               (original.openingHours ?: "") != current.openingHours ||
               (original.additionalInfo ?: "") != current.additionalInfo ||
               original.latitude != current.latitude ||
               original.longitude != current.longitude ||
               original.gpoCount != current.gpoCount ||
               original.isFavourite != current.isFavorite ||
               (original.businessName ?: "") != current.businessName ||
               (original.streetAddress ?: "") != current.streetAddress ||
               (original.suburb ?: "") != current.suburb ||
               (original.state ?: "") != current.state ||
               (original.postcode ?: "") != current.postcode ||
               original.imagePaths != current.photoPaths
    }
}
