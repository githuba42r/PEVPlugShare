package com.pevplugshare.data.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\t\n\u0002\u0010\u0002\n\u0002\b\u0004\u0018\u0000 )2\u00020\u0001:\u0001)B%\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ\u001c\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\r0\fH\u0096@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u000e\u0010\u000fJ*\u0010\u0010\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00120\u00110\f2\u0006\u0010\u0013\u001a\u00020\rH\u0096@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u0014\u0010\u0015J\"\u0010\u0016\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\r0\u00110\fH\u0096@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u0017\u0010\u000fJ(\u0010\u0018\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\r0\u00190\fH\u0096@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u001a\u0010\u000fJ\u000e\u0010\u001b\u001a\u00020\u001cH\u0096@\u00a2\u0006\u0002\u0010\u000fJ\u000e\u0010\u001d\u001a\u00020\u001cH\u0096@\u00a2\u0006\u0002\u0010\u000fJ\u000e\u0010\u001e\u001a\u00020\u001cH\u0096@\u00a2\u0006\u0002\u0010\u000fJ4\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020\r0\f2\u0006\u0010 \u001a\u00020\r2\u0006\u0010!\u001a\u00020\r2\u0006\u0010\"\u001a\u00020\u001cH\u0096@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b#\u0010$J\u000e\u0010%\u001a\u00020&H\u0096@\u00a2\u0006\u0002\u0010\u000fJ\u001c\u0010\'\u001a\b\u0012\u0004\u0012\u00020&0\fH\u0096@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b(\u0010\u000fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006*"}, d2 = {"Lcom/pevplugshare/data/repository/AuthRepositoryImpl;", "Lcom/pevplugshare/domain/repository/AuthRepository;", "apiClient", "Lcom/pevplugshare/data/remote/OsmandApiClient;", "tokenManager", "Lcom/pevplugshare/data/remote/TokenManager;", "context", "Landroid/content/Context;", "cookieManager", "Lcom/pevplugshare/data/remote/CookieManager;", "(Lcom/pevplugshare/data/remote/OsmandApiClient;Lcom/pevplugshare/data/remote/TokenManager;Landroid/content/Context;Lcom/pevplugshare/data/remote/CookieManager;)V", "autoLogin", "Lkotlin/Result;", "", "autoLogin-IoAF18A", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "downloadFavoritesGpx", "", "Lcom/pevplugshare/data/model/Location;", "categoryName", "downloadFavoritesGpx-gIAlu-s", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getFavoritesCategories", "getFavoritesCategories-IoAF18A", "getUserInfo", "Lkotlin/Pair;", "getUserInfo-IoAF18A", "hasSessionToken", "", "hasStoredCredentials", "isLoggedIn", "loginWithWebApi", "username", "password", "rememberMe", "loginWithWebApi-BWLJW6A", "(Ljava/lang/String;Ljava/lang/String;ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "logout", "", "refreshWebApiSession", "refreshWebApiSession-IoAF18A", "Companion", "app_debug"})
public final class AuthRepositoryImpl implements com.pevplugshare.domain.repository.AuthRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.remote.OsmandApiClient apiClient = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.remote.TokenManager tokenManager = null;
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.remote.CookieManager cookieManager = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "OsmAndAuth";
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.data.repository.AuthRepositoryImpl.Companion Companion = null;
    
    public AuthRepositoryImpl(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.remote.OsmandApiClient apiClient, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.remote.TokenManager tokenManager, @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.remote.CookieManager cookieManager) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object hasStoredCredentials(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object hasSessionToken(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object logout(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object isLoggedIn(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/pevplugshare/data/repository/AuthRepositoryImpl$Companion;", "", "()V", "TAG", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}