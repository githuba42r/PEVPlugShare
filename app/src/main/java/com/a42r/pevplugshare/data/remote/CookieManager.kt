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

class CookieManager(context: Context) {
    companion object {
        private const val TAG = "CookieManager"
        private const val PREFS_NAME = "osmand_cookies"
        private const val MASTER_KEY_ALIAS = "_androidx_security_master_key_"
        private const val KEY_COOKIES_PREFIX = "cookies_"
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

    private val gson = Gson()
    private val memoryCache = mutableMapOf<String, List<Cookie>>()

    fun saveCookies(host: String, cookies: List<Cookie>) {
        val serializableCookies = cookies.map { SerializableCookie(it) }
        val json = gson.toJson(serializableCookies)

        encryptedPrefs.edit()
            .putString(KEY_COOKIES_PREFIX + host, json)
            .apply()

        memoryCache[host] = cookies
    }

    fun loadCookies(host: String): List<Cookie> {
        memoryCache[host]?.let { return it.filter { !it.hasExpired() } }

        val json = encryptedPrefs.getString(KEY_COOKIES_PREFIX + host, null) ?: return emptyList()

        return try {
            val type = object : TypeToken<List<SerializableCookie>>() {}.type
            val serializableCookies: List<SerializableCookie> = gson.fromJson(json, type)
            val cookies = serializableCookies.mapNotNull { it.toCookie() }
                .filter { !it.hasExpired() }

            memoryCache[host] = cookies
            cookies
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearCookies() {
        memoryCache.clear()
        encryptedPrefs.edit().clear().apply()
    }

    fun hasCookies(host: String): Boolean {
        return loadCookies(host).isNotEmpty()
    }

    private fun Cookie.hasExpired(): Boolean {
        return expiresAt < System.currentTimeMillis()
    }
}

data class SerializableCookie(
    val name: String,
    val value: String,
    val expiresAt: Long,
    val domain: String,
    val path: String,
    val secure: Boolean,
    val httpOnly: Boolean,
    val hostOnly: Boolean
) {
    constructor(cookie: Cookie) : this(
        name = cookie.name,
        value = cookie.value,
        expiresAt = cookie.expiresAt,
        domain = cookie.domain,
        path = cookie.path,
        secure = cookie.secure,
        httpOnly = cookie.httpOnly,
        hostOnly = cookie.hostOnly
    )

    fun toCookie(): Cookie? {
        return try {
            Cookie.Builder()
                .name(name)
                .value(value)
                .expiresAt(expiresAt)
                .apply {
                    if (hostOnly) {
                        hostOnlyDomain(domain)
                    } else {
                        domain(domain)
                    }
                }
                .path(path)
                .apply {
                    if (secure) secure()
                    if (httpOnly) httpOnly()
                }
                .build()
        } catch (e: Exception) {
            null
        }
    }
}
