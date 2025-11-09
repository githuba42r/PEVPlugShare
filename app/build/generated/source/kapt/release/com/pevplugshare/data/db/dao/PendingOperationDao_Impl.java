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
import com.pevplugshare.data.db.entity.PendingOperationEntity;
import com.pevplugshare.data.db.entity.PendingOperationType;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
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
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PendingOperationDao_Impl implements PendingOperationDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PendingOperationEntity> __insertionAdapterOfPendingOperationEntity;

  private final EntityDeletionOrUpdateAdapter<PendingOperationEntity> __deletionAdapterOfPendingOperationEntity;

  private final EntityDeletionOrUpdateAdapter<PendingOperationEntity> __updateAdapterOfPendingOperationEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOperationById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteLocationOperations;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOperationsByTypeAndLocation;

  public PendingOperationDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPendingOperationEntity = new EntityInsertionAdapter<PendingOperationEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `pending_operations` (`id`,`locationId`,`operationType`,`operationData`,`timestamp`,`retryCount`,`maxRetries`,`lastRetryTime`,`errorMessage`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PendingOperationEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getLocationId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getLocationId());
        }
        statement.bindString(3, __PendingOperationType_enumToString(entity.getOperationType()));
        if (entity.getOperationData() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getOperationData());
        }
        statement.bindLong(5, entity.getTimestamp());
        statement.bindLong(6, entity.getRetryCount());
        statement.bindLong(7, entity.getMaxRetries());
        if (entity.getLastRetryTime() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getLastRetryTime());
        }
        if (entity.getErrorMessage() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getErrorMessage());
        }
      }
    };
    this.__deletionAdapterOfPendingOperationEntity = new EntityDeletionOrUpdateAdapter<PendingOperationEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `pending_operations` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PendingOperationEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfPendingOperationEntity = new EntityDeletionOrUpdateAdapter<PendingOperationEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `pending_operations` SET `id` = ?,`locationId` = ?,`operationType` = ?,`operationData` = ?,`timestamp` = ?,`retryCount` = ?,`maxRetries` = ?,`lastRetryTime` = ?,`errorMessage` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PendingOperationEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getLocationId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getLocationId());
        }
        statement.bindString(3, __PendingOperationType_enumToString(entity.getOperationType()));
        if (entity.getOperationData() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getOperationData());
        }
        statement.bindLong(5, entity.getTimestamp());
        statement.bindLong(6, entity.getRetryCount());
        statement.bindLong(7, entity.getMaxRetries());
        if (entity.getLastRetryTime() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getLastRetryTime());
        }
        if (entity.getErrorMessage() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getErrorMessage());
        }
        statement.bindLong(10, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteOperationById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM pending_operations WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteLocationOperations = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM pending_operations WHERE locationId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteOperationsByTypeAndLocation = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM pending_operations WHERE operationType = ? AND locationId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertOperation(final PendingOperationEntity operation,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfPendingOperationEntity.insertAndReturnId(operation);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOperation(final PendingOperationEntity operation,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfPendingOperationEntity.handle(operation);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateOperation(final PendingOperationEntity operation,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfPendingOperationEntity.handle(operation);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOperationById(final long operationId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOperationById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, operationId);
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
          __preparedStmtOfDeleteOperationById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteLocationOperations(final String locationId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteLocationOperations.acquire();
        int _argIndex = 1;
        if (locationId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, locationId);
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
          __preparedStmtOfDeleteLocationOperations.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOperationsByTypeAndLocation(final PendingOperationType operationType,
      final String locationId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOperationsByTypeAndLocation.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, __PendingOperationType_enumToString(operationType));
        _argIndex = 2;
        if (locationId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, locationId);
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
          __preparedStmtOfDeleteOperationsByTypeAndLocation.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getOperationById(final long operationId,
      final Continuation<? super PendingOperationEntity> $completion) {
    final String _sql = "SELECT * FROM pending_operations WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, operationId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PendingOperationEntity>() {
      @Override
      @Nullable
      public PendingOperationEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLocationId = CursorUtil.getColumnIndexOrThrow(_cursor, "locationId");
          final int _cursorIndexOfOperationType = CursorUtil.getColumnIndexOrThrow(_cursor, "operationType");
          final int _cursorIndexOfOperationData = CursorUtil.getColumnIndexOrThrow(_cursor, "operationData");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfRetryCount = CursorUtil.getColumnIndexOrThrow(_cursor, "retryCount");
          final int _cursorIndexOfMaxRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "maxRetries");
          final int _cursorIndexOfLastRetryTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastRetryTime");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final PendingOperationEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpLocationId;
            if (_cursor.isNull(_cursorIndexOfLocationId)) {
              _tmpLocationId = null;
            } else {
              _tmpLocationId = _cursor.getString(_cursorIndexOfLocationId);
            }
            final PendingOperationType _tmpOperationType;
            _tmpOperationType = __PendingOperationType_stringToEnum(_cursor.getString(_cursorIndexOfOperationType));
            final String _tmpOperationData;
            if (_cursor.isNull(_cursorIndexOfOperationData)) {
              _tmpOperationData = null;
            } else {
              _tmpOperationData = _cursor.getString(_cursorIndexOfOperationData);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpRetryCount;
            _tmpRetryCount = _cursor.getInt(_cursorIndexOfRetryCount);
            final int _tmpMaxRetries;
            _tmpMaxRetries = _cursor.getInt(_cursorIndexOfMaxRetries);
            final Long _tmpLastRetryTime;
            if (_cursor.isNull(_cursorIndexOfLastRetryTime)) {
              _tmpLastRetryTime = null;
            } else {
              _tmpLastRetryTime = _cursor.getLong(_cursorIndexOfLastRetryTime);
            }
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            _result = new PendingOperationEntity(_tmpId,_tmpLocationId,_tmpOperationType,_tmpOperationData,_tmpTimestamp,_tmpRetryCount,_tmpMaxRetries,_tmpLastRetryTime,_tmpErrorMessage);
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
  public Object getLocationOperations(final String locationId,
      final Continuation<? super List<PendingOperationEntity>> $completion) {
    final String _sql = "SELECT * FROM pending_operations WHERE locationId = ? ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (locationId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, locationId);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PendingOperationEntity>>() {
      @Override
      @NonNull
      public List<PendingOperationEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLocationId = CursorUtil.getColumnIndexOrThrow(_cursor, "locationId");
          final int _cursorIndexOfOperationType = CursorUtil.getColumnIndexOrThrow(_cursor, "operationType");
          final int _cursorIndexOfOperationData = CursorUtil.getColumnIndexOrThrow(_cursor, "operationData");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfRetryCount = CursorUtil.getColumnIndexOrThrow(_cursor, "retryCount");
          final int _cursorIndexOfMaxRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "maxRetries");
          final int _cursorIndexOfLastRetryTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastRetryTime");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final List<PendingOperationEntity> _result = new ArrayList<PendingOperationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PendingOperationEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpLocationId;
            if (_cursor.isNull(_cursorIndexOfLocationId)) {
              _tmpLocationId = null;
            } else {
              _tmpLocationId = _cursor.getString(_cursorIndexOfLocationId);
            }
            final PendingOperationType _tmpOperationType;
            _tmpOperationType = __PendingOperationType_stringToEnum(_cursor.getString(_cursorIndexOfOperationType));
            final String _tmpOperationData;
            if (_cursor.isNull(_cursorIndexOfOperationData)) {
              _tmpOperationData = null;
            } else {
              _tmpOperationData = _cursor.getString(_cursorIndexOfOperationData);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpRetryCount;
            _tmpRetryCount = _cursor.getInt(_cursorIndexOfRetryCount);
            final int _tmpMaxRetries;
            _tmpMaxRetries = _cursor.getInt(_cursorIndexOfMaxRetries);
            final Long _tmpLastRetryTime;
            if (_cursor.isNull(_cursorIndexOfLastRetryTime)) {
              _tmpLastRetryTime = null;
            } else {
              _tmpLastRetryTime = _cursor.getLong(_cursorIndexOfLastRetryTime);
            }
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            _item = new PendingOperationEntity(_tmpId,_tmpLocationId,_tmpOperationType,_tmpOperationData,_tmpTimestamp,_tmpRetryCount,_tmpMaxRetries,_tmpLastRetryTime,_tmpErrorMessage);
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
  public Object getAllPendingOperations(
      final Continuation<? super List<PendingOperationEntity>> $completion) {
    final String _sql = "SELECT * FROM pending_operations ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PendingOperationEntity>>() {
      @Override
      @NonNull
      public List<PendingOperationEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLocationId = CursorUtil.getColumnIndexOrThrow(_cursor, "locationId");
          final int _cursorIndexOfOperationType = CursorUtil.getColumnIndexOrThrow(_cursor, "operationType");
          final int _cursorIndexOfOperationData = CursorUtil.getColumnIndexOrThrow(_cursor, "operationData");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfRetryCount = CursorUtil.getColumnIndexOrThrow(_cursor, "retryCount");
          final int _cursorIndexOfMaxRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "maxRetries");
          final int _cursorIndexOfLastRetryTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastRetryTime");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final List<PendingOperationEntity> _result = new ArrayList<PendingOperationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PendingOperationEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpLocationId;
            if (_cursor.isNull(_cursorIndexOfLocationId)) {
              _tmpLocationId = null;
            } else {
              _tmpLocationId = _cursor.getString(_cursorIndexOfLocationId);
            }
            final PendingOperationType _tmpOperationType;
            _tmpOperationType = __PendingOperationType_stringToEnum(_cursor.getString(_cursorIndexOfOperationType));
            final String _tmpOperationData;
            if (_cursor.isNull(_cursorIndexOfOperationData)) {
              _tmpOperationData = null;
            } else {
              _tmpOperationData = _cursor.getString(_cursorIndexOfOperationData);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpRetryCount;
            _tmpRetryCount = _cursor.getInt(_cursorIndexOfRetryCount);
            final int _tmpMaxRetries;
            _tmpMaxRetries = _cursor.getInt(_cursorIndexOfMaxRetries);
            final Long _tmpLastRetryTime;
            if (_cursor.isNull(_cursorIndexOfLastRetryTime)) {
              _tmpLastRetryTime = null;
            } else {
              _tmpLastRetryTime = _cursor.getLong(_cursorIndexOfLastRetryTime);
            }
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            _item = new PendingOperationEntity(_tmpId,_tmpLocationId,_tmpOperationType,_tmpOperationData,_tmpTimestamp,_tmpRetryCount,_tmpMaxRetries,_tmpLastRetryTime,_tmpErrorMessage);
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
  public Object getRetryableOperations(
      final Continuation<? super List<PendingOperationEntity>> $completion) {
    final String _sql = "SELECT * FROM pending_operations WHERE retryCount < maxRetries ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PendingOperationEntity>>() {
      @Override
      @NonNull
      public List<PendingOperationEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLocationId = CursorUtil.getColumnIndexOrThrow(_cursor, "locationId");
          final int _cursorIndexOfOperationType = CursorUtil.getColumnIndexOrThrow(_cursor, "operationType");
          final int _cursorIndexOfOperationData = CursorUtil.getColumnIndexOrThrow(_cursor, "operationData");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfRetryCount = CursorUtil.getColumnIndexOrThrow(_cursor, "retryCount");
          final int _cursorIndexOfMaxRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "maxRetries");
          final int _cursorIndexOfLastRetryTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastRetryTime");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final List<PendingOperationEntity> _result = new ArrayList<PendingOperationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PendingOperationEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpLocationId;
            if (_cursor.isNull(_cursorIndexOfLocationId)) {
              _tmpLocationId = null;
            } else {
              _tmpLocationId = _cursor.getString(_cursorIndexOfLocationId);
            }
            final PendingOperationType _tmpOperationType;
            _tmpOperationType = __PendingOperationType_stringToEnum(_cursor.getString(_cursorIndexOfOperationType));
            final String _tmpOperationData;
            if (_cursor.isNull(_cursorIndexOfOperationData)) {
              _tmpOperationData = null;
            } else {
              _tmpOperationData = _cursor.getString(_cursorIndexOfOperationData);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpRetryCount;
            _tmpRetryCount = _cursor.getInt(_cursorIndexOfRetryCount);
            final int _tmpMaxRetries;
            _tmpMaxRetries = _cursor.getInt(_cursorIndexOfMaxRetries);
            final Long _tmpLastRetryTime;
            if (_cursor.isNull(_cursorIndexOfLastRetryTime)) {
              _tmpLastRetryTime = null;
            } else {
              _tmpLastRetryTime = _cursor.getLong(_cursorIndexOfLastRetryTime);
            }
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            _item = new PendingOperationEntity(_tmpId,_tmpLocationId,_tmpOperationType,_tmpOperationData,_tmpTimestamp,_tmpRetryCount,_tmpMaxRetries,_tmpLastRetryTime,_tmpErrorMessage);
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
  public Object getFailedOperations(
      final Continuation<? super List<PendingOperationEntity>> $completion) {
    final String _sql = "SELECT * FROM pending_operations WHERE retryCount >= maxRetries ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PendingOperationEntity>>() {
      @Override
      @NonNull
      public List<PendingOperationEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLocationId = CursorUtil.getColumnIndexOrThrow(_cursor, "locationId");
          final int _cursorIndexOfOperationType = CursorUtil.getColumnIndexOrThrow(_cursor, "operationType");
          final int _cursorIndexOfOperationData = CursorUtil.getColumnIndexOrThrow(_cursor, "operationData");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfRetryCount = CursorUtil.getColumnIndexOrThrow(_cursor, "retryCount");
          final int _cursorIndexOfMaxRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "maxRetries");
          final int _cursorIndexOfLastRetryTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastRetryTime");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final List<PendingOperationEntity> _result = new ArrayList<PendingOperationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PendingOperationEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpLocationId;
            if (_cursor.isNull(_cursorIndexOfLocationId)) {
              _tmpLocationId = null;
            } else {
              _tmpLocationId = _cursor.getString(_cursorIndexOfLocationId);
            }
            final PendingOperationType _tmpOperationType;
            _tmpOperationType = __PendingOperationType_stringToEnum(_cursor.getString(_cursorIndexOfOperationType));
            final String _tmpOperationData;
            if (_cursor.isNull(_cursorIndexOfOperationData)) {
              _tmpOperationData = null;
            } else {
              _tmpOperationData = _cursor.getString(_cursorIndexOfOperationData);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpRetryCount;
            _tmpRetryCount = _cursor.getInt(_cursorIndexOfRetryCount);
            final int _tmpMaxRetries;
            _tmpMaxRetries = _cursor.getInt(_cursorIndexOfMaxRetries);
            final Long _tmpLastRetryTime;
            if (_cursor.isNull(_cursorIndexOfLastRetryTime)) {
              _tmpLastRetryTime = null;
            } else {
              _tmpLastRetryTime = _cursor.getLong(_cursorIndexOfLastRetryTime);
            }
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            _item = new PendingOperationEntity(_tmpId,_tmpLocationId,_tmpOperationType,_tmpOperationData,_tmpTimestamp,_tmpRetryCount,_tmpMaxRetries,_tmpLastRetryTime,_tmpErrorMessage);
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
  public Flow<Integer> getPendingOperationCount() {
    final String _sql = "SELECT COUNT(*) FROM pending_operations";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"pending_operations"}, new Callable<Integer>() {
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
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Integer> getFailedOperationCount() {
    final String _sql = "SELECT COUNT(*) FROM pending_operations WHERE retryCount >= maxRetries";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"pending_operations"}, new Callable<Integer>() {
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
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getOperationsByType(final PendingOperationType operationType,
      final Continuation<? super List<PendingOperationEntity>> $completion) {
    final String _sql = "SELECT * FROM pending_operations WHERE operationType = ? ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, __PendingOperationType_enumToString(operationType));
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PendingOperationEntity>>() {
      @Override
      @NonNull
      public List<PendingOperationEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLocationId = CursorUtil.getColumnIndexOrThrow(_cursor, "locationId");
          final int _cursorIndexOfOperationType = CursorUtil.getColumnIndexOrThrow(_cursor, "operationType");
          final int _cursorIndexOfOperationData = CursorUtil.getColumnIndexOrThrow(_cursor, "operationData");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfRetryCount = CursorUtil.getColumnIndexOrThrow(_cursor, "retryCount");
          final int _cursorIndexOfMaxRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "maxRetries");
          final int _cursorIndexOfLastRetryTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastRetryTime");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final List<PendingOperationEntity> _result = new ArrayList<PendingOperationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PendingOperationEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpLocationId;
            if (_cursor.isNull(_cursorIndexOfLocationId)) {
              _tmpLocationId = null;
            } else {
              _tmpLocationId = _cursor.getString(_cursorIndexOfLocationId);
            }
            final PendingOperationType _tmpOperationType;
            _tmpOperationType = __PendingOperationType_stringToEnum(_cursor.getString(_cursorIndexOfOperationType));
            final String _tmpOperationData;
            if (_cursor.isNull(_cursorIndexOfOperationData)) {
              _tmpOperationData = null;
            } else {
              _tmpOperationData = _cursor.getString(_cursorIndexOfOperationData);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpRetryCount;
            _tmpRetryCount = _cursor.getInt(_cursorIndexOfRetryCount);
            final int _tmpMaxRetries;
            _tmpMaxRetries = _cursor.getInt(_cursorIndexOfMaxRetries);
            final Long _tmpLastRetryTime;
            if (_cursor.isNull(_cursorIndexOfLastRetryTime)) {
              _tmpLastRetryTime = null;
            } else {
              _tmpLastRetryTime = _cursor.getLong(_cursorIndexOfLastRetryTime);
            }
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            _item = new PendingOperationEntity(_tmpId,_tmpLocationId,_tmpOperationType,_tmpOperationData,_tmpTimestamp,_tmpRetryCount,_tmpMaxRetries,_tmpLastRetryTime,_tmpErrorMessage);
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
  public Object getRetryableOperationsByType(final PendingOperationType operationType,
      final Continuation<? super List<PendingOperationEntity>> $completion) {
    final String _sql = "SELECT * FROM pending_operations WHERE operationType = ? AND retryCount < maxRetries ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, __PendingOperationType_enumToString(operationType));
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PendingOperationEntity>>() {
      @Override
      @NonNull
      public List<PendingOperationEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLocationId = CursorUtil.getColumnIndexOrThrow(_cursor, "locationId");
          final int _cursorIndexOfOperationType = CursorUtil.getColumnIndexOrThrow(_cursor, "operationType");
          final int _cursorIndexOfOperationData = CursorUtil.getColumnIndexOrThrow(_cursor, "operationData");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfRetryCount = CursorUtil.getColumnIndexOrThrow(_cursor, "retryCount");
          final int _cursorIndexOfMaxRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "maxRetries");
          final int _cursorIndexOfLastRetryTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastRetryTime");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final List<PendingOperationEntity> _result = new ArrayList<PendingOperationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PendingOperationEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpLocationId;
            if (_cursor.isNull(_cursorIndexOfLocationId)) {
              _tmpLocationId = null;
            } else {
              _tmpLocationId = _cursor.getString(_cursorIndexOfLocationId);
            }
            final PendingOperationType _tmpOperationType;
            _tmpOperationType = __PendingOperationType_stringToEnum(_cursor.getString(_cursorIndexOfOperationType));
            final String _tmpOperationData;
            if (_cursor.isNull(_cursorIndexOfOperationData)) {
              _tmpOperationData = null;
            } else {
              _tmpOperationData = _cursor.getString(_cursorIndexOfOperationData);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpRetryCount;
            _tmpRetryCount = _cursor.getInt(_cursorIndexOfRetryCount);
            final int _tmpMaxRetries;
            _tmpMaxRetries = _cursor.getInt(_cursorIndexOfMaxRetries);
            final Long _tmpLastRetryTime;
            if (_cursor.isNull(_cursorIndexOfLastRetryTime)) {
              _tmpLastRetryTime = null;
            } else {
              _tmpLastRetryTime = _cursor.getLong(_cursorIndexOfLastRetryTime);
            }
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            _item = new PendingOperationEntity(_tmpId,_tmpLocationId,_tmpOperationType,_tmpOperationData,_tmpTimestamp,_tmpRetryCount,_tmpMaxRetries,_tmpLastRetryTime,_tmpErrorMessage);
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

  private String __PendingOperationType_enumToString(@NonNull final PendingOperationType _value) {
    switch (_value) {
      case ADD_LOCATION: return "ADD_LOCATION";
      case UPDATE_LOCATION: return "UPDATE_LOCATION";
      case DELETE_LOCATION: return "DELETE_LOCATION";
      case ADD_PHOTO: return "ADD_PHOTO";
      case DELETE_PHOTO: return "DELETE_PHOTO";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private PendingOperationType __PendingOperationType_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "ADD_LOCATION": return PendingOperationType.ADD_LOCATION;
      case "UPDATE_LOCATION": return PendingOperationType.UPDATE_LOCATION;
      case "DELETE_LOCATION": return PendingOperationType.DELETE_LOCATION;
      case "ADD_PHOTO": return PendingOperationType.ADD_PHOTO;
      case "DELETE_PHOTO": return PendingOperationType.DELETE_PHOTO;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
