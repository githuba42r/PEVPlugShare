package com.pevplugshare.data.db.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.pevplugshare.data.db.entity.LocationEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LocationDao_Impl implements LocationDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LocationEntity> __insertionAdapterOfLocationEntity;

  private final EntityDeletionOrUpdateAdapter<LocationEntity> __deletionAdapterOfLocationEntity;

  private final EntityDeletionOrUpdateAdapter<LocationEntity> __updateAdapterOfLocationEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteLocationById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllLocations;

  public LocationDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLocationEntity = new EntityInsertionAdapter<LocationEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `locations` (`id`,`name`,`description`,`latitude`,`longitude`,`socketTypes`,`publicPrivate`,`hasFee`,`gpoCount`,`openingHours`,`additionalInfo`,`providerName`,`providerWebsite`,`chargingCosts`,`color`,`icon`,`lastModified`,`isFavourite`,`syncStatus`,`descriptionHash`,`aiParsed`,`aiParsedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocationEntity entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getName());
        }
        if (entity.getDescription() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDescription());
        }
        statement.bindDouble(4, entity.getLatitude());
        statement.bindDouble(5, entity.getLongitude());
        if (entity.getSocketTypes() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getSocketTypes());
        }
        if (entity.getPublicPrivate() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getPublicPrivate());
        }
        final int _tmp = entity.getHasFee() ? 1 : 0;
        statement.bindLong(8, _tmp);
        statement.bindLong(9, entity.getGpoCount());
        if (entity.getOpeningHours() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getOpeningHours());
        }
        if (entity.getAdditionalInfo() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getAdditionalInfo());
        }
        if (entity.getProviderName() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getProviderName());
        }
        if (entity.getProviderWebsite() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getProviderWebsite());
        }
        if (entity.getChargingCosts() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getChargingCosts());
        }
        if (entity.getColor() == null) {
          statement.bindNull(15);
        } else {
          statement.bindString(15, entity.getColor());
        }
        if (entity.getIcon() == null) {
          statement.bindNull(16);
        } else {
          statement.bindString(16, entity.getIcon());
        }
        statement.bindLong(17, entity.getLastModified());
        final int _tmp_1 = entity.isFavourite() ? 1 : 0;
        statement.bindLong(18, _tmp_1);
        if (entity.getSyncStatus() == null) {
          statement.bindNull(19);
        } else {
          statement.bindString(19, entity.getSyncStatus());
        }
        if (entity.getDescriptionHash() == null) {
          statement.bindNull(20);
        } else {
          statement.bindString(20, entity.getDescriptionHash());
        }
        final int _tmp_2 = entity.getAiParsed() ? 1 : 0;
        statement.bindLong(21, _tmp_2);
        if (entity.getAiParsedAt() == null) {
          statement.bindNull(22);
        } else {
          statement.bindLong(22, entity.getAiParsedAt());
        }
      }
    };
    this.__deletionAdapterOfLocationEntity = new EntityDeletionOrUpdateAdapter<LocationEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `locations` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocationEntity entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
      }
    };
    this.__updateAdapterOfLocationEntity = new EntityDeletionOrUpdateAdapter<LocationEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `locations` SET `id` = ?,`name` = ?,`description` = ?,`latitude` = ?,`longitude` = ?,`socketTypes` = ?,`publicPrivate` = ?,`hasFee` = ?,`gpoCount` = ?,`openingHours` = ?,`additionalInfo` = ?,`providerName` = ?,`providerWebsite` = ?,`chargingCosts` = ?,`color` = ?,`icon` = ?,`lastModified` = ?,`isFavourite` = ?,`syncStatus` = ?,`descriptionHash` = ?,`aiParsed` = ?,`aiParsedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocationEntity entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getName());
        }
        if (entity.getDescription() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDescription());
        }
        statement.bindDouble(4, entity.getLatitude());
        statement.bindDouble(5, entity.getLongitude());
        if (entity.getSocketTypes() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getSocketTypes());
        }
        if (entity.getPublicPrivate() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getPublicPrivate());
        }
        final int _tmp = entity.getHasFee() ? 1 : 0;
        statement.bindLong(8, _tmp);
        statement.bindLong(9, entity.getGpoCount());
        if (entity.getOpeningHours() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getOpeningHours());
        }
        if (entity.getAdditionalInfo() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getAdditionalInfo());
        }
        if (entity.getProviderName() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getProviderName());
        }
        if (entity.getProviderWebsite() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getProviderWebsite());
        }
        if (entity.getChargingCosts() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getChargingCosts());
        }
        if (entity.getColor() == null) {
          statement.bindNull(15);
        } else {
          statement.bindString(15, entity.getColor());
        }
        if (entity.getIcon() == null) {
          statement.bindNull(16);
        } else {
          statement.bindString(16, entity.getIcon());
        }
        statement.bindLong(17, entity.getLastModified());
        final int _tmp_1 = entity.isFavourite() ? 1 : 0;
        statement.bindLong(18, _tmp_1);
        if (entity.getSyncStatus() == null) {
          statement.bindNull(19);
        } else {
          statement.bindString(19, entity.getSyncStatus());
        }
        if (entity.getDescriptionHash() == null) {
          statement.bindNull(20);
        } else {
          statement.bindString(20, entity.getDescriptionHash());
        }
        final int _tmp_2 = entity.getAiParsed() ? 1 : 0;
        statement.bindLong(21, _tmp_2);
        if (entity.getAiParsedAt() == null) {
          statement.bindNull(22);
        } else {
          statement.bindLong(22, entity.getAiParsedAt());
        }
        if (entity.getId() == null) {
          statement.bindNull(23);
        } else {
          statement.bindString(23, entity.getId());
        }
      }
    };
    this.__preparedStmtOfDeleteLocationById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM locations WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllLocations = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM locations";
        return _query;
      }
    };
  }

  @Override
  public Object insertLocation(final LocationEntity location,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLocationEntity.insert(location);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertLocations(final List<LocationEntity> locations,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLocationEntity.insert(locations);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteLocation(final LocationEntity location,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfLocationEntity.handle(location);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateLocation(final LocationEntity location,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfLocationEntity.handle(location);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteLocationById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteLocationById.acquire();
        int _argIndex = 1;
        if (id == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, id);
        }
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteLocationById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllLocations(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllLocations.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllLocations.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getLocationById(final String id,
      final Continuation<? super LocationEntity> $completion) {
    final String _sql = "SELECT * FROM locations WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (id == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, id);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LocationEntity>() {
      @Override
      @Nullable
      public LocationEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfSocketTypes = CursorUtil.getColumnIndexOrThrow(_cursor, "socketTypes");
          final int _cursorIndexOfPublicPrivate = CursorUtil.getColumnIndexOrThrow(_cursor, "publicPrivate");
          final int _cursorIndexOfHasFee = CursorUtil.getColumnIndexOrThrow(_cursor, "hasFee");
          final int _cursorIndexOfGpoCount = CursorUtil.getColumnIndexOrThrow(_cursor, "gpoCount");
          final int _cursorIndexOfOpeningHours = CursorUtil.getColumnIndexOrThrow(_cursor, "openingHours");
          final int _cursorIndexOfAdditionalInfo = CursorUtil.getColumnIndexOrThrow(_cursor, "additionalInfo");
          final int _cursorIndexOfProviderName = CursorUtil.getColumnIndexOrThrow(_cursor, "providerName");
          final int _cursorIndexOfProviderWebsite = CursorUtil.getColumnIndexOrThrow(_cursor, "providerWebsite");
          final int _cursorIndexOfChargingCosts = CursorUtil.getColumnIndexOrThrow(_cursor, "chargingCosts");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final int _cursorIndexOfLastModified = CursorUtil.getColumnIndexOrThrow(_cursor, "lastModified");
          final int _cursorIndexOfIsFavourite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavourite");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfDescriptionHash = CursorUtil.getColumnIndexOrThrow(_cursor, "descriptionHash");
          final int _cursorIndexOfAiParsed = CursorUtil.getColumnIndexOrThrow(_cursor, "aiParsed");
          final int _cursorIndexOfAiParsedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "aiParsedAt");
          final LocationEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final String _tmpSocketTypes;
            if (_cursor.isNull(_cursorIndexOfSocketTypes)) {
              _tmpSocketTypes = null;
            } else {
              _tmpSocketTypes = _cursor.getString(_cursorIndexOfSocketTypes);
            }
            final String _tmpPublicPrivate;
            if (_cursor.isNull(_cursorIndexOfPublicPrivate)) {
              _tmpPublicPrivate = null;
            } else {
              _tmpPublicPrivate = _cursor.getString(_cursorIndexOfPublicPrivate);
            }
            final boolean _tmpHasFee;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasFee);
            _tmpHasFee = _tmp != 0;
            final int _tmpGpoCount;
            _tmpGpoCount = _cursor.getInt(_cursorIndexOfGpoCount);
            final String _tmpOpeningHours;
            if (_cursor.isNull(_cursorIndexOfOpeningHours)) {
              _tmpOpeningHours = null;
            } else {
              _tmpOpeningHours = _cursor.getString(_cursorIndexOfOpeningHours);
            }
            final String _tmpAdditionalInfo;
            if (_cursor.isNull(_cursorIndexOfAdditionalInfo)) {
              _tmpAdditionalInfo = null;
            } else {
              _tmpAdditionalInfo = _cursor.getString(_cursorIndexOfAdditionalInfo);
            }
            final String _tmpProviderName;
            if (_cursor.isNull(_cursorIndexOfProviderName)) {
              _tmpProviderName = null;
            } else {
              _tmpProviderName = _cursor.getString(_cursorIndexOfProviderName);
            }
            final String _tmpProviderWebsite;
            if (_cursor.isNull(_cursorIndexOfProviderWebsite)) {
              _tmpProviderWebsite = null;
            } else {
              _tmpProviderWebsite = _cursor.getString(_cursorIndexOfProviderWebsite);
            }
            final String _tmpChargingCosts;
            if (_cursor.isNull(_cursorIndexOfChargingCosts)) {
              _tmpChargingCosts = null;
            } else {
              _tmpChargingCosts = _cursor.getString(_cursorIndexOfChargingCosts);
            }
            final String _tmpColor;
            if (_cursor.isNull(_cursorIndexOfColor)) {
              _tmpColor = null;
            } else {
              _tmpColor = _cursor.getString(_cursorIndexOfColor);
            }
            final String _tmpIcon;
            if (_cursor.isNull(_cursorIndexOfIcon)) {
              _tmpIcon = null;
            } else {
              _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
            }
            final long _tmpLastModified;
            _tmpLastModified = _cursor.getLong(_cursorIndexOfLastModified);
            final boolean _tmpIsFavourite;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsFavourite);
            _tmpIsFavourite = _tmp_1 != 0;
            final String _tmpSyncStatus;
            if (_cursor.isNull(_cursorIndexOfSyncStatus)) {
              _tmpSyncStatus = null;
            } else {
              _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            }
            final String _tmpDescriptionHash;
            if (_cursor.isNull(_cursorIndexOfDescriptionHash)) {
              _tmpDescriptionHash = null;
            } else {
              _tmpDescriptionHash = _cursor.getString(_cursorIndexOfDescriptionHash);
            }
            final boolean _tmpAiParsed;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfAiParsed);
            _tmpAiParsed = _tmp_2 != 0;
            final Long _tmpAiParsedAt;
            if (_cursor.isNull(_cursorIndexOfAiParsedAt)) {
              _tmpAiParsedAt = null;
            } else {
              _tmpAiParsedAt = _cursor.getLong(_cursorIndexOfAiParsedAt);
            }
            _result = new LocationEntity(_tmpId,_tmpName,_tmpDescription,_tmpLatitude,_tmpLongitude,_tmpSocketTypes,_tmpPublicPrivate,_tmpHasFee,_tmpGpoCount,_tmpOpeningHours,_tmpAdditionalInfo,_tmpProviderName,_tmpProviderWebsite,_tmpChargingCosts,_tmpColor,_tmpIcon,_tmpLastModified,_tmpIsFavourite,_tmpSyncStatus,_tmpDescriptionHash,_tmpAiParsed,_tmpAiParsedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<LocationEntity>> getAllLocations() {
    final String _sql = "SELECT * FROM locations ORDER BY lastModified DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"locations"}, new Callable<List<LocationEntity>>() {
      @Override
      @NonNull
      public List<LocationEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfSocketTypes = CursorUtil.getColumnIndexOrThrow(_cursor, "socketTypes");
          final int _cursorIndexOfPublicPrivate = CursorUtil.getColumnIndexOrThrow(_cursor, "publicPrivate");
          final int _cursorIndexOfHasFee = CursorUtil.getColumnIndexOrThrow(_cursor, "hasFee");
          final int _cursorIndexOfGpoCount = CursorUtil.getColumnIndexOrThrow(_cursor, "gpoCount");
          final int _cursorIndexOfOpeningHours = CursorUtil.getColumnIndexOrThrow(_cursor, "openingHours");
          final int _cursorIndexOfAdditionalInfo = CursorUtil.getColumnIndexOrThrow(_cursor, "additionalInfo");
          final int _cursorIndexOfProviderName = CursorUtil.getColumnIndexOrThrow(_cursor, "providerName");
          final int _cursorIndexOfProviderWebsite = CursorUtil.getColumnIndexOrThrow(_cursor, "providerWebsite");
          final int _cursorIndexOfChargingCosts = CursorUtil.getColumnIndexOrThrow(_cursor, "chargingCosts");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final int _cursorIndexOfLastModified = CursorUtil.getColumnIndexOrThrow(_cursor, "lastModified");
          final int _cursorIndexOfIsFavourite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavourite");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfDescriptionHash = CursorUtil.getColumnIndexOrThrow(_cursor, "descriptionHash");
          final int _cursorIndexOfAiParsed = CursorUtil.getColumnIndexOrThrow(_cursor, "aiParsed");
          final int _cursorIndexOfAiParsedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "aiParsedAt");
          final List<LocationEntity> _result = new ArrayList<LocationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocationEntity _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final String _tmpSocketTypes;
            if (_cursor.isNull(_cursorIndexOfSocketTypes)) {
              _tmpSocketTypes = null;
            } else {
              _tmpSocketTypes = _cursor.getString(_cursorIndexOfSocketTypes);
            }
            final String _tmpPublicPrivate;
            if (_cursor.isNull(_cursorIndexOfPublicPrivate)) {
              _tmpPublicPrivate = null;
            } else {
              _tmpPublicPrivate = _cursor.getString(_cursorIndexOfPublicPrivate);
            }
            final boolean _tmpHasFee;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasFee);
            _tmpHasFee = _tmp != 0;
            final int _tmpGpoCount;
            _tmpGpoCount = _cursor.getInt(_cursorIndexOfGpoCount);
            final String _tmpOpeningHours;
            if (_cursor.isNull(_cursorIndexOfOpeningHours)) {
              _tmpOpeningHours = null;
            } else {
              _tmpOpeningHours = _cursor.getString(_cursorIndexOfOpeningHours);
            }
            final String _tmpAdditionalInfo;
            if (_cursor.isNull(_cursorIndexOfAdditionalInfo)) {
              _tmpAdditionalInfo = null;
            } else {
              _tmpAdditionalInfo = _cursor.getString(_cursorIndexOfAdditionalInfo);
            }
            final String _tmpProviderName;
            if (_cursor.isNull(_cursorIndexOfProviderName)) {
              _tmpProviderName = null;
            } else {
              _tmpProviderName = _cursor.getString(_cursorIndexOfProviderName);
            }
            final String _tmpProviderWebsite;
            if (_cursor.isNull(_cursorIndexOfProviderWebsite)) {
              _tmpProviderWebsite = null;
            } else {
              _tmpProviderWebsite = _cursor.getString(_cursorIndexOfProviderWebsite);
            }
            final String _tmpChargingCosts;
            if (_cursor.isNull(_cursorIndexOfChargingCosts)) {
              _tmpChargingCosts = null;
            } else {
              _tmpChargingCosts = _cursor.getString(_cursorIndexOfChargingCosts);
            }
            final String _tmpColor;
            if (_cursor.isNull(_cursorIndexOfColor)) {
              _tmpColor = null;
            } else {
              _tmpColor = _cursor.getString(_cursorIndexOfColor);
            }
            final String _tmpIcon;
            if (_cursor.isNull(_cursorIndexOfIcon)) {
              _tmpIcon = null;
            } else {
              _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
            }
            final long _tmpLastModified;
            _tmpLastModified = _cursor.getLong(_cursorIndexOfLastModified);
            final boolean _tmpIsFavourite;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsFavourite);
            _tmpIsFavourite = _tmp_1 != 0;
            final String _tmpSyncStatus;
            if (_cursor.isNull(_cursorIndexOfSyncStatus)) {
              _tmpSyncStatus = null;
            } else {
              _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            }
            final String _tmpDescriptionHash;
            if (_cursor.isNull(_cursorIndexOfDescriptionHash)) {
              _tmpDescriptionHash = null;
            } else {
              _tmpDescriptionHash = _cursor.getString(_cursorIndexOfDescriptionHash);
            }
            final boolean _tmpAiParsed;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfAiParsed);
            _tmpAiParsed = _tmp_2 != 0;
            final Long _tmpAiParsedAt;
            if (_cursor.isNull(_cursorIndexOfAiParsedAt)) {
              _tmpAiParsedAt = null;
            } else {
              _tmpAiParsedAt = _cursor.getLong(_cursorIndexOfAiParsedAt);
            }
            _item = new LocationEntity(_tmpId,_tmpName,_tmpDescription,_tmpLatitude,_tmpLongitude,_tmpSocketTypes,_tmpPublicPrivate,_tmpHasFee,_tmpGpoCount,_tmpOpeningHours,_tmpAdditionalInfo,_tmpProviderName,_tmpProviderWebsite,_tmpChargingCosts,_tmpColor,_tmpIcon,_tmpLastModified,_tmpIsFavourite,_tmpSyncStatus,_tmpDescriptionHash,_tmpAiParsed,_tmpAiParsedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<LocationEntity>> getFavouriteLocations() {
    final String _sql = "SELECT * FROM locations WHERE isFavourite = 1 ORDER BY lastModified DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"locations"}, new Callable<List<LocationEntity>>() {
      @Override
      @NonNull
      public List<LocationEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfSocketTypes = CursorUtil.getColumnIndexOrThrow(_cursor, "socketTypes");
          final int _cursorIndexOfPublicPrivate = CursorUtil.getColumnIndexOrThrow(_cursor, "publicPrivate");
          final int _cursorIndexOfHasFee = CursorUtil.getColumnIndexOrThrow(_cursor, "hasFee");
          final int _cursorIndexOfGpoCount = CursorUtil.getColumnIndexOrThrow(_cursor, "gpoCount");
          final int _cursorIndexOfOpeningHours = CursorUtil.getColumnIndexOrThrow(_cursor, "openingHours");
          final int _cursorIndexOfAdditionalInfo = CursorUtil.getColumnIndexOrThrow(_cursor, "additionalInfo");
          final int _cursorIndexOfProviderName = CursorUtil.getColumnIndexOrThrow(_cursor, "providerName");
          final int _cursorIndexOfProviderWebsite = CursorUtil.getColumnIndexOrThrow(_cursor, "providerWebsite");
          final int _cursorIndexOfChargingCosts = CursorUtil.getColumnIndexOrThrow(_cursor, "chargingCosts");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final int _cursorIndexOfLastModified = CursorUtil.getColumnIndexOrThrow(_cursor, "lastModified");
          final int _cursorIndexOfIsFavourite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavourite");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfDescriptionHash = CursorUtil.getColumnIndexOrThrow(_cursor, "descriptionHash");
          final int _cursorIndexOfAiParsed = CursorUtil.getColumnIndexOrThrow(_cursor, "aiParsed");
          final int _cursorIndexOfAiParsedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "aiParsedAt");
          final List<LocationEntity> _result = new ArrayList<LocationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocationEntity _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final String _tmpSocketTypes;
            if (_cursor.isNull(_cursorIndexOfSocketTypes)) {
              _tmpSocketTypes = null;
            } else {
              _tmpSocketTypes = _cursor.getString(_cursorIndexOfSocketTypes);
            }
            final String _tmpPublicPrivate;
            if (_cursor.isNull(_cursorIndexOfPublicPrivate)) {
              _tmpPublicPrivate = null;
            } else {
              _tmpPublicPrivate = _cursor.getString(_cursorIndexOfPublicPrivate);
            }
            final boolean _tmpHasFee;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasFee);
            _tmpHasFee = _tmp != 0;
            final int _tmpGpoCount;
            _tmpGpoCount = _cursor.getInt(_cursorIndexOfGpoCount);
            final String _tmpOpeningHours;
            if (_cursor.isNull(_cursorIndexOfOpeningHours)) {
              _tmpOpeningHours = null;
            } else {
              _tmpOpeningHours = _cursor.getString(_cursorIndexOfOpeningHours);
            }
            final String _tmpAdditionalInfo;
            if (_cursor.isNull(_cursorIndexOfAdditionalInfo)) {
              _tmpAdditionalInfo = null;
            } else {
              _tmpAdditionalInfo = _cursor.getString(_cursorIndexOfAdditionalInfo);
            }
            final String _tmpProviderName;
            if (_cursor.isNull(_cursorIndexOfProviderName)) {
              _tmpProviderName = null;
            } else {
              _tmpProviderName = _cursor.getString(_cursorIndexOfProviderName);
            }
            final String _tmpProviderWebsite;
            if (_cursor.isNull(_cursorIndexOfProviderWebsite)) {
              _tmpProviderWebsite = null;
            } else {
              _tmpProviderWebsite = _cursor.getString(_cursorIndexOfProviderWebsite);
            }
            final String _tmpChargingCosts;
            if (_cursor.isNull(_cursorIndexOfChargingCosts)) {
              _tmpChargingCosts = null;
            } else {
              _tmpChargingCosts = _cursor.getString(_cursorIndexOfChargingCosts);
            }
            final String _tmpColor;
            if (_cursor.isNull(_cursorIndexOfColor)) {
              _tmpColor = null;
            } else {
              _tmpColor = _cursor.getString(_cursorIndexOfColor);
            }
            final String _tmpIcon;
            if (_cursor.isNull(_cursorIndexOfIcon)) {
              _tmpIcon = null;
            } else {
              _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
            }
            final long _tmpLastModified;
            _tmpLastModified = _cursor.getLong(_cursorIndexOfLastModified);
            final boolean _tmpIsFavourite;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsFavourite);
            _tmpIsFavourite = _tmp_1 != 0;
            final String _tmpSyncStatus;
            if (_cursor.isNull(_cursorIndexOfSyncStatus)) {
              _tmpSyncStatus = null;
            } else {
              _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            }
            final String _tmpDescriptionHash;
            if (_cursor.isNull(_cursorIndexOfDescriptionHash)) {
              _tmpDescriptionHash = null;
            } else {
              _tmpDescriptionHash = _cursor.getString(_cursorIndexOfDescriptionHash);
            }
            final boolean _tmpAiParsed;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfAiParsed);
            _tmpAiParsed = _tmp_2 != 0;
            final Long _tmpAiParsedAt;
            if (_cursor.isNull(_cursorIndexOfAiParsedAt)) {
              _tmpAiParsedAt = null;
            } else {
              _tmpAiParsedAt = _cursor.getLong(_cursorIndexOfAiParsedAt);
            }
            _item = new LocationEntity(_tmpId,_tmpName,_tmpDescription,_tmpLatitude,_tmpLongitude,_tmpSocketTypes,_tmpPublicPrivate,_tmpHasFee,_tmpGpoCount,_tmpOpeningHours,_tmpAdditionalInfo,_tmpProviderName,_tmpProviderWebsite,_tmpChargingCosts,_tmpColor,_tmpIcon,_tmpLastModified,_tmpIsFavourite,_tmpSyncStatus,_tmpDescriptionHash,_tmpAiParsed,_tmpAiParsedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<LocationEntity>> searchLocations(final String query) {
    final String _sql = "SELECT * FROM locations WHERE name LIKE '%' || ? || '%' ORDER BY lastModified DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (query == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, query);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"locations"}, new Callable<List<LocationEntity>>() {
      @Override
      @NonNull
      public List<LocationEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfSocketTypes = CursorUtil.getColumnIndexOrThrow(_cursor, "socketTypes");
          final int _cursorIndexOfPublicPrivate = CursorUtil.getColumnIndexOrThrow(_cursor, "publicPrivate");
          final int _cursorIndexOfHasFee = CursorUtil.getColumnIndexOrThrow(_cursor, "hasFee");
          final int _cursorIndexOfGpoCount = CursorUtil.getColumnIndexOrThrow(_cursor, "gpoCount");
          final int _cursorIndexOfOpeningHours = CursorUtil.getColumnIndexOrThrow(_cursor, "openingHours");
          final int _cursorIndexOfAdditionalInfo = CursorUtil.getColumnIndexOrThrow(_cursor, "additionalInfo");
          final int _cursorIndexOfProviderName = CursorUtil.getColumnIndexOrThrow(_cursor, "providerName");
          final int _cursorIndexOfProviderWebsite = CursorUtil.getColumnIndexOrThrow(_cursor, "providerWebsite");
          final int _cursorIndexOfChargingCosts = CursorUtil.getColumnIndexOrThrow(_cursor, "chargingCosts");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final int _cursorIndexOfLastModified = CursorUtil.getColumnIndexOrThrow(_cursor, "lastModified");
          final int _cursorIndexOfIsFavourite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavourite");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfDescriptionHash = CursorUtil.getColumnIndexOrThrow(_cursor, "descriptionHash");
          final int _cursorIndexOfAiParsed = CursorUtil.getColumnIndexOrThrow(_cursor, "aiParsed");
          final int _cursorIndexOfAiParsedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "aiParsedAt");
          final List<LocationEntity> _result = new ArrayList<LocationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocationEntity _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final String _tmpSocketTypes;
            if (_cursor.isNull(_cursorIndexOfSocketTypes)) {
              _tmpSocketTypes = null;
            } else {
              _tmpSocketTypes = _cursor.getString(_cursorIndexOfSocketTypes);
            }
            final String _tmpPublicPrivate;
            if (_cursor.isNull(_cursorIndexOfPublicPrivate)) {
              _tmpPublicPrivate = null;
            } else {
              _tmpPublicPrivate = _cursor.getString(_cursorIndexOfPublicPrivate);
            }
            final boolean _tmpHasFee;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasFee);
            _tmpHasFee = _tmp != 0;
            final int _tmpGpoCount;
            _tmpGpoCount = _cursor.getInt(_cursorIndexOfGpoCount);
            final String _tmpOpeningHours;
            if (_cursor.isNull(_cursorIndexOfOpeningHours)) {
              _tmpOpeningHours = null;
            } else {
              _tmpOpeningHours = _cursor.getString(_cursorIndexOfOpeningHours);
            }
            final String _tmpAdditionalInfo;
            if (_cursor.isNull(_cursorIndexOfAdditionalInfo)) {
              _tmpAdditionalInfo = null;
            } else {
              _tmpAdditionalInfo = _cursor.getString(_cursorIndexOfAdditionalInfo);
            }
            final String _tmpProviderName;
            if (_cursor.isNull(_cursorIndexOfProviderName)) {
              _tmpProviderName = null;
            } else {
              _tmpProviderName = _cursor.getString(_cursorIndexOfProviderName);
            }
            final String _tmpProviderWebsite;
            if (_cursor.isNull(_cursorIndexOfProviderWebsite)) {
              _tmpProviderWebsite = null;
            } else {
              _tmpProviderWebsite = _cursor.getString(_cursorIndexOfProviderWebsite);
            }
            final String _tmpChargingCosts;
            if (_cursor.isNull(_cursorIndexOfChargingCosts)) {
              _tmpChargingCosts = null;
            } else {
              _tmpChargingCosts = _cursor.getString(_cursorIndexOfChargingCosts);
            }
            final String _tmpColor;
            if (_cursor.isNull(_cursorIndexOfColor)) {
              _tmpColor = null;
            } else {
              _tmpColor = _cursor.getString(_cursorIndexOfColor);
            }
            final String _tmpIcon;
            if (_cursor.isNull(_cursorIndexOfIcon)) {
              _tmpIcon = null;
            } else {
              _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
            }
            final long _tmpLastModified;
            _tmpLastModified = _cursor.getLong(_cursorIndexOfLastModified);
            final boolean _tmpIsFavourite;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsFavourite);
            _tmpIsFavourite = _tmp_1 != 0;
            final String _tmpSyncStatus;
            if (_cursor.isNull(_cursorIndexOfSyncStatus)) {
              _tmpSyncStatus = null;
            } else {
              _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            }
            final String _tmpDescriptionHash;
            if (_cursor.isNull(_cursorIndexOfDescriptionHash)) {
              _tmpDescriptionHash = null;
            } else {
              _tmpDescriptionHash = _cursor.getString(_cursorIndexOfDescriptionHash);
            }
            final boolean _tmpAiParsed;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfAiParsed);
            _tmpAiParsed = _tmp_2 != 0;
            final Long _tmpAiParsedAt;
            if (_cursor.isNull(_cursorIndexOfAiParsedAt)) {
              _tmpAiParsedAt = null;
            } else {
              _tmpAiParsedAt = _cursor.getLong(_cursorIndexOfAiParsedAt);
            }
            _item = new LocationEntity(_tmpId,_tmpName,_tmpDescription,_tmpLatitude,_tmpLongitude,_tmpSocketTypes,_tmpPublicPrivate,_tmpHasFee,_tmpGpoCount,_tmpOpeningHours,_tmpAdditionalInfo,_tmpProviderName,_tmpProviderWebsite,_tmpChargingCosts,_tmpColor,_tmpIcon,_tmpLastModified,_tmpIsFavourite,_tmpSyncStatus,_tmpDescriptionHash,_tmpAiParsed,_tmpAiParsedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getLocationsNearby(final double latitude, final double longitude,
      final double radiusKm, final Continuation<? super List<LocationEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM locations \n"
            + "        WHERE (6371 * acos(cos(radians(?)) * cos(radians(latitude)) * cos(radians(longitude) - radians(?)) + sin(radians(?)) * sin(radians(latitude)))) < ?\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 4);
    int _argIndex = 1;
    _statement.bindDouble(_argIndex, latitude);
    _argIndex = 2;
    _statement.bindDouble(_argIndex, longitude);
    _argIndex = 3;
    _statement.bindDouble(_argIndex, latitude);
    _argIndex = 4;
    _statement.bindDouble(_argIndex, radiusKm);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocationEntity>>() {
      @Override
      @NonNull
      public List<LocationEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfSocketTypes = CursorUtil.getColumnIndexOrThrow(_cursor, "socketTypes");
          final int _cursorIndexOfPublicPrivate = CursorUtil.getColumnIndexOrThrow(_cursor, "publicPrivate");
          final int _cursorIndexOfHasFee = CursorUtil.getColumnIndexOrThrow(_cursor, "hasFee");
          final int _cursorIndexOfGpoCount = CursorUtil.getColumnIndexOrThrow(_cursor, "gpoCount");
          final int _cursorIndexOfOpeningHours = CursorUtil.getColumnIndexOrThrow(_cursor, "openingHours");
          final int _cursorIndexOfAdditionalInfo = CursorUtil.getColumnIndexOrThrow(_cursor, "additionalInfo");
          final int _cursorIndexOfProviderName = CursorUtil.getColumnIndexOrThrow(_cursor, "providerName");
          final int _cursorIndexOfProviderWebsite = CursorUtil.getColumnIndexOrThrow(_cursor, "providerWebsite");
          final int _cursorIndexOfChargingCosts = CursorUtil.getColumnIndexOrThrow(_cursor, "chargingCosts");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final int _cursorIndexOfLastModified = CursorUtil.getColumnIndexOrThrow(_cursor, "lastModified");
          final int _cursorIndexOfIsFavourite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavourite");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfDescriptionHash = CursorUtil.getColumnIndexOrThrow(_cursor, "descriptionHash");
          final int _cursorIndexOfAiParsed = CursorUtil.getColumnIndexOrThrow(_cursor, "aiParsed");
          final int _cursorIndexOfAiParsedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "aiParsedAt");
          final List<LocationEntity> _result = new ArrayList<LocationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocationEntity _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final String _tmpSocketTypes;
            if (_cursor.isNull(_cursorIndexOfSocketTypes)) {
              _tmpSocketTypes = null;
            } else {
              _tmpSocketTypes = _cursor.getString(_cursorIndexOfSocketTypes);
            }
            final String _tmpPublicPrivate;
            if (_cursor.isNull(_cursorIndexOfPublicPrivate)) {
              _tmpPublicPrivate = null;
            } else {
              _tmpPublicPrivate = _cursor.getString(_cursorIndexOfPublicPrivate);
            }
            final boolean _tmpHasFee;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasFee);
            _tmpHasFee = _tmp != 0;
            final int _tmpGpoCount;
            _tmpGpoCount = _cursor.getInt(_cursorIndexOfGpoCount);
            final String _tmpOpeningHours;
            if (_cursor.isNull(_cursorIndexOfOpeningHours)) {
              _tmpOpeningHours = null;
            } else {
              _tmpOpeningHours = _cursor.getString(_cursorIndexOfOpeningHours);
            }
            final String _tmpAdditionalInfo;
            if (_cursor.isNull(_cursorIndexOfAdditionalInfo)) {
              _tmpAdditionalInfo = null;
            } else {
              _tmpAdditionalInfo = _cursor.getString(_cursorIndexOfAdditionalInfo);
            }
            final String _tmpProviderName;
            if (_cursor.isNull(_cursorIndexOfProviderName)) {
              _tmpProviderName = null;
            } else {
              _tmpProviderName = _cursor.getString(_cursorIndexOfProviderName);
            }
            final String _tmpProviderWebsite;
            if (_cursor.isNull(_cursorIndexOfProviderWebsite)) {
              _tmpProviderWebsite = null;
            } else {
              _tmpProviderWebsite = _cursor.getString(_cursorIndexOfProviderWebsite);
            }
            final String _tmpChargingCosts;
            if (_cursor.isNull(_cursorIndexOfChargingCosts)) {
              _tmpChargingCosts = null;
            } else {
              _tmpChargingCosts = _cursor.getString(_cursorIndexOfChargingCosts);
            }
            final String _tmpColor;
            if (_cursor.isNull(_cursorIndexOfColor)) {
              _tmpColor = null;
            } else {
              _tmpColor = _cursor.getString(_cursorIndexOfColor);
            }
            final String _tmpIcon;
            if (_cursor.isNull(_cursorIndexOfIcon)) {
              _tmpIcon = null;
            } else {
              _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
            }
            final long _tmpLastModified;
            _tmpLastModified = _cursor.getLong(_cursorIndexOfLastModified);
            final boolean _tmpIsFavourite;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsFavourite);
            _tmpIsFavourite = _tmp_1 != 0;
            final String _tmpSyncStatus;
            if (_cursor.isNull(_cursorIndexOfSyncStatus)) {
              _tmpSyncStatus = null;
            } else {
              _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            }
            final String _tmpDescriptionHash;
            if (_cursor.isNull(_cursorIndexOfDescriptionHash)) {
              _tmpDescriptionHash = null;
            } else {
              _tmpDescriptionHash = _cursor.getString(_cursorIndexOfDescriptionHash);
            }
            final boolean _tmpAiParsed;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfAiParsed);
            _tmpAiParsed = _tmp_2 != 0;
            final Long _tmpAiParsedAt;
            if (_cursor.isNull(_cursorIndexOfAiParsedAt)) {
              _tmpAiParsedAt = null;
            } else {
              _tmpAiParsedAt = _cursor.getLong(_cursorIndexOfAiParsedAt);
            }
            _item = new LocationEntity(_tmpId,_tmpName,_tmpDescription,_tmpLatitude,_tmpLongitude,_tmpSocketTypes,_tmpPublicPrivate,_tmpHasFee,_tmpGpoCount,_tmpOpeningHours,_tmpAdditionalInfo,_tmpProviderName,_tmpProviderWebsite,_tmpChargingCosts,_tmpColor,_tmpIcon,_tmpLastModified,_tmpIsFavourite,_tmpSyncStatus,_tmpDescriptionHash,_tmpAiParsed,_tmpAiParsedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
