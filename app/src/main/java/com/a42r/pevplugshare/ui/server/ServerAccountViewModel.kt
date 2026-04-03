package com.a42r.pevplugshare.ui.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.data.remote.TokenManager
import com.a42r.pevplugshare.data.repository.ServerAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerAccountViewModel @Inject constructor(
    private val serverAuthRepository: ServerAuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    sealed class State {
        object Disconnected : State()
        object Loading : State()
        data class Connected(val email: String, val displayName: String, val serverUrl: String) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Disconnected)
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        if (serverAuthRepository.isConnected()) {
            _state.value = State.Connected(
                email = serverAuthRepository.getServerEmail() ?: "",
                displayName = serverAuthRepository.getServerDisplayName() ?: "",
                serverUrl = serverAuthRepository.getServerUrl() ?: ""
            )
        }
    }

    fun connectWithGoogle(serverUrl: String, googleIdToken: String) {
        _state.value = State.Loading
        viewModelScope.launch {
            tokenManager.saveServerUrl(serverUrl)
            val result = serverAuthRepository.authenticateWithGoogle(googleIdToken)
            result.onSuccess { user ->
                _state.value = State.Connected(user.email, user.displayName, serverUrl)
            }.onFailure { e ->
                _state.value = State.Error(e.message ?: "Authentication failed")
            }
        }
    }

    fun disconnect() {
        serverAuthRepository.disconnect()
        _state.value = State.Disconnected
    }
}
