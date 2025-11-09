package com.pevplugshare.ui.quicklocation;

/**
 * Quick location workflow: select/take photos, extract metadata, optionally edit, run AI analysis, then go to add location
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000h\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010%\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010!\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\b\u0005\u0018\u00002\u00020\u0001:\u0001(B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0017\u001a\u0004\u0018\u00010\u00182\u0006\u0010\u0019\u001a\u00020\u0006H\u0002J\n\u0010\u001a\u001a\u0004\u0018\u00010\u0018H\u0002J\b\u0010\u001b\u001a\u00020\u001cH\u0002J\b\u0010\u001d\u001a\u00020\u001cH\u0002J\u0012\u0010\u001e\u001a\u00020\u001c2\b\u0010\u001f\u001a\u0004\u0018\u00010 H\u0014J\b\u0010!\u001a\u00020\u001cH\u0014J\u0016\u0010\"\u001a\u00020\u001c2\f\u0010#\u001a\b\u0012\u0004\u0012\u00020\u00060$H\u0002J\b\u0010%\u001a\u00020\u001cH\u0002J\b\u0010&\u001a\u00020\u001cH\u0002J\b\u0010\'\u001a\u00020\u001cH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u000b\u001a\u000e\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\u000e0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u0011\u001a\u0010\u0012\f\u0012\n \u0013*\u0004\u0018\u00010\r0\r0\u0012X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\r0\u0015X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u0016\u001a\u0010\u0012\f\u0012\n \u0013*\u0004\u0018\u00010\u00060\u00060\u0012X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006)"}, d2 = {"Lcom/pevplugshare/ui/quicklocation/QuickLocationActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/pevplugshare/databinding/ActivityQuickLocationBinding;", "currentPhotoUri", "Landroid/net/Uri;", "currentStep", "Lcom/pevplugshare/ui/quicklocation/QuickLocationActivity$Step;", "job", "Lkotlinx/coroutines/CompletableJob;", "photoMetadata", "", "", "Lcom/pevplugshare/data/model/PhotoMetadata;", "scope", "Lkotlinx/coroutines/CoroutineScope;", "selectPhotos", "Landroidx/activity/result/ActivityResultLauncher;", "kotlin.jvm.PlatformType", "selectedPhotoPaths", "", "takeCameraPhoto", "copyPhotoToCache", "Ljava/io/File;", "uri", "createImageFile", "goToAddLocation", "", "launchCamera", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "processPhotoUris", "uris", "", "showCameraWorkflowDialog", "showPhotoSelectionDialog", "showPhotoTakenDialog", "Step", "app_debug"})
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
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String> selectPhotos = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<android.net.Uri> takeCameraPhoto = null;
    
    public QuickLocationActivity() {
        super();
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
    
    private final void showPhotoSelectionDialog() {
    }
    
    private final void goToAddLocation() {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0007\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007\u00a8\u0006\b"}, d2 = {"Lcom/pevplugshare/ui/quicklocation/QuickLocationActivity$Step;", "", "(Ljava/lang/String;I)V", "SELECT_PHOTOS", "TAKE_PHOTOS", "EDIT_PHOTOS", "AI_ANALYSIS", "ADD_LOCATION", "app_debug"})
    public static enum Step {
        /*public static final*/ SELECT_PHOTOS /* = new SELECT_PHOTOS() */,
        /*public static final*/ TAKE_PHOTOS /* = new TAKE_PHOTOS() */,
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