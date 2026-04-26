package com.a42r.pevplugshare.ui.sharing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.data.api.DirectShareBody
import com.a42r.pevplugshare.data.api.DirectShareResponse
import com.a42r.pevplugshare.data.api.ServerFriendGroup
import com.a42r.pevplugshare.data.api.ServerLocationShare
import com.a42r.pevplugshare.data.api.ServerUser
import com.a42r.pevplugshare.data.api.ShareWithBody
import com.a42r.pevplugshare.data.remote.ServerApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShareLocationViewModel @Inject constructor(
    private val serverApiClient: ServerApiClient
) : ViewModel() {

    private val _friends = MutableStateFlow<List<ServerUser>>(emptyList())
    val friends: StateFlow<List<ServerUser>> = _friends.asStateFlow()

    private val _groups = MutableStateFlow<List<ServerFriendGroup>>(emptyList())
    val groups: StateFlow<List<ServerFriendGroup>> = _groups.asStateFlow()

    private val _currentShares = MutableStateFlow<List<ServerLocationShare>>(emptyList())
    val currentShares: StateFlow<List<ServerLocationShare>> = _currentShares.asStateFlow()

    private val _directShareResult = MutableStateFlow<DirectShareResponse?>(null)
    val directShareResult: StateFlow<DirectShareResponse?> = _directShareResult.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()

    fun load(locationId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val service = serverApiClient.getService()

                val friendsResp = service.getFriends()
                if (friendsResp.isSuccessful) {
                    _friends.value = friendsResp.body()?.data ?: emptyList()
                }

                val groupsResp = service.getGroups()
                if (groupsResp.isSuccessful) {
                    _groups.value = groupsResp.body()?.data ?: emptyList()
                }

                val sharesResp = service.getLocationShares(locationId)
                if (sharesResp.isSuccessful) {
                    _currentShares.value = sharesResp.body()?.data ?: emptyList()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load data"
            } finally {
                _loading.value = false
            }
        }
    }

    fun shareWithUser(locationId: String, userId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val service = serverApiClient.getService()
                val resp = service.shareLocation(locationId, ShareWithBody(userId = userId))
                if (resp.isSuccessful) {
                    _success.value = "Location shared successfully"
                    load(locationId)
                } else {
                    _error.value = "Failed to share: ${resp.code()}"
                    _loading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to share"
                _loading.value = false
            }
        }
    }

    fun shareWithGroup(locationId: String, groupId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val service = serverApiClient.getService()
                val resp = service.shareLocation(locationId, ShareWithBody(groupId = groupId))
                if (resp.isSuccessful) {
                    _success.value = "Location shared with group"
                    load(locationId)
                } else {
                    _error.value = "Failed to share with group: ${resp.code()}"
                    _loading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to share with group"
                _loading.value = false
            }
        }
    }

    fun revokeShare(locationId: String, shareId: String) {
        viewModelScope.launch {
            try {
                val service = serverApiClient.getService()
                val resp = service.revokeShare(locationId, shareId)
                if (resp.isSuccessful) {
                    load(locationId)
                } else {
                    _error.value = "Failed to revoke share: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to revoke share"
            }
        }
    }

    fun generateDirectLink(locationId: String, singleUse: Boolean = false) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val service = serverApiClient.getService()
                val resp = service.generateDirectShare(locationId, DirectShareBody(singleUse = singleUse))
                if (resp.isSuccessful) {
                    _directShareResult.value = resp.body()
                } else {
                    _error.value = "Failed to generate link: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to generate link"
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearSuccess() {
        _success.value = null
    }

    fun clearDirectShareResult() {
        _directShareResult.value = null
    }
}
