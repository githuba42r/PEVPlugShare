package com.a42r.pevplugshare.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.service.OpenRouterOAuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for OpenRouter OAuth flow
 */
@HiltViewModel
class OpenRouterOAuthViewModel @Inject constructor(
    application: Application,
    private val oauthService: OpenRouterOAuthService
) : AndroidViewModel(application) {

    // Current PKCE state (stored in memory during OAuth flow)
    private var currentPKCEState: OpenRouterOAuthService.PKCEState? = null

    // OAuth flow state
    private val _oauthState = MutableLiveData<OAuthState>()
    val oauthState: LiveData<OAuthState> = _oauthState

    // API key result
    private val _apiKey = MutableLiveData<String?>()
    val apiKey: LiveData<String?> = _apiKey

    sealed class OAuthState {
        object Idle : OAuthState()
        object Registering : OAuthState()
        data class ReadyToAuthorize(val authUrl: String) : OAuthState()
        object ExchangingCode : OAuthState()
        data class Success(val apiKey: String) : OAuthState()
        data class Error(val message: String) : OAuthState()
    }

    /**
     * Start OAuth flow - register session and prepare authorization URL
     */
    fun startOAuthFlow() {
        _oauthState.value = OAuthState.Registering

        viewModelScope.launch {
            try {
                // Register session with callback service
                val pkceState = oauthService.registerOAuthSession()
                currentPKCEState = pkceState

                // Build authorization URL
                val authUrl = oauthService.buildAuthorizationUrl(pkceState)

                // Ready to open browser
                _oauthState.value = OAuthState.ReadyToAuthorize(authUrl)
            } catch (e: Exception) {
                _oauthState.value = OAuthState.Error("Registration failed: ${e.message}")
            }
        }
    }

    /**
     * Open authorization URL in Custom Tab
     */
    fun openAuthorizationUrl(authUrl: String) {
        try {
            val intent = CustomTabsIntent.Builder().build()
            intent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.launchUrl(getApplication(), Uri.parse(authUrl))
        } catch (e: Exception) {
            _oauthState.value = OAuthState.Error("Failed to open browser: ${e.message}")
        }
    }

    /**
     * Handle deep link callback with authorization code
     */
    fun handleCallback(code: String, provider: String) {
        val pkceState = currentPKCEState
        if (pkceState == null) {
            _oauthState.value = OAuthState.Error("No active OAuth session")
            return
        }

        if (provider != "openrouter") {
            _oauthState.value = OAuthState.Error("Unexpected provider: $provider")
            return
        }

        _oauthState.value = OAuthState.ExchangingCode

        viewModelScope.launch {
            try {
                // Exchange code for API key
                val apiKey = oauthService.exchangeCodeForKey(code, pkceState.codeVerifier)

                // Clear PKCE state
                currentPKCEState = null

                // Success!
                _apiKey.value = apiKey
                _oauthState.value = OAuthState.Success(apiKey)
            } catch (e: Exception) {
                currentPKCEState = null
                _oauthState.value = OAuthState.Error("Token exchange failed: ${e.message}")
            }
        }
    }

    /**
     * Handle OAuth error callback
     */
    fun handleError(error: String, errorDescription: String?) {
        currentPKCEState = null
        val message = errorDescription ?: error
        _oauthState.value = OAuthState.Error("OAuth error: $message")
    }

    /**
     * Reset OAuth state
     */
    fun reset() {
        currentPKCEState = null
        _oauthState.value = OAuthState.Idle
        _apiKey.value = null
    }
}
