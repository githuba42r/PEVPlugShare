package com.pevplugshare.data.db.dao;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0004\n\u0002\u0010\u0006\n\u0002\b\u000b\bg\u0018\u00002\u00020\u0001J\u000e\u0010\u0002\u001a\u00020\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u0005\u001a\u00020\u00032\u0006\u0010\u0006\u001a\u00020\u0007H\u00a7@\u00a2\u0006\u0002\u0010\bJ\u0016\u0010\t\u001a\u00020\u00032\u0006\u0010\n\u001a\u00020\u000bH\u00a7@\u00a2\u0006\u0002\u0010\fJ\u0014\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00070\u000f0\u000eH\'J\u0014\u0010\u0010\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00070\u000f0\u000eH\'J\u0018\u0010\u0011\u001a\u0004\u0018\u00010\u00072\u0006\u0010\n\u001a\u00020\u000bH\u00a7@\u00a2\u0006\u0002\u0010\fJ,\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00070\u000f2\u0006\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u00142\u0006\u0010\u0016\u001a\u00020\u0014H\u00a7@\u00a2\u0006\u0002\u0010\u0017J\u0016\u0010\u0018\u001a\u00020\u00032\u0006\u0010\u0006\u001a\u00020\u0007H\u00a7@\u00a2\u0006\u0002\u0010\bJ\u001c\u0010\u0019\u001a\u00020\u00032\f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00070\u000fH\u00a7@\u00a2\u0006\u0002\u0010\u001bJ\u001c\u0010\u001c\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00070\u000f0\u000e2\u0006\u0010\u001d\u001a\u00020\u000bH\'J\u0016\u0010\u001e\u001a\u00020\u00032\u0006\u0010\u0006\u001a\u00020\u0007H\u00a7@\u00a2\u0006\u0002\u0010\b\u00a8\u0006\u001f"}, d2 = {"Lcom/pevplugshare/data/db/dao/LocationDao;", "", "deleteAllLocations", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteLocation", "location", "Lcom/pevplugshare/data/db/entity/LocationEntity;", "(Lcom/pevplugshare/data/db/entity/LocationEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteLocationById", "id", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllLocations", "Lkotlinx/coroutines/flow/Flow;", "", "getFavouriteLocations", "getLocationById", "getLocationsNearby", "latitude", "", "longitude", "radiusKm", "(DDDLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "insertLocation", "insertLocations", "locations", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "searchLocations", "query", "updateLocation", "app_release"})
@androidx.room.Dao()
public abstract interface LocationDao {
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertLocation(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.LocationEntity location, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertLocations(@org.jetbrains.annotations.NotNull()
    java.util.List<com.pevplugshare.data.db.entity.LocationEntity> locations, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Update()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateLocation(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.LocationEntity location, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Delete()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteLocation(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.LocationEntity location, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM locations WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteLocationById(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM locations WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getLocationById(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pevplugshare.data.db.entity.LocationEntity> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM locations ORDER BY lastModified DESC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.pevplugshare.data.db.entity.LocationEntity>> getAllLocations();
    
    @androidx.room.Query(value = "SELECT * FROM locations WHERE isFavourite = 1 ORDER BY lastModified DESC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.pevplugshare.data.db.entity.LocationEntity>> getFavouriteLocations();
    
    @androidx.room.Query(value = "SELECT * FROM locations WHERE name LIKE \'%\' || :query || \'%\' ORDER BY lastModified DESC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.pevplugshare.data.db.entity.LocationEntity>> searchLocations(@org.jetbrains.annotations.NotNull()
    java.lang.String query);
    
    @androidx.room.Query(value = "DELETE FROM locations")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteAllLocations(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "\n        SELECT * FROM locations \n        WHERE (6371 * acos(cos(radians(:latitude)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:longitude)) + sin(radians(:latitude)) * sin(radians(latitude)))) < :radiusKm\n    ")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getLocationsNearby(double latitude, double longitude, double radiusKm, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pevplugshare.data.db.entity.LocationEntity>> $completion);
}