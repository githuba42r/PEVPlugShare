package com.a42r.pevplugshare.di

import com.a42r.pevplugshare.data.db.dao.PendingOperationDao
import com.a42r.pevplugshare.data.repository.PendingOperationRepository
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
        locationDao: com.a42r.pevplugshare.data.db.dao.LocationDao
    ): com.a42r.pevplugshare.data.repository.LocalLocationRepository {
        return com.a42r.pevplugshare.data.repository.LocalLocationRepository(locationDao)
    }
}
