package com.pevplugshare.data.storage;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u0000 $2\u00020\u0001:\u0002$%B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0010\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u0006H\u0002J\u000e\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0011J\u0018\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u00132\u0006\u0010\u0015\u001a\u00020\u0016H\u0002J\u0018\u0010\u0017\u001a\u00020\u000f2\u0006\u0010\u0018\u001a\u00020\u00112\b\u0010\u0019\u001a\u0004\u0018\u00010\u0011J\u0010\u0010\u001a\u001a\u00020\u00112\u0006\u0010\u0010\u001a\u00020\u0011H\u0002J\u0014\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00060\u001c2\u0006\u0010\u0010\u001a\u00020\u0011J\u0010\u0010\u001d\u001a\u0004\u0018\u00010\u00062\u0006\u0010\u0018\u001a\u00020\u0011J\u000e\u0010\u001e\u001a\u00020\f2\u0006\u0010\u0018\u001a\u00020\u0011J\u0006\u0010\u001f\u001a\u00020 J.\u0010!\u001a\u0010\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u0011\u0018\u00010\"2\u0006\u0010\u0014\u001a\u00020\u00132\u0006\u0010\u0010\u001a\u00020\u00112\b\b\u0002\u0010#\u001a\u00020\u0016R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\u00020\u00068BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0007\u0010\bR\u0014\u0010\t\u001a\u00020\u00068BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\n\u0010\b\u00a8\u0006&"}, d2 = {"Lcom/pevplugshare/data/storage/PhotoStorageManager;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "photosDirectory", "Ljava/io/File;", "getPhotosDirectory", "()Ljava/io/File;", "thumbnailsDirectory", "getThumbnailsDirectory", "calculateDirSize", "", "dir", "clearLocationPhotos", "", "locationId", "", "createThumbnail", "Landroid/graphics/Bitmap;", "bitmap", "maxSize", "", "deletePhoto", "photoPath", "thumbnailPath", "generatePhotoFileName", "getLocationPhotos", "", "getPhotoFile", "getPhotoSize", "getStorageInfo", "Lcom/pevplugshare/data/storage/PhotoStorageManager$StorageInfo;", "savePhotoFromBitmap", "Lkotlin/Pair;", "quality", "Companion", "StorageInfo", "app_release"})
public final class PhotoStorageManager {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "PhotoStorageManager";
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.data.storage.PhotoStorageManager.Companion Companion = null;
    
    public PhotoStorageManager(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    private final java.io.File getPhotosDirectory() {
        return null;
    }
    
    private final java.io.File getThumbnailsDirectory() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final kotlin.Pair<java.lang.String, java.lang.String> savePhotoFromBitmap(@org.jetbrains.annotations.NotNull()
    android.graphics.Bitmap bitmap, @org.jetbrains.annotations.NotNull()
    java.lang.String locationId, int quality) {
        return null;
    }
    
    public final boolean deletePhoto(@org.jetbrains.annotations.NotNull()
    java.lang.String photoPath, @org.jetbrains.annotations.Nullable()
    java.lang.String thumbnailPath) {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.io.File getPhotoFile(@org.jetbrains.annotations.NotNull()
    java.lang.String photoPath) {
        return null;
    }
    
    public final long getPhotoSize(@org.jetbrains.annotations.NotNull()
    java.lang.String photoPath) {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.io.File> getLocationPhotos(@org.jetbrains.annotations.NotNull()
    java.lang.String locationId) {
        return null;
    }
    
    public final boolean clearLocationPhotos(@org.jetbrains.annotations.NotNull()
    java.lang.String locationId) {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.storage.PhotoStorageManager.StorageInfo getStorageInfo() {
        return null;
    }
    
    private final java.lang.String generatePhotoFileName(java.lang.String locationId) {
        return null;
    }
    
    private final android.graphics.Bitmap createThumbnail(android.graphics.Bitmap bitmap, int maxSize) {
        return null;
    }
    
    private final long calculateDirSize(java.io.File dir) {
        return 0L;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/pevplugshare/data/storage/PhotoStorageManager$Companion;", "", "()V", "TAG", "", "app_release"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\t\n\u0002\b\f\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0006\b\u0086\b\u0018\u00002\u00020\u0001B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0007J\t\u0010\r\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u000e\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u000f\u001a\u00020\u0005H\u00c6\u0003J\'\u0010\u0010\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u0005H\u00c6\u0001J\u0013\u0010\u0011\u001a\u00020\u00122\b\u0010\u0013\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\u0006\u0010\u0014\u001a\u00020\u0015J\u0010\u0010\u0016\u001a\u00020\u00152\u0006\u0010\u0017\u001a\u00020\u0005H\u0002J\u0006\u0010\u0018\u001a\u00020\u0015J\t\u0010\u0019\u001a\u00020\u0003H\u00d6\u0001J\t\u0010\u001a\u001a\u00020\u0015H\u00d6\u0001R\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\t\u00a8\u0006\u001b"}, d2 = {"Lcom/pevplugshare/data/storage/PhotoStorageManager$StorageInfo;", "", "totalPhotos", "", "totalSizeBytes", "", "availableSpaceBytes", "(IJJ)V", "getAvailableSpaceBytes", "()J", "getTotalPhotos", "()I", "getTotalSizeBytes", "component1", "component2", "component3", "copy", "equals", "", "other", "formatAvailableSpace", "", "formatBytes", "bytes", "formatSize", "hashCode", "toString", "app_release"})
    public static final class StorageInfo {
        private final int totalPhotos = 0;
        private final long totalSizeBytes = 0L;
        private final long availableSpaceBytes = 0L;
        
        public StorageInfo(int totalPhotos, long totalSizeBytes, long availableSpaceBytes) {
            super();
        }
        
        public final int getTotalPhotos() {
            return 0;
        }
        
        public final long getTotalSizeBytes() {
            return 0L;
        }
        
        public final long getAvailableSpaceBytes() {
            return 0L;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String formatSize() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String formatAvailableSpace() {
            return null;
        }
        
        private final java.lang.String formatBytes(long bytes) {
            return null;
        }
        
        public final int component1() {
            return 0;
        }
        
        public final long component2() {
            return 0L;
        }
        
        public final long component3() {
            return 0L;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.pevplugshare.data.storage.PhotoStorageManager.StorageInfo copy(int totalPhotos, long totalSizeBytes, long availableSpaceBytes) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
}