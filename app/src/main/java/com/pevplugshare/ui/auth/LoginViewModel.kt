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
                
                result.onSuccess {
                    // Test fetching categories after successful login
                    android.util.Log.d("LoginViewModel", "Auto-login successful, fetching favorites categories...")
                    authRepository.getFavoritesCategories()
                    
                    _loginState.value = LoginState.Success
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
                
                result.onSuccess {
                    // Test fetching categories after successful login
                    android.util.Log.d("LoginViewModel", "Login successful, fetching favorites categories...")
                    authRepository.getFavoritesCategories()
                    
                    _loginState.value = LoginState.Success
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
