package com.a42r.pevplugshare.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.a42r.pevplugshare.data.db.converters.Converters
import com.a42r.pevplugshare.data.db.dao.LocationDao
import com.a42r.pevplugshare.data.db.dao.PhotoDao
import com.a42r.pevplugshare.data.db.dao.PendingLocationDao
import com.a42r.pevplugshare.data.db.dao.PendingOperationDao
import com.a42r.pevplugshare.data.db.dao.SyncStateDao
import com.a42r.pevplugshare.data.db.dao.UserPreferencesDao
import com.a42r.pevplugshare.data.db.entity.LocationEntity
import com.a42r.pevplugshare.data.db.entity.PendingLocationEntity
import com.a42r.pevplugshare.data.db.entity.PendingOperationEntity
import com.a42r.pevplugshare.data.db.entity.PhotoEntity
import com.a42r.pevplugshare.data.db.entity.SyncStateEntity
import com.a42r.pevplugshare.data.db.entity.UserPreferencesEntity

@Database(
    entities = [
        LocationEntity::class,
        PhotoEntity::class,
        SyncStateEntity::class,
        UserPreferencesEntity::class,
        PendingLocationEntity::class,
        PendingOperationEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
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
