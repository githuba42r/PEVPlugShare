package com.a42r.pevplugshare.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.util.BackupCreationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BackupCreationViewModel @Inject constructor(
    application: Application,
    private val backupHelper: BackupCreationHelper
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "BackupCreationViewModel"
    }

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress

    private val _progressMessage = MutableStateFlow("")
    val progressMessage: StateFlow<String> = _progressMessage

    private var backupJob: Job? = null

    sealed class BackupState {
        object Idle : BackupState()
        object Creating : BackupState()
        data class Success(
            val backupFile: File,
            val itemCount: Int,
            val fileSize: Long,
            val photosCount: Int,
            val databaseSize: Long
        ) : BackupState()

        data class Error(val message: String) : BackupState()
    }

    /**
     * Create a backup with the specified options.
     *
     * @param includePhotos Whether to include location_images directory
     * @param description Optional description for the backup
     */
    fun createBackup(includePhotos: Boolean = true, description: String = "") {
        if (_backupState.value is BackupState.Creating) {
            Log.w(TAG, "Backup already in progress")
            return
        }

        Log.i(TAG, "Starting backup creation (includePhotos=$includePhotos, description='$description')")
        _backupState.value = BackupState.Creating
        _progress.value = 0
        _progressMessage.value = "Preparing backup..."

        backupJob = viewModelScope.launch {
            try {
                val progressCallback = BackupCreationHelper.ProgressCallback { current, total, message ->
                    val progressPercent = if (total > 0) (current * 100) / total else 0
                    _progress.value = progressPercent
                    _progressMessage.value = message
                    Log.d(TAG, "Progress: $progressPercent% - $message")
                }

                val result = backupHelper.createBackup(
                    includePhotos = includePhotos,
                    description = description,
                    progressCallback = progressCallback
                )

                Log.i(
                    TAG,
                    "Backup created successfully: ${result.file.name} " +
                            "(size=${formatFileSize(result.fileSize)}, items=${result.itemCount})"
                )

                _progress.value = 100
                _progressMessage.value = "Backup complete!"
                _backupState.value = BackupState.Success(
                    backupFile = result.file,
                    itemCount = result.itemCount,
                    fileSize = result.fileSize,
                    photosCount = result.photosCount,
                    databaseSize = result.databaseSize
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error creating backup", e)
                val errorMessage = e.message ?: "Unknown error"
                _backupState.value = BackupState.Error(errorMessage)
                _progress.value = 0
                _progressMessage.value = "Failed"
            }
        }
    }

    /**
     * Cancel the ongoing backup operation.
     */
    fun cancelBackup() {
        if (_backupState.value is BackupState.Creating) {
            Log.i(TAG, "Cancelling backup...")
            backupJob?.cancel()
            _backupState.value = BackupState.Idle
            _progress.value = 0
            _progressMessage.value = ""
        }
    }

    /**
     * Reset the state to Idle.
     */
    fun resetState() {
        _backupState.value = BackupState.Idle
        _progress.value = 0
        _progressMessage.value = ""
        backupJob = null
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
        }
    }
}
