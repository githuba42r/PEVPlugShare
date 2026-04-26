package com.a42r.pevplugshare.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.util.BackupRestoreHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    application: Application,
    private val restoreHelper: BackupRestoreHelper
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "BackupRestoreViewModel"
    }

    private val _restoreState = MutableLiveData<RestoreState>(RestoreState.Idle)
    val restoreState: LiveData<RestoreState> = _restoreState

    private val _backupInfo = MutableLiveData<BackupRestoreHelper.BackupInfo?>()
    val backupInfo: LiveData<BackupRestoreHelper.BackupInfo?> = _backupInfo

    sealed class RestoreState {
        object Idle : RestoreState()
        object SelectingFile : RestoreState()
        data class FileSelected(val fileName: String) : RestoreState()
        object Validating : RestoreState()
        data class Restoring(val message: String = "Restoring...", val progress: Int = 0) : RestoreState()
        object Success : RestoreState()
        data class Error(val message: String) : RestoreState()
    }

    fun onFileSelected(file: File) {
        Log.d(TAG, "File selected: ${file.name}")
        _restoreState.value = RestoreState.FileSelected(file.name)
        validateBackup(file)
    }

    private fun validateBackup(file: File) {
        _restoreState.value = RestoreState.Validating

        viewModelScope.launch {
            try {
                val info = restoreHelper.getBackupInfo(file)
                _backupInfo.value = info

                if (info == null) {
                    _restoreState.value = RestoreState.Error("Invalid backup file format")
                } else if (!info.hasDatabase) {
                    _restoreState.value = RestoreState.Error("Backup file does not contain a database")
                } else {
                    _restoreState.value = RestoreState.FileSelected(info.fileName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Validation error", e)
                _restoreState.value = RestoreState.Error("Validation failed: ${e.message}")
            }
        }
    }

    fun restoreBackup(file: File) {
        _restoreState.value = RestoreState.Restoring()
        Log.i(TAG, "Starting backup restore: ${file.name}")

        viewModelScope.launch {
            try {
                val progressCallback = BackupRestoreHelper.ProgressCallback { message, progress ->
                    Log.d(TAG, "Progress callback: $progress% - $message")
                    _restoreState.postValue(RestoreState.Restoring(message, progress))
                }

                val result = restoreHelper.restoreFromZip(file, progressCallback)

                when (result) {
                    is BackupRestoreHelper.RestoreResult.Success -> {
                        Log.i(TAG, "Restore completed successfully")
                        _restoreState.value = RestoreState.Success
                    }
                    is BackupRestoreHelper.RestoreResult.Error -> {
                        Log.e(TAG, "Restore error: ${result.message}")
                        _restoreState.value = RestoreState.Error(result.message)
                    }
                    is BackupRestoreHelper.RestoreResult.Progress -> {
                        Log.d(TAG, "Restore progress: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during restore", e)
                _restoreState.value = RestoreState.Error("Restore failed: ${e.message}")
            }
        }
    }

    fun resetState() {
        _restoreState.value = RestoreState.Idle
        _backupInfo.value = null
    }
}
