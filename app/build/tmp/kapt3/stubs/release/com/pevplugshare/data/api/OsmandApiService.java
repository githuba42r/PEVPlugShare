package com.pevplugshare.data.api;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0007\bf\u0018\u0000 #2\u00020\u0001:\u0001#J,\u0010\u0002\u001a\u00020\u00032\b\b\u0001\u0010\u0004\u001a\u00020\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u00062\b\b\u0001\u0010\u0007\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\bJ\"\u0010\t\u001a\u00020\n2\b\b\u0001\u0010\u000b\u001a\u00020\u00062\b\b\u0001\u0010\u0007\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\fJ@\u0010\r\u001a\u00020\u000e2\b\b\u0003\u0010\u000f\u001a\u00020\u00062\b\b\u0001\u0010\u0010\u001a\u00020\u00062\b\b\u0001\u0010\u0011\u001a\u00020\u00062\b\b\u0001\u0010\u0012\u001a\u00020\u00062\b\b\u0001\u0010\u0013\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\u0014J\u0018\u0010\u0015\u001a\u00020\u00162\b\b\u0001\u0010\u0007\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\u0017J\u0018\u0010\u0018\u001a\u00020\u00192\b\b\u0001\u0010\u0007\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\u0017J\"\u0010\u001a\u001a\u00020\u00162\b\b\u0001\u0010\u001b\u001a\u00020\u00062\b\b\u0001\u0010\u0007\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\fJ\u0018\u0010\u001c\u001a\u00020\u001d2\b\b\u0001\u0010\u0007\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\u0017J6\u0010\u001e\u001a\u00020\u000e2\b\b\u0003\u0010\u000f\u001a\u00020\u00062\b\b\u0001\u0010\u001f\u001a\u00020\u00062\b\b\u0001\u0010\u0011\u001a\u00020\u00062\b\b\u0001\u0010\u0012\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010 J6\u0010!\u001a\u00020\u00032\b\b\u0001\u0010\u000b\u001a\u00020\u00062\b\b\u0001\u0010\u0004\u001a\u00020\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u00062\b\b\u0001\u0010\u0007\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\"\u00a8\u0006$"}, d2 = {"Lcom/pevplugshare/data/api/OsmandApiService;", "", "createMarker", "Lcom/pevplugshare/data/api/OsmandMarker;", "marker", "category", "", "authorization", "(Lcom/pevplugshare/data/api/OsmandMarker;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteMarker", "", "markerId", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAccessToken", "Lcom/pevplugshare/data/api/OsmandAuthResponse;", "grantType", "code", "clientId", "clientSecret", "redirectUri", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllFavorites", "Lcom/pevplugshare/data/api/OsmandMarkersResponse;", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getCategories", "Lcom/pevplugshare/data/api/OsmandCategoriesResponse;", "getMarkersByCategory", "categoryName", "getUserInfo", "Lcom/pevplugshare/data/api/OsmandUserInfo;", "refreshAccessToken", "refreshToken", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateMarker", "(Ljava/lang/String;Lcom/pevplugshare/data/api/OsmandMarker;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "app_release"})
public abstract interface OsmandApiService {
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String BASE_URL = "https://api.osmand.net/api/";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String OAUTH_URL = "https://osmand.net/api/";
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.data.api.OsmandApiService.Companion Companion = null;
    
    @retrofit2.http.FormUrlEncoded()
    @retrofit2.http.POST(value = "token")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getAccessToken(@retrofit2.http.Field(value = "grant_type")
    @org.jetbrains.annotations.NotNull()
    java.lang.String grantType, @retrofit2.http.Field(value = "code")
    @org.jetbrains.annotations.NotNull()
    java.lang.String code, @retrofit2.http.Field(value = "client_id")
    @org.jetbrains.annotations.NotNull()
    java.lang.String clientId, @retrofit2.http.Field(value = "client_secret")
    @org.jetbrains.annotations.NotNull()
    java.lang.String clientSecret, @retrofit2.http.Field(value = "redirect_uri")
    @org.jetbrains.annotations.NotNull()
    java.lang.String redirectUri, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pevplugshare.data.api.OsmandAuthResponse> $completion);
    
    @retrofit2.http.FormUrlEncoded()
    @retrofit2.http.POST(value = "token")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object refreshAccessToken(@retrofit2.http.Field(value = "grant_type")
    @org.jetbrains.annotations.NotNull()
    java.lang.String grantType, @retrofit2.http.Field(value = "refresh_token")
    @org.jetbrains.annotations.NotNull()
    java.lang.String refreshToken, @retrofit2.http.Field(value = "client_id")
    @org.jetbrains.annotations.NotNull()
    java.lang.String clientId, @retrofit2.http.Field(value = "client_secret")
    @org.jetbrains.annotations.NotNull()
    java.lang.String clientSecret, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pevplugshare.data.api.OsmandAuthResponse> $completion);
    
    @retrofit2.http.GET(value = "me")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getUserInfo(@retrofit2.http.Header(value = "Authorization")
    @org.jetbrains.annotations.NotNull()
    java.lang.String authorization, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pevplugshare.data.api.OsmandUserInfo> $completion);
    
    @retrofit2.http.GET(value = "v1/favorites/categories")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getCategories(@retrofit2.http.Header(value = "Authorization")
    @org.jetbrains.annotations.NotNull()
    java.lang.String authorization, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pevplugshare.data.api.OsmandCategoriesResponse> $completion);
    
    @retrofit2.http.GET(value = "v1/favorites/category/{category}")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getMarkersByCategory(@retrofit2.http.Path(value = "category")
    @org.jetbrains.annotations.NotNull()
    java.lang.String categoryName, @retrofit2.http.Header(value = "Authorization")
    @org.jetbrains.annotations.NotNull()
    java.lang.String authorization, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pevplugshare.data.api.OsmandMarkersResponse> $completion);
    
    @retrofit2.http.POST(value = "v1/favorites/marker")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object createMarker(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.api.OsmandMarker marker, @retrofit2.http.Query(value = "category")
    @org.jetbrains.annotations.NotNull()
    java.lang.String category, @retrofit2.http.Header(value = "Authorization")
    @org.jetbrains.annotations.NotNull()
    java.lang.String authorization, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pevplugshare.data.api.OsmandMarker> $completion);
    
    @retrofit2.http.PUT(value = "v1/favorites/marker/{id}")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateMarker(@retrofit2.http.Path(value = "id")
    @org.jetbrains.annotations.NotNull()
    java.lang.String markerId, @retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.api.OsmandMarker marker, @retrofit2.http.Query(value = "category")
    @org.jetbrains.annotations.NotNull()
    java.lang.String category, @retrofit2.http.Header(value = "Authorization")
    @org.jetbrains.annotations.NotNull()
    java.lang.String authorization, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pevplugshare.data.api.OsmandMarker> $completion);
    
    @retrofit2.http.DELETE(value = "v1/favorites/marker/{id}")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteMarker(@retrofit2.http.Path(value = "id")
    @org.jetbrains.annotations.NotNull()
    java.lang.String markerId, @retrofit2.http.Header(value = "Authorization")
    @org.jetbrains.annotations.NotNull()
    java.lang.String authorization, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @retrofit2.http.GET(value = "v1/favorites")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getAllFavorites(@retrofit2.http.Header(value = "Authorization")
    @org.jetbrains.annotations.NotNull()
    java.lang.String authorization, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pevplugshare.data.api.OsmandMarkersResponse> $completion);
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/pevplugshare/data/api/OsmandApiService$Companion;", "", "()V", "BASE_URL", "", "OAUTH_URL", "app_release"})
    public static final class Companion {
        @org.jetbrains.annotations.NotNull()
        public static final java.lang.String BASE_URL = "https://api.osmand.net/api/";
        @org.jetbrains.annotations.NotNull()
        public static final java.lang.String OAUTH_URL = "https://osmand.net/api/";
        
        private Companion() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}