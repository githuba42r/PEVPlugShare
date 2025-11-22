package com.a42r.pevplugshare.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.a42r.pevplugshare.data.db.entity.LocationEntity
import com.a42r.pevplugshare.data.db.entity.PendingLocationEntity
import com.a42r.pevplugshare.data.db.entity.PhotoEntity
import com.a42r.pevplugshare.data.db.entity.SyncStateEntity
import com.a42r.pevplugshare.data.db.entity.UserPreferencesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<LocationEntity>)

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Delete
    suspend fun deleteLocation(location: LocationEntity)

    @Query("DELETE FROM locations WHERE id = :id")
    suspend fun deleteLocationById(id: String)

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun getLocationById(id: String): LocationEntity?

    @Query("SELECT * FROM locations ORDER BY lastModified DESC")
    fun getAllLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE isFavourite = 1 ORDER BY lastModified DESC")
    fun getFavouriteLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE name LIKE '%' || :query || '%' ORDER BY lastModified DESC")
    fun searchLocations(query: String): Flow<List<LocationEntity>>

    @Query("DELETE FROM locations")
    suspend fun deleteAllLocations()

    @Query("""
        SELECT * FROM locations 
        WHERE (6371 * acos(cos(radians(:latitude)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:longitude)) + sin(radians(:latitude)) * sin(radians(latitude)))) < :radiusKm
    """)
    suspend fun getLocationsNearby(latitude: Double, longitude: Double, radiusKm: Double): List<LocationEntity>
}

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Delete
    suspend fun deletePhoto(photo: PhotoEntity)

    @Query("SELECT * FROM location_photos WHERE locationId = :locationId")
    suspend fun getPhotosByLocation(locationId: String): List<PhotoEntity>

    @Query("DELETE FROM location_photos WHERE locationId = :locationId")
    suspend fun deletePhotosByLocation(locationId: String)

    @Query("DELETE FROM location_photos WHERE id = :id")
    suspend fun deletePhotoById(id: String)

    @Query("SELECT * FROM location_photos WHERE filePath = :filePath")
    suspend fun getPhotoByFilePath(filePath: String): PhotoEntity?

    @Query("DELETE FROM location_photos WHERE filePath = :filePath")
    suspend fun deletePhotoByFilePath(filePath: String)

    @Query("UPDATE location_photos SET locationId = :newLocationId WHERE locationId = :oldLocationId")
    suspend fun updatePhotoLocationId(oldLocationId: String, newLocationId: String)

    @Query("UPDATE location_photos SET filePath = :newFilePath WHERE filePath = :oldFilePath")
    suspend fun updatePhotoFilePath(oldFilePath: String, newFilePath: String)
}

@Dao
interface SyncStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncState(syncState: SyncStateEntity)

    @Query("SELECT * FROM sync_state WHERE id = 'sync_state'")
    suspend fun getSyncState(): SyncStateEntity?

    @Query("SELECT * FROM sync_state WHERE id = 'sync_state'")
    fun getSyncStateFlow(): Flow<SyncStateEntity?>

    @Update
    suspend fun updateSyncState(syncState: SyncStateEntity)
}

@Dao
interface UserPreferencesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(prefs: UserPreferencesEntity)

    @Query("SELECT * FROM user_preferences WHERE id = 'user_prefs'")
    suspend fun getPreferences(): UserPreferencesEntity?

    @Query("SELECT * FROM user_preferences WHERE id = 'user_prefs'")
    fun getPreferencesFlow(): Flow<UserPreferencesEntity?>

    @Update
    suspend fun updatePreferences(prefs: UserPreferencesEntity)
}

@Dao
interface PendingLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingLocation(location: PendingLocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingLocations(locations: List<PendingLocationEntity>)

    @Query("SELECT * FROM pending_locations WHERE syncAttempts < 3 ORDER BY createdAt ASC")
    suspend fun getPendingLocations(): List<PendingLocationEntity>

    @Query("SELECT COUNT(*) FROM pending_locations")
    suspend fun getPendingLocationCount(): Int

    @Query("DELETE FROM pending_locations WHERE id = :id")
    suspend fun deletePendingLocation(id: String)

    @Query("UPDATE pending_locations SET syncAttempts = syncAttempts + 1, lastSyncAttempt = :timestamp WHERE id = :id")
    suspend fun incrementSyncAttempts(id: String, timestamp: Long)
}

@Dao
interface SavedForLaterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedForLater(savedForLater: com.a42r.pevplugshare.data.db.entity.SavedForLaterEntity)

    @Query("SELECT * FROM saved_for_later ORDER BY createdAt DESC")
    fun getAllSavedForLater(): Flow<List<com.a42r.pevplugshare.data.db.entity.SavedForLaterEntity>>

    @Query("SELECT * FROM saved_for_later ORDER BY createdAt DESC")
    suspend fun getAllSavedForLaterList(): List<com.a42r.pevplugshare.data.db.entity.SavedForLaterEntity>

    @Query("SELECT COUNT(*) FROM saved_for_later")
    suspend fun getSavedForLaterCount(): Int

    @Query("SELECT COUNT(*) FROM saved_for_later")
    fun getSavedForLaterCountFlow(): Flow<Int>

    @Query("SELECT * FROM saved_for_later WHERE id = :id")
    suspend fun getSavedForLaterById(id: String): com.a42r.pevplugshare.data.db.entity.SavedForLaterEntity?

    @Query("DELETE FROM saved_for_later WHERE id = :id")
    suspend fun deleteSavedForLater(id: String)

    @Query("DELETE FROM saved_for_later")
    suspend fun deleteAllSavedForLater()
}
