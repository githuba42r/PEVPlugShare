package com.pevplugshare.ui.quicklocation;

/**
 * Gallery view for editing multiple photos with focus areas and crop/zoom
 */
@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010%\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010!\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\tH\u0002J\u0012\u0010\u0016\u001a\u00020\u00142\b\u0010\u0017\u001a\u0004\u0018\u00010\u0018H\u0014J\b\u0010\u0019\u001a\u00020\u0014H\u0014J\b\u0010\u001a\u001a\u00020\u0014H\u0002J\b\u0010\u001b\u001a\u00020\u0014H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0007\u001a\u000e\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\f\u001a\b\u0012\u0004\u0012\u00020\t0\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0012\u0010\u0010\u001a\u0004\u0018\u00010\u0011X\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\u0012\u00a8\u0006\u001c"}, d2 = {"Lcom/pevplugshare/ui/quicklocation/PhotoGalleryEditorActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "adapter", "Lcom/pevplugshare/ui/quicklocation/PhotoGalleryAdapter;", "binding", "Lcom/pevplugshare/databinding/ActivityPhotoGalleryEditorBinding;", "focusAreas", "", "", "job", "Lkotlinx/coroutines/CompletableJob;", "photoPaths", "", "scope", "Lkotlinx/coroutines/CoroutineScope;", "selectedPhotoIndex", "", "Ljava/lang/Integer;", "launchPhotoEditor", "", "photoPath", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "proceedWithEditedPhotos", "setupUI", "app_debug"})
public final class PhotoGalleryEditorActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.pevplugshare.databinding.ActivityPhotoGalleryEditorBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> photoPaths = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.String> focusAreas = null;
    private com.pevplugshare.ui.quicklocation.PhotoGalleryAdapter adapter;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CompletableJob job = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope scope = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Integer selectedPhotoIndex;
    
    public PhotoGalleryEditorActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupUI() {
    }
    
    private final void launchPhotoEditor(java.lang.String photoPath) {
    }
    
    private final void proceedWithEditedPhotos() {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
}