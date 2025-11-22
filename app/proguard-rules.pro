# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep native methods - required for JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep NativeSecrets class and methods
-keep class com.a42r.pevplugshare.util.NativeSecrets {
    public *;
}

# Keep OAuthSignature utility
-keep class com.a42r.pevplugshare.util.OAuthSignature {
    public *;
}

# Keep OAuth service classes
-keep class com.a42r.pevplugshare.service.OpenRouterOAuthService {
    public *;
}
-keep class com.a42r.pevplugshare.service.OpenRouterOAuthService$PKCEState {
    *;
}

# Keep ViewModels for Hilt injection
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}

# Hilt rules
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel {
    <init>(...);
}

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# OkHttp (if used)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson (if used)
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.Dao

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Optimize
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose

# Preserve line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
