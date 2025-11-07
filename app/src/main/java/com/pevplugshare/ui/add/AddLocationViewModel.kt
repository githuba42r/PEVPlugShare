package com.pevplugshare.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pevplugshare.data.model.Location
import com.pevplugshare.data.model.LocationAccessType
import com.pevplugshare.data.model.SocketType
import com.pevplugshare.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class AddLocationState {
    object Idle : AddLocationState()
    object Loading : AddLocationState()
    object Success : AddLocationState()
    data class Error(val message: String) : AddLocationState()
    object Duplicate : AddLocationState()
}

data class LocationFormData(
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val gpoCount: Int = 1,
    val socketTypes: List<SocketType> = listOf(SocketType.AS3112),
    val publicPrivate: LocationAccessType = LocationAccessType.PUBLIC,
    val hasFee: Boolean = false,
    val description: String = "",
    val openingHours: String = "",
    val additionalInfo: String = "",
    val providerName: String = "",
    val providerWebsite: String = "",
    val chargingCosts: String = ""
)

@HiltViewModel
class AddLocationViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _addLocationState = MutableStateFlow<AddLocationState>(AddLocationState.Idle)
    val addLocationState: StateFlow<AddLocationState> = _addLocationState

    private val _formData = MutableStateFlow(LocationFormData())
    val formData: StateFlow<LocationFormData> = _formData

    fun updateFormData(data: LocationFormData) {
        _formData.value = data
    }

    fun updateName(name: String) {
        _formData.value = _formData.value.copy(name = name)
    }

    fun updateGpoCount(count: Int) {
        _formData.value = _formData.value.copy(gpoCount = count.coerceIn(1, 10))
    }

    fun updateSocketTypes(types: List<SocketType>) {
        _formData.value = _formData.value.copy(socketTypes = types)
    }

    fun updateAccessType(type: LocationAccessType) {
        _formData.value = _formData.value.copy(publicPrivate = type)
    }

    fun updateHasFee(hasFee: Boolean) {
        _formData.value = _formData.value.copy(hasFee = hasFee)
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

    fun updateProviderName(name: String) {
        _formData.value = _formData.value.copy(providerName = name)
    }

    fun updateProviderWebsite(website: String) {
        _formData.value = _formData.value.copy(providerWebsite = website)
    }

    fun updateChargingCosts(costs: String) {
        _formData.value = _formData.value.copy(chargingCosts = costs)
    }

    fun setCurrentLocation(latitude: Double, longitude: Double) {
        _formData.value = _formData.value.copy(latitude = latitude, longitude = longitude)
    }

    fun submitLocation() {
        val data = _formData.value
        
        // Validation
        if (data.name.isBlank()) {
            _addLocationState.value = AddLocationState.Error("Location name is required")
            return
        }
        
        if (data.latitude == 0.0 || data.longitude == 0.0) {
            _addLocationState.value = AddLocationState.Error("GPS location is required")
            return
        }

        viewModelScope.launch {
            _addLocationState.value = AddLocationState.Loading

            try {
                val location = Location(
                    id = UUID.randomUUID().toString(),
                    name = data.name,
                    description = data.description.ifBlank { null },
                    latitude = data.latitude,
                    longitude = data.longitude,
                    socketTypes = data.socketTypes,
                    publicPrivate = data.publicPrivate,
                    hasFee = data.hasFee,
                    gpoCount = data.gpoCount,
                    openingHours = data.openingHours.ifBlank { null },
                    additionalInfo = data.additionalInfo.ifBlank { null },
                    providerName = data.providerName.ifBlank { null },
                    providerWebsite = data.providerWebsite.ifBlank { null },
                    chargingCosts = data.chargingCosts.ifBlank { null },
                    color = "#1f6b2a",
                    icon = "charging_station",
                    lastModified = System.currentTimeMillis()
                )

                // Add to location repository (which handles OSMAnd sync)
                locationRepository.addLocation(location, "PEV Charging Stations")
                    .onSuccess {
                        _addLocationState.value = AddLocationState.Success
                    }
                    .onFailure { error ->
                        _addLocationState.value = AddLocationState.Error(
                            error.message ?: "Failed to add location"
                        )
                    }
            } catch (e: Exception) {
                _addLocationState.value = AddLocationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetForm() {
        _formData.value = LocationFormData()
        _addLocationState.value = AddLocationState.Idle
    }
}
