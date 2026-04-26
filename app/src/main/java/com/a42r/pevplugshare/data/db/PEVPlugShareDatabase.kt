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
    version = 12,
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

        // Migrations for very old databases to bring them to version 7
        private val MIGRATION_1_6 = object : Migration(1, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {}
        }

        private val MIGRATION_2_6 = object : Migration(2, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {}
        }

        private val MIGRATION_3_6 = object : Migration(3, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {}
        }

        private val MIGRATION_4_6 = object : Migration(4, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {}
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {}
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Ensure SavedForLater table exists
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

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    android.util.Log.i("DB_MIGRATION", "Starting MIGRATION_11_12")
                    val cursor = database.query("SELECT COUNT(*) as count FROM locations")
                    if (cursor.moveToFirst()) {
                        val count = cursor.getInt(0)
                        android.util.Log.i("DB_MIGRATION", "Location count before migration: $count")
                    }
                    cursor.close()

                    database.execSQL("ALTER TABLE locations ADD COLUMN serverId TEXT DEFAULT NULL")
                    database.execSQL("ALTER TABLE locations ADD COLUMN serverVersion INTEGER DEFAULT NULL")
                    database.execSQL("ALTER TABLE locations ADD COLUMN serverSyncStatus TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
                    database.execSQL("ALTER TABLE locations ADD COLUMN serverLastSyncedAt INTEGER DEFAULT NULL")

                    val cursor2 = database.query("SELECT COUNT(*) as count FROM locations")
                    if (cursor2.moveToFirst()) {
                        val count = cursor2.getInt(0)
                        android.util.Log.i("DB_MIGRATION", "Location count after migration: $count")
                    }
                    cursor2.close()

                    android.util.Log.i("DB_MIGRATION", "MIGRATION_11_12 completed successfully")
                } catch (e: Exception) {
                    android.util.Log.e("DB_MIGRATION", "MIGRATION_11_12 failed: ${e.message}", e)
                    throw e
                }
            }
        }

        fun getDatabase(context: Context): PEVPlugShareDatabase {
            return Instance ?: synchronized(this) {
                try {
                    android.util.Log.i("DB_INIT", "Opening database...")

                    // Get database path and run diagnostics BEFORE Room opens it
                    val dbPath = context.getDatabasePath("pevplugshare.db").absolutePath
                    android.util.Log.i("DB_INIT", "Database path: $dbPath")

                    // Check raw database state before Room opens it
                    val diagnosticsBefore = DatabaseDiagnostics.getDatabaseDiagnostics(dbPath)
                    if (diagnosticsBefore != null) {
                        android.util.Log.i("DB_INIT", "Database state BEFORE Room opens:")
                        android.util.Log.i("DB_INIT", "  Version: ${diagnosticsBefore.version}")
                        android.util.Log.i("DB_INIT", "  Tables: ${diagnosticsBefore.tables}")
                        android.util.Log.i("DB_INIT", "  Location count: ${diagnosticsBefore.locationCount}")
                        android.util.Log.i("DB_INIT", "  SavedForLater count: ${diagnosticsBefore.savedForLaterCount}")

                        if (diagnosticsBefore.version < 12) {
                            android.util.Log.w("DB_INIT", "Database version is ${diagnosticsBefore.version}, app expects 12 - migration needed!")
                        }
                    } else {
                        android.util.Log.i("DB_INIT", "No existing database file found")
                    }

                    android.util.Log.i("DB_INIT", "Building Room database with 11 migrations...")
                    val db = Room.databaseBuilder(
                        context.applicationContext,
                        PEVPlugShareDatabase::class.java,
                        "pevplugshare.db"
                    )
                        .addMigrations(
                            MIGRATION_1_6, MIGRATION_2_6, MIGRATION_3_6, MIGRATION_4_6, MIGRATION_5_6,
                            MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12
                        )
                        .build()

                    android.util.Log.i("DB_INIT", "✓ Room database instance created and opened")

                    // Check database state AFTER Room opens it
                    val diagnosticsAfter = DatabaseDiagnostics.getDatabaseDiagnostics(dbPath)
                    if (diagnosticsAfter != null) {
                        android.util.Log.i("DB_INIT", "Database state AFTER Room opens:")
                        android.util.Log.i("DB_INIT", "  Version: ${diagnosticsAfter.version}")
                        android.util.Log.i("DB_INIT", "  Tables: ${diagnosticsAfter.tables}")
                        android.util.Log.i("DB_INIT", "  Location count: ${diagnosticsAfter.locationCount}")
                        android.util.Log.i("DB_INIT", "  SavedForLater count: ${diagnosticsAfter.savedForLaterCount}")
                    }

                    // Compare if data was lost during Room initialization
                    if (diagnosticsBefore != null && diagnosticsAfter != null) {
                        if (diagnosticsBefore.locationCount > 0 && diagnosticsAfter.locationCount == 0) {
                            android.util.Log.e("DB_INIT", "ERROR: Lost ${diagnosticsBefore.locationCount} locations during Room initialization!")
                            android.util.Log.e("DB_INIT", "  Before: ${diagnosticsBefore.locationCount} locations in v${diagnosticsBefore.version}")
                            android.util.Log.e("DB_INIT", "  After: ${diagnosticsAfter.locationCount} locations in v${diagnosticsAfter.version}")
                            android.util.Log.e("DB_INIT", "  This indicates fallbackToDestructiveMigration() was called or migration failed")
                            android.util.Log.e("DB_INIT", "  Checking if DB_MIGRATION logs appeared - if not, migration didn't run")
                        } else if (diagnosticsBefore.version != diagnosticsAfter.version) {
                            android.util.Log.i("DB_INIT", "Version upgraded: ${diagnosticsBefore.version} -> ${diagnosticsAfter.version}")
                        }
                    }

                    Instance = db
                    db
                } catch (e: Exception) {
                    android.util.Log.e("DB_INIT", "Failed to open database: ${e.message}", e)
                    throw e
                }
            }
        }

        fun clearDatabase() {
            synchronized(this) {
                Instance?.close()
                Instance = null
            }
        }
    }
}
