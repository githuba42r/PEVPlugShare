package com.a42r.pevplugshare.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.a42r.pevplugshare.data.db.converters.Converters
import com.a42r.pevplugshare.data.db.dao.LocationDao
import com.a42r.pevplugshare.data.db.dao.PhotoDao
import com.a42r.pevplugshare.data.db.dao.PendingLocationDao
import com.a42r.pevplugshare.data.db.dao.PendingOperationDao
import com.a42r.pevplugshare.data.db.dao.SavedForLaterDao
import com.a42r.pevplugshare.data.db.dao.SyncStateDao
import com.a42r.pevplugshare.data.db.dao.UserPreferencesDao
import com.a42r.pevplugshare.data.db.entity.LocationEntity
import com.a42r.pevplugshare.data.db.entity.PendingLocationEntity
import com.a42r.pevplugshare.data.db.entity.PendingOperationEntity
import com.a42r.pevplugshare.data.db.entity.PhotoEntity
import com.a42r.pevplugshare.data.db.entity.SavedForLaterEntity
import com.a42r.pevplugshare.data.db.entity.SyncStateEntity
import com.a42r.pevplugshare.data.db.entity.UserPreferencesEntity

@Database(
    entities = [
        LocationEntity::class,
        PhotoEntity::class,
        SyncStateEntity::class,
        UserPreferencesEntity::class,
        PendingLocationEntity::class,
        PendingOperationEntity::class,
        SavedForLaterEntity::class
    ],
    version = 8,
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
    abstract fun savedForLaterDao(): SavedForLaterDao

    companion object {
        @Volatile
        private var Instance: PEVPlugShareDatabase? = null

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add OSMAnd sync tracking fields
                database.execSQL("""
                    ALTER TABLE locations 
                    ADD COLUMN osmandOriginalName TEXT DEFAULT NULL
                """)
                database.execSQL("""
                    ALTER TABLE locations 
                    ADD COLUMN osmandGpxIndex INTEGER DEFAULT NULL
                """)
                database.execSQL("""
                    ALTER TABLE locations 
                    ADD COLUMN osmandSyncSource TEXT NOT NULL DEFAULT 'LOCAL'
                """)
                database.execSQL("""
                    ALTER TABLE locations 
                    ADD COLUMN osmandLastSyncedAt INTEGER DEFAULT NULL
                """)
                database.execSQL("""
                    ALTER TABLE locations 
                    ADD COLUMN osmandSyncedToServer INTEGER NOT NULL DEFAULT 0
                """)
                
                // Set existing synced locations as potentially from OSMAND
                // We assume locations with syncStatus = 'SYNCED' may have come from OSMAnd
                database.execSQL("""
                    UPDATE locations 
                    SET osmandSyncSource = 'OSMAND',
                        osmandSyncedToServer = 1,
                        osmandOriginalName = name
                    WHERE syncStatus = 'SYNCED'
                """)
            }
        }

        fun getDatabase(context: Context): PEVPlugShareDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    PEVPlugShareDatabase::class.java,
                    "pevplugshare.db"
                )
                    .addMigrations(MIGRATION_7_8)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
