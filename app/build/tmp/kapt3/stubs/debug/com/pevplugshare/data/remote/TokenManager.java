package com.pevplugshare.data.remote;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000J\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0002\b\b\n\u0002\u0010\u000b\n\u0002\b\n\n\u0002\u0010\b\n\u0002\b\u0013\u0018\u0000 72\u00020\u0001:\u000267B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\t\u001a\u00020\nJ\b\u0010\u000b\u001a\u0004\u0018\u00010\fJ\b\u0010\r\u001a\u0004\u0018\u00010\fJ\u0006\u0010\u000e\u001a\u00020\u000fJ\u0006\u0010\u0010\u001a\u00020\u0011J\b\u0010\u0012\u001a\u0004\u0018\u00010\fJ\b\u0010\u0013\u001a\u0004\u0018\u00010\fJ\b\u0010\u0014\u001a\u0004\u0018\u00010\fJ\b\u0010\u0015\u001a\u0004\u0018\u00010\fJ\b\u0010\u0016\u001a\u0004\u0018\u00010\fJ\b\u0010\u0017\u001a\u0004\u0018\u00010\fJ\b\u0010\u0018\u001a\u0004\u0018\u00010\fJ\u0006\u0010\u0019\u001a\u00020\u001aJ\u0006\u0010\u001b\u001a\u00020\u001aJ\u0006\u0010\u001c\u001a\u00020\u001aJ\u0006\u0010\u001d\u001a\u00020\u001aJ\u0006\u0010\u001e\u001a\u00020\u001aJ\u0006\u0010\u001f\u001a\u00020\u001aJ\u0006\u0010 \u001a\u00020\u001aJ\u0006\u0010!\u001a\u00020\nJ\u001f\u0010\"\u001a\u00020\n2\u0006\u0010#\u001a\u00020\f2\n\b\u0002\u0010$\u001a\u0004\u0018\u00010%\u00a2\u0006\u0002\u0010&J\u000e\u0010\'\u001a\u00020\n2\u0006\u0010(\u001a\u00020\u0011J\u000e\u0010)\u001a\u00020\n2\u0006\u0010*\u001a\u00020\fJ\u000e\u0010+\u001a\u00020\n2\u0006\u0010#\u001a\u00020\fJ\u000e\u0010,\u001a\u00020\n2\u0006\u0010-\u001a\u00020\fJ\u000e\u0010.\u001a\u00020\n2\u0006\u0010/\u001a\u00020\fJ\u0016\u00100\u001a\u00020\n2\u0006\u00101\u001a\u00020\f2\u0006\u00102\u001a\u00020\fJ \u00103\u001a\u00020\n2\u0006\u00102\u001a\u00020\f2\u0006\u00104\u001a\u00020\f2\b\b\u0002\u00105\u001a\u00020\u001aR\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u00068"}, d2 = {"Lcom/pevplugshare/data/remote/TokenManager;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "encryptedPrefs", "Landroid/content/SharedPreferences;", "masterKey", "Landroidx/security/crypto/MasterKey;", "clearStoredCredentials", "", "getAccessToken", "", "getAuthHeader", "getAuthType", "Lcom/pevplugshare/data/remote/TokenManager$AuthType;", "getMapZoomLevel", "", "getOpenRouterApiKey", "getPassword", "getRefreshToken", "getSelectedAiModel", "getSelectedCategory", "getUserId", "getUsername", "hasAiApiKeyConfigured", "", "hasAiModelSelected", "hasStoredCredentials", "isAiAnalysisEnabled", "isLoggedIn", "isRememberMeEnabled", "isTokenExpired", "logout", "saveAccessToken", "token", "expirySeconds", "", "(Ljava/lang/String;Ljava/lang/Integer;)V", "saveMapZoomLevel", "zoomLevel", "saveOpenRouterApiKey", "apiKey", "saveRefreshToken", "saveSelectedAiModel", "modelId", "saveSelectedCategory", "category", "saveUserInfo", "userId", "username", "saveWebApiCredentials", "password", "rememberMe", "AuthType", "Companion", "app_debug"})
public final class TokenManager {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREFS_NAME = "osmand_auth_prefs";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_ACCESS_TOKEN = "access_token";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_REFRESH_TOKEN = "refresh_token";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_TOKEN_EXPIRY = "token_expiry";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_USER_ID = "user_id";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_USERNAME = "username";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_PASSWORD = "password";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_AUTH_TYPE = "auth_type";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_REMEMBER_ME = "remember_me";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_SELECTED_CATEGORY = "selected_category";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_OPENROUTER_API_KEY = "openrouter_api_key";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_SELECTED_AI_MODEL = "selected_ai_model";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_MAP_ZOOM_LEVEL = "map_zoom_level";
    private static final float DEFAULT_MAP_ZOOM = 18.0F;
    @org.jetbrains.annotations.NotNull()
    private final androidx.security.crypto.MasterKey masterKey = null;
    @org.jetbrains.annotations.NotNull()
    private final android.content.SharedPreferences encryptedPrefs = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.data.remote.TokenManager.Companion Companion = null;
    
    public TokenManager(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    public final void saveAccessToken(@org.jetbrains.annotations.NotNull()
    java.lang.String token, @org.jetbrains.annotations.Nullable()
    java.lang.Integer expirySeconds) {
    }
    
    public final void saveRefreshToken(@org.jetbrains.annotations.NotNull()
    java.lang.String token) {
    }
    
    public final void saveUserInfo(@org.jetbrains.annotations.NotNull()
    java.lang.String userId, @org.jetbrains.annotations.NotNull()
    java.lang.String username) {
    }
    
    public final void saveWebApiCredentials(@org.jetbrains.annotations.NotNull()
    java.lang.String username, @org.jetbrains.annotations.NotNull()
    java.lang.String password, boolean rememberMe) {
    }
    
    public final boolean isRememberMeEnabled() {
        return false;
    }
    
    public final boolean hasStoredCredentials() {
        return false;
    }
    
    public final void clearStoredCredentials() {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getPassword() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.remote.TokenManager.AuthType getAuthType() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getAccessToken() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getRefreshToken() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getUserId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getUsername() {
        return null;
    }
    
    public final boolean isTokenExpired() {
        return false;
    }
    
    public final boolean isLoggedIn() {
        return false;
    }
    
    public final void saveOpenRouterApiKey(@org.jetbrains.annotations.NotNull()
    java.lang.String apiKey) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getOpenRouterApiKey() {
        return null;
    }
    
    public final void saveSelectedAiModel(@org.jetbrains.annotations.NotNull()
    java.lang.String modelId) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getSelectedAiModel() {
        return null;
    }
    
    public final void logout() {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getAuthHeader() {
        return null;
    }
    
    public final void saveSelectedCategory(@org.jetbrains.annotations.NotNull()
    java.lang.String category) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getSelectedCategory() {
        return null;
    }
    
    public final void saveMapZoomLevel(float zoomLevel) {
    }
    
    public final float getMapZoomLevel() {
        return 0.0F;
    }
    
    public final boolean hasAiApiKeyConfigured() {
        return false;
    }
    
    public final boolean hasAiModelSelected() {
        return false;
    }
    
    public final boolean isAiAnalysisEnabled() {
        return false;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0004\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004\u00a8\u0006\u0005"}, d2 = {"Lcom/pevplugshare/data/remote/TokenManager$AuthType;", "", "(Ljava/lang/String;I)V", "OAUTH", "WEB_API", "app_debug"})
    public static enum AuthType {
        /*public static final*/ OAUTH /* = new OAUTH() */,
        /*public static final*/ WEB_API /* = new WEB_API() */;
        
        AuthType() {
        }
        
        @org.jetbrains.annotations.NotNull()
        public static kotlin.enums.EnumEntries<com.pevplugshare.data.remote.TokenManager.AuthType> getEntries() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\u000e\n\u0002\b\r\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/pevplugshare/data/remote/TokenManager$Companion;", "", "()V", "DEFAULT_MAP_ZOOM", "", "KEY_ACCESS_TOKEN", "", "KEY_AUTH_TYPE", "KEY_MAP_ZOOM_LEVEL", "KEY_OPENROUTER_API_KEY", "KEY_PASSWORD", "KEY_REFRESH_TOKEN", "KEY_REMEMBER_ME", "KEY_SELECTED_AI_MODEL", "KEY_SELECTED_CATEGORY", "KEY_TOKEN_EXPIRY", "KEY_USERNAME", "KEY_USER_ID", "PREFS_NAME", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}