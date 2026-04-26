package com.a42r.pevplugshare.di

import android.content.Context
import com.a42r.pevplugshare.util.BackupRestoreHelper
import com.a42r.pevplugshare.util.BackupCreationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UtilityModule {

    @Provides
    fun provideBackupRestoreHelper(
        @ApplicationContext context: Context
    ): BackupRestoreHelper {
        return BackupRestoreHelper(context)
    }

    @Provides
    fun provideBackupCreationHelper(
        @ApplicationContext context: Context
    ): BackupCreationHelper {
        return BackupCreationHelper(context)
    }
}
