package com.a42r.pevplugshare.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.a42r.pevplugshare.data.repository.SyncManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class AutoSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "AutoSyncWorker"
        private const val PERIODIC_SYNC_TAG = "auto_sync_work"
        private const val SYNC_INTERVAL_HOURS = 6L

        fun scheduleAutoSync(context: Context) {
            val syncRequest = PeriodicWorkRequestBuilder<AutoSyncWorker>(
                SYNC_INTERVAL_HOURS,
                TimeUnit.HOURS
            ).apply {
                setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                addTag(PERIODIC_SYNC_TAG)
            }.build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_SYNC_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )

            Log.d(TAG, "Auto sync scheduled")
        }

        fun cancelAutoSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_SYNC_TAG)
            Log.d(TAG, "Auto sync cancelled")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting auto sync")

            val result = syncManager.syncPendingOperations()

            if (result) {
                Log.d(TAG, "Auto sync completed successfully")
                Result.success()
            } else {
                Log.d(TAG, "Auto sync failed, will retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auto sync error", e)
            Result.retry()
        }
    }
}
