package com.pevplugshare.ui.quicklocation;

/**
 * Quick location workflow: select/take photos, pick location on map, edit photos, AI analysis, then add location
 */
@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0090\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010%\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0002\b\u0003\n\u0002\u0010!\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0010 \n\u0002\b\u0006\b\u0007\u0018\u00002\u00020\u0001:\u0001EB\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010&\u001a\u0004\u0018\u00010\'2\u0006\u0010(\u001a\u00020\u0006H\u0002J\n\u0010)\u001a\u0004\u0018\u00010\'H\u0002J@\u0010*\u001a\u00020+26\u0010,\u001a2\u0012\u0013\u0012\u00110\u001a\u00a2\u0006\f\b.\u0012\b\b/\u0012\u0004\b\b(0\u0012\u0013\u0012\u00110\u001a\u00a2\u0006\f\b.\u0012\b\b/\u0012\u0004\b\b(1\u0012\u0004\u0012\u00020+0-H\u0002J\b\u00102\u001a\u00020+H\u0002J\u0012\u00103\u001a\u00020+2\b\u00104\u001a\u0004\u0018\u000105H\u0014J\b\u00106\u001a\u00020+H\u0014J\b\u00107\u001a\u00020+H\u0002J\b\u00108\u001a\u00020+H\u0002J\b\u00109\u001a\u00020+H\u0002J\b\u0010:\u001a\u00020+H\u0002J\b\u0010;\u001a\u00020+H\u0002J\b\u0010<\u001a\u00020+H\u0002J\b\u0010=\u001a\u00020+H\u0002J\u0016\u0010>\u001a\u00020+2\f\u0010?\u001a\b\u0012\u0004\u0012\u00020\u00060@H\u0002J\b\u0010A\u001a\u00020+H\u0002J\b\u0010B\u001a\u00020+H\u0002J\b\u0010C\u001a\u00020+H\u0002J\b\u0010D\u001a\u00020+H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\r\u001a\u000e\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u00020\u00100\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u0013\u001a\u0010\u0012\f\u0012\n \u0015*\u0004\u0018\u00010\u000f0\u000f0\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0012\u0010\u0016\u001a\u0004\u0018\u00010\u0017X\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\u0018R\u0012\u0010\u0019\u001a\u0004\u0018\u00010\u001aX\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\u001bR\u0012\u0010\u001c\u001a\u0004\u0018\u00010\u001aX\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\u001bR\u0014\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u000f0\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u001f\u001a\u0010\u0012\f\u0012\n \u0015*\u0004\u0018\u00010\u00060\u00060\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001e\u0010 \u001a\u00020!8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\"\u0010#\"\u0004\b$\u0010%\u00a8\u0006F"}, d2 = {"Lcom/pevplugshare/ui/quicklocation/QuickLocationActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/pevplugshare/databinding/ActivityQuickLocationBinding;", "currentPhotoUri", "Landroid/net/Uri;", "currentStep", "Lcom/pevplugshare/ui/quicklocation/QuickLocationActivity$Step;", "fusedLocationClient", "Lcom/google/android/gms/location/FusedLocationProviderClient;", "job", "Lkotlinx/coroutines/CompletableJob;", "photoMetadata", "", "", "Lcom/pevplugshare/data/model/PhotoMetadata;", "scope", "Lkotlinx/coroutines/CoroutineScope;", "selectPhotos", "Landroidx/activity/result/ActivityResultLauncher;", "kotlin.jvm.PlatformType", "selectedBearing", "", "Ljava/lang/Float;", "selectedLatitude", "", "Ljava/lang/Double;", "selectedLongitude", "selectedPhotoPaths", "", "takeCameraPhoto", "tokenManager", "Lcom/pevplugshare/data/remote/TokenManager;", "getTokenManager", "()Lcom/pevplugshare/data/remote/TokenManager;", "setTokenManager", "(Lcom/pevplugshare/data/remote/TokenManager;)V", "copyPhotoToCache", "Ljava/io/File;", "uri", "createImageFile", "getCurrentLocation", "", "callback", "Lkotlin/Function2;", "Lkotlin/ParameterName;", "name", "lat", "lng", "launchCamera", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "proceedAfterAiAnalysis", "proceedAfterLocationPicker", "proceedAfterPhotoEditing", "proceedToAddLocation", "proceedToAiAnalysis", "proceedToLocationPicker", "proceedToPhotoEditing", "processPhotoUris", "uris", "", "showCameraWorkflowDialog", "showLocationPickerDialog", "showPhotoProgressDialog", "showPhotoTakenDialog", "Step", "app_debug"})
public final class QuickLocationActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.pevplugshare.databinding.ActivityQuickLocationBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> selectedPhotoPaths = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, com.pevplugshare.data.model.PhotoMetadata> photoMetadata = null;
    @org.jetbrains.annotations.NotNull()
    private com.pevplugshare.ui.quicklocation.QuickLocationActivity.Step currentStep = com.pevplugshare.ui.quicklocation.QuickLocationActivity.Step.SELECT_PHOTOS;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CompletableJob job = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope scope = null;
    @org.jetbrains.annotations.Nullable()
    private android.net.Uri currentPhotoUri;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Double selectedLatitude;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Double selectedLongitude;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Float selectedBearing;
    private com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;
    @javax.inject.Inject()
    public com.pevplugshare.data.remote.TokenManager tokenManager;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String> selectPhotos = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<android.net.Uri> takeCameraPhoto = null;
    
    public QuickLocationActivity() {
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
    
    private final void showCameraWorkflowDialog() {
    }
    
    private final void launchCamera() {
    }
    
    private final java.io.File createImageFile() {
        return null;
    }
    
    private final void showPhotoTakenDialog() {
    }
    
    private final void processPhotoUris(java.util.List<? extends android.net.Uri> uris) {
    }
    
    private final java.io.File copyPhotoToCache(android.net.Uri uri) {
        return null;
    }
    
    private final void proceedToLocationPicker() {
    }
    
    private final void getCurrentLocation(kotlin.jvm.functions.Function2<? super java.lang.Double, ? super java.lang.Double, kotlin.Unit> callback) {
    }
    
    private final void showLocationPickerDialog() {
    }
    
    private final void proceedAfterLocationPicker() {
    }
    
    private final void showPhotoProgressDialog() {
    }
    
    private final void proceedToPhotoEditing() {
    }
    
    private final void proceedAfterPhotoEditing() {
    }
    
    private final void proceedToAiAnalysis() {
    }
    
    private final void proceedAfterAiAnalysis() {
    }
    
    private final void proceedToAddLocation() {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\b\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007j\u0002\b\b\u00a8\u0006\t"}, d2 = {"Lcom/pevplugshare/ui/quicklocation/QuickLocationActivity$Step;", "", "(Ljava/lang/String;I)V", "SELECT_PHOTOS", "TAKE_PHOTOS", "PICK_LOCATION", "EDIT_PHOTOS", "AI_ANALYSIS", "ADD_LOCATION", "app_debug"})
    public static enum Step {
        /*public static final*/ SELECT_PHOTOS /* = new SELECT_PHOTOS() */,
        /*public static final*/ TAKE_PHOTOS /* = new TAKE_PHOTOS() */,
        /*public static final*/ PICK_LOCATION /* = new PICK_LOCATION() */,
        /*public static final*/ EDIT_PHOTOS /* = new EDIT_PHOTOS() */,
        /*public static final*/ AI_ANALYSIS /* = new AI_ANALYSIS() */,
        /*public static final*/ ADD_LOCATION /* = new ADD_LOCATION() */;
        
        Step() {
        }
        
        @org.jetbrains.annotations.NotNull()
        public static kotlin.enums.EnumEntries<com.pevplugshare.ui.quicklocation.QuickLocationActivity.Step> getEntries() {
            return null;
        }
    }
}