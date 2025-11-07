package com.pevplugshare.di

import android.content.Context
import com.pevplugshare.data.db.PEVPlugShareDatabase
import com.pevplugshare.data.db.dao.LocationDao
import com.pevplugshare.data.db.dao.PhotoDao
import com.pevplugshare.data.db.dao.PendingLocationDao
import com.pevplugshare.data.db.dao.SyncStateDao
import com.pevplugshare.data.db.dao.UserPreferencesDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context
    ): PEVPlugShareDatabase {
        return PEVPlugShareDatabase.getDatabase(context)
    }

    @Singleton
    @Provides
    fun provideLocationDao(database: PEVPlugShareDatabase): LocationDao {
        return database.locationDao()
    }

    @Singleton
    @Provides
    fun providePhotoDao(database: PEVPlugShareDatabase): PhotoDao {
        return database.photoDao()
    }

    @Singleton
    @Provides
    fun provideSyncStateDao(database: PEVPlugShareDatabase): SyncStateDao {
        return database.syncStateDao()
    }

    @Singleton
    @Provides
    fun provideUserPreferencesDao(database: PEVPlugShareDatabase): UserPreferencesDao {
        return database.userPreferencesDao()
    }

    @Singleton
    @Provides
    fun providePendingLocationDao(database: PEVPlugShareDatabase): PendingLocationDao {
        return database.pendingLocationDao()
    }
}
