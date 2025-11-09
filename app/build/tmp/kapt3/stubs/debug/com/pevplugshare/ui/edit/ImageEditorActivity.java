package com.pevplugshare.ui.edit;

/**
 * Activity for editing images: crop, zoom, and define focus areas
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000N\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\b\u0018\u0000 !2\u00020\u0001:\u0001!B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0010\u001a\u00020\u0011H\u0002J\u0010\u0010\u0012\u001a\u00020\u00112\u0006\u0010\u0013\u001a\u00020\u0014H\u0002J\u0010\u0010\u0015\u001a\u00020\u00112\u0006\u0010\u0016\u001a\u00020\u0017H\u0002J\u0012\u0010\u0018\u001a\u00020\u00112\b\u0010\u0019\u001a\u0004\u0018\u00010\u001aH\u0014J\b\u0010\u001b\u001a\u00020\u0011H\u0002J\b\u0010\u001c\u001a\u00020\u0011H\u0002J\b\u0010\u001d\u001a\u00020\u0011H\u0002J\b\u0010\u001e\u001a\u00020\u0011H\u0002J\u0010\u0010\u001f\u001a\u00020\u00112\u0006\u0010 \u001a\u00020\u0006H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\t\u001a\u0004\u0018\u00010\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\""}, d2 = {"Lcom/pevplugshare/ui/edit/ImageEditorActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/pevplugshare/databinding/ActivityImageEditorBinding;", "currentZoom", "", "editBitmap", "Landroid/graphics/Bitmap;", "focusRect", "Landroid/graphics/RectF;", "focusStartX", "focusStartY", "isDrawingFocus", "", "originalBitmap", "cropImage", "", "handleFocusDrawing", "event", "Landroid/view/MotionEvent;", "loadImage", "imagePath", "", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "refreshImage", "refreshImageWithFocus", "saveImage", "setupUI", "zoomImage", "factor", "Companion", "app_debug"})
public final class ImageEditorActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.pevplugshare.databinding.ActivityImageEditorBinding binding;
    private android.graphics.Bitmap originalBitmap;
    private android.graphics.Bitmap editBitmap;
    private float currentZoom = 1.0F;
    @org.jetbrains.annotations.Nullable()
    private android.graphics.RectF focusRect;
    private boolean isDrawingFocus = false;
    private float focusStartX = 0.0F;
    private float focusStartY = 0.0F;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_IMAGE_PATH = "image_path";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_OUTPUT_PATH = "output_path";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_FOCUS_RECT = "focus_rect";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_METADATA = "metadata";
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.ui.edit.ImageEditorActivity.Companion Companion = null;
    
    public ImageEditorActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void loadImage(java.lang.String imagePath) {
    }
    
    private final void setupUI() {
    }
    
    private final void zoomImage(float factor) {
    }
    
    private final void handleFocusDrawing(android.view.MotionEvent event) {
    }
    
    private final void refreshImageWithFocus() {
    }
    
    private final void refreshImage() {
    }
    
    private final void cropImage() {
    }
    
    private final void saveImage() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\b"}, d2 = {"Lcom/pevplugshare/ui/edit/ImageEditorActivity$Companion;", "", "()V", "EXTRA_FOCUS_RECT", "", "EXTRA_IMAGE_PATH", "EXTRA_METADATA", "EXTRA_OUTPUT_PATH", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}