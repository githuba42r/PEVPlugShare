package com.pevplugshare.data.remote

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

    fun logout() {
        encryptedPrefs.edit().clear().apply()
    }

    fun getAuthHeader(): String? {
        val token = getAccessToken() ?: return null
        return "Bearer $token"
    }
}
