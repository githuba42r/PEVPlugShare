package com.a42r.pevplugshare.examples

import android.util.Log
import com.a42r.pevplugshare.data.model.Location
import com.a42r.pevplugshare.data.model.LocationAccessType
import com.a42r.pevplugshare.data.model.SyncStatus
import com.a42r.pevplugshare.data.repository.AuthRepositoryImpl
import com.a42r.pevplugshare.data.repository.OsmandLocationSyncService
import javax.inject.Inject

/**
 * Example usage of OSMAnd favorite update and add functionality.
 *
 * IMPORTANT: This is a reference example only. Do not include in production builds.
 * Copy the relevant code snippets into your actual ViewModels or repositories.
 */
class OsmandFavoriteUpdateExample @Inject constructor(
    private val osmandSyncService: OsmandLocationSyncService,
    private val authRepository: AuthRepositoryImpl
) {
    companion object {
        private const val TAG = "OsmandUpdateExample"
    }

    /**
     * Example 1: Update an existing favorite's location and description
     */
    suspend fun exampleUpdateExistingFavorite() {
        Log.d(TAG, "=== Example 1: Update Existing Favorite ===")

        // Step 1: Download current favorites to get the exact state
        val categoryName = "Charging Locations"
        val favoritesResult = authRepository.downloadFavoritesGpx(categoryName)

        if (favoritesResult.isFailure) {
            Log.e(TAG, "Failed to download favorites: ${favoritesResult.exceptionOrNull()?.message}")
            return
        }

        val favorites = favoritesResult.getOrNull() ?: emptyList()
        Log.d(TAG, "Downloaded ${favorites.size} favorites")

        // Step 2: Find the favorite you want to update (by name, ID, or other criteria)
        val targetIndex = 0  // For this example, update the first one
        val existingFavorite = favorites.getOrNull(targetIndex)

        if (existingFavorite == null) {
            Log.e(TAG, "No favorite found at index $targetIndex")
            return
        }

        Log.d(TAG, "Found favorite: ${existingFavorite.name} at index $targetIndex")

        // Step 3: Create updated version with new data
        val updatedFavorite = existingFavorite.copy(
            description = "Updated description from app",
            latitude = existingFavorite.latitude + 0.0001,  // Slight position change
            longitude = existingFavorite.longitude + 0.0001,
            lastModified = System.currentTimeMillis()
        )

        // Step 4: Call updateFavorite with the exact current name and index
        val updateResult = osmandSyncService.updateFavorite(
            location = updatedFavorite,
            categoryName = categoryName,
            currentName = existingFavorite.name,  // EXACT current name on server
            index = targetIndex  // Zero-based index in the GPX file
        )

        // Step 5: Handle the result
        when {
            updateResult.isSuccess -> {
                Log.d(TAG, "✅ Successfully updated favorite '${existingFavorite.name}'!")
            }
            updateResult.isFailure -> {
                Log.e(TAG, "❌ Failed to update: ${updateResult.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Example 2: Add a completely new favorite
     */
    suspend fun exampleAddNewFavorite() {
        Log.d(TAG, "=== Example 2: Add New Favorite ===")

        // Step 1: Create a new Location object with all required data
        val newLocation = Location(
            id = "new-location-${System.currentTimeMillis()}",  // Generate unique ID
            name = "New Charging Station",
            description = "Added from mobile app",
            latitude = -27.468953,
            longitude = 153.1039064,
            socketTypes = emptyList(),
            publicPrivate = LocationAccessType.PUBLIC,
            hasFee = false,
            gpoCount = 2,
            openingHours = "24/7",
            additionalInfo = "Added via OSMAnd sync",
            color = "#FF2C2C",  // Red color
            icon = "charging_station",
            lastModified = System.currentTimeMillis(),
            isFavourite = true,
            syncStatus = SyncStatus.PENDING,
            photos = emptyList(),
            businessName = "New Business",
            streetAddress = "123 Main St",
            suburb = "Brisbane",
            state = "QLD",
            postcode = "4000"
        )

        // Step 2: Call addFavorite (it will create the group if it doesn't exist)
        val categoryName = "Charging Locations"
        val addResult = osmandSyncService.addFavorite(
            location = newLocation,
            categoryName = categoryName
        )

        // Step 3: Handle the result
        when {
            addResult.isSuccess -> {
                Log.d(TAG, "✅ Successfully added new favorite '${newLocation.name}'!")
            }
            addResult.isFailure -> {
                Log.e(TAG, "❌ Failed to add: ${addResult.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Example 3: Update a favorite's name (requires special handling)
     */
    suspend fun exampleRenameExistingFavorite() {
        Log.d(TAG, "=== Example 3: Rename Existing Favorite ===")

        val categoryName = "Charging Locations"

        // Step 1: Download current favorites
        val favoritesResult = authRepository.downloadFavoritesGpx(categoryName)
        if (favoritesResult.isFailure) return

        val favorites = favoritesResult.getOrNull() ?: emptyList()
        val targetIndex = 0
        val existingFavorite = favorites.getOrNull(targetIndex) ?: return

        val oldName = existingFavorite.name
        val newName = "Renamed Location"

        Log.d(TAG, "Renaming '$oldName' to '$newName'")

        // Step 2: Create updated version with new name
        val renamedFavorite = existingFavorite.copy(
            name = newName,
            lastModified = System.currentTimeMillis()
        )

        // Step 3: Update with OLD name as currentName parameter
        val updateResult = osmandSyncService.updateFavorite(
            location = renamedFavorite,
            categoryName = categoryName,
            currentName = oldName,  // Use the OLD name here!
            index = targetIndex
        )

        when {
            updateResult.isSuccess -> {
                Log.d(TAG, "✅ Successfully renamed favorite!")
            }
            updateResult.isFailure -> {
                Log.e(TAG, "❌ Failed to rename: ${updateResult.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Example 4: Move a favorite to a different category
     */
    suspend fun exampleMoveFavoriteToNewCategory() {
        Log.d(TAG, "=== Example 4: Move Favorite to Different Category ===")

        val oldCategory = "Charging Locations"
        val newCategory = "My Favorites"

        // Step 1: Download from old category
        val favoritesResult = authRepository.downloadFavoritesGpx(oldCategory)
        if (favoritesResult.isFailure) return

        val favorites = favoritesResult.getOrNull() ?: emptyList()
        val favoriteToMove = favorites.firstOrNull() ?: return

        Log.d(TAG, "Moving '${favoriteToMove.name}' from '$oldCategory' to '$newCategory'")

        // Step 2: Add to new category
        val addResult = osmandSyncService.addFavorite(
            location = favoriteToMove,
            categoryName = newCategory
        )

        if (addResult.isSuccess) {
            Log.d(TAG, "✅ Added to new category")
            // Note: You would need to implement a delete function to remove from old category
            // For now, it will exist in both categories
        } else {
            Log.e(TAG, "❌ Failed to move: ${addResult.exceptionOrNull()?.message}")
        }
    }

    /**
     * Example 5: Batch update multiple favorites
     */
    suspend fun exampleBatchUpdateFavorites() {
        Log.d(TAG, "=== Example 5: Batch Update Favorites ===")

        val categoryName = "Charging Locations"

        // Step 1: Download all favorites
        val favoritesResult = authRepository.downloadFavoritesGpx(categoryName)
        if (favoritesResult.isFailure) return

        val favorites = favoritesResult.getOrNull() ?: emptyList()
        Log.d(TAG, "Updating ${favorites.size} favorites...")

        var successCount = 0
        var failCount = 0

        // Step 2: Update each one
        favorites.forEachIndexed { index, favorite ->
            // Add a common prefix to all descriptions
            val updated = favorite.copy(
                description = "[Verified] ${favorite.description ?: ""}",
                lastModified = System.currentTimeMillis()
            )

            val result = osmandSyncService.updateFavorite(
                location = updated,
                categoryName = categoryName,
                currentName = favorite.name,
                index = index
            )

            if (result.isSuccess) {
                successCount++
                Log.d(TAG, "Updated ${index + 1}/${favorites.size}: ${favorite.name}")
            } else {
                failCount++
                Log.e(TAG, "Failed ${index + 1}/${favorites.size}: ${favorite.name}")
            }

            // Optional: Add small delay between updates to avoid rate limiting
            kotlinx.coroutines.delay(500)
        }

        Log.d(TAG, "Batch update complete: $successCount success, $failCount failed")
    }

    /**
     * Example 6: Error handling and retry logic
     */
    suspend fun exampleErrorHandlingAndRetry() {
        Log.d(TAG, "=== Example 6: Error Handling ===")

        val categoryName = "Charging Locations"
        val newLocation = Location(
            id = "test-${System.currentTimeMillis()}",
            name = "Test Location",
            latitude = -27.5,
            longitude = 153.0,
            socketTypes = emptyList(),
            publicPrivate = LocationAccessType.PUBLIC,
            hasFee = false,
            gpoCount = 1,
            color = "#FF0000",
            icon = "special_star",
            lastModified = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )

        // The updateFavorite and addFavorite functions already include retry logic
        // for error 106 (timestamp mismatch). You can add additional retry logic
        // for network errors if needed:

        var attempts = 0
        val maxAttempts = 3
        var success = false

        while (attempts < maxAttempts && !success) {
            attempts++
            Log.d(TAG, "Attempt $attempts/$maxAttempts")

            val result = osmandSyncService.addFavorite(
                location = newLocation,
                categoryName = categoryName
            )

            when {
                result.isSuccess -> {
                    Log.d(TAG, "✅ Success on attempt $attempts")
                    success = true
                }
                result.isFailure -> {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e(TAG, "❌ Attempt $attempts failed: $error")

                    if (attempts < maxAttempts) {
                        Log.d(TAG, "Retrying in 2 seconds...")
                        kotlinx.coroutines.delay(2000)
                    }
                }
            }
        }

        if (!success) {
            Log.e(TAG, "❌ Failed after $maxAttempts attempts")
        }
    }
}
