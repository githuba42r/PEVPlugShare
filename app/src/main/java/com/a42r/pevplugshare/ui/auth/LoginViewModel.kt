package com.a42r.pevplugshare.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.domain.repository.AuthRepository
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
    private val authRepository: AuthRepository,
    private val tokenManager: com.a42r.pevplugshare.data.remote.TokenManager
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    suspend fun hasStoredCredentials(): Boolean {
        return authRepository.hasStoredCredentials()
    }
    
    suspend fun hasValidSessionToken(): Boolean {
        return authRepository.hasSessionToken()
    }

    fun attemptAutoLogin() {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val result = authRepository.autoLogin()
                
                result.onSuccess { userId ->
                    // Verify login by fetching categories
                    android.util.Log.d("LoginViewModel", "Auto-login successful, verifying session...")
                    val verifyResult = authRepository.getFavoritesCategories()
                    
                    if (verifyResult.isSuccess) {
                        android.util.Log.d("LoginViewModel", "Session verified, proceeding to main activity")
                        // Get username and mark OSMand as connected
                        val username = tokenManager.getUsername() ?: userId
                        tokenManager.setOsmandConnected(true, username)
                        _loginState.value = LoginState.Success
                    } else {
                        android.util.Log.e("LoginViewModel", "Failed to verify session: ${verifyResult.exceptionOrNull()?.message}")
                        _loginState.value = LoginState.Error("Failed to verify session")
                    }
                }.onFailure { error ->
                    _loginState.value = LoginState.Error(error.message ?: "Auto-login failed")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Auto-login failed")
            }
        }
    }

    fun loginWithCredentials(username: String, password: String, rememberMe: Boolean) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val result = authRepository.loginWithWebApi(username, password, rememberMe)
                
                result.onSuccess { userId ->
                    // Verify login by fetching categories
                    android.util.Log.d("LoginViewModel", "Login successful, verifying session...")
                    val verifyResult = authRepository.getFavoritesCategories()
                    
                    if (verifyResult.isSuccess) {
                        android.util.Log.d("LoginViewModel", "Session verified, proceeding to main activity")
                        // Mark OSMand as connected
                        tokenManager.setOsmandConnected(true, username)
                        _loginState.value = LoginState.Success
                    } else {
                        android.util.Log.e("LoginViewModel", "Failed to verify session: ${verifyResult.exceptionOrNull()?.message}")
                        _loginState.value = LoginState.Error("Failed to verify session")
                    }
                }.onFailure { error ->
                    _loginState.value = LoginState.Error(error.message ?: "Login failed")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun getFavoritesCategories(onResult: (List<String>) -> Unit) {
        viewModelScope.launch {
            try {
                val result = authRepository.getFavoritesCategories()
                result.onSuccess { categories ->
                    onResult(categories)
                }.onFailure {
                    onResult(emptyList())
                }
            } catch (e: Exception) {
                onResult(emptyList())
            }
        }
    }
}
