package com.pevplugshare.data.db.entity;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u0006\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\b\n\u0002\b\u0006\n\u0002\u0010\t\n\u0002\b3\b\u0087\b\u0018\u00002\u00020\u0001B\u00ad\u0001\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\u0007\u0012\u0006\u0010\t\u001a\u00020\u0003\u0012\u0006\u0010\n\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u000b\u001a\u00020\f\u0012\u0006\u0010\r\u001a\u00020\u000e\u0012\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0012\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0013\u001a\u0004\u0018\u00010\u0003\u0012\u0006\u0010\u0014\u001a\u00020\u0015\u0012\b\b\u0002\u0010\u0016\u001a\u00020\u000e\u0012\n\b\u0002\u0010\u0017\u001a\u0004\u0018\u00010\u0015\u00a2\u0006\u0002\u0010\u0018J\t\u00101\u001a\u00020\u0003H\u00c6\u0003J\u000b\u00102\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u00103\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u00104\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u00105\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u00106\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u00107\u001a\u00020\u0015H\u00c6\u0003J\t\u00108\u001a\u00020\u000eH\u00c6\u0003J\u0010\u00109\u001a\u0004\u0018\u00010\u0015H\u00c6\u0003\u00a2\u0006\u0002\u0010%J\t\u0010:\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010;\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010<\u001a\u00020\u0007H\u00c6\u0003J\t\u0010=\u001a\u00020\u0007H\u00c6\u0003J\t\u0010>\u001a\u00020\u0003H\u00c6\u0003J\t\u0010?\u001a\u00020\u0003H\u00c6\u0003J\t\u0010@\u001a\u00020\fH\u00c6\u0003J\t\u0010A\u001a\u00020\u000eH\u00c6\u0003J\u00c6\u0001\u0010B\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\u00072\b\b\u0002\u0010\t\u001a\u00020\u00032\b\b\u0002\u0010\n\u001a\u00020\u00032\b\b\u0002\u0010\u000b\u001a\u00020\f2\b\b\u0002\u0010\r\u001a\u00020\u000e2\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0012\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0013\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0014\u001a\u00020\u00152\b\b\u0002\u0010\u0016\u001a\u00020\u000e2\n\b\u0002\u0010\u0017\u001a\u0004\u0018\u00010\u0015H\u00c6\u0001\u00a2\u0006\u0002\u0010CJ\u0013\u0010D\u001a\u00020\f2\b\u0010E\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010F\u001a\u00020\u000eH\u00d6\u0001J\t\u0010G\u001a\u00020\u0003H\u00d6\u0001R\u0013\u0010\u0010\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u001aR\u0013\u0010\u0013\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001aR\u0011\u0010\u0014\u001a\u00020\u0015\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u001dR\u0013\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001aR\u0011\u0010\r\u001a\u00020\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010 R\u0011\u0010\u000b\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010\"R\u0016\u0010\u0002\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010\u001aR\u0015\u0010\u0017\u001a\u0004\u0018\u00010\u0015\u00a2\u0006\n\n\u0002\u0010&\u001a\u0004\b$\u0010%R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\'\u0010(R\u0011\u0010\b\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b)\u0010(R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b*\u0010\u001aR\u0013\u0010\u000f\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b+\u0010\u001aR\u0013\u0010\u0011\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b,\u0010\u001aR\u0013\u0010\u0012\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b-\u0010\u001aR\u0011\u0010\n\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b.\u0010\u001aR\u0011\u0010\t\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b/\u0010\u001aR\u0011\u0010\u0016\u001a\u00020\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b0\u0010 \u00a8\u0006H"}, d2 = {"Lcom/pevplugshare/data/db/entity/PendingLocationEntity;", "", "id", "", "name", "description", "latitude", "", "longitude", "socketTypes", "publicPrivate", "hasFee", "", "gpoCount", "", "openingHours", "additionalInfo", "providerName", "providerWebsite", "chargingCosts", "createdAt", "", "syncAttempts", "lastSyncAttempt", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;DDLjava/lang/String;Ljava/lang/String;ZILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JILjava/lang/Long;)V", "getAdditionalInfo", "()Ljava/lang/String;", "getChargingCosts", "getCreatedAt", "()J", "getDescription", "getGpoCount", "()I", "getHasFee", "()Z", "getId", "getLastSyncAttempt", "()Ljava/lang/Long;", "Ljava/lang/Long;", "getLatitude", "()D", "getLongitude", "getName", "getOpeningHours", "getProviderName", "getProviderWebsite", "getPublicPrivate", "getSocketTypes", "getSyncAttempts", "component1", "component10", "component11", "component12", "component13", "component14", "component15", "component16", "component17", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;DDLjava/lang/String;Ljava/lang/String;ZILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JILjava/lang/Long;)Lcom/pevplugshare/data/db/entity/PendingLocationEntity;", "equals", "other", "hashCode", "toString", "app_debug"})
@androidx.room.Entity(tableName = "pending_locations")
public final class PendingLocationEntity {
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
    private final long createdAt = 0L;
    private final int syncAttempts = 0;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Long lastSyncAttempt = null;
    
    public PendingLocationEntity(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.Nullable()
    java.lang.String description, double latitude, double longitude, @org.jetbrains.annotations.NotNull()
    java.lang.String socketTypes, @org.jetbrains.annotations.NotNull()
    java.lang.String publicPrivate, boolean hasFee, int gpoCount, @org.jetbrains.annotations.Nullable()
    java.lang.String openingHours, @org.jetbrains.annotations.Nullable()
    java.lang.String additionalInfo, @org.jetbrains.annotations.Nullable()
    java.lang.String providerName, @org.jetbrains.annotations.Nullable()
    java.lang.String providerWebsite, @org.jetbrains.annotations.Nullable()
    java.lang.String chargingCosts, long createdAt, int syncAttempts, @org.jetbrains.annotations.Nullable()
    java.lang.Long lastSyncAttempt) {
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
    
    public final long getCreatedAt() {
        return 0L;
    }
    
    public final int getSyncAttempts() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long getLastSyncAttempt() {
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
    
    public final long component15() {
        return 0L;
    }
    
    public final int component16() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long component17() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
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
    public final com.pevplugshare.data.db.entity.PendingLocationEntity copy(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.Nullable()
    java.lang.String description, double latitude, double longitude, @org.jetbrains.annotations.NotNull()
    java.lang.String socketTypes, @org.jetbrains.annotations.NotNull()
    java.lang.String publicPrivate, boolean hasFee, int gpoCount, @org.jetbrains.annotations.Nullable()
    java.lang.String openingHours, @org.jetbrains.annotations.Nullable()
    java.lang.String additionalInfo, @org.jetbrains.annotations.Nullable()
    java.lang.String providerName, @org.jetbrains.annotations.Nullable()
    java.lang.String providerWebsite, @org.jetbrains.annotations.Nullable()
    java.lang.String chargingCosts, long createdAt, int syncAttempts, @org.jetbrains.annotations.Nullable()
    java.lang.Long lastSyncAttempt) {
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