package com.a42r.pevplugshare.ui.auth

import androidx.lifecycle.ViewModel
import com.a42r.pevplugshare.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    suspend fun hasSessionCookie(): Boolean {
        return authRepository.hasSessionToken()
    }

    suspend fun hasStoredCredentials(): Boolean {
        return authRepository.hasStoredCredentials()
    }

    suspend fun validateSessionInBackground(): Boolean {
        return try {
            // Try to load categories to validate the session
            val result = authRepository.getFavoritesCategories()
            result.isSuccess && result.getOrNull()?.isNotEmpty() == true
        } catch (e: Exception) {
            android.util.Log.e("SplashViewModel", "Session validation failed: ${e.message}")
            false
        }
    }

    suspend fun attemptAutoLoginInBackground(): Boolean {
        return try {
            val result = authRepository.autoLogin()
            
            result.onSuccess {
                android.util.Log.d("SplashViewModel", "Auto-login successful, validating session...")
                // Verify the login by fetching categories
                val verifyResult = authRepository.getFavoritesCategories()
                verifyResult.isSuccess
            }.onFailure { error ->
                android.util.Log.e("SplashViewModel", "Auto-login failed: ${error.message}")
            }
            
            result.isSuccess
        } catch (e: Exception) {
            android.util.Log.e("SplashViewModel", "Auto-login error: ${e.message}")
            false
        }
    }
}
