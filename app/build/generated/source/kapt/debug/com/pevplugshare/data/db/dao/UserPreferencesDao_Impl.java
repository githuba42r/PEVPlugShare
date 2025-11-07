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
import com.pevplugshare.data.db.entity.UserPreferencesEntity;
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
public final class UserPreferencesDao_Impl implements UserPreferencesDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<UserPreferencesEntity> __insertionAdapterOfUserPreferencesEntity;

  private final EntityDeletionOrUpdateAdapter<UserPreferencesEntity> __updateAdapterOfUserPreferencesEntity;

  public UserPreferencesDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUserPreferencesEntity = new EntityInsertionAdapter<UserPreferencesEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `user_preferences` (`id`,`selectedCategory`,`syncFrequencyHours`,`osmandAccount`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserPreferencesEntity entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        if (entity.getSelectedCategory() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getSelectedCategory());
        }
        statement.bindLong(3, entity.getSyncFrequencyHours());
        if (entity.getOsmandAccount() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getOsmandAccount());
        }
      }
    };
    this.__updateAdapterOfUserPreferencesEntity = new EntityDeletionOrUpdateAdapter<UserPreferencesEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `user_preferences` SET `id` = ?,`selectedCategory` = ?,`syncFrequencyHours` = ?,`osmandAccount` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserPreferencesEntity entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        if (entity.getSelectedCategory() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getSelectedCategory());
        }
        statement.bindLong(3, entity.getSyncFrequencyHours());
        if (entity.getOsmandAccount() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getOsmandAccount());
        }
        if (entity.getId() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getId());
        }
      }
    };
  }

  @Override
  public Object insertPreferences(final UserPreferencesEntity prefs,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUserPreferencesEntity.insert(prefs);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePreferences(final UserPreferencesEntity prefs,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfUserPreferencesEntity.handle(prefs);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getPreferences(final Continuation<? super UserPreferencesEntity> $completion) {
    final String _sql = "SELECT * FROM user_preferences WHERE id = 'user_prefs'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<UserPreferencesEntity>() {
      @Override
      @Nullable
      public UserPreferencesEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSelectedCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "selectedCategory");
          final int _cursorIndexOfSyncFrequencyHours = CursorUtil.getColumnIndexOrThrow(_cursor, "syncFrequencyHours");
          final int _cursorIndexOfOsmandAccount = CursorUtil.getColumnIndexOrThrow(_cursor, "osmandAccount");
          final UserPreferencesEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpSelectedCategory;
            if (_cursor.isNull(_cursorIndexOfSelectedCategory)) {
              _tmpSelectedCategory = null;
            } else {
              _tmpSelectedCategory = _cursor.getString(_cursorIndexOfSelectedCategory);
            }
            final int _tmpSyncFrequencyHours;
            _tmpSyncFrequencyHours = _cursor.getInt(_cursorIndexOfSyncFrequencyHours);
            final String _tmpOsmandAccount;
            if (_cursor.isNull(_cursorIndexOfOsmandAccount)) {
              _tmpOsmandAccount = null;
            } else {
              _tmpOsmandAccount = _cursor.getString(_cursorIndexOfOsmandAccount);
            }
            _result = new UserPreferencesEntity(_tmpId,_tmpSelectedCategory,_tmpSyncFrequencyHours,_tmpOsmandAccount);
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
  public Flow<UserPreferencesEntity> getPreferencesFlow() {
    final String _sql = "SELECT * FROM user_preferences WHERE id = 'user_prefs'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"user_preferences"}, new Callable<UserPreferencesEntity>() {
      @Override
      @Nullable
      public UserPreferencesEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSelectedCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "selectedCategory");
          final int _cursorIndexOfSyncFrequencyHours = CursorUtil.getColumnIndexOrThrow(_cursor, "syncFrequencyHours");
          final int _cursorIndexOfOsmandAccount = CursorUtil.getColumnIndexOrThrow(_cursor, "osmandAccount");
          final UserPreferencesEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpSelectedCategory;
            if (_cursor.isNull(_cursorIndexOfSelectedCategory)) {
              _tmpSelectedCategory = null;
            } else {
              _tmpSelectedCategory = _cursor.getString(_cursorIndexOfSelectedCategory);
            }
            final int _tmpSyncFrequencyHours;
            _tmpSyncFrequencyHours = _cursor.getInt(_cursorIndexOfSyncFrequencyHours);
            final String _tmpOsmandAccount;
            if (_cursor.isNull(_cursorIndexOfOsmandAccount)) {
              _tmpOsmandAccount = null;
            } else {
              _tmpOsmandAccount = _cursor.getString(_cursorIndexOfOsmandAccount);
            }
            _result = new UserPreferencesEntity(_tmpId,_tmpSelectedCategory,_tmpSyncFrequencyHours,_tmpOsmandAccount);
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
