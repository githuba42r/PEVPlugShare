package com.pevplugshare.ui.settings;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000X\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010%\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u0006\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0010\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0015\u001a\u00020\u0016H\u0002J\u0010\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u0005H\u0002J\u001c\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00050\u00042\u0006\u0010\u001b\u001a\u00020\u0005H\u0082@\u00a2\u0006\u0002\u0010\u001cJ\u0010\u0010\u001d\u001a\u00020\u00052\u0006\u0010\u001e\u001a\u00020\u001fH\u0002J\u0010\u0010 \u001a\u00020\u00052\u0006\u0010\u0019\u001a\u00020\u0005H\u0002J\u0012\u0010!\u001a\u0004\u0018\u00010\u00052\u0006\u0010\u0019\u001a\u00020\u0005H\u0002J\u0010\u0010\"\u001a\u00020\u00052\u0006\u0010\u0019\u001a\u00020\u0005H\u0002J\u0012\u0010#\u001a\u00020\u00162\b\u0010$\u001a\u0004\u0018\u00010%H\u0014J\b\u0010&\u001a\u00020\u0016H\u0002J\b\u0010\'\u001a\u00020\u0016H\u0002J\b\u0010(\u001a\u00020\u0016H\u0002J\b\u0010)\u001a\u00020\u0016H\u0002J\b\u0010*\u001a\u00020\u0016H\u0002J\u0010\u0010+\u001a\u00020\u00162\u0006\u0010,\u001a\u00020\u0005H\u0002J\b\u0010-\u001a\u00020\u0016H\u0002J\u0016\u0010.\u001a\u00020\u00162\f\u0010/\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004H\u0002J\b\u00100\u001a\u00020\u0016H\u0002J\u0010\u00101\u001a\u00020\u00162\u0006\u00102\u001a\u00020\u0018H\u0002J\u001a\u00103\u001a\u00020\u00162\u0006\u0010\u001b\u001a\u00020\u00052\b\b\u0002\u00104\u001a\u00020\tH\u0002R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\f0\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001e\u0010\u000f\u001a\u00020\u00108\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0011\u0010\u0012\"\u0004\b\u0013\u0010\u0014\u00a8\u00065"}, d2 = {"Lcom/pevplugshare/ui/settings/SettingsActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "availableModels", "", "", "binding", "Lcom/pevplugshare/databinding/ActivitySettingsBinding;", "modelChanged", "", "modelPricingMap", "", "Lcom/pevplugshare/ui/settings/model/ModelPricing;", "okHttpClient", "Lokhttp3/OkHttpClient;", "tokenManager", "Lcom/pevplugshare/data/remote/TokenManager;", "getTokenManager", "()Lcom/pevplugshare/data/remote/TokenManager;", "setTokenManager", "(Lcom/pevplugshare/data/remote/TokenManager;)V", "checkExistingApiKey", "", "createAiModelFromId", "Lcom/pevplugshare/ui/settings/model/AiModel;", "modelId", "fetchAvailableModels", "apiKey", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "formatCost", "costPerMillion", "", "formatModelDisplayName", "getModelContextSize", "getModelCost", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "resetSettings", "saveSettings", "setupToolbar", "setupUI", "showApiKeyEntry", "showError", "message", "showModelSelectionDialog", "showValidApiKey", "models", "updateBottomButtons", "updateSelectedModelDisplay", "model", "validateAndSaveApiKey", "silent", "app_release"})
public final class SettingsActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.pevplugshare.databinding.ActivitySettingsBinding binding;
    @javax.inject.Inject()
    public com.pevplugshare.data.remote.TokenManager tokenManager;
    @org.jetbrains.annotations.NotNull()
    private final okhttp3.OkHttpClient okHttpClient = null;
    @org.jetbrains.annotations.NotNull()
    private java.util.List<java.lang.String> availableModels;
    @org.jetbrains.annotations.NotNull()
    private java.util.Map<java.lang.String, com.pevplugshare.ui.settings.model.ModelPricing> modelPricingMap;
    private boolean modelChanged = false;
    
    public SettingsActivity() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.remote.TokenManager getTokenManager() {
        return null;
    }
    
    public final void setTokenManager(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.remote.TokenManager p0) {
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupToolbar() {
    }
    
    private final void setupUI() {
    }
    
    private final void saveSettings() {
    }
    
    private final void resetSettings() {
    }
    
    private final void showModelSelectionDialog() {
    }
    
    private final void updateBottomButtons() {
    }
    
    private final void checkExistingApiKey() {
    }
    
    private final void showApiKeyEntry() {
    }
    
    private final void showValidApiKey(java.util.List<java.lang.String> models) {
    }
    
    private final void updateSelectedModelDisplay(com.pevplugshare.ui.settings.model.AiModel model) {
    }
    
    private final com.pevplugshare.ui.settings.model.AiModel createAiModelFromId(java.lang.String modelId) {
        return null;
    }
    
    private final java.lang.String getModelContextSize(java.lang.String modelId) {
        return null;
    }
    
    private final java.lang.String formatModelDisplayName(java.lang.String modelId) {
        return null;
    }
    
    private final java.lang.String getModelCost(java.lang.String modelId) {
        return null;
    }
    
    private final void validateAndSaveApiKey(java.lang.String apiKey, boolean silent) {
    }
    
    private final void showError(java.lang.String message) {
    }
    
    private final java.lang.Object fetchAvailableModels(java.lang.String apiKey, kotlin.coroutines.Continuation<? super java.util.List<java.lang.String>> $completion) {
        return null;
    }
    
    private final java.lang.String formatCost(double costPerMillion) {
        return null;
    }
}