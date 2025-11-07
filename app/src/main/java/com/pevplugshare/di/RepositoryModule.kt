package com.pevplugshare.di

import com.pevplugshare.data.db.dao.PendingOperationDao
import com.pevplugshare.data.repository.PendingOperationRepository
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
}
