package com.pevplugshare.data.db.entity;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u0006\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\b\n\u0002\b\b\n\u0002\u0010\t\n\u0002\b?\b\u0087\b\u0018\u00002\u00020\u0001B\u00dd\u0001\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\u0007\u0012\u0006\u0010\t\u001a\u00020\u0003\u0012\u0006\u0010\n\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u000b\u001a\u00020\f\u0012\u0006\u0010\r\u001a\u00020\u000e\u0012\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0012\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0013\u001a\u0004\u0018\u00010\u0003\u0012\u0006\u0010\u0014\u001a\u00020\u0003\u0012\u0006\u0010\u0015\u001a\u00020\u0003\u0012\u0006\u0010\u0016\u001a\u00020\u0017\u0012\b\b\u0002\u0010\u0018\u001a\u00020\f\u0012\b\b\u0002\u0010\u0019\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u001a\u001a\u0004\u0018\u00010\u0003\u0012\b\b\u0002\u0010\u001b\u001a\u00020\f\u0012\n\b\u0002\u0010\u001c\u001a\u0004\u0018\u00010\u0017\u00a2\u0006\u0002\u0010\u001dJ\t\u0010:\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010;\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010<\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010=\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010>\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010?\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010@\u001a\u00020\u0003H\u00c6\u0003J\t\u0010A\u001a\u00020\u0003H\u00c6\u0003J\t\u0010B\u001a\u00020\u0017H\u00c6\u0003J\t\u0010C\u001a\u00020\fH\u00c6\u0003J\t\u0010D\u001a\u00020\u0003H\u00c6\u0003J\t\u0010E\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010F\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010G\u001a\u00020\fH\u00c6\u0003J\u0010\u0010H\u001a\u0004\u0018\u00010\u0017H\u00c6\u0003\u00a2\u0006\u0002\u0010#J\u000b\u0010I\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010J\u001a\u00020\u0007H\u00c6\u0003J\t\u0010K\u001a\u00020\u0007H\u00c6\u0003J\t\u0010L\u001a\u00020\u0003H\u00c6\u0003J\t\u0010M\u001a\u00020\u0003H\u00c6\u0003J\t\u0010N\u001a\u00020\fH\u00c6\u0003J\t\u0010O\u001a\u00020\u000eH\u00c6\u0003J\u00fa\u0001\u0010P\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\u00072\b\b\u0002\u0010\t\u001a\u00020\u00032\b\b\u0002\u0010\n\u001a\u00020\u00032\b\b\u0002\u0010\u000b\u001a\u00020\f2\b\b\u0002\u0010\r\u001a\u00020\u000e2\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0012\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0013\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0014\u001a\u00020\u00032\b\b\u0002\u0010\u0015\u001a\u00020\u00032\b\b\u0002\u0010\u0016\u001a\u00020\u00172\b\b\u0002\u0010\u0018\u001a\u00020\f2\b\b\u0002\u0010\u0019\u001a\u00020\u00032\n\b\u0002\u0010\u001a\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u001b\u001a\u00020\f2\n\b\u0002\u0010\u001c\u001a\u0004\u0018\u00010\u0017H\u00c6\u0001\u00a2\u0006\u0002\u0010QJ\u0013\u0010R\u001a\u00020\f2\b\u0010S\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010T\u001a\u00020\u000eH\u00d6\u0001J\t\u0010U\u001a\u00020\u0003H\u00d6\u0001R\u0013\u0010\u0010\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001fR\u0011\u0010\u001b\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010!R\u0015\u0010\u001c\u001a\u0004\u0018\u00010\u0017\u00a2\u0006\n\n\u0002\u0010$\u001a\u0004\b\"\u0010#R\u0013\u0010\u0013\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010\u001fR\u0011\u0010\u0014\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\u001fR\u0013\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\'\u0010\u001fR\u0013\u0010\u001a\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010\u001fR\u0011\u0010\r\u001a\u00020\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b)\u0010*R\u0011\u0010\u000b\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b+\u0010!R\u0011\u0010\u0015\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b,\u0010\u001fR\u0016\u0010\u0002\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b-\u0010\u001fR\u0011\u0010\u0018\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010!R\u0011\u0010\u0016\u001a\u00020\u0017\u00a2\u0006\b\n\u0000\u001a\u0004\b.\u0010/R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b0\u00101R\u0011\u0010\b\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b2\u00101R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b3\u0010\u001fR\u0013\u0010\u000f\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b4\u0010\u001fR\u0013\u0010\u0011\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b5\u0010\u001fR\u0013\u0010\u0012\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b6\u0010\u001fR\u0011\u0010\n\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b7\u0010\u001fR\u0011\u0010\t\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b8\u0010\u001fR\u0011\u0010\u0019\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b9\u0010\u001f\u00a8\u0006V"}, d2 = {"Lcom/pevplugshare/data/db/entity/LocationEntity;", "", "id", "", "name", "description", "latitude", "", "longitude", "socketTypes", "publicPrivate", "hasFee", "", "gpoCount", "", "openingHours", "additionalInfo", "providerName", "providerWebsite", "chargingCosts", "color", "icon", "lastModified", "", "isFavourite", "syncStatus", "descriptionHash", "aiParsed", "aiParsedAt", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;DDLjava/lang/String;Ljava/lang/String;ZILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZLjava/lang/String;Ljava/lang/String;ZLjava/lang/Long;)V", "getAdditionalInfo", "()Ljava/lang/String;", "getAiParsed", "()Z", "getAiParsedAt", "()Ljava/lang/Long;", "Ljava/lang/Long;", "getChargingCosts", "getColor", "getDescription", "getDescriptionHash", "getGpoCount", "()I", "getHasFee", "getIcon", "getId", "getLastModified", "()J", "getLatitude", "()D", "getLongitude", "getName", "getOpeningHours", "getProviderName", "getProviderWebsite", "getPublicPrivate", "getSocketTypes", "getSyncStatus", "component1", "component10", "component11", "component12", "component13", "component14", "component15", "component16", "component17", "component18", "component19", "component2", "component20", "component21", "component22", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;DDLjava/lang/String;Ljava/lang/String;ZILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZLjava/lang/String;Ljava/lang/String;ZLjava/lang/Long;)Lcom/pevplugshare/data/db/entity/LocationEntity;", "equals", "other", "hashCode", "toString", "app_release"})
@androidx.room.Entity(tableName = "locations")
public final class LocationEntity {
    @androidx.room.PrimaryKey()
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String id = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String name = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String description = null;
    private final double latitude = 0.0;
    private final double longitude = 0.0;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String socketTypes = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String publicPrivate = null;
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
    private final java.lang.String syncStatus = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String descriptionHash = null;
    private final boolean aiParsed = false;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Long aiParsedAt = null;
    
    public LocationEntity(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.Nullable()
    java.lang.String description, double latitude, double longitude, @org.jetbrains.annotations.NotNull()
    java.lang.String socketTypes, @org.jetbrains.annotations.NotNull()
    java.lang.String publicPrivate, boolean hasFee, int gpoCount, @org.jetbrains.annotations.Nullable()
    java.lang.String openingHours, @org.jetbrains.annotations.Nullable()
    java.lang.String additionalInfo, @org.jetbrains.annotations.Nullable()
    java.lang.String providerName, @org.jetbrains.annotations.Nullable()
    java.lang.String providerWebsite, @org.jetbrains.annotations.Nullable()
    java.lang.String chargingCosts, @org.jetbrains.annotations.NotNull()
    java.lang.String color, @org.jetbrains.annotations.NotNull()
    java.lang.String icon, long lastModified, boolean isFavourite, @org.jetbrains.annotations.NotNull()
    java.lang.String syncStatus, @org.jetbrains.annotations.Nullable()
    java.lang.String descriptionHash, boolean aiParsed, @org.jetbrains.annotations.Nullable()
    java.lang.Long aiParsedAt) {
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
    public final java.lang.String getSocketTypes() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getPublicPrivate() {
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
    public final java.lang.String getSyncStatus() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getDescriptionHash() {
        return null;
    }
    
    public final boolean getAiParsed() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long getAiParsedAt() {
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
    public final java.lang.String component19() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component20() {
        return null;
    }
    
    public final boolean component21() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long component22() {
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
    public final java.lang.String component6() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component7() {
        return null;
    }
    
    public final boolean component8() {
        return false;
    }
    
    public final int component9() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.db.entity.LocationEntity copy(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.Nullable()
    java.lang.String description, double latitude, double longitude, @org.jetbrains.annotations.NotNull()
    java.lang.String socketTypes, @org.jetbrains.annotations.NotNull()
    java.lang.String publicPrivate, boolean hasFee, int gpoCount, @org.jetbrains.annotations.Nullable()
    java.lang.String openingHours, @org.jetbrains.annotations.Nullable()
    java.lang.String additionalInfo, @org.jetbrains.annotations.Nullable()
    java.lang.String providerName, @org.jetbrains.annotations.Nullable()
    java.lang.String providerWebsite, @org.jetbrains.annotations.Nullable()
    java.lang.String chargingCosts, @org.jetbrains.annotations.NotNull()
    java.lang.String color, @org.jetbrains.annotations.NotNull()
    java.lang.String icon, long lastModified, boolean isFavourite, @org.jetbrains.annotations.NotNull()
    java.lang.String syncStatus, @org.jetbrains.annotations.Nullable()
    java.lang.String descriptionHash, boolean aiParsed, @org.jetbrains.annotations.Nullable()
    java.lang.Long aiParsedAt) {
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