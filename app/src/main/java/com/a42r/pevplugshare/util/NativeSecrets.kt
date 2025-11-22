package com.a42r.pevplugshare.util

/**
 * Access secrets stored in native code (C++)
 *
 * This provides better security than BuildConfig strings:
 * - Secrets are in native .so library, not DEX
 * - Requires reverse engineering native code
 * - Can be further obfuscated at native level
 *
 * Security Notes:
 * - Still extractable with tools like `strings libpevplugshare.so`
 * - For maximum security, use server-side key exchange
 * - Consider runtime decryption with device-specific keys
 * - Always use ProGuard/R8 in release builds
 *
 * Usage:
 * ```kotlin
 * val secret = NativeSecrets.getOAuthSharedSecret()
 * val url = NativeSecrets.getOAuthCallbackUrl()
 * ```
 */
object NativeSecrets {

    init {
        System.loadLibrary("pevplugshare")
    }

    /**
     * Get OAuth callback service URL
     * @return Callback service URL (dev or prod based on build type)
     */
    external fun getOAuthCallbackUrl(): String

    /**
     * Get OAuth shared secret for HMAC signatures
     * @return Shared secret (dev or prod based on build type)
     */
    external fun getOAuthSharedSecret(): String

    /**
     * Get deep link scheme for OAuth callbacks
     * @return Deep link URL scheme
     */
    external fun getDeepLinkScheme(): String
}
