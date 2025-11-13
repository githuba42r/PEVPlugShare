package com.a42r.pevplugshare.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {
    companion object {
        private const val PREFS_NAME = "osmand_auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_AUTH_TYPE = "auth_type"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_SELECTED_CATEGORY = "selected_category"
        private const val KEY_OPENROUTER_API_KEY = "openrouter_api_key"
        private const val KEY_SELECTED_AI_MODEL = "selected_ai_model"
        private const val KEY_MAP_ZOOM_LEVEL = "map_zoom_level"
        private const val DEFAULT_MAP_ZOOM = 18.0f
    }

    enum class AuthType {
        OAUTH,
        WEB_API
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAccessToken(token: String, expirySeconds: Int? = null) {
        encryptedPrefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, token)
            if (expirySeconds != null) {
                putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + (expirySeconds * 1000))
            }
            apply()
        }
    }

    fun saveRefreshToken(token: String) {
        encryptedPrefs.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun saveUserInfo(userId: String, username: String) {
        encryptedPrefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            apply()
        }
    }

    fun saveWebApiCredentials(username: String, password: String, rememberMe: Boolean = false) {
        encryptedPrefs.edit().apply {
            putString(KEY_USERNAME, username)
            if (rememberMe) {
                putString(KEY_PASSWORD, password)
                putBoolean(KEY_REMEMBER_ME, true)
            } else {
                remove(KEY_PASSWORD)
                putBoolean(KEY_REMEMBER_ME, false)
            }
            putString(KEY_AUTH_TYPE, AuthType.WEB_API.name)
            apply()
        }
    }

    fun isRememberMeEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_REMEMBER_ME, false)
    }

    fun hasStoredCredentials(): Boolean {
        return !getUsername().isNullOrEmpty() && !getPassword().isNullOrEmpty()
    }

    fun clearStoredCredentials() {
        encryptedPrefs.edit().apply {
            remove(KEY_USERNAME)
            remove(KEY_PASSWORD)
            remove(KEY_REMEMBER_ME)
            apply()
        }
    }

    fun getPassword(): String? = encryptedPrefs.getString(KEY_PASSWORD, null)

    fun getAuthType(): AuthType {
        val type = encryptedPrefs.getString(KEY_AUTH_TYPE, AuthType.OAUTH.name)
        return AuthType.valueOf(type ?: AuthType.OAUTH.name)
    }

    fun getAccessToken(): String? = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)

    fun getUserId(): String? = encryptedPrefs.getString(KEY_USER_ID, null)

    fun getUsername(): String? = encryptedPrefs.getString(KEY_USERNAME, null)

    fun isTokenExpired(): Boolean {
        val expiry = encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0)
        return expiry > 0 && System.currentTimeMillis() > expiry
    }

    fun isLoggedIn(): Boolean {
        return !getAccessToken().isNullOrEmpty()
    }

    fun saveOpenRouterApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_OPENROUTER_API_KEY, apiKey).apply()
    }

    fun getOpenRouterApiKey(): String? {
        return encryptedPrefs.getString(KEY_OPENROUTER_API_KEY, null)
    }
    
    fun saveSelectedAiModel(modelId: String) {
        encryptedPrefs.edit().putString(KEY_SELECTED_AI_MODEL, modelId).apply()
    }
    
    fun getSelectedAiModel(): String? {
        return encryptedPrefs.getString(KEY_SELECTED_AI_MODEL, null)
    }

    fun logout() {
        encryptedPrefs.edit().clear().apply()
    }

    fun getAuthHeader(): String? {
        val token = getAccessToken() ?: return null
        return "Bearer $token"
    }

    fun saveSelectedCategory(category: String) {
        encryptedPrefs.edit().putString(KEY_SELECTED_CATEGORY, category).apply()
    }

    fun getSelectedCategory(): String? {
        return encryptedPrefs.getString(KEY_SELECTED_CATEGORY, null)
    }
    
    fun saveMapZoomLevel(zoomLevel: Float) {
        encryptedPrefs.edit().putFloat(KEY_MAP_ZOOM_LEVEL, zoomLevel).apply()
    }
    
    fun getMapZoomLevel(): Float {
        return encryptedPrefs.getFloat(KEY_MAP_ZOOM_LEVEL, DEFAULT_MAP_ZOOM)
    }

    fun hasAiApiKeyConfigured(): Boolean {
        return !getOpenRouterApiKey().isNullOrEmpty()
    }

    fun hasAiModelSelected(): Boolean {
        return !getSelectedAiModel().isNullOrEmpty()
    }

    fun isAiAnalysisEnabled(): Boolean {
        return hasAiApiKeyConfigured() && hasAiModelSelected()
    }
}
