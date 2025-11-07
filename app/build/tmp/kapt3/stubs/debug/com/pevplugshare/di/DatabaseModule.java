package com.pevplugshare.di;

@dagger.Module()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0003\u001a\u00020\u00042\b\b\u0001\u0010\u0005\u001a\u00020\u0006H\u0007J\u0010\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u0004H\u0007J\u0010\u0010\n\u001a\u00020\u000b2\u0006\u0010\t\u001a\u00020\u0004H\u0007J\u0010\u0010\f\u001a\u00020\r2\u0006\u0010\t\u001a\u00020\u0004H\u0007J\u0010\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\t\u001a\u00020\u0004H\u0007J\u0010\u0010\u0010\u001a\u00020\u00112\u0006\u0010\t\u001a\u00020\u0004H\u0007J\u0010\u0010\u0012\u001a\u00020\u00132\u0006\u0010\t\u001a\u00020\u0004H\u0007\u00a8\u0006\u0014"}, d2 = {"Lcom/pevplugshare/di/DatabaseModule;", "", "()V", "provideDatabase", "Lcom/pevplugshare/data/db/PEVPlugShareDatabase;", "context", "Landroid/content/Context;", "provideLocationDao", "Lcom/pevplugshare/data/db/dao/LocationDao;", "database", "providePendingLocationDao", "Lcom/pevplugshare/data/db/dao/PendingLocationDao;", "providePendingOperationDao", "Lcom/pevplugshare/data/db/dao/PendingOperationDao;", "providePhotoDao", "Lcom/pevplugshare/data/db/dao/PhotoDao;", "provideSyncStateDao", "Lcom/pevplugshare/data/db/dao/SyncStateDao;", "provideUserPreferencesDao", "Lcom/pevplugshare/data/db/dao/UserPreferencesDao;", "app_debug"})
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public final class DatabaseModule {
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.di.DatabaseModule INSTANCE = null;
    
    private DatabaseModule() {
        super();
    }
    
    @javax.inject.Singleton()
    @dagger.Provides()
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.db.PEVPlugShareDatabase provideDatabase(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @javax.inject.Singleton()
    @dagger.Provides()
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.db.dao.LocationDao provideLocationDao(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.PEVPlugShareDatabase database) {
        return null;
    }
    
    @javax.inject.Singleton()
    @dagger.Provides()
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.db.dao.PhotoDao providePhotoDao(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.PEVPlugShareDatabase database) {
        return null;
    }
    
    @javax.inject.Singleton()
    @dagger.Provides()
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.db.dao.SyncStateDao provideSyncStateDao(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.PEVPlugShareDatabase database) {
        return null;
    }
    
    @javax.inject.Singleton()
    @dagger.Provides()
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.db.dao.UserPreferencesDao provideUserPreferencesDao(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.PEVPlugShareDatabase database) {
        return null;
    }
    
    @javax.inject.Singleton()
    @dagger.Provides()
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.db.dao.PendingLocationDao providePendingLocationDao(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.PEVPlugShareDatabase database) {
        return null;
    }
    
    @javax.inject.Singleton()
    @dagger.Provides()
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.db.dao.PendingOperationDao providePendingOperationDao(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.PEVPlugShareDatabase database) {
        return null;
    }
}