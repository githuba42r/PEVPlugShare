package com.pevplugshare.data.model;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000N\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\b\n\u0002\b\b\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b<\b\u0086\b\u0018\u00002\u00020\u0001B\u00dd\u0001\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\u0007\u0012\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\n\u0012\u0006\u0010\f\u001a\u00020\r\u0012\b\b\u0002\u0010\u000e\u001a\u00020\u000f\u0012\u0006\u0010\u0010\u001a\u00020\u0011\u0012\n\b\u0002\u0010\u0012\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0013\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0014\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0015\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0016\u001a\u0004\u0018\u00010\u0003\u0012\u0006\u0010\u0017\u001a\u00020\u0003\u0012\u0006\u0010\u0018\u001a\u00020\u0003\u0012\u0006\u0010\u0019\u001a\u00020\u001a\u0012\b\b\u0002\u0010\u001b\u001a\u00020\u000f\u0012\b\b\u0002\u0010\u001c\u001a\u00020\u001d\u0012\u000e\b\u0002\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u001f0\n\u0012\n\b\u0002\u0010 \u001a\u0004\u0018\u00010\u0007\u00a2\u0006\u0002\u0010!J\t\u0010@\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010A\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010B\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010C\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010D\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010E\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010F\u001a\u00020\u0003H\u00c6\u0003J\t\u0010G\u001a\u00020\u0003H\u00c6\u0003J\t\u0010H\u001a\u00020\u001aH\u00c6\u0003J\t\u0010I\u001a\u00020\u000fH\u00c6\u0003J\t\u0010J\u001a\u00020\u001dH\u00c6\u0003J\t\u0010K\u001a\u00020\u0003H\u00c6\u0003J\u000f\u0010L\u001a\b\u0012\u0004\u0012\u00020\u001f0\nH\u00c6\u0003J\u0010\u0010M\u001a\u0004\u0018\u00010\u0007H\u00c6\u0003\u00a2\u0006\u0002\u0010(J\u000b\u0010N\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010O\u001a\u00020\u0007H\u00c6\u0003J\t\u0010P\u001a\u00020\u0007H\u00c6\u0003J\u000f\u0010Q\u001a\b\u0012\u0004\u0012\u00020\u000b0\nH\u00c6\u0003J\t\u0010R\u001a\u00020\rH\u00c6\u0003J\t\u0010S\u001a\u00020\u000fH\u00c6\u0003J\t\u0010T\u001a\u00020\u0011H\u00c6\u0003J\u00fa\u0001\u0010U\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\u00072\u000e\b\u0002\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\n2\b\b\u0002\u0010\f\u001a\u00020\r2\b\b\u0002\u0010\u000e\u001a\u00020\u000f2\b\b\u0002\u0010\u0010\u001a\u00020\u00112\n\b\u0002\u0010\u0012\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0013\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0014\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0015\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0016\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0017\u001a\u00020\u00032\b\b\u0002\u0010\u0018\u001a\u00020\u00032\b\b\u0002\u0010\u0019\u001a\u00020\u001a2\b\b\u0002\u0010\u001b\u001a\u00020\u000f2\b\b\u0002\u0010\u001c\u001a\u00020\u001d2\u000e\b\u0002\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u001f0\n2\n\b\u0002\u0010 \u001a\u0004\u0018\u00010\u0007H\u00c6\u0001\u00a2\u0006\u0002\u0010VJ\u0013\u0010W\u001a\u00020\u000f2\b\u0010X\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010Y\u001a\u00020\u0011H\u00d6\u0001J\t\u0010Z\u001a\u00020\u0003H\u00d6\u0001R\u0013\u0010\u0013\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010#R\u0013\u0010\u0016\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010#R\u0011\u0010\u0017\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010#R\u0013\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010#R\u0015\u0010 \u001a\u0004\u0018\u00010\u0007\u00a2\u0006\n\n\u0002\u0010)\u001a\u0004\b\'\u0010(R\u0011\u0010\u0010\u001a\u00020\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b*\u0010+R\u0011\u0010\u000e\u001a\u00020\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b,\u0010-R\u0011\u0010\u0018\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b.\u0010#R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b/\u0010#R\u0011\u0010\u001b\u001a\u00020\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010-R\u0011\u0010\u0019\u001a\u00020\u001a\u00a2\u0006\b\n\u0000\u001a\u0004\b0\u00101R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b2\u00103R\u0011\u0010\b\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b4\u00103R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b5\u0010#R\u0013\u0010\u0012\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b6\u0010#R\u0017\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u001f0\n\u00a2\u0006\b\n\u0000\u001a\u0004\b7\u00108R\u0013\u0010\u0014\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b9\u0010#R\u0013\u0010\u0015\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b:\u0010#R\u0011\u0010\f\u001a\u00020\r\u00a2\u0006\b\n\u0000\u001a\u0004\b;\u0010<R\u0017\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\n\u00a2\u0006\b\n\u0000\u001a\u0004\b=\u00108R\u0011\u0010\u001c\u001a\u00020\u001d\u00a2\u0006\b\n\u0000\u001a\u0004\b>\u0010?\u00a8\u0006["}, d2 = {"Lcom/pevplugshare/data/model/Location;", "", "id", "", "name", "description", "latitude", "", "longitude", "socketTypes", "", "Lcom/pevplugshare/data/model/SocketType;", "publicPrivate", "Lcom/pevplugshare/data/model/LocationAccessType;", "hasFee", "", "gpoCount", "", "openingHours", "additionalInfo", "providerName", "providerWebsite", "chargingCosts", "color", "icon", "lastModified", "", "isFavourite", "syncStatus", "Lcom/pevplugshare/data/model/SyncStatus;", "photos", "Lcom/pevplugshare/data/model/Photo;", "distanceMeters", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;DDLjava/util/List;Lcom/pevplugshare/data/model/LocationAccessType;ZILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZLcom/pevplugshare/data/model/SyncStatus;Ljava/util/List;Ljava/lang/Double;)V", "getAdditionalInfo", "()Ljava/lang/String;", "getChargingCosts", "getColor", "getDescription", "getDistanceMeters", "()Ljava/lang/Double;", "Ljava/lang/Double;", "getGpoCount", "()I", "getHasFee", "()Z", "getIcon", "getId", "getLastModified", "()J", "getLatitude", "()D", "getLongitude", "getName", "getOpeningHours", "getPhotos", "()Ljava/util/List;", "getProviderName", "getProviderWebsite", "getPublicPrivate", "()Lcom/pevplugshare/data/model/LocationAccessType;", "getSocketTypes", "getSyncStatus", "()Lcom/pevplugshare/data/model/SyncStatus;", "component1", "component10", "component11", "component12", "component13", "component14", "component15", "component16", "component17", "component18", "component19", "component2", "component20", "component21", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;DDLjava/util/List;Lcom/pevplugshare/data/model/LocationAccessType;ZILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZLcom/pevplugshare/data/model/SyncStatus;Ljava/util/List;Ljava/lang/Double;)Lcom/pevplugshare/data/model/Location;", "equals", "other", "hashCode", "toString", "app_debug"})
public final class Location {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String id = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String name = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String description = null;
    private final double latitude = 0.0;
    private final double longitude = 0.0;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.pevplugshare.data.model.SocketType> socketTypes = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.model.LocationAccessType publicPrivate = null;
    private final boolean hasFee = false;
    private final int gpoCount = 0;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String openingHours = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String additionalInfo = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String providerName = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String providerWebsite = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String chargingCosts = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String color = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String icon = null;
    private final long lastModified = 0L;
    private final boolean isFavourite = false;
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.model.SyncStatus syncStatus = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.pevplugshare.data.model.Photo> photos = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Double distanceMeters = null;
    
    public Location(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.Nullable()
    java.lang.String description, double latitude, double longitude, @org.jetbrains.annotations.NotNull()
    java.util.List<? extends com.pevplugshare.data.model.SocketType> socketTypes, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.model.LocationAccessType publicPrivate, boolean hasFee, int gpoCount, @org.jetbrains.annotations.Nullable()
    java.lang.String openingHours, @org.jetbrains.annotations.Nullable()
    java.lang.String additionalInfo, @org.jetbrains.annotations.Nullable()
    java.lang.String providerName, @org.jetbrains.annotations.Nullable()
    java.lang.String providerWebsite, @org.jetbrains.annotations.Nullable()
    java.lang.String chargingCosts, @org.jetbrains.annotations.NotNull()
    java.lang.String color, @org.jetbrains.annotations.NotNull()
    java.lang.String icon, long lastModified, boolean isFavourite, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.model.SyncStatus syncStatus, @org.jetbrains.annotations.NotNull()
    java.util.List<com.pevplugshare.data.model.Photo> photos, @org.jetbrains.annotations.Nullable()
    java.lang.Double distanceMeters) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getName() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getDescription() {
        return null;
    }
    
    public final double getLatitude() {
        return 0.0;
    }
    
    public final double getLongitude() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.pevplugshare.data.model.SocketType> getSocketTypes() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.model.LocationAccessType getPublicPrivate() {
        return null;
    }
    
    public final boolean getHasFee() {
        return false;
    }
    
    public final int getGpoCount() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getOpeningHours() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getAdditionalInfo() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getProviderName() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getProviderWebsite() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getChargingCosts() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getColor() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getIcon() {
        return null;
    }
    
    public final long getLastModified() {
        return 0L;
    }
    
    public final boolean isFavourite() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.model.SyncStatus getSyncStatus() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.pevplugshare.data.model.Photo> getPhotos() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double getDistanceMeters() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component10() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component11() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component12() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component13() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component14() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component15() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component16() {
        return null;
    }
    
    public final long component17() {
        return 0L;
    }
    
    public final boolean component18() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.model.SyncStatus component19() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.pevplugshare.data.model.Photo> component20() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double component21() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component3() {
        return null;
    }
    
    public final double component4() {
        return 0.0;
    }
    
    public final double component5() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.pevplugshare.data.model.SocketType> component6() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.model.LocationAccessType component7() {
        return null;
    }
    
    public final boolean component8() {
        return false;
    }
    
    public final int component9() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.model.Location copy(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.Nullable()
    java.lang.String description, double latitude, double longitude, @org.jetbrains.annotations.NotNull()
    java.util.List<? extends com.pevplugshare.data.model.SocketType> socketTypes, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.model.LocationAccessType publicPrivate, boolean hasFee, int gpoCount, @org.jetbrains.annotations.Nullable()
    java.lang.String openingHours, @org.jetbrains.annotations.Nullable()
    java.lang.String additionalInfo, @org.jetbrains.annotations.Nullable()
    java.lang.String providerName, @org.jetbrains.annotations.Nullable()
    java.lang.String providerWebsite, @org.jetbrains.annotations.Nullable()
    java.lang.String chargingCosts, @org.jetbrains.annotations.NotNull()
    java.lang.String color, @org.jetbrains.annotations.NotNull()
    java.lang.String icon, long lastModified, boolean isFavourite, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.model.SyncStatus syncStatus, @org.jetbrains.annotations.NotNull()
    java.util.List<com.pevplugshare.data.model.Photo> photos, @org.jetbrains.annotations.Nullable()
    java.lang.Double distanceMeters) {
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