package com.a42r.pevplugshare.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.data.api.FriendRequestBody
import com.a42r.pevplugshare.data.api.ServerFriendship
import com.a42r.pevplugshare.data.api.ServerUser
import com.a42r.pevplugshare.data.remote.ServerApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val serverApiClient: ServerApiClient
) : ViewModel() {

    private val _friends = MutableStateFlow<List<ServerUser>>(emptyList())
    val friends: StateFlow<List<ServerUser>> = _friends.asStateFlow()

    private val _incomingRequests = MutableStateFlow<List<ServerFriendship>>(emptyList())
    val incomingRequests: StateFlow<List<ServerFriendship>> = _incomingRequests.asStateFlow()

    private val _outgoingRequests = MutableStateFlow<List<ServerFriendship>>(emptyList())
    val outgoingRequests: StateFlow<List<ServerFriendship>> = _outgoingRequests.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun loadAll() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val service = serverApiClient.getService()

                val friendsResp = service.getFriends()
                if (friendsResp.isSuccessful) {
                    _friends.value = friendsResp.body()?.data ?: emptyList()
                }

                val incomingResp = service.getIncomingRequests()
                if (incomingResp.isSuccessful) {
                    _incomingRequests.value = incomingResp.body()?.data ?: emptyList()
                }

                val outgoingResp = service.getOutgoingRequests()
                if (outgoingResp.isSuccessful) {
                    _outgoingRequests.value = outgoingResp.body()?.data ?: emptyList()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load friends"
            } finally {
                _loading.value = false
            }
        }
    }

    fun sendRequest(email: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val service = serverApiClient.getService()
                val resp = service.sendFriendRequest(FriendRequestBody(email = email))
                if (resp.isSuccessful) {
                    loadAll()
                } else {
                    _error.value = "Failed to send request: ${resp.code()}"
                    _loading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to send request"
                _loading.value = false
            }
        }
    }

    fun acceptRequest(id: String) {
        viewModelScope.launch {
            try {
                val service = serverApiClient.getService()
                val resp = service.acceptFriendRequest(id)
                if (resp.isSuccessful) {
                    loadAll()
                } else {
                    _error.value = "Failed to accept request: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to accept request"
            }
        }
    }

    fun declineRequest(id: String) {
        viewModelScope.launch {
            try {
                val service = serverApiClient.getService()
                val resp = service.declineFriendRequest(id)
                if (resp.isSuccessful) {
                    loadAll()
                } else {
                    _error.value = "Failed to decline request: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to decline request"
            }
        }
    }

    fun removeFriend(userId: String) {
        viewModelScope.launch {
            try {
                val service = serverApiClient.getService()
                val resp = service.removeFriend(userId)
                if (resp.isSuccessful) {
                    loadAll()
                } else {
                    _error.value = "Failed to remove friend: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to remove friend"
            }
        }
    }
}
