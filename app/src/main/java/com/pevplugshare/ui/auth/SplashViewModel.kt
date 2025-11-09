package com.pevplugshare.ui.auth

import androidx.lifecycle.ViewModel
import com.pevplugshare.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    suspend fun isUserLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }

    suspend fun validateSession(): Boolean {
        // Try to load categories to validate the session
        val result = authRepository.getFavoritesCategories()
        return result.isSuccess && result.getOrNull()?.isNotEmpty() == true
    }
}
