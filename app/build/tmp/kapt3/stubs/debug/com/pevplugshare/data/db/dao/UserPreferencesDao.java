package com.pevplugshare.data.db.dao;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\bg\u0018\u00002\u00020\u0001J\u0010\u0010\u0002\u001a\u0004\u0018\u00010\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0004J\u0010\u0010\u0005\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00030\u0006H\'J\u0016\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u0003H\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0016\u0010\u000b\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u0003H\u00a7@\u00a2\u0006\u0002\u0010\n\u00a8\u0006\f"}, d2 = {"Lcom/pevplugshare/data/db/dao/UserPreferencesDao;", "", "getPreferences", "Lcom/pevplugshare/data/db/entity/UserPreferencesEntity;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getPreferencesFlow", "Lkotlinx/coroutines/flow/Flow;", "insertPreferences", "", "prefs", "(Lcom/pevplugshare/data/db/entity/UserPreferencesEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updatePreferences", "app_debug"})
@androidx.room.Dao()
public abstract interface UserPreferencesDao {
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertPreferences(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.UserPreferencesEntity prefs, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM user_preferences WHERE id = \'user_prefs\'")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getPreferences(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pevplugshare.data.db.entity.UserPreferencesEntity> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM user_preferences WHERE id = \'user_prefs\'")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<com.pevplugshare.data.db.entity.UserPreferencesEntity> getPreferencesFlow();
    
    @androidx.room.Update()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updatePreferences(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.UserPreferencesEntity prefs, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}