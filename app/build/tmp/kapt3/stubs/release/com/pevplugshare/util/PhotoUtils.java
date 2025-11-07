package com.pevplugshare.util;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010$\n\u0002\b\u0003\n\u0002\u0010\u0007\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J \u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00042\u0006\u0010\b\u001a\u00020\u00042\b\b\u0002\u0010\t\u001a\u00020\nJ\u001c\u0010\u000b\u001a\u0010\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\r\u0018\u00010\f2\u0006\u0010\u000e\u001a\u00020\u0004J \u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00102\u0006\u0010\u0012\u001a\u00020\u00062\u0006\u0010\u0013\u001a\u00020\u0006H\u0002J\u001a\u0010\u0014\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00040\u00152\u0006\u0010\u000e\u001a\u00020\u0004J\u001c\u0010\u0016\u001a\u0010\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\n\u0018\u00010\f2\u0006\u0010\u000e\u001a\u00020\u0004J\u0018\u0010\u0017\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00102\u0006\u0010\u0018\u001a\u00020\u0019H\u0002J\u0010\u0010\u001a\u001a\u0004\u0018\u00010\u00102\u0006\u0010\u000e\u001a\u00020\u0004J\u000e\u0010\u001b\u001a\u00020\u00062\u0006\u0010\u000e\u001a\u00020\u0004R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001c"}, d2 = {"Lcom/pevplugshare/util/PhotoUtils;", "", "()V", "TAG", "", "compressImage", "", "inputPath", "outputPath", "quality", "", "extractLocationFromExif", "Lkotlin/Pair;", "", "photoPath", "flipBitmap", "Landroid/graphics/Bitmap;", "bitmap", "horizontal", "vertical", "getExifData", "", "getImageDimensions", "rotateBitmap", "degree", "", "rotateImageIfNeeded", "validatePhotoFile", "app_release"})
public final class PhotoUtils {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "PhotoUtils";
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.util.PhotoUtils INSTANCE = null;
    
    private PhotoUtils() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Map<java.lang.String, java.lang.String> getExifData(@org.jetbrains.annotations.NotNull()
    java.lang.String photoPath) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final android.graphics.Bitmap rotateImageIfNeeded(@org.jetbrains.annotations.NotNull()
    java.lang.String photoPath) {
        return null;
    }
    
    public final boolean compressImage(@org.jetbrains.annotations.NotNull()
    java.lang.String inputPath, @org.jetbrains.annotations.NotNull()
    java.lang.String outputPath, int quality) {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final kotlin.Pair<java.lang.Integer, java.lang.Integer> getImageDimensions(@org.jetbrains.annotations.NotNull()
    java.lang.String photoPath) {
        return null;
    }
    
    public final boolean validatePhotoFile(@org.jetbrains.annotations.NotNull()
    java.lang.String photoPath) {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final kotlin.Pair<java.lang.Double, java.lang.Double> extractLocationFromExif(@org.jetbrains.annotations.NotNull()
    java.lang.String photoPath) {
        return null;
    }
    
    private final android.graphics.Bitmap rotateBitmap(android.graphics.Bitmap bitmap, float degree) {
        return null;
    }
    
    private final android.graphics.Bitmap flipBitmap(android.graphics.Bitmap bitmap, boolean horizontal, boolean vertical) {
        return null;
    }
}