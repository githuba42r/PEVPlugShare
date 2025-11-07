package com.pevplugshare.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pevplugshare.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun exchangeCodeForToken(code: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val clientId = "osmand"
                val clientSecret = "" // OSMAnd public app doesn't require secret
                val redirectUri = "com.pevplugshare://auth/callback"

                val result = authRepository.getAccessTokenFromCode(
                    code = code,
                    clientId = clientId,
                    clientSecret = clientSecret,
                    redirectUri = redirectUri
                )

                result.onSuccess { token ->
                    // Fetch user info to store username
                    val userResult = authRepository.getUserInfo()
                    userResult.onSuccess {
                        _loginState.value = LoginState.Success
                    }.onFailure { error ->
                        _loginState.value = LoginState.Error(error.message ?: "Failed to get user info")
                    }
                }.onFailure { error ->
                    _loginState.value = LoginState.Error(error.message ?: "Authentication failed")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
