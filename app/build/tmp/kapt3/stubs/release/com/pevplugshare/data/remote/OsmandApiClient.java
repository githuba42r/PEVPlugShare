package com.pevplugshare.data.remote;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000N\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0006\u0010\u0018\u001a\u00020\u0019J\u0006\u0010\u001a\u001a\u00020\u0016R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0015\u001a\n \u0017*\u0004\u0018\u00010\u00160\u0016X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001b"}, d2 = {"Lcom/pevplugshare/data/remote/OsmandApiClient;", "", "tokenManager", "Lcom/pevplugshare/data/remote/TokenManager;", "cookieManager", "Lcom/pevplugshare/data/remote/CookieManager;", "(Lcom/pevplugshare/data/remote/TokenManager;Lcom/pevplugshare/data/remote/CookieManager;)V", "api", "Lcom/pevplugshare/data/api/OsmandApiService;", "getApi", "()Lcom/pevplugshare/data/api/OsmandApiService;", "authInterceptor", "Lokhttp3/Interceptor;", "cookieJar", "Lokhttp3/CookieJar;", "gson", "Lcom/google/gson/Gson;", "httpClient", "Lokhttp3/OkHttpClient;", "loggingInterceptor", "Lokhttp3/logging/HttpLoggingInterceptor;", "retrofit", "Lretrofit2/Retrofit;", "kotlin.jvm.PlatformType", "clearCookies", "", "getWebApiRetrofit", "app_release"})
public final class OsmandApiClient {
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.remote.TokenManager tokenManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.remote.CookieManager cookieManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.gson.Gson gson = null;
    @org.jetbrains.annotations.NotNull()
    private final okhttp3.CookieJar cookieJar = null;
    @org.jetbrains.annotations.NotNull()
    private final okhttp3.logging.HttpLoggingInterceptor loggingInterceptor = null;
    @org.jetbrains.annotations.NotNull()
    private final okhttp3.Interceptor authInterceptor = null;
    @org.jetbrains.annotations.NotNull()
    private final okhttp3.OkHttpClient httpClient = null;
    private final retrofit2.Retrofit retrofit = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.api.OsmandApiService api = null;
    
    public OsmandApiClient(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.remote.TokenManager tokenManager, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.remote.CookieManager cookieManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.api.OsmandApiService getApi() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final retrofit2.Retrofit getWebApiRetrofit() {
        return null;
    }
    
    public final void clearCookies() {
    }
}