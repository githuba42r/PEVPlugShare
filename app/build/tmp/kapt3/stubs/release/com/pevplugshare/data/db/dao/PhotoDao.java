package com.pevplugshare.data.db.dao;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010 \n\u0002\b\u0005\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0007\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0016\u0010\u000b\u001a\u00020\u00032\u0006\u0010\f\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u001c\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00050\u000e2\u0006\u0010\f\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0016\u0010\u000f\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u001c\u0010\u0010\u001a\u00020\u00032\f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00050\u000eH\u00a7@\u00a2\u0006\u0002\u0010\u0012\u00a8\u0006\u0013"}, d2 = {"Lcom/pevplugshare/data/db/dao/PhotoDao;", "", "deletePhoto", "", "photo", "Lcom/pevplugshare/data/db/entity/PhotoEntity;", "(Lcom/pevplugshare/data/db/entity/PhotoEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deletePhotoById", "id", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deletePhotosByLocation", "locationId", "getPhotosByLocation", "", "insertPhoto", "insertPhotos", "photos", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_release"})
@androidx.room.Dao()
public abstract interface PhotoDao {
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertPhoto(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.PhotoEntity photo, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertPhotos(@org.jetbrains.annotations.NotNull()
    java.util.List<com.pevplugshare.data.db.entity.PhotoEntity> photos, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Delete()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deletePhoto(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.PhotoEntity photo, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM location_photos WHERE locationId = :locationId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getPhotosByLocation(@org.jetbrains.annotations.NotNull()
    java.lang.String locationId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pevplugshare.data.db.entity.PhotoEntity>> $completion);
    
    @androidx.room.Query(value = "DELETE FROM location_photos WHERE locationId = :locationId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deletePhotosByLocation(@org.jetbrains.annotations.NotNull()
    java.lang.String locationId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM location_photos WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deletePhotoById(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}