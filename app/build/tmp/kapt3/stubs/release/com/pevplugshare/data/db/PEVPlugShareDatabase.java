package com.pevplugshare.data.db;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\'\u0018\u0000 \r2\u00020\u0001:\u0001\rB\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H&J\b\u0010\u0005\u001a\u00020\u0006H&J\b\u0010\u0007\u001a\u00020\bH&J\b\u0010\t\u001a\u00020\nH&J\b\u0010\u000b\u001a\u00020\fH&\u00a8\u0006\u000e"}, d2 = {"Lcom/pevplugshare/data/db/PEVPlugShareDatabase;", "Landroidx/room/RoomDatabase;", "()V", "locationDao", "Lcom/pevplugshare/data/db/dao/LocationDao;", "pendingLocationDao", "Lcom/pevplugshare/data/db/dao/PendingLocationDao;", "photoDao", "Lcom/pevplugshare/data/db/dao/PhotoDao;", "syncStateDao", "Lcom/pevplugshare/data/db/dao/SyncStateDao;", "userPreferencesDao", "Lcom/pevplugshare/data/db/dao/UserPreferencesDao;", "Companion", "app_release"})
@androidx.room.Database(entities = {com.pevplugshare.data.db.entity.LocationEntity.class, com.pevplugshare.data.db.entity.PhotoEntity.class, com.pevplugshare.data.db.entity.SyncStateEntity.class, com.pevplugshare.data.db.entity.UserPreferencesEntity.class, com.pevplugshare.data.db.entity.PendingLocationEntity.class}, version = 1, exportSchema = false)
public abstract class PEVPlugShareDatabase extends androidx.room.RoomDatabase {
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private static volatile com.pevplugshare.data.db.PEVPlugShareDatabase Instance;
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.data.db.PEVPlugShareDatabase.Companion Companion = null;
    
    public PEVPlugShareDatabase() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.pevplugshare.data.db.dao.LocationDao locationDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.pevplugshare.data.db.dao.PhotoDao photoDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.pevplugshare.data.db.dao.SyncStateDao syncStateDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.pevplugshare.data.db.dao.UserPreferencesDao userPreferencesDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.pevplugshare.data.db.dao.PendingLocationDao pendingLocationDao();
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u0007R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\b"}, d2 = {"Lcom/pevplugshare/data/db/PEVPlugShareDatabase$Companion;", "", "()V", "Instance", "Lcom/pevplugshare/data/db/PEVPlugShareDatabase;", "getDatabase", "context", "Landroid/content/Context;", "app_release"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.pevplugshare.data.db.PEVPlugShareDatabase getDatabase(@org.jetbrains.annotations.NotNull()
        android.content.Context context) {
            return null;
        }
    }
}