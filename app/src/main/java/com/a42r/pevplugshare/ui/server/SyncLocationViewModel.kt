package com.a42r.pevplugshare.ui.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.data.db.dao.LocationDao
import com.a42r.pevplugshare.data.db.dao.PhotoDao
import com.a42r.pevplugshare.data.db.entity.LocationEntity
import com.a42r.pevplugshare.data.repository.ServerSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SyncLocationViewModel @Inject constructor(
    private val locationDao: LocationDao,
    private val serverSyncRepository: ServerSyncRepository,
    private val photoDao: PhotoDao
) : ViewModel() {

    sealed class UploadState {
        object Idle : UploadState()
        data class Uploading(val current: Int, val total: Int) : UploadState()
        data class Done(val succeeded: Int, val failed: Int) : UploadState()
        data class Error(val message: String) : UploadState()
    }

    private val _locations = MutableStateFlow<List<LocationEntity>>(emptyList())
    val locations: StateFlow<List<LocationEntity>> = _locations.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    init {
        viewModelScope.launch {
            val list = locationDao.getAllLocations().first()
            _locations.value = list
        }
    }

    fun uploadSelected(selectedIds: Set<String>) {
        val toUpload = _locations.value.filter { it.id in selectedIds }
        if (toUpload.isEmpty()) return

        viewModelScope.launch {
            var succeeded = 0
            var failed = 0
            toUpload.forEachIndexed { index, entity ->
                _uploadState.value = UploadState.Uploading(index + 1, toUpload.size)
                val result = serverSyncRepository.uploadLocation(entity)
                if (result.isSuccess) {
                    succeeded++
                    val serverLoc = result.getOrNull()
                    // Upload photos if any
                    if (serverLoc != null) {
                        val photos = photoDao.getPhotosByLocation(entity.id)
                        photos.forEach { photo ->
                            val file = File(photo.filePath)
                            if (file.exists()) {
                                serverSyncRepository.uploadPhoto(serverLoc.id, file)
                            }
                        }
                    }
                } else {
                    failed++
                }
            }
            _uploadState.value = UploadState.Done(succeeded, failed)
        }
    }
}
