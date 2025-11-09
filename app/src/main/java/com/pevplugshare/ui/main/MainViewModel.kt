package com.pevplugshare.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pevplugshare.data.model.Location
import com.pevplugshare.data.network.ConnectivityObserver
import com.pevplugshare.data.network.NetworkStatus
import com.pevplugshare.data.repository.SyncManager
import com.pevplugshare.domain.repository.LocationRepository
import com.pevplugshare.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
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
    private val authRepository: AuthRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val syncManager: SyncManager,
    private val tokenManager: com.pevplugshare.data.remote.TokenManager
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

    private val _networkStatus = MutableStateFlow<NetworkStatus>(NetworkStatus.ONLINE)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus

    private val _hasPendingOperations = MutableStateFlow(false)
    val hasPendingOperations: StateFlow<Boolean> = _hasPendingOperations

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    init {
        observeNetworkStatus()
        observeSyncStatus()
    }

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            connectivityObserver.observe().collectLatest { status ->
                _networkStatus.value = status
                _isOffline.value = status == NetworkStatus.OFFLINE || status == NetworkStatus.UNAVAILABLE
                
                if (status == NetworkStatus.ONLINE) {
                    checkPendingOperationsAndSync()
                }
            }
        }
    }

    private fun observeSyncStatus() {
        viewModelScope.launch {
            syncManager.syncState.collectLatest { state ->
                _isSyncing.value = state.name == "SYNCING"
            }
        }
    }

    private suspend fun checkPendingOperationsAndSync() {
        val hasPending = syncManager.hasPendingOperations()
        _hasPendingOperations.value = hasPending
        
        if (hasPending) {
            syncManager.syncPendingOperations()
        }
    }

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories

    private val _needsCategorySelection = MutableStateFlow(false)
    val needsCategorySelection: StateFlow<Boolean> = _needsCategorySelection

    fun loadCategories() {
        viewModelScope.launch {
            try {
                val result = authRepository.getFavoritesCategories()
                result.onSuccess { categoryList ->
                    android.util.Log.d("MainViewModel", "Loaded ${categoryList.size} categories from OsmAnd")
                    _categories.value = categoryList
                    
                    // Check if we have a saved category
                    val savedCategory = tokenManager.getSelectedCategory()
                    android.util.Log.d("MainViewModel", "Saved category: $savedCategory")
                    
                    if (categoryList.isNotEmpty()) {
                        if (savedCategory != null && categoryList.contains(savedCategory)) {
                            // Saved category is valid, use it
                            android.util.Log.d("MainViewModel", "Using saved category: $savedCategory")
                            _selectedCategory.value = savedCategory
                            loadLocations(savedCategory)
                        } else {
                            // Saved category doesn't exist or is null, prompt user
                            android.util.Log.d("MainViewModel", "Saved category invalid, prompting user to select")
                            _needsCategorySelection.value = true
                        }
                    } else {
                        // Fallback to default category
                        val defaultCategory = "Charging Stations"
                        _selectedCategory.value = defaultCategory
                        _categories.value = listOf(defaultCategory)
                        tokenManager.saveSelectedCategory(defaultCategory)
                        loadLocations(defaultCategory)
                    }
                }.onFailure {
                    // Fallback to default category on error
                    val defaultCategory = "Charging Stations"
                    _selectedCategory.value = defaultCategory
                    _categories.value = listOf(defaultCategory)
                    tokenManager.saveSelectedCategory(defaultCategory)
                    loadLocations(defaultCategory)
                }
            } catch (e: Exception) {
                // Fallback to default category
                val defaultCategory = "Charging Stations"
                _selectedCategory.value = defaultCategory
                _categories.value = listOf(defaultCategory)
                tokenManager.saveSelectedCategory(defaultCategory)
                loadLocations(defaultCategory)
            }
        }
    }
    
    suspend fun validateSessionAndLoadData(): Boolean {
        return try {
            // Try to load categories which will validate the session
            val result = authRepository.getFavoritesCategories()
            result.onSuccess { categoryList ->
                android.util.Log.d("MainViewModel", "Session valid, loaded ${categoryList.size} categories")
                _categories.value = categoryList
                
                // Load data for selected category
                val savedCategory = tokenManager.getSelectedCategory()
                if (savedCategory != null && categoryList.contains(savedCategory)) {
                    _selectedCategory.value = savedCategory
                    loadLocations(savedCategory)
                }
            }.onFailure { error ->
                android.util.Log.e("MainViewModel", "Session validation failed: ${error.message}")
                
                // Try auto-login if we have stored credentials
                if (authRepository.hasStoredCredentials()) {
                    android.util.Log.d("MainViewModel", "Attempting auto-login...")
                    val loginResult = authRepository.autoLogin()
                    loginResult.onSuccess {
                        // Auto-login succeeded, retry loading categories
                        loadCategories()
                        return@onFailure
                    }
                }
                return false
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Exception during session validation: ${e.message}")
            false
        }
    }

    fun refreshCategories() {
        viewModelScope.launch {
            android.util.Log.d("MainViewModel", "Refreshing categories from OsmAnd...")
            try {
                val result = authRepository.getFavoritesCategories()
                result.onSuccess { categoryList ->
                    android.util.Log.d("MainViewModel", "Received ${categoryList.size} categories: ${categoryList.joinToString()}")
                    _categories.value = categoryList
                }.onFailure { error ->
                    android.util.Log.e("MainViewModel", "Failed to refresh categories: ${error.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Exception refreshing categories: ${e.message}")
            }
        }
    }

    fun selectCategory(categoryName: String) {
        android.util.Log.d("MainViewModel", "Category selected: $categoryName")
        tokenManager.saveSelectedCategory(categoryName)
        _selectedCategory.value = categoryName
        _needsCategorySelection.value = false
        loadLocations(categoryName)
    }

    fun dismissCategorySelectionPrompt() {
        _needsCategorySelection.value = false
    }

    fun loadLocations(categoryName: String) {
        viewModelScope.launch {
            _locationListState.value = LocationListState.Loading
            
            try {
                // First, try to load from local database
                android.util.Log.d("MainViewModel", "Loading locations for category: $categoryName from local DB")
                val dbResult = locationRepository.getLocationsByCategory(categoryName)
                
                if (dbResult.isSuccess) {
                    val dbLocations = dbResult.getOrNull() ?: emptyList()
                    android.util.Log.d("MainViewModel", "Found ${dbLocations.size} locations in local DB")
                    
                    if (dbLocations.isNotEmpty()) {
                        // Have local data, show it immediately
                        val sorted = sortLocationsByDistance(dbLocations)
                        _locationListState.value = LocationListState.Success(sorted)
                        _isOffline.value = false
                        return@launch
                    }
                }
                
                // No local data or empty, download from GPX
                android.util.Log.d("MainViewModel", "No local data, downloading GPX for category: $categoryName")
                val gpxResult = authRepository.downloadFavoritesGpx(categoryName)
                
                gpxResult.onSuccess { locations ->
                    android.util.Log.d("MainViewModel", "Downloaded ${locations.size} locations from GPX")
                    
                    if (locations.isEmpty()) {
                        _locationListState.value = LocationListState.Empty
                    } else {
                        // Save to database
                        android.util.Log.d("MainViewModel", "Saving ${locations.size} locations to database...")
                        var savedCount = 0
                        locations.forEach { location ->
                            try {
                                val result = locationRepository.addLocation(location, categoryName)
                                if (result.isSuccess) {
                                    savedCount++
                                } else {
                                    android.util.Log.e("MainViewModel", "Failed to save location ${location.name}: ${result.exceptionOrNull()?.message}")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("MainViewModel", "Exception saving location ${location.name}: ${e.message}")
                            }
                        }
                        android.util.Log.d("MainViewModel", "Saved $savedCount out of ${locations.size} locations")
                        
                        // Sort and display
                        val sorted = sortLocationsByDistance(locations)
                        android.util.Log.d("MainViewModel", "Setting state to Success with ${sorted.size} locations")
                        _locationListState.value = LocationListState.Success(sorted)
                    }
                    _isOffline.value = false
                }.onFailure { error ->
                    android.util.Log.e("MainViewModel", "Failed to download GPX: ${error.message}")
                    _locationListState.value = LocationListState.Error(error.message ?: "Failed to load locations")
                    _isOffline.value = true
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error loading locations: ${e.message}", e)
                _locationListState.value = LocationListState.Error(e.message ?: "Failed to load locations")
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

    suspend fun manualSync() {
        checkPendingOperationsAndSync()
    }

    suspend fun hasStoredCredentials(): Boolean {
        return tokenManager.hasStoredCredentials()
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
    
    fun getMapZoomLevel(): Float {
        return tokenManager.getMapZoomLevel()
    }

    private fun sortLocationsByDistance(locations: List<Location>): List<Location> {
        val userLoc = _userLocation.value ?: return locations
        return locations.sortedBy { location ->
            val dx = location.latitude - userLoc.first
            val dy = location.longitude - userLoc.second
            dx * dx + dy * dy
        }
    }
}
