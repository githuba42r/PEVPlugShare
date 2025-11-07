package com.pevplugshare.ui.photo;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000J\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0006\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\u0010\u001a\u00020\u0011J\u000e\u0010\u0012\u001a\u00020\u00112\u0006\u0010\u0013\u001a\u00020\u0014J\u000e\u0010\u0015\u001a\u00020\u00112\u0006\u0010\u0016\u001a\u00020\u0017J\u000e\u0010\u0018\u001a\u00020\u00112\u0006\u0010\u0019\u001a\u00020\u0014J\u0016\u0010\u001a\u001a\u00020\u00112\u0006\u0010\u001b\u001a\u00020\u001c2\u0006\u0010\u001d\u001a\u00020\u001cR\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\b\u001a\b\u0012\u0004\u0012\u00020\t0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00070\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\t0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\rR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001e"}, d2 = {"Lcom/pevplugshare/ui/photo/PhotoCaptureViewModel;", "Landroidx/lifecycle/ViewModel;", "photoStorageManager", "Lcom/pevplugshare/data/storage/PhotoStorageManager;", "(Lcom/pevplugshare/data/storage/PhotoStorageManager;)V", "_captureData", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/pevplugshare/ui/photo/PhotoCaptureData;", "_photoCaptureState", "Lcom/pevplugshare/ui/photo/PhotoCaptureState;", "captureData", "Lkotlinx/coroutines/flow/StateFlow;", "getCaptureData", "()Lkotlinx/coroutines/flow/StateFlow;", "photoCaptureState", "getPhotoCaptureState", "resetCapture", "", "savePhoto", "locationId", "", "setPhotoBitmap", "bitmap", "Landroid/graphics/Bitmap;", "updateCaption", "caption", "updateLocation", "latitude", "", "longitude", "app_release"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class PhotoCaptureViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.storage.PhotoStorageManager photoStorageManager = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pevplugshare.ui.photo.PhotoCaptureState> _photoCaptureState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pevplugshare.ui.photo.PhotoCaptureState> photoCaptureState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pevplugshare.ui.photo.PhotoCaptureData> _captureData = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pevplugshare.ui.photo.PhotoCaptureData> captureData = null;
    
    @javax.inject.Inject()
    public PhotoCaptureViewModel(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.storage.PhotoStorageManager photoStorageManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pevplugshare.ui.photo.PhotoCaptureState> getPhotoCaptureState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pevplugshare.ui.photo.PhotoCaptureData> getCaptureData() {
        return null;
    }
    
    public final void setPhotoBitmap(@org.jetbrains.annotations.NotNull()
    android.graphics.Bitmap bitmap) {
    }
    
    public final void updateCaption(@org.jetbrains.annotations.NotNull()
    java.lang.String caption) {
    }
    
    public final void updateLocation(double latitude, double longitude) {
    }
    
    public final void savePhoto(@org.jetbrains.annotations.NotNull()
    java.lang.String locationId) {
    }
    
    public final void resetCapture() {
    }
}