package com.a42r.pevplugshare.ui.sharing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.data.api.ServerLocation
import com.a42r.pevplugshare.data.remote.ServerApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedWithMeViewModel @Inject constructor(
    private val serverApiClient: ServerApiClient
) : ViewModel() {

    private val _locations = MutableStateFlow<List<ServerLocation>>(emptyList())
    val locations: StateFlow<List<ServerLocation>> = _locations.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val service = serverApiClient.getService()
                val resp = service.getSharedLocations()
                if (resp.isSuccessful) {
                    _locations.value = resp.body()?.data ?: emptyList()
                } else {
                    _error.value = "Failed to load: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load shared locations"
            } finally {
                _loading.value = false
            }
        }
    }

    fun hideShare(shareId: String) {
        viewModelScope.launch {
            try {
                val service = serverApiClient.getService()
                val resp = service.hideShare(shareId)
                if (resp.isSuccessful) {
                    load()
                } else {
                    _error.value = "Failed to hide: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to hide"
            }
        }
    }

    fun unhideShare(shareId: String) {
        viewModelScope.launch {
            try {
                val service = serverApiClient.getService()
                val resp = service.unhideShare(shareId)
                if (resp.isSuccessful) {
                    load()
                } else {
                    _error.value = "Failed to unhide: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to unhide"
            }
        }
    }
}
