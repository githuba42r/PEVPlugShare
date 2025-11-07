package com.pevplugshare.ui.photo;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000J\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0007\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0019\u001a\u00020\u001aH\u0002J\b\u0010\u001b\u001a\u00020\u001aH\u0002J\u0012\u0010\u001c\u001a\u00020\u001a2\b\u0010\u001d\u001a\u0004\u0018\u00010\u001eH\u0014J\b\u0010\u001f\u001a\u00020\u001aH\u0014J\b\u0010 \u001a\u00020\u001aH\u0002J\b\u0010!\u001a\u00020\u001aH\u0002J\b\u0010\"\u001a\u00020\u001aH\u0002J\b\u0010#\u001a\u00020\u001aH\u0002J\b\u0010$\u001a\u00020\u001aH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u0007\u001a\u0010\u0012\f\u0012\n \n*\u0004\u0018\u00010\t0\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\r\u001a\u0004\u0018\u00010\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u000f\u001a\u00020\t8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0012\u0010\u0013\u001a\u0004\b\u0010\u0010\u0011R\u001b\u0010\u0014\u001a\u00020\u00158BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0018\u0010\u0013\u001a\u0004\b\u0016\u0010\u0017\u00a8\u0006%"}, d2 = {"Lcom/pevplugshare/ui/photo/PhotoCaptureActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/pevplugshare/databinding/ActivityPhotoCaptureBinding;", "cameraExecutor", "Ljava/util/concurrent/ExecutorService;", "cameraPermissionLauncher", "Landroidx/activity/result/ActivityResultLauncher;", "", "kotlin.jvm.PlatformType", "fusedLocationClient", "Lcom/google/android/gms/location/FusedLocationProviderClient;", "imageCapture", "Landroidx/camera/core/ImageCapture;", "locationId", "getLocationId", "()Ljava/lang/String;", "locationId$delegate", "Lkotlin/Lazy;", "viewModel", "Lcom/pevplugshare/ui/photo/PhotoCaptureViewModel;", "getViewModel", "()Lcom/pevplugshare/ui/photo/PhotoCaptureViewModel;", "viewModel$delegate", "collectPhotoData", "", "getCurrentLocation", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "requestCameraPermissionAndStart", "setupObservers", "setupUI", "startCamera", "takePhoto", "app_debug"})
public final class PhotoCaptureActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.pevplugshare.databinding.ActivityPhotoCaptureBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    private com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;
    @org.jetbrains.annotations.Nullable()
    private java.util.concurrent.ExecutorService cameraExecutor;
    @org.jetbrains.annotations.Nullable()
    private androidx.camera.core.ImageCapture imageCapture;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy locationId$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String> cameraPermissionLauncher = null;
    
    public PhotoCaptureActivity() {
        super();
    }
    
    private final com.pevplugshare.ui.photo.PhotoCaptureViewModel getViewModel() {
        return null;
    }
    
    private final java.lang.String getLocationId() {
        return null;
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupUI() {
    }
    
    private final void setupObservers() {
    }
    
    private final void requestCameraPermissionAndStart() {
    }
    
    private final void startCamera() {
    }
    
    private final void takePhoto() {
    }
    
    private final void getCurrentLocation() {
    }
    
    private final void collectPhotoData() {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
}