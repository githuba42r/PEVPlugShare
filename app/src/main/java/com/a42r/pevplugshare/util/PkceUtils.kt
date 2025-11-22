package com.a42r.pevplugshare.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Utility class for OAuth 2.0 PKCE (Proof Key for Code Exchange) flow
 * Implements RFC 7636 standards for generating code verifier and challenge
 */
object PkceUtils {

    /**
     * Generate a cryptographically random code verifier
     *
     * Per RFC 7636:
     * - Length: 43-128 characters
     * - Characters: A-Z, a-z, 0-9, and the punctuation characters -._~ (unreserved characters)
     *
     * @return A URL-safe random string suitable for use as a code verifier
     */
    fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(32) // 32 bytes = 256 bits of entropy
        secureRandom.nextBytes(bytes)

        // Base64 URL-safe encoding without padding
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /**
     * Generate a code challenge from a code verifier using SHA-256
     *
     * Per RFC 7636:
     * - code_challenge = BASE64URL(SHA256(ASCII(code_verifier)))
     *
     * @param codeVerifier The code verifier to hash
     * @return A URL-safe base64-encoded SHA-256 hash of the code verifier
     */
    fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(bytes)

        // Base64 URL-safe encoding without padding
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
