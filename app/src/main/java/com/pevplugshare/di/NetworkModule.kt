package com.pevplugshare.di

import android.content.Context
import com.pevplugshare.data.remote.OsmandApiClient
import com.pevplugshare.data.remote.TokenManager
import com.pevplugshare.data.repository.AuthRepositoryImpl
import com.pevplugshare.data.repository.LocationRepositoryImpl
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
    fun provideOsmandApiClient(
        tokenManager: TokenManager
    ): OsmandApiClient {
        return OsmandApiClient(tokenManager)
    }

    @Singleton
    @Provides
    fun provideAuthRepository(
        apiClient: OsmandApiClient,
        tokenManager: TokenManager
    ): AuthRepository {
        return AuthRepositoryImpl(apiClient, tokenManager)
    }

    @Singleton
    @Provides
    fun provideLocationRepository(
        apiClient: OsmandApiClient,
        tokenManager: TokenManager
    ): LocationRepository {
        return LocationRepositoryImpl(apiClient, tokenManager)
    }

    @Singleton
    @Provides
    fun providePhotoStorageManager(
        @ApplicationContext context: Context
    ): PhotoStorageManager {
        return PhotoStorageManager(context)
    }
}
