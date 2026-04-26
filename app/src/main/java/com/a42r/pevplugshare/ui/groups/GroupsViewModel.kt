package com.a42r.pevplugshare.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.data.api.CreateGroupBody
import com.a42r.pevplugshare.data.api.ServerFriendGroup
import com.a42r.pevplugshare.data.remote.ServerApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val serverApiClient: ServerApiClient
) : ViewModel() {

    private val _groups = MutableStateFlow<List<ServerFriendGroup>>(emptyList())
    val groups: StateFlow<List<ServerFriendGroup>> = _groups.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun loadGroups() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val service = serverApiClient.getService()
                val resp = service.getGroups()
                if (resp.isSuccessful) {
                    _groups.value = resp.body()?.data ?: emptyList()
                } else {
                    _error.value = "Failed to load groups: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load groups"
            } finally {
                _loading.value = false
            }
        }
    }

    fun createGroup(name: String, description: String?) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val service = serverApiClient.getService()
                val resp = service.createGroup(CreateGroupBody(name = name, description = description?.ifBlank { null }))
                if (resp.isSuccessful) {
                    loadGroups()
                } else {
                    _error.value = "Failed to create group: ${resp.code()}"
                    _loading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create group"
                _loading.value = false
            }
        }
    }
}
