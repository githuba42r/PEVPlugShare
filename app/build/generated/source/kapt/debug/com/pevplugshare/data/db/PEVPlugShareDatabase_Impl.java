package com.pevplugshare.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.pevplugshare.data.db.dao.LocationDao;
import com.pevplugshare.data.db.dao.LocationDao_Impl;
import com.pevplugshare.data.db.dao.PendingLocationDao;
import com.pevplugshare.data.db.dao.PendingLocationDao_Impl;
import com.pevplugshare.data.db.dao.PhotoDao;
import com.pevplugshare.data.db.dao.PhotoDao_Impl;
import com.pevplugshare.data.db.dao.SyncStateDao;
import com.pevplugshare.data.db.dao.SyncStateDao_Impl;
import com.pevplugshare.data.db.dao.UserPreferencesDao;
import com.pevplugshare.data.db.dao.UserPreferencesDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PEVPlugShareDatabase_Impl extends PEVPlugShareDatabase {
  private volatile LocationDao _locationDao;

  private volatile PhotoDao _photoDao;

  private volatile SyncStateDao _syncStateDao;

  private volatile UserPreferencesDao _userPreferencesDao;

  private volatile PendingLocationDao _pendingLocationDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `locations` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `socketTypes` TEXT NOT NULL, `publicPrivate` TEXT NOT NULL, `hasFee` INTEGER NOT NULL, `gpoCount` INTEGER NOT NULL, `openingHours` TEXT, `additionalInfo` TEXT, `providerName` TEXT, `providerWebsite` TEXT, `chargingCosts` TEXT, `color` TEXT NOT NULL, `icon` TEXT NOT NULL, `lastModified` INTEGER NOT NULL, `isFavourite` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `location_photos` (`id` TEXT NOT NULL, `locationId` TEXT NOT NULL, `filePath` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `exifMetadata` TEXT, `gpxData` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `sync_state` (`id` TEXT NOT NULL, `lastSyncTime` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, `pendingCount` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_preferences` (`id` TEXT NOT NULL, `selectedCategory` TEXT, `syncFrequencyHours` INTEGER NOT NULL, `osmandAccount` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `pending_locations` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `socketTypes` TEXT NOT NULL, `publicPrivate` TEXT NOT NULL, `hasFee` INTEGER NOT NULL, `gpoCount` INTEGER NOT NULL, `openingHours` TEXT, `additionalInfo` TEXT, `providerName` TEXT, `providerWebsite` TEXT, `chargingCosts` TEXT, `createdAt` INTEGER NOT NULL, `syncAttempts` INTEGER NOT NULL, `lastSyncAttempt` INTEGER, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '61b30d6400c1d0ed239f8b4df47c9342')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `locations`");
        db.execSQL("DROP TABLE IF EXISTS `location_photos`");
        db.execSQL("DROP TABLE IF EXISTS `sync_state`");
        db.execSQL("DROP TABLE IF EXISTS `user_preferences`");
        db.execSQL("DROP TABLE IF EXISTS `pending_locations`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsLocations = new HashMap<String, TableInfo.Column>(19);
        _columnsLocations.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("latitude", new TableInfo.Column("latitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("longitude", new TableInfo.Column("longitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("socketTypes", new TableInfo.Column("socketTypes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("publicPrivate", new TableInfo.Column("publicPrivate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("hasFee", new TableInfo.Column("hasFee", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("gpoCount", new TableInfo.Column("gpoCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("openingHours", new TableInfo.Column("openingHours", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("additionalInfo", new TableInfo.Column("additionalInfo", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("providerName", new TableInfo.Column("providerName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("providerWebsite", new TableInfo.Column("providerWebsite", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("chargingCosts", new TableInfo.Column("chargingCosts", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("color", new TableInfo.Column("color", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("icon", new TableInfo.Column("icon", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("lastModified", new TableInfo.Column("lastModified", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("isFavourite", new TableInfo.Column("isFavourite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("syncStatus", new TableInfo.Column("syncStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLocations = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLocations = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLocations = new TableInfo("locations", _columnsLocations, _foreignKeysLocations, _indicesLocations);
        final TableInfo _existingLocations = TableInfo.read(db, "locations");
        if (!_infoLocations.equals(_existingLocations)) {
          return new RoomOpenHelper.ValidationResult(false, "locations(com.pevplugshare.data.db.entity.LocationEntity).\n"
                  + " Expected:\n" + _infoLocations + "\n"
                  + " Found:\n" + _existingLocations);
        }
        final HashMap<String, TableInfo.Column> _columnsLocationPhotos = new HashMap<String, TableInfo.Column>(6);
        _columnsLocationPhotos.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationPhotos.put("locationId", new TableInfo.Column("locationId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationPhotos.put("filePath", new TableInfo.Column("filePath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationPhotos.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationPhotos.put("exifMetadata", new TableInfo.Column("exifMetadata", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationPhotos.put("gpxData", new TableInfo.Column("gpxData", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLocationPhotos = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLocationPhotos = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLocationPhotos = new TableInfo("location_photos", _columnsLocationPhotos, _foreignKeysLocationPhotos, _indicesLocationPhotos);
        final TableInfo _existingLocationPhotos = TableInfo.read(db, "location_photos");
        if (!_infoLocationPhotos.equals(_existingLocationPhotos)) {
          return new RoomOpenHelper.ValidationResult(false, "location_photos(com.pevplugshare.data.db.entity.PhotoEntity).\n"
                  + " Expected:\n" + _infoLocationPhotos + "\n"
                  + " Found:\n" + _existingLocationPhotos);
        }
        final HashMap<String, TableInfo.Column> _columnsSyncState = new HashMap<String, TableInfo.Column>(4);
        _columnsSyncState.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncState.put("lastSyncTime", new TableInfo.Column("lastSyncTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncState.put("syncStatus", new TableInfo.Column("syncStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncState.put("pendingCount", new TableInfo.Column("pendingCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSyncState = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSyncState = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSyncState = new TableInfo("sync_state", _columnsSyncState, _foreignKeysSyncState, _indicesSyncState);
        final TableInfo _existingSyncState = TableInfo.read(db, "sync_state");
        if (!_infoSyncState.equals(_existingSyncState)) {
          return new RoomOpenHelper.ValidationResult(false, "sync_state(com.pevplugshare.data.db.entity.SyncStateEntity).\n"
                  + " Expected:\n" + _infoSyncState + "\n"
                  + " Found:\n" + _existingSyncState);
        }
        final HashMap<String, TableInfo.Column> _columnsUserPreferences = new HashMap<String, TableInfo.Column>(4);
        _columnsUserPreferences.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserPreferences.put("selectedCategory", new TableInfo.Column("selectedCategory", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserPreferences.put("syncFrequencyHours", new TableInfo.Column("syncFrequencyHours", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserPreferences.put("osmandAccount", new TableInfo.Column("osmandAccount", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUserPreferences = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUserPreferences = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUserPreferences = new TableInfo("user_preferences", _columnsUserPreferences, _foreignKeysUserPreferences, _indicesUserPreferences);
        final TableInfo _existingUserPreferences = TableInfo.read(db, "user_preferences");
        if (!_infoUserPreferences.equals(_existingUserPreferences)) {
          return new RoomOpenHelper.ValidationResult(false, "user_preferences(com.pevplugshare.data.db.entity.UserPreferencesEntity).\n"
                  + " Expected:\n" + _infoUserPreferences + "\n"
                  + " Found:\n" + _existingUserPreferences);
        }
        final HashMap<String, TableInfo.Column> _columnsPendingLocations = new HashMap<String, TableInfo.Column>(17);
        _columnsPendingLocations.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingLocations.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingLocations.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingLocations.put("latitude", new TableInfo.Column("latitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingLocations.put("longitude", new TableInfo.Column("longitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingLocations.put("socketTypes", new TableInfo.Column("socketTypes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingLocations.put("publicPrivate", new TableInfo.Column("publicPrivate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingLocations.put("hasFee", new TableInfo.Column("hasFee", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingLocations.put("gpoCount", new TableInfo.Column("gpoCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingLocations.put("openingHours", new TableInfo.Column("openingHours", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingLocations.put("additionalInfo", new TableInfo.Column("additionalInfo", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingLocations.put("providerName", new TableInfo.Column("providerName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingLocations.put("providerWebsite", new TableInfo.Column("providerWebsite", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingLocations.put("chargingCosts", new TableInfo.Column("chargingCosts", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingLocations.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingLocations.put("syncAttempts", new TableInfo.Column("syncAttempts", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingLocations.put("lastSyncAttempt", new TableInfo.Column("lastSyncAttempt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPendingLocations = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPendingLocations = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPendingLocations = new TableInfo("pending_locations", _columnsPendingLocations, _foreignKeysPendingLocations, _indicesPendingLocations);
        final TableInfo _existingPendingLocations = TableInfo.read(db, "pending_locations");
        if (!_infoPendingLocations.equals(_existingPendingLocations)) {
          return new RoomOpenHelper.ValidationResult(false, "pending_locations(com.pevplugshare.data.db.entity.PendingLocationEntity).\n"
                  + " Expected:\n" + _infoPendingLocations + "\n"
                  + " Found:\n" + _existingPendingLocations);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "61b30d6400c1d0ed239f8b4df47c9342", "0aac868fb80f7082f089ca4b15869754");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "locations","location_photos","sync_state","user_preferences","pending_locations");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `locations`");
      _db.execSQL("DELETE FROM `location_photos`");
      _db.execSQL("DELETE FROM `sync_state`");
      _db.execSQL("DELETE FROM `user_preferences`");
      _db.execSQL("DELETE FROM `pending_locations`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(LocationDao.class, LocationDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PhotoDao.class, PhotoDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SyncStateDao.class, SyncStateDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(UserPreferencesDao.class, UserPreferencesDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PendingLocationDao.class, PendingLocationDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public LocationDao locationDao() {
    if (_locationDao != null) {
      return _locationDao;
    } else {
      synchronized(this) {
        if(_locationDao == null) {
          _locationDao = new LocationDao_Impl(this);
        }
        return _locationDao;
      }
    }
  }

  @Override
  public PhotoDao photoDao() {
    if (_photoDao != null) {
      return _photoDao;
    } else {
      synchronized(this) {
        if(_photoDao == null) {
          _photoDao = new PhotoDao_Impl(this);
        }
        return _photoDao;
      }
    }
  }

  @Override
  public SyncStateDao syncStateDao() {
    if (_syncStateDao != null) {
      return _syncStateDao;
    } else {
      synchronized(this) {
        if(_syncStateDao == null) {
          _syncStateDao = new SyncStateDao_Impl(this);
        }
        return _syncStateDao;
      }
    }
  }

  @Override
  public UserPreferencesDao userPreferencesDao() {
    if (_userPreferencesDao != null) {
      return _userPreferencesDao;
    } else {
      synchronized(this) {
        if(_userPreferencesDao == null) {
          _userPreferencesDao = new UserPreferencesDao_Impl(this);
        }
        return _userPreferencesDao;
      }
    }
  }

  @Override
  public PendingLocationDao pendingLocationDao() {
    if (_pendingLocationDao != null) {
      return _pendingLocationDao;
    } else {
      synchronized(this) {
        if(_pendingLocationDao == null) {
          _pendingLocationDao = new PendingLocationDao_Impl(this);
        }
        return _pendingLocationDao;
      }
    }
  }
}
