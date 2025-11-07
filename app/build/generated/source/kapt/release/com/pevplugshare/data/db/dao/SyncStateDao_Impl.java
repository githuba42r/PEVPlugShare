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
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.pevplugshare.data.db.entity.SyncStateEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SyncStateDao_Impl implements SyncStateDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SyncStateEntity> __insertionAdapterOfSyncStateEntity;

  private final EntityDeletionOrUpdateAdapter<SyncStateEntity> __updateAdapterOfSyncStateEntity;

  public SyncStateDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSyncStateEntity = new EntityInsertionAdapter<SyncStateEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `sync_state` (`id`,`lastSyncTime`,`syncStatus`,`pendingCount`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SyncStateEntity entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        statement.bindLong(2, entity.getLastSyncTime());
        if (entity.getSyncStatus() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getSyncStatus());
        }
        statement.bindLong(4, entity.getPendingCount());
      }
    };
    this.__updateAdapterOfSyncStateEntity = new EntityDeletionOrUpdateAdapter<SyncStateEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `sync_state` SET `id` = ?,`lastSyncTime` = ?,`syncStatus` = ?,`pendingCount` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SyncStateEntity entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        statement.bindLong(2, entity.getLastSyncTime());
        if (entity.getSyncStatus() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getSyncStatus());
        }
        statement.bindLong(4, entity.getPendingCount());
        if (entity.getId() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getId());
        }
      }
    };
  }

  @Override
  public Object insertSyncState(final SyncStateEntity syncState,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSyncStateEntity.insert(syncState);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSyncState(final SyncStateEntity syncState,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfSyncStateEntity.handle(syncState);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getSyncState(final Continuation<? super SyncStateEntity> $completion) {
    final String _sql = "SELECT * FROM sync_state WHERE id = 'sync_state'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SyncStateEntity>() {
      @Override
      @Nullable
      public SyncStateEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLastSyncTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncTime");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfPendingCount = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingCount");
          final SyncStateEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final long _tmpLastSyncTime;
            _tmpLastSyncTime = _cursor.getLong(_cursorIndexOfLastSyncTime);
            final String _tmpSyncStatus;
            if (_cursor.isNull(_cursorIndexOfSyncStatus)) {
              _tmpSyncStatus = null;
            } else {
              _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            }
            final int _tmpPendingCount;
            _tmpPendingCount = _cursor.getInt(_cursorIndexOfPendingCount);
            _result = new SyncStateEntity(_tmpId,_tmpLastSyncTime,_tmpSyncStatus,_tmpPendingCount);
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
  public Flow<SyncStateEntity> getSyncStateFlow() {
    final String _sql = "SELECT * FROM sync_state WHERE id = 'sync_state'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sync_state"}, new Callable<SyncStateEntity>() {
      @Override
      @Nullable
      public SyncStateEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLastSyncTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncTime");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfPendingCount = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingCount");
          final SyncStateEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final long _tmpLastSyncTime;
            _tmpLastSyncTime = _cursor.getLong(_cursorIndexOfLastSyncTime);
            final String _tmpSyncStatus;
            if (_cursor.isNull(_cursorIndexOfSyncStatus)) {
              _tmpSyncStatus = null;
            } else {
              _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            }
            final int _tmpPendingCount;
            _tmpPendingCount = _cursor.getInt(_cursorIndexOfPendingCount);
            _result = new SyncStateEntity(_tmpId,_tmpLastSyncTime,_tmpSyncStatus,_tmpPendingCount);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
