package com.a42r.pevplugshare.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.data.api.AddMembersBody
import com.a42r.pevplugshare.data.api.ChangeRoleBody
import com.a42r.pevplugshare.data.api.ServerFriendGroup
import com.a42r.pevplugshare.data.api.ServerGroupMember
import com.a42r.pevplugshare.data.remote.ServerApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val serverApiClient: ServerApiClient
) : ViewModel() {

    private val _group = MutableStateFlow<ServerFriendGroup?>(null)
    val group: StateFlow<ServerFriendGroup?> = _group.asStateFlow()

    private val _members = MutableStateFlow<List<ServerGroupMember>>(emptyList())
    val members: StateFlow<List<ServerGroupMember>> = _members.asStateFlow()

    private val _inviteUrl = MutableStateFlow<String?>(null)
    val inviteUrl: StateFlow<String?> = _inviteUrl.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    fun loadGroup(id: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val service = serverApiClient.getService()
                val groupResp = service.getGroup(id)
                if (groupResp.isSuccessful) {
                    _group.value = groupResp.body()
                } else {
                    _error.value = "Failed to load group: ${groupResp.code()}"
                }

                val membersResp = service.getGroupMembers(id)
                if (membersResp.isSuccessful) {
                    _members.value = membersResp.body() ?: emptyList()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load group"
            } finally {
                _loading.value = false
            }
        }
    }

    fun addMembers(groupId: String, userIds: List<String>) {
        viewModelScope.launch {
            try {
                val service = serverApiClient.getService()
                val resp = service.addGroupMembers(groupId, AddMembersBody(userIds))
                if (resp.isSuccessful) {
                    loadGroup(groupId)
                } else {
                    _error.value = "Failed to add members: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add members"
            }
        }
    }

    fun removeMember(groupId: String, userId: String) {
        viewModelScope.launch {
            try {
                val service = serverApiClient.getService()
                val resp = service.removeGroupMember(groupId, userId)
                if (resp.isSuccessful) {
                    loadGroup(groupId)
                } else {
                    _error.value = "Failed to remove member: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to remove member"
            }
        }
    }

    fun changeRole(groupId: String, userId: String, role: String) {
        viewModelScope.launch {
            try {
                val service = serverApiClient.getService()
                val resp = service.changeGroupMemberRole(groupId, userId, ChangeRoleBody(role))
                if (resp.isSuccessful) {
                    loadGroup(groupId)
                } else {
                    _error.value = "Failed to change role: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to change role"
            }
        }
    }

    fun generateInvite(groupId: String) {
        viewModelScope.launch {
            try {
                val service = serverApiClient.getService()
                val resp = service.generateGroupInvite(groupId)
                if (resp.isSuccessful) {
                    _inviteUrl.value = resp.body()?.url
                } else {
                    _error.value = "Failed to generate invite: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to generate invite"
            }
        }
    }

    fun deleteGroup(id: String) {
        viewModelScope.launch {
            try {
                val service = serverApiClient.getService()
                val resp = service.deleteGroup(id)
                if (resp.isSuccessful) {
                    _deleted.value = true
                } else {
                    _error.value = "Failed to delete group: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete group"
            }
        }
    }
}
