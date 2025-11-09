package com.pevplugshare.util;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000V\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\b\u0005\n\u0002\u0010\u0007\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J \u0010\u0006\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u0004H\u0002J\"\u0010\u000b\u001a\u0004\u0018\u00010\f2\u0006\u0010\r\u001a\u00020\u000e2\u0006\u0010\t\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u0004H\u0002J\u0016\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u000eJ\u001e\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u000e2\u0006\u0010\u0016\u001a\u00020\u0004J\u000e\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0011\u001a\u00020\u0012J\u0016\u0010\u0019\u001a\u00020\u00182\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u000eJ\u001c\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00180\u001b2\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u000eJ(\u0010\u001c\u001a\u0004\u0018\u00010\u00182\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u001d\u001a\u00020\u00182\u0006\u0010\u0013\u001a\u00020\u000e2\u0006\u0010\u0016\u001a\u00020\u0004J\u0018\u0010\u001e\u001a\u00020\f2\u0006\u0010\u001f\u001a\u00020\f2\u0006\u0010 \u001a\u00020!H\u0002J\u0018\u0010\"\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000e2\u0006\u0010\u001f\u001a\u00020\fH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006#"}, d2 = {"Lcom/pevplugshare/util/ImageUtils;", "", "()V", "JPEG_QUALITY", "", "MAX_IMAGE_DIMENSION", "calculateInSampleSize", "options", "Landroid/graphics/BitmapFactory$Options;", "reqWidth", "reqHeight", "decodeSampledBitmap", "Landroid/graphics/Bitmap;", "path", "", "deleteAllLocationImages", "", "context", "Landroid/content/Context;", "locationId", "deleteLocationImage", "", "imageIndex", "getAppImagesDirectory", "Ljava/io/File;", "getLocationImageDirectory", "getLocationImages", "", "resizeAndSaveImage", "sourceFile", "rotateImage", "bitmap", "degrees", "", "rotateImageIfRequired", "app_debug"})
public final class ImageUtils {
    private static final int MAX_IMAGE_DIMENSION = 1024;
    private static final int JPEG_QUALITY = 85;
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.util.ImageUtils INSTANCE = null;
    
    private ImageUtils() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.io.File getAppImagesDirectory(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.io.File getLocationImageDirectory(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    java.lang.String locationId) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.io.File resizeAndSaveImage(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    java.io.File sourceFile, @org.jetbrains.annotations.NotNull()
    java.lang.String locationId, int imageIndex) {
        return null;
    }
    
    private final android.graphics.Bitmap decodeSampledBitmap(java.lang.String path, int reqWidth, int reqHeight) {
        return null;
    }
    
    private final int calculateInSampleSize(android.graphics.BitmapFactory.Options options, int reqWidth, int reqHeight) {
        return 0;
    }
    
    private final android.graphics.Bitmap rotateImageIfRequired(java.lang.String path, android.graphics.Bitmap bitmap) {
        return null;
    }
    
    private final android.graphics.Bitmap rotateImage(android.graphics.Bitmap bitmap, float degrees) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.io.File> getLocationImages(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    java.lang.String locationId) {
        return null;
    }
    
    public final boolean deleteLocationImage(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    java.lang.String locationId, int imageIndex) {
        return false;
    }
    
    public final void deleteAllLocationImages(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    java.lang.String locationId) {
    }
}