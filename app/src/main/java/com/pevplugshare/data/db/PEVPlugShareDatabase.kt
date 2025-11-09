package com.pevplugshare.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pevplugshare.data.db.dao.LocationDao
import com.pevplugshare.data.db.dao.PhotoDao
import com.pevplugshare.data.db.dao.PendingLocationDao
import com.pevplugshare.data.db.dao.PendingOperationDao
import com.pevplugshare.data.db.dao.SyncStateDao
import com.pevplugshare.data.db.dao.UserPreferencesDao
import com.pevplugshare.data.db.entity.LocationEntity
import com.pevplugshare.data.db.entity.PendingLocationEntity
import com.pevplugshare.data.db.entity.PendingOperationEntity
import com.pevplugshare.data.db.entity.PhotoEntity
import com.pevplugshare.data.db.entity.SyncStateEntity
import com.pevplugshare.data.db.entity.UserPreferencesEntity

@Database(
    entities = [
        LocationEntity::class,
        PhotoEntity::class,
        SyncStateEntity::class,
        UserPreferencesEntity::class,
        PendingLocationEntity::class,
        PendingOperationEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class PEVPlugShareDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun photoDao(): PhotoDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun pendingLocationDao(): PendingLocationDao
    abstract fun pendingOperationDao(): PendingOperationDao

    companion object {
        @Volatile
        private var Instance: PEVPlugShareDatabase? = null

        fun getDatabase(context: Context): PEVPlugShareDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    PEVPlugShareDatabase::class.java,
                    "pevplugshare.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
