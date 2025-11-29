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
    version = 11,
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
                // Add SavedForLater table - version 7 to 8
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS saved_for_later (
                        id TEXT PRIMARY KEY NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        bearing REAL,
                        photoPaths TEXT NOT NULL,
                        photoMetadata TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
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

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new OSMand sync tracking fields for optional OSMand integration
                database.execSQL("""
                    ALTER TABLE locations
                    ADD COLUMN osmandSyncedUsernames TEXT DEFAULT NULL
                """)
                database.execSQL("""
                    ALTER TABLE locations
                    ADD COLUMN osmandSyncStatus TEXT NOT NULL DEFAULT 'LOCAL_ONLY'
                """)

                // Update existing locations that have been synced to OSMAnd
                // If osmandSyncedToServer is true, they're SYNCED; otherwise LOCAL_ONLY
                database.execSQL("""
                    UPDATE locations
                    SET osmandSyncStatus = CASE
                        WHEN osmandSyncedToServer = 1 THEN 'SYNCED'
                        ELSE 'LOCAL_ONLY'
                    END
                """)
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Set all locations to SYNCED status with current timestamp
                val currentTime = System.currentTimeMillis()
                database.execSQL("""
                    UPDATE locations
                    SET syncStatus = 'SYNCED',
                        osmandSyncStatus = 'SYNCED',
                        osmandSyncedToServer = 1,
                        osmandLastSyncedAt = $currentTime
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
                    .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
