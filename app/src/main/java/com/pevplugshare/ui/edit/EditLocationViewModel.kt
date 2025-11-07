package com.pevplugshare.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pevplugshare.data.model.Location
import com.pevplugshare.data.repository.LocationRepositoryImpl
import com.pevplugshare.util.LocationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EditLocationState {
    object Loading : EditLocationState()
    data class Loaded(val location: Location) : EditLocationState()
    object Updating : EditLocationState()
    object UpdateSuccess : EditLocationState()
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
    val gpoCount: Int = 1
)

@HiltViewModel
class EditLocationViewModel @Inject constructor(
    private val locationRepository: com.pevplugshare.domain.repository.LocationRepository
) : ViewModel() {

    private val _editState = MutableStateFlow<EditLocationState>(EditLocationState.Loading)
    val editState: StateFlow<EditLocationState> = _editState

    private val _formData = MutableStateFlow(EditLocationFormData())
    val formData: StateFlow<EditLocationFormData> = _formData

    fun loadLocation(locationId: String) {
        viewModelScope.launch {
            _editState.value = EditLocationState.Loading
            try {
                val location = Location(
                    id = locationId,
                    name = "",
                    description = "",
                    latitude = 0.0,
                    longitude = 0.0,
                    socketTypes = emptyList(),
                    publicPrivate = com.pevplugshare.data.model.LocationAccessType.PUBLIC,
                    hasFee = false,
                    providerName = "",
                    providerWebsite = "",
                    chargingCosts = "",
                    openingHours = "",
                    additionalInfo = "",
                    color = "#1f6b2a",
                    icon = "place",
                    lastModified = System.currentTimeMillis(),
                    gpoCount = 1
                )
                _editState.value = EditLocationState.Loaded(location)
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
                _editState.value = EditLocationState.UpdateSuccess
            } catch (e: Exception) {
                _editState.value = EditLocationState.Error(e.message ?: "Update failed")
            }
        }
    }

    fun resetForm() {
        _formData.value = EditLocationFormData()
        _editState.value = EditLocationState.Loading
    }
}
