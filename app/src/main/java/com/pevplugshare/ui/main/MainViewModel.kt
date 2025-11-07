package com.pevplugshare.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pevplugshare.data.model.Location
import com.pevplugshare.domain.repository.LocationRepository
import com.pevplugshare.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LocationListState {
    object Loading : LocationListState()
    data class Success(val locations: List<Location>) : LocationListState()
    data class Error(val message: String) : LocationListState()
    object Empty : LocationListState()
}

sealed class CategoryState {
    object Loading : CategoryState()
    data class Success(val categories: List<Pair<String, String>>) : CategoryState() // name to color
    data class Error(val message: String) : CategoryState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _locationListState = MutableStateFlow<LocationListState>(LocationListState.Loading)
    val locationListState: StateFlow<LocationListState> = _locationListState

    private val _categoryState = MutableStateFlow<CategoryState>(CategoryState.Loading)
    val categoryState: StateFlow<CategoryState> = _categoryState

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    fun loadCategories() {
        viewModelScope.launch {
            _categoryState.value = CategoryState.Loading
            locationRepository.getCategories().onSuccess { categories ->
                val categoryPairs = categories.map { it.name to (it.color ?: "#1f6b2a") }
                _categoryState.value = CategoryState.Success(categoryPairs)
                // Auto-select first category
                if (categoryPairs.isNotEmpty()) {
                    selectCategory(categoryPairs[0].first)
                }
            }.onFailure { error ->
                _categoryState.value = CategoryState.Error(error.message ?: "Failed to load categories")
                _isOffline.value = true
            }
        }
    }

    fun selectCategory(categoryName: String) {
        _selectedCategory.value = categoryName
        loadLocations(categoryName)
    }

    fun loadLocations(categoryName: String) {
        viewModelScope.launch {
            _locationListState.value = LocationListState.Loading
            locationRepository.getLocationsByCategory(categoryName)
                .onSuccess { locations ->
                    if (locations.isEmpty()) {
                        _locationListState.value = LocationListState.Empty
                    } else {
                        // Sort by distance if user location available
                        val sorted = sortLocationsByDistance(locations)
                        _locationListState.value = LocationListState.Success(sorted)
                    }
                    _isOffline.value = false
                }
                .onFailure { error ->
                    _locationListState.value = LocationListState.Error(error.message ?: "Failed to load locations")
                    _isOffline.value = true
                }
        }
    }

    fun updateUserLocation(latitude: Double, longitude: Double) {
        _userLocation.value = Pair(latitude, longitude)
        // Re-sort locations with new position
        if (_locationListState.value is LocationListState.Success) {
            val currentLocations = (_locationListState.value as LocationListState.Success).locations
            val sorted = sortLocationsByDistance(currentLocations)
            _locationListState.value = LocationListState.Success(sorted)
        }
    }

    fun toggleFavourite(location: Location) {
        if (_locationListState.value is LocationListState.Success) {
            val current = (_locationListState.value as LocationListState.Success).locations
            val updated = current.map {
                if (it.id == location.id) it.copy(isFavourite = !it.isFavourite) else it
            }
            _locationListState.value = LocationListState.Success(updated)
        }
    }

    fun syncLocations() {
        val category = _selectedCategory.value ?: return
        loadLocations(category)
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    private fun sortLocationsByDistance(locations: List<Location>): List<Location> {
        val userLoc = _userLocation.value ?: return locations
        return locations.sortedBy { location ->
            val dx = location.latitude - userLoc.first
            val dy = location.longitude - userLoc.second
            dx * dx + dy * dy // Simple distance approximation (good enough for sorting)
        }
    }
}
