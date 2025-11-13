package com.a42r.pevplugshare.ui.photo

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.data.model.LocationPhoto
import com.a42r.pevplugshare.data.storage.PhotoStorageManager
import com.a42r.pevplugshare.util.PhotoUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PhotoCaptureState {
    object Idle : PhotoCaptureState()
    object Loading : PhotoCaptureState()
    object CameraPrepared : PhotoCaptureState()
    data class PhotoCaptured(val bitmap: Bitmap) : PhotoCaptureState()
    data class PhotoSaved(val photo: LocationPhoto) : PhotoCaptureState()
    data class Error(val message: String) : PhotoCaptureState()
}

data class PhotoCaptureData(
    val bitmap: Bitmap? = null,
    val caption: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val exifData: Map<String, String> = emptyMap()
)

@HiltViewModel
class PhotoCaptureViewModel @Inject constructor(
    private val photoStorageManager: PhotoStorageManager
) : ViewModel() {

    private val _photoCaptureState = MutableStateFlow<PhotoCaptureState>(PhotoCaptureState.Idle)
    val photoCaptureState: StateFlow<PhotoCaptureState> = _photoCaptureState

    private val _captureData = MutableStateFlow(PhotoCaptureData())
    val captureData: StateFlow<PhotoCaptureData> = _captureData

    fun setPhotoBitmap(bitmap: Bitmap) {
        _captureData.value = _captureData.value.copy(bitmap = bitmap)
        _photoCaptureState.value = PhotoCaptureState.PhotoCaptured(bitmap)
    }

    fun updateCaption(caption: String) {
        _captureData.value = _captureData.value.copy(caption = caption)
    }

    fun updateLocation(latitude: Double, longitude: Double) {
        _captureData.value = _captureData.value.copy(latitude = latitude, longitude = longitude)
    }

    fun savePhoto(locationId: String) {
        val data = _captureData.value
        if (data.bitmap == null) {
            _photoCaptureState.value = PhotoCaptureState.Error("No photo to save")
            return
        }

        viewModelScope.launch {
            _photoCaptureState.value = PhotoCaptureState.Loading

            try {
                val (photoPath, thumbnailPath) = photoStorageManager.savePhotoFromBitmap(
                    data.bitmap,
                    locationId
                ) ?: throw Exception("Failed to save photo")

                val fileSize = photoStorageManager.getPhotoSize(photoPath)
                val exifData = PhotoUtils.extractLocationFromExif(photoPath)

                val photo = LocationPhoto(
                    locationId = locationId,
                    photoPath = photoPath,
                    thumbnailPath = thumbnailPath,
                    caption = data.caption.ifBlank { null },
                    latitude = exifData?.first ?: data.latitude,
                    longitude = exifData?.second ?: data.longitude,
                    timestamp = System.currentTimeMillis(),
                    fileSize = fileSize,
                    uploadedToOsmand = false
                )

                _photoCaptureState.value = PhotoCaptureState.PhotoSaved(photo)
                resetCapture()
            } catch (e: Exception) {
                _photoCaptureState.value = PhotoCaptureState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetCapture() {
        _captureData.value = PhotoCaptureData()
        _photoCaptureState.value = PhotoCaptureState.Idle
    }
}
