package com.a42r.pevplugshare.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.data.model.Location
import com.a42r.pevplugshare.data.model.SocketType
import com.a42r.pevplugshare.data.model.LocationAccessType
import com.a42r.pevplugshare.data.network.ConnectivityObserver
import com.a42r.pevplugshare.data.network.NetworkStatus
import com.a42r.pevplugshare.data.repository.SyncManager
import com.a42r.pevplugshare.domain.repository.LocationRepository
import com.a42r.pevplugshare.domain.repository.AuthRepository
import com.a42r.pevplugshare.domain.repository.SavedForLaterRepository
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

data class FilterState(
    val costFilter: CostFilter = CostFilter.ALL,
    val openNowOnly: Boolean = false,
    val minSockets: Int = 0,
    val maxSockets: Int = 10,
    val accessFilter: AccessFilter = AccessFilter.ALL,
    val socketTypes: Set<SocketType> = emptySet(),
    val favoritesOnly: Boolean = false
) {
    fun isActive(): Boolean {
        return costFilter != CostFilter.ALL ||
                openNowOnly ||
                minSockets > 0 ||
                maxSockets < 10 ||
                accessFilter != AccessFilter.ALL ||
                socketTypes.size < SocketType.values().size ||
                favoritesOnly
    }

    fun reset(): FilterState {
        return FilterState()
    }
}

enum class CostFilter {
    ALL, FREE, PAID
}

enum class AccessFilter {
    ALL, PUBLIC, PRIVATE
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val authRepository: AuthRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val syncManager: SyncManager,
    private val tokenManager: com.a42r.pevplugshare.data.remote.TokenManager,
    private val savedForLaterRepository: SavedForLaterRepository
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

    private val _syncProgress = MutableStateFlow<SyncProgress?>(null)
    val syncProgress: StateFlow<SyncProgress?> = _syncProgress

    private val _savedForLaterCount = MutableStateFlow(0)
    val savedForLaterCount: StateFlow<Int> = _savedForLaterCount

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState

    private var allLocations: List<Location> = emptyList()

    init {
        observeNetworkStatus()
        observeSyncStatus()
        observeSavedForLaterCount()
    }

    private fun observeSavedForLaterCount() {
        viewModelScope.launch {
            savedForLaterRepository.getSavedForLaterCountFlow().collectLatest { count ->
                _savedForLaterCount.value = count
            }
        }
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
                android.util.Log.d("MainViewModel", "Saved category: $savedCategory")

                if (categoryList.isNotEmpty()) {
                    if (savedCategory != null && categoryList.contains(savedCategory)) {
                        // Saved category is valid, use it
                        android.util.Log.d("MainViewModel", "Using saved category: $savedCategory")
                        _selectedCategory.value = savedCategory
                        loadLocations(savedCategory)
                    } else {
                        // Saved category doesn't exist or is null, show empty state and prompt user
                        android.util.Log.d("MainViewModel", "No valid saved category, setting to empty and prompting user to select")
                        _locationListState.value = LocationListState.Empty
                        _needsCategorySelection.value = true
                    }
                } else {
                    // Empty category list, try default
                    val defaultCategory = "Charging Stations"
                    _selectedCategory.value = defaultCategory
                    _categories.value = listOf(defaultCategory)
                    tokenManager.saveSelectedCategory(defaultCategory)
                    loadLocations(defaultCategory)
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

    fun loadFromLocalDatabase() {
        viewModelScope.launch {
            android.util.Log.d("MainViewModel", "Loading from local database only (OSMand not connected)")
            _locationListState.value = LocationListState.Loading

            try {
                // Use saved category or default
                val categoryName = tokenManager.getSelectedCategory() ?: "Charging Locations"
                _selectedCategory.value = categoryName

                // Load from local database only
                val dbResult = locationRepository.getLocationsByCategory(categoryName)

                if (dbResult.isSuccess) {
                    val locations = dbResult.getOrNull() ?: emptyList()
                    android.util.Log.d("MainViewModel", "Found ${locations.size} locations in local database")

                    if (locations.isNotEmpty()) {
                        allLocations = locations
                        val sorted = sortLocationsByDistance(locations)
                        val filtered = applyFilters(sorted)
                        _locationListState.value = LocationListState.Success(filtered)
                    } else {
                        _locationListState.value = LocationListState.Empty
                    }
                } else {
                    _locationListState.value = LocationListState.Empty
                }
                _isOffline.value = true // Indicate that we're in local-only mode
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error loading from local database: ${e.message}")
                _locationListState.value = LocationListState.Error(e.message ?: "Unknown error")
            }
        }
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
                        allLocations = dbLocations
                        val sorted = sortLocationsByDistance(dbLocations)
                        val filtered = applyFilters(sorted)
                        _locationListState.value = LocationListState.Success(filtered)
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
                        _syncProgress.value = null
                    } else {
                        // Show progress indicator
                        _syncProgress.value = SyncProgress(
                            current = 0,
                            total = locations.size,
                            categoryName = categoryName
                        )

                        // Save all locations to local database in one batch (no API calls)
                        android.util.Log.d("MainViewModel", "Saving ${locations.size} locations to local database...")
                        val saveResult = locationRepository.saveLocationsToLocalDb(locations, categoryName)
                        
                        saveResult.onSuccess { savedCount ->
                            android.util.Log.d("MainViewModel", "✅ Saved $savedCount locations to local database")
                        }.onFailure { error ->
                            android.util.Log.e("MainViewModel", "❌ Failed to save locations: ${error.message}")
                        }

                        // Clear progress
                        _syncProgress.value = null

                        // Sort and display
                        allLocations = locations
                        val sorted = sortLocationsByDistance(locations)
                        val filtered = applyFilters(sorted)
                        android.util.Log.d("MainViewModel", "Setting state to Success with ${filtered.size} locations")
                        _locationListState.value = LocationListState.Success(filtered)
                    }
                    _isOffline.value = false
                }.onFailure { error ->
                    android.util.Log.e("MainViewModel", "Failed to download GPX: ${error.message}")
                    _syncProgress.value = null
                    _locationListState.value = LocationListState.Error(error.message ?: "Failed to load locations")
                    _isOffline.value = true
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error loading locations: ${e.message}", e)
                _syncProgress.value = null
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
        viewModelScope.launch {
            try {
                // Toggle the favorite status
                val updatedLocation = location.copy(isFavourite = !location.isFavourite)

                // Save to database
                val categoryName = _selectedCategory.value ?: return@launch
                val result = locationRepository.updateLocation(updatedLocation, categoryName)

                if (result.isSuccess) {
                    // Update UI state
                    if (_locationListState.value is LocationListState.Success) {
                        val current = (_locationListState.value as LocationListState.Success).locations
                        val updated = current.map {
                            if (it.id == location.id) updatedLocation else it
                        }
                        // Update allLocations cache
                        allLocations = updated
                        // Reapply filters
                        val filtered = applyFilters(sortLocationsByDistance(updated))
                        _locationListState.value = LocationListState.Success(filtered)
                    }
                    android.util.Log.d("MainViewModel", "Toggled favourite for ${location.name}: ${updatedLocation.isFavourite}")
                } else {
                    android.util.Log.e("MainViewModel", "Failed to toggle favourite: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error toggling favourite: ${e.message}", e)
            }
        }
    }

    fun syncLocations() {
        val category = _selectedCategory.value ?: return
        loadLocations(category)
    }

    suspend fun addLocation(location: Location) {
        try {
            val categoryName = _selectedCategory.value ?: "default"
            val result = locationRepository.addLocation(location, categoryName)
            
            if (result.isSuccess) {
                android.util.Log.d("MainViewModel", "Location added successfully: ${location.name}")
                // Refresh the location list
                syncLocations()
            } else {
                android.util.Log.e("MainViewModel", "Failed to add location: ${result.exceptionOrNull()?.message}")
                throw result.exceptionOrNull() ?: Exception("Failed to add location")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error adding location: ${e.message}", e)
            throw e
        }
    }

    suspend fun manualSync() {
        checkPendingOperationsAndSync()
    }

    suspend fun hasStoredCredentials(): Boolean {
        return authRepository.hasStoredCredentials()
    }

    fun isOsmandConnected(): Boolean {
        return tokenManager.isOsmandConnected()
    }

    fun getTokenManager(): com.a42r.pevplugshare.data.remote.TokenManager {
        return tokenManager
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            // Disconnect OSMand
            tokenManager.disconnectOsmand()
        }
    }

    suspend fun deleteAllOsmandLocations(): Int {
        return try {
            // Get the category to fetch locations
            val category = tokenManager.getSelectedCategory() ?: "Charging Locations"
            val locations = locationRepository.getLocationsByCategory(category).getOrNull() ?: emptyList()

            // Filter for OsmAnd locations and delete their photos
            val osmandLocations = locations.filter { it.osmandSyncSource == "OSMAND" }
            osmandLocations.forEach { location ->
                // Delete physical image files
                location.imagePaths.forEach { imagePath ->
                    com.a42r.pevplugshare.util.ImageUtils.deleteImageFile(imagePath)
                }
            }

            // Delete all OsmAnd locations from database
            val deletedCount = locationRepository.deleteAllOsmandLocations().getOrElse { 0 }
            deletedCount
        } catch (e: Exception) {
            0
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

    fun updateFilter(newFilter: FilterState) {
        _filterState.value = newFilter
        applyFiltersAndUpdate()
    }

    fun toggleFavoritesFilter() {
        val currentFilter = _filterState.value
        _filterState.value = if (currentFilter.favoritesOnly) {
            // Turn off favorites filter
            currentFilter.copy(favoritesOnly = false)
        } else {
            // Turn on favorites filter (reset other filters)
            FilterState(favoritesOnly = true)
        }
        applyFiltersAndUpdate()
    }

    fun resetFilters() {
        _filterState.value = FilterState()
        applyFiltersAndUpdate()
    }

    private fun applyFiltersAndUpdate() {
        if (allLocations.isEmpty()) return

        val sorted = sortLocationsByDistance(allLocations)
        val filtered = applyFilters(sorted)
        _locationListState.value = if (filtered.isEmpty()) {
            LocationListState.Empty
        } else {
            LocationListState.Success(filtered)
        }
    }

    private fun applyFilters(locations: List<Location>): List<Location> {
        val filter = _filterState.value

        return locations.filter { location ->
            // Favorites filter - when active, only show favorites
            if (filter.favoritesOnly) {
                return@filter location.isFavourite
            }

            // Cost filter - null hasFee matches any filter
            val passesCostFilter = when (filter.costFilter) {
                CostFilter.ALL -> true
                CostFilter.FREE -> !location.hasFee
                CostFilter.PAID -> location.hasFee
            }
            if (!passesCostFilter) return@filter false

            // Opening hours filter (open now) - null openingHours always passes
            if (filter.openNowOnly) {
                // If no opening hours defined, assume always open (matches the filter)
                if (!location.openingHours.isNullOrEmpty() && !isOpenNow(location)) {
                    return@filter false
                }
            }

            // Socket count filter - maxSockets of 10 means "10 or more"
            // gpoCount of 0 (or null-equivalent) matches any socket count filter
            if (location.gpoCount > 0) {
                if (location.gpoCount < filter.minSockets) {
                    return@filter false
                }
                if (filter.maxSockets < 10 && location.gpoCount > filter.maxSockets) {
                    return@filter false
                }
            }

            // Access type filter - null publicPrivate matches any filter
            val passesAccessFilter = when (filter.accessFilter) {
                AccessFilter.ALL -> true
                AccessFilter.PUBLIC -> location.publicPrivate == LocationAccessType.PUBLIC
                AccessFilter.PRIVATE -> location.publicPrivate == LocationAccessType.PRIVATE
            }
            if (!passesAccessFilter) return@filter false

            // Socket types filter - empty/null socketTypes matches any filter
            // If no socket types are selected, show all locations
            if (filter.socketTypes.isNotEmpty()) {
                // If location has no socket types defined, it matches any filter
                if (location.socketTypes.isNotEmpty()) {
                    val hasMatchingSocketType = location.socketTypes.any { it in filter.socketTypes }
                    if (!hasMatchingSocketType) return@filter false
                }
            }

            true
        }
    }

    private fun isOpenNow(location: Location): Boolean {
        // Simple implementation - check if has opening hours
        // A more sophisticated implementation would parse opening hours and check current time
        // For now, if no opening hours specified, assume always open
        if (location.openingHours.isNullOrBlank()) return true

        // Check for 24/7 indicators
        val hours = location.openingHours.lowercase()
        if (hours.contains("24") || hours.contains("24/7") || hours.contains("24x7")) {
            return true
        }

        // For other cases, assume open (could be enhanced with time parsing)
        return true
    }
}

data class SyncProgress(
    val current: Int,
    val total: Int,
    val categoryName: String
)

