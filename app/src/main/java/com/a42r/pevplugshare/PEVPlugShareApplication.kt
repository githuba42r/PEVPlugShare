package com.a42r.pevplugshare

import android.app.Application
import com.a42r.pevplugshare.util.FileMigrationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PEVPlugShareApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FileMigrationHelper.migrateBackupFilesAsync(this)
    }
}
