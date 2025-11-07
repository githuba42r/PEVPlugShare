package com.pevplugshare.ui.add;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b-\b\u0086\b\u0018\u00002\u00020\u0001B\u008d\u0001\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0007\u001a\u00020\b\u0012\u000e\b\u0002\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\n\u0012\b\b\u0002\u0010\f\u001a\u00020\r\u0012\b\b\u0002\u0010\u000e\u001a\u00020\u000f\u0012\b\b\u0002\u0010\u0010\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0011\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0012\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0013\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0014\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0015\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0016J\t\u0010*\u001a\u00020\u0003H\u00c6\u0003J\t\u0010+\u001a\u00020\u0003H\u00c6\u0003J\t\u0010,\u001a\u00020\u0003H\u00c6\u0003J\t\u0010-\u001a\u00020\u0003H\u00c6\u0003J\t\u0010.\u001a\u00020\u0003H\u00c6\u0003J\t\u0010/\u001a\u00020\u0005H\u00c6\u0003J\t\u00100\u001a\u00020\u0005H\u00c6\u0003J\t\u00101\u001a\u00020\bH\u00c6\u0003J\u000f\u00102\u001a\b\u0012\u0004\u0012\u00020\u000b0\nH\u00c6\u0003J\t\u00103\u001a\u00020\rH\u00c6\u0003J\t\u00104\u001a\u00020\u000fH\u00c6\u0003J\t\u00105\u001a\u00020\u0003H\u00c6\u0003J\t\u00106\u001a\u00020\u0003H\u00c6\u0003J\u0091\u0001\u00107\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00052\b\b\u0002\u0010\u0007\u001a\u00020\b2\u000e\b\u0002\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\n2\b\b\u0002\u0010\f\u001a\u00020\r2\b\b\u0002\u0010\u000e\u001a\u00020\u000f2\b\b\u0002\u0010\u0010\u001a\u00020\u00032\b\b\u0002\u0010\u0011\u001a\u00020\u00032\b\b\u0002\u0010\u0012\u001a\u00020\u00032\b\b\u0002\u0010\u0013\u001a\u00020\u00032\b\b\u0002\u0010\u0014\u001a\u00020\u00032\b\b\u0002\u0010\u0015\u001a\u00020\u0003H\u00c6\u0001J\u0013\u00108\u001a\u00020\u000f2\b\u00109\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010:\u001a\u00020\bH\u00d6\u0001J\t\u0010;\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0012\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018R\u0011\u0010\u0015\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0018R\u0011\u0010\u0010\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0018R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001cR\u0011\u0010\u000e\u001a\u00020\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001eR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010 R\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010 R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010\u0018R\u0011\u0010\u0011\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010\u0018R\u0011\u0010\u0013\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010\u0018R\u0011\u0010\u0014\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010\u0018R\u0011\u0010\f\u001a\u00020\r\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\'R\u0017\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\n\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010)\u00a8\u0006<"}, d2 = {"Lcom/pevplugshare/ui/add/LocationFormData;", "", "name", "", "latitude", "", "longitude", "gpoCount", "", "socketTypes", "", "Lcom/pevplugshare/data/model/SocketType;", "publicPrivate", "Lcom/pevplugshare/data/model/LocationAccessType;", "hasFee", "", "description", "openingHours", "additionalInfo", "providerName", "providerWebsite", "chargingCosts", "(Ljava/lang/String;DDILjava/util/List;Lcom/pevplugshare/data/model/LocationAccessType;ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "getAdditionalInfo", "()Ljava/lang/String;", "getChargingCosts", "getDescription", "getGpoCount", "()I", "getHasFee", "()Z", "getLatitude", "()D", "getLongitude", "getName", "getOpeningHours", "getProviderName", "getProviderWebsite", "getPublicPrivate", "()Lcom/pevplugshare/data/model/LocationAccessType;", "getSocketTypes", "()Ljava/util/List;", "component1", "component10", "component11", "component12", "component13", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "other", "hashCode", "toString", "app_debug"})
public final class LocationFormData {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String name = null;
    private final double latitude = 0.0;
    private final double longitude = 0.0;
    private final int gpoCount = 0;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.pevplugshare.data.model.SocketType> socketTypes = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.model.LocationAccessType publicPrivate = null;
    private final boolean hasFee = false;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String description = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String openingHours = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String additionalInfo = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String providerName = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String providerWebsite = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String chargingCosts = null;
    
    public LocationFormData(@org.jetbrains.annotations.NotNull()
    java.lang.String name, double latitude, double longitude, int gpoCount, @org.jetbrains.annotations.NotNull()
    java.util.List<? extends com.pevplugshare.data.model.SocketType> socketTypes, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.model.LocationAccessType publicPrivate, boolean hasFee, @org.jetbrains.annotations.NotNull()
    java.lang.String description, @org.jetbrains.annotations.NotNull()
    java.lang.String openingHours, @org.jetbrains.annotations.NotNull()
    java.lang.String additionalInfo, @org.jetbrains.annotations.NotNull()
    java.lang.String providerName, @org.jetbrains.annotations.NotNull()
    java.lang.String providerWebsite, @org.jetbrains.annotations.NotNull()
    java.lang.String chargingCosts) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getName() {
        return null;
    }
    
    public final double getLatitude() {
        return 0.0;
    }
    
    public final double getLongitude() {
        return 0.0;
    }
    
    public final int getGpoCount() {
        return 0;
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
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getDescription() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getOpeningHours() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getAdditionalInfo() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getProviderName() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getProviderWebsite() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getChargingCosts() {
        return null;
    }
    
    public LocationFormData() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component10() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component11() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component12() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component13() {
        return null;
    }
    
    public final double component2() {
        return 0.0;
    }
    
    public final double component3() {
        return 0.0;
    }
    
    public final int component4() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.pevplugshare.data.model.SocketType> component5() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.model.LocationAccessType component6() {
        return null;
    }
    
    public final boolean component7() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component8() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component9() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.ui.add.LocationFormData copy(@org.jetbrains.annotations.NotNull()
    java.lang.String name, double latitude, double longitude, int gpoCount, @org.jetbrains.annotations.NotNull()
    java.util.List<? extends com.pevplugshare.data.model.SocketType> socketTypes, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.model.LocationAccessType publicPrivate, boolean hasFee, @org.jetbrains.annotations.NotNull()
    java.lang.String description, @org.jetbrains.annotations.NotNull()
    java.lang.String openingHours, @org.jetbrains.annotations.NotNull()
    java.lang.String additionalInfo, @org.jetbrains.annotations.NotNull()
    java.lang.String providerName, @org.jetbrains.annotations.NotNull()
    java.lang.String providerWebsite, @org.jetbrains.annotations.NotNull()
    java.lang.String chargingCosts) {
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