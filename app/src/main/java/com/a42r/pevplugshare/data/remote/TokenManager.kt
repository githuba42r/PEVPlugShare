package com.a42r.pevplugshare.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Cookie
import java.io.File
import java.security.GeneralSecurityException
import java.security.KeyStore

class TokenManager(context: Context) {
    companion object {
        private const val TAG = "TokenManager"
        private const val PREFS_NAME = "osmand_auth_prefs"
        private const val MASTER_KEY_ALIAS = "_androidx_security_master_key_"
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
        private const val KEY_OSMAND_SYNC_ON_UPDATE = "osmand_sync_on_update"
        private const val KEY_OSMAND_VERIFY_DESCRIPTION = "osmand_verify_description"
        private const val KEY_OSMAND_LOCATION_MATCH_DISTANCE = "osmand_location_match_distance"
        private const val DEFAULT_OSMAND_MATCH_DISTANCE = 10 // Default 10 meters
        private const val KEY_OSMAND_CONNECTED = "osmand_connected"
        private const val KEY_OSMAND_CONNECTED_USERNAME = "osmand_connected_username"
        private const val KEY_PKCE_CODE_VERIFIER = "pkce_code_verifier"

        private const val KEY_SERVER_JWT = "server_jwt"
        private const val KEY_SERVER_USER_ID = "server_user_id"
        private const val KEY_SERVER_EMAIL = "server_email"
        private const val KEY_SERVER_DISPLAY_NAME = "server_display_name"
        private const val KEY_SERVER_CONNECTED = "server_connected"
        private const val KEY_SERVER_URL = "server_url"
    }

    enum class AuthType {
        OAUTH,
        WEB_API
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = createEncryptedPreferences(context)

    /**
     * Creates EncryptedSharedPreferences with error handling for reinstall scenarios.
     * If decryption fails (e.g., after app reinstall), clears corrupted data and recreates.
     */
    private fun createEncryptedPreferences(context: Context): SharedPreferences {
        return try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences, likely due to app reinstall. Clearing corrupted data.", e)

            // Delete the corrupted SharedPreferences file
            val prefsFile = File(
                "${context.applicationInfo.dataDir}/shared_prefs/$PREFS_NAME.xml"
            )
            if (prefsFile.exists()) {
                prefsFile.delete()
                Log.d(TAG, "Deleted corrupted preferences file")
            }

            // Delete the corrupted master key from Android Keystore
            try {
                val keyStore = KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)
                if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                    keyStore.deleteEntry(MASTER_KEY_ALIAS)
                    Log.d(TAG, "Deleted corrupted master key from keystore")
                }
            } catch (e2: Exception) {
                Log.w(TAG, "Failed to delete master key from keystore", e2)
            }

            // Recreate the master key and EncryptedSharedPreferences
            val newMasterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                newMasterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

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

    fun clearOpenRouterApiKey() {
        encryptedPrefs.edit().remove(KEY_OPENROUTER_API_KEY).apply()
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

    // OsmAnd Sync Preferences
    fun setSyncToOsmandOnUpdate(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_OSMAND_SYNC_ON_UPDATE, enabled).apply()
    }

    fun getSyncToOsmandOnUpdate(): Boolean {
        return encryptedPrefs.getBoolean(KEY_OSMAND_SYNC_ON_UPDATE, true) // Default true = prompt
    }

    fun setVerifyDescriptionBeforeSync(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_OSMAND_VERIFY_DESCRIPTION, enabled).apply()
    }

    fun getVerifyDescriptionBeforeSync(): Boolean {
        return encryptedPrefs.getBoolean(KEY_OSMAND_VERIFY_DESCRIPTION, true) // Default true = verify
    }

    fun setOsmandLocationMatchDistance(meters: Int) {
        encryptedPrefs.edit().putInt(KEY_OSMAND_LOCATION_MATCH_DISTANCE, meters).apply()
    }

    fun getOsmandLocationMatchDistance(): Int {
        return encryptedPrefs.getInt(KEY_OSMAND_LOCATION_MATCH_DISTANCE, DEFAULT_OSMAND_MATCH_DISTANCE)
    }

    // OSMand connection state management
    fun setOsmandConnected(connected: Boolean, username: String? = null) {
        encryptedPrefs.edit().apply {
            putBoolean(KEY_OSMAND_CONNECTED, connected)
            if (connected && username != null) {
                putString(KEY_OSMAND_CONNECTED_USERNAME, username)
            } else if (!connected) {
                remove(KEY_OSMAND_CONNECTED_USERNAME)
            }
            apply()
        }
    }

    fun isOsmandConnected(): Boolean {
        return encryptedPrefs.getBoolean(KEY_OSMAND_CONNECTED, false)
    }

    fun getOsmandConnectedUsername(): String? {
        return encryptedPrefs.getString(KEY_OSMAND_CONNECTED_USERNAME, null)
    }

    fun disconnectOsmand() {
        encryptedPrefs.edit().apply {
            putBoolean(KEY_OSMAND_CONNECTED, false)
            remove(KEY_OSMAND_CONNECTED_USERNAME)
            apply()
        }
    }

    // PKCE OAuth flow state management
    fun savePkceCodeVerifier(codeVerifier: String) {
        encryptedPrefs.edit().putString(KEY_PKCE_CODE_VERIFIER, codeVerifier).apply()
    }

    fun getPkceCodeVerifier(): String? {
        return encryptedPrefs.getString(KEY_PKCE_CODE_VERIFIER, null)
    }

    fun clearPkceCodeVerifier() {
        encryptedPrefs.edit().remove(KEY_PKCE_CODE_VERIFIER).apply()
    }

    // Central server integration
    fun saveServerJwt(token: String) { encryptedPrefs.edit().putString(KEY_SERVER_JWT, token).apply() }
    fun getServerJwt(): String? = encryptedPrefs.getString(KEY_SERVER_JWT, null)

    fun saveServerUserInfo(userId: String, email: String, displayName: String) {
        encryptedPrefs.edit()
            .putString(KEY_SERVER_USER_ID, userId)
            .putString(KEY_SERVER_EMAIL, email)
            .putString(KEY_SERVER_DISPLAY_NAME, displayName)
            .putBoolean(KEY_SERVER_CONNECTED, true)
            .apply()
    }

    fun getServerUserId(): String? = encryptedPrefs.getString(KEY_SERVER_USER_ID, null)
    fun getServerEmail(): String? = encryptedPrefs.getString(KEY_SERVER_EMAIL, null)
    fun getServerDisplayName(): String? = encryptedPrefs.getString(KEY_SERVER_DISPLAY_NAME, null)
    fun isServerConnected(): Boolean = encryptedPrefs.getBoolean(KEY_SERVER_CONNECTED, false)

    fun saveServerUrl(url: String) { encryptedPrefs.edit().putString(KEY_SERVER_URL, url).apply() }
    fun getServerUrl(): String? = encryptedPrefs.getString(KEY_SERVER_URL, null)

    fun disconnectServer() {
        encryptedPrefs.edit()
            .remove(KEY_SERVER_JWT).remove(KEY_SERVER_USER_ID).remove(KEY_SERVER_EMAIL)
            .remove(KEY_SERVER_DISPLAY_NAME).remove(KEY_SERVER_URL)
            .putBoolean(KEY_SERVER_CONNECTED, false)
            .apply()
    }
}
