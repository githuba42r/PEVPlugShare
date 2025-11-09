package com.pevplugshare.data.remote;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010%\n\u0002\u0010\u000e\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0007\u0018\u0000 \u00192\u00020\u0001:\u0001\u0019B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\u0010\u001a\u00020\u0011J\u000e\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\rJ\u0014\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000e2\u0006\u0010\u0014\u001a\u00020\rJ\u001c\u0010\u0016\u001a\u00020\u00112\u0006\u0010\u0014\u001a\u00020\r2\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000eJ\f\u0010\u0018\u001a\u00020\u0013*\u00020\u000fH\u0002R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R \u0010\u000b\u001a\u0014\u0012\u0004\u0012\u00020\r\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000f0\u000e0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001a"}, d2 = {"Lcom/pevplugshare/data/remote/CookieManager;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "encryptedPrefs", "Landroid/content/SharedPreferences;", "gson", "Lcom/google/gson/Gson;", "masterKey", "Landroidx/security/crypto/MasterKey;", "memoryCache", "", "", "", "Lokhttp3/Cookie;", "clearCookies", "", "hasCookies", "", "host", "loadCookies", "saveCookies", "cookies", "hasExpired", "Companion", "app_debug"})
public final class CookieManager {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREFS_NAME = "osmand_cookies";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_COOKIES_PREFIX = "cookies_";
    @org.jetbrains.annotations.NotNull()
    private final androidx.security.crypto.MasterKey masterKey = null;
    @org.jetbrains.annotations.NotNull()
    private final android.content.SharedPreferences encryptedPrefs = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.gson.Gson gson = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.util.List<okhttp3.Cookie>> memoryCache = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.data.remote.CookieManager.Companion Companion = null;
    
    public CookieManager(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    public final void saveCookies(@org.jetbrains.annotations.NotNull()
    java.lang.String host, @org.jetbrains.annotations.NotNull()
    java.util.List<okhttp3.Cookie> cookies) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<okhttp3.Cookie> loadCookies(@org.jetbrains.annotations.NotNull()
    java.lang.String host) {
        return null;
    }
    
    public final void clearCookies() {
    }
    
    public final boolean hasCookies(@org.jetbrains.annotations.NotNull()
    java.lang.String host) {
        return false;
    }
    
    private final boolean hasExpired(okhttp3.Cookie $this$hasExpired) {
        return false;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/pevplugshare/data/remote/CookieManager$Companion;", "", "()V", "KEY_COOKIES_PREFIX", "", "PREFS_NAME", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}