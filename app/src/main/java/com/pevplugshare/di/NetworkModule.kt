package com.pevplugshare.di

import android.content.Context
import com.pevplugshare.data.db.dao.LocationDao
import com.pevplugshare.data.db.dao.PendingOperationDao
import com.pevplugshare.data.network.ConnectivityObserver
import com.pevplugshare.data.remote.CookieManager
import com.pevplugshare.data.remote.OsmandApiClient
import com.pevplugshare.data.remote.TokenManager
import com.pevplugshare.data.repository.AuthRepositoryImpl
import com.pevplugshare.data.repository.LocalLocationRepository
import com.pevplugshare.data.repository.PendingOperationRepository
import com.pevplugshare.data.storage.PhotoStorageManager
import com.pevplugshare.domain.repository.AuthRepository
import com.pevplugshare.domain.repository.LocationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun provideTokenManager(
        @ApplicationContext context: Context
    ): TokenManager {
        return TokenManager(context)
    }

    @Singleton
    @Provides
    fun provideCookieManager(
        @ApplicationContext context: Context
    ): CookieManager {
        return CookieManager(context)
    }

    @Singleton
    @Provides
    fun provideOsmandApiClient(
        tokenManager: TokenManager,
        cookieManager: CookieManager
    ): OsmandApiClient {
        return OsmandApiClient(tokenManager, cookieManager)
    }

    @Singleton
    @Provides
    fun provideAuthRepository(
        apiClient: OsmandApiClient,
        tokenManager: TokenManager,
        @ApplicationContext context: Context,
        cookieManager: CookieManager
    ): AuthRepository {
        return AuthRepositoryImpl(apiClient, tokenManager, context, cookieManager)
    }

    @Singleton
    @Provides
    fun provideLocationRepository(
        locationDao: LocationDao
    ): LocationRepository {
        return LocalLocationRepository(locationDao)
    }

    @Singleton
    @Provides
    fun providePhotoStorageManager(
        @ApplicationContext context: Context
    ): PhotoStorageManager {
        return PhotoStorageManager(context)
    }

    @Singleton
    @Provides
    fun provideConnectivityObserver(
        @ApplicationContext context: Context
    ): ConnectivityObserver {
        return ConnectivityObserver.create(context)
    }
}
