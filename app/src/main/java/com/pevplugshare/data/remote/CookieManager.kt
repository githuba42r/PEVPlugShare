package com.pevplugshare.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Cookie

class CookieManager(context: Context) {
    companion object {
        private const val PREFS_NAME = "osmand_cookies"
        private const val KEY_COOKIES_PREFIX = "cookies_"
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
