package com.pevplugshare.data.remote;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\u0014\u001a\u00020\u0012R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0011\u001a\n \u0013*\u0004\u0018\u00010\u00120\u0012X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0015"}, d2 = {"Lcom/pevplugshare/data/remote/OsmandApiClient;", "", "tokenManager", "Lcom/pevplugshare/data/remote/TokenManager;", "(Lcom/pevplugshare/data/remote/TokenManager;)V", "api", "Lcom/pevplugshare/data/api/OsmandApiService;", "getApi", "()Lcom/pevplugshare/data/api/OsmandApiService;", "authInterceptor", "Lokhttp3/Interceptor;", "gson", "Lcom/google/gson/Gson;", "httpClient", "Lokhttp3/OkHttpClient;", "loggingInterceptor", "Lokhttp3/logging/HttpLoggingInterceptor;", "retrofit", "Lretrofit2/Retrofit;", "kotlin.jvm.PlatformType", "getOAuthRetrofit", "app_debug"})
public final class OsmandApiClient {
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.remote.TokenManager tokenManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.gson.Gson gson = null;
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
    com.pevplugshare.data.remote.TokenManager tokenManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.api.OsmandApiService getApi() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final retrofit2.Retrofit getOAuthRetrofit() {
        return null;
    }
}