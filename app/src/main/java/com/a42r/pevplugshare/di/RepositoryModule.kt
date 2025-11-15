package com.a42r.pevplugshare.di

import com.a42r.pevplugshare.data.db.dao.LocationDao
import com.a42r.pevplugshare.data.db.dao.PendingOperationDao
import com.a42r.pevplugshare.data.db.dao.PhotoDao
import com.a42r.pevplugshare.data.remote.GeocodingService
import com.a42r.pevplugshare.domain.repository.LocationRepository
import com.a42r.pevplugshare.data.repository.LocalLocationRepository
import com.a42r.pevplugshare.data.repository.PendingOperationRepository
import com.a42r.pevplugshare.data.repository.PhotoRepositoryImpl
import com.a42r.pevplugshare.domain.repository.PhotoRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun providePendingOperationRepository(
        pendingOperationDao: PendingOperationDao
    ): PendingOperationRepository {
        return PendingOperationRepository(pendingOperationDao)
    }

    @Singleton
    @Provides
    fun provideLocalLocationRepository(
        locationDao: LocationDao,
        geocodingService: GeocodingService
    ): LocationRepository {
        return LocalLocationRepository(locationDao, geocodingService)
    }

    @Singleton
    @Provides
    fun providePhotoRepository(
        photoDao: PhotoDao
    ): PhotoRepository {
        return PhotoRepositoryImpl(photoDao)
    }
}
