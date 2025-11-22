#include <jni.h>
#include <string>

/**
 * Native secrets storage - secrets injected at build time
 *
 * Secrets are NOT hardcoded in this file. They are:
 * 1. Read from secrets.properties (not in git)
 * 2. Passed as compile definitions via CMake
 * 3. Injected during build process
 *
 * To set up secrets:
 * 1. Copy secrets.properties.template to secrets.properties
 * 2. Fill in your actual secret values
 * 3. Build the app - secrets will be injected automatically
 *
 * Security notes:
 * - secrets.properties is in .gitignore and will never be committed
 * - Secrets are still extractable from .so file (defense in depth)
 * - ProGuard/R8 obfuscates Java code
 * - Debug symbols stripped from release builds
 */

// These macros are defined at build time by CMake
// They come from secrets.properties which is NOT in git
#ifndef OAUTH_DEV_CALLBACK_URL
#define OAUTH_DEV_CALLBACK_URL "https://example.com"
#endif

#ifndef OAUTH_DEV_SHARED_SECRET
#define OAUTH_DEV_SHARED_SECRET "MISSING_SECRET"
#endif

#ifndef OAUTH_DEV_DEEP_LINK_SCHEME
#define OAUTH_DEV_DEEP_LINK_SCHEME "yourapp://oauth/callback"
#endif

#ifndef OAUTH_PROD_CALLBACK_URL
#define OAUTH_PROD_CALLBACK_URL "https://example.com"
#endif

#ifndef OAUTH_PROD_SHARED_SECRET
#define OAUTH_PROD_SHARED_SECRET "MISSING_SECRET"
#endif

#ifndef OAUTH_PROD_DEEP_LINK_SCHEME
#define OAUTH_PROD_DEEP_LINK_SCHEME "yourapp://oauth/callback"
#endif

extern "C" JNIEXPORT jstring JNICALL
Java_com_a42r_pevplugshare_util_NativeSecrets_getOAuthCallbackUrl(
        JNIEnv* env,
        jobject /* this */) {
    #ifdef NDEBUG
        // Production
        return env->NewStringUTF(OAUTH_PROD_CALLBACK_URL);
    #else
        // Development
        return env->NewStringUTF(OAUTH_DEV_CALLBACK_URL);
    #endif
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_a42r_pevplugshare_util_NativeSecrets_getOAuthSharedSecret(
        JNIEnv* env,
        jobject /* this */) {
    // WARNING: This is still extractable from the .so file
    // For maximum security, consider:
    // 1. Server-side key exchange
    // 2. Runtime decryption with device-specific keys
    // 3. Key derived from multiple sources (device ID, timestamp, etc.)

    #ifdef NDEBUG
        // Production secret - injected at build time
        return env->NewStringUTF(OAUTH_PROD_SHARED_SECRET);
    #else
        // Development secret - injected at build time
        return env->NewStringUTF(OAUTH_DEV_SHARED_SECRET);
    #endif
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_a42r_pevplugshare_util_NativeSecrets_getDeepLinkScheme(
        JNIEnv* env,
        jobject /* this */) {
    #ifdef NDEBUG
        // Production
        return env->NewStringUTF(OAUTH_PROD_DEEP_LINK_SCHEME);
    #else
        // Development
        return env->NewStringUTF(OAUTH_DEV_DEEP_LINK_SCHEME);
    #endif
}
