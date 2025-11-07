package com.pevplugshare.di;

@dagger.Module()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0007J\u0018\u0010\t\u001a\u00020\n2\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0007J\u0010\u0010\u000b\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0007J\u0012\u0010\f\u001a\u00020\b2\b\b\u0001\u0010\r\u001a\u00020\u000eH\u0007\u00a8\u0006\u000f"}, d2 = {"Lcom/pevplugshare/di/NetworkModule;", "", "()V", "provideAuthRepository", "Lcom/pevplugshare/domain/repository/AuthRepository;", "apiClient", "Lcom/pevplugshare/data/remote/OsmandApiClient;", "tokenManager", "Lcom/pevplugshare/data/remote/TokenManager;", "provideLocationRepository", "Lcom/pevplugshare/domain/repository/LocationRepository;", "provideOsmandApiClient", "provideTokenManager", "context", "Landroid/content/Context;", "app_release"})
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public final class NetworkModule {
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.di.NetworkModule INSTANCE = null;
    
    private NetworkModule() {
        super();
    }
    
    @javax.inject.Singleton()
    @dagger.Provides()
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.remote.TokenManager provideTokenManager(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @javax.inject.Singleton()
    @dagger.Provides()
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.remote.OsmandApiClient provideOsmandApiClient(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.remote.TokenManager tokenManager) {
        return null;
    }
    
    @javax.inject.Singleton()
    @dagger.Provides()
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.domain.repository.AuthRepository provideAuthRepository(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.remote.OsmandApiClient apiClient, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.remote.TokenManager tokenManager) {
        return null;
    }
    
    @javax.inject.Singleton()
    @dagger.Provides()
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.domain.repository.LocationRepository provideLocationRepository(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.remote.OsmandApiClient apiClient, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.remote.TokenManager tokenManager) {
        return null;
    }
}