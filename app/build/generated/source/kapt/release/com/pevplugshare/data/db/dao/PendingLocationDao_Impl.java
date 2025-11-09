package com.pevplugshare.data.db.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.pevplugshare.data.db.entity.PendingLocationEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PendingLocationDao_Impl implements PendingLocationDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PendingLocationEntity> __insertionAdapterOfPendingLocationEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeletePendingLocation;

  private final SharedSQLiteStatement __preparedStmtOfIncrementSyncAttempts;

  public PendingLocationDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPendingLocationEntity = new EntityInsertionAdapter<PendingLocationEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `pending_locations` (`id`,`name`,`description`,`latitude`,`longitude`,`socketTypes`,`publicPrivate`,`hasFee`,`gpoCount`,`openingHours`,`additionalInfo`,`providerName`,`providerWebsite`,`chargingCosts`,`createdAt`,`syncAttempts`,`lastSyncAttempt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PendingLocationEntity entity) {
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
        statement.bindLong(15, entity.getCreatedAt());
        statement.bindLong(16, entity.getSyncAttempts());
        if (entity.getLastSyncAttempt() == null) {
          statement.bindNull(17);
        } else {
          statement.bindLong(17, entity.getLastSyncAttempt());
        }
      }
    };
    this.__preparedStmtOfDeletePendingLocation = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM pending_locations WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfIncrementSyncAttempts = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE pending_locations SET syncAttempts = syncAttempts + 1, lastSyncAttempt = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertPendingLocation(final PendingLocationEntity location,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPendingLocationEntity.insert(location);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertPendingLocations(final List<PendingLocationEntity> locations,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPendingLocationEntity.insert(locations);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePendingLocation(final String id,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeletePendingLocation.acquire();
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
          __preparedStmtOfDeletePendingLocation.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object incrementSyncAttempts(final String id, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementSyncAttempts.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
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
          __preparedStmtOfIncrementSyncAttempts.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getPendingLocations(
      final Continuation<? super List<PendingLocationEntity>> $completion) {
    final String _sql = "SELECT * FROM pending_locations WHERE syncAttempts < 3 ORDER BY createdAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PendingLocationEntity>>() {
      @Override
      @NonNull
      public List<PendingLocationEntity> call() throws Exception {
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
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfSyncAttempts = CursorUtil.getColumnIndexOrThrow(_cursor, "syncAttempts");
          final int _cursorIndexOfLastSyncAttempt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncAttempt");
          final List<PendingLocationEntity> _result = new ArrayList<PendingLocationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PendingLocationEntity _item;
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
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final int _tmpSyncAttempts;
            _tmpSyncAttempts = _cursor.getInt(_cursorIndexOfSyncAttempts);
            final Long _tmpLastSyncAttempt;
            if (_cursor.isNull(_cursorIndexOfLastSyncAttempt)) {
              _tmpLastSyncAttempt = null;
            } else {
              _tmpLastSyncAttempt = _cursor.getLong(_cursorIndexOfLastSyncAttempt);
            }
            _item = new PendingLocationEntity(_tmpId,_tmpName,_tmpDescription,_tmpLatitude,_tmpLongitude,_tmpSocketTypes,_tmpPublicPrivate,_tmpHasFee,_tmpGpoCount,_tmpOpeningHours,_tmpAdditionalInfo,_tmpProviderName,_tmpProviderWebsite,_tmpChargingCosts,_tmpCreatedAt,_tmpSyncAttempts,_tmpLastSyncAttempt);
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

  @Override
  public Object getPendingLocationCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM pending_locations";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final Integer _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(0);
            }
            _result = _tmp;
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
