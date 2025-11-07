package com.pevplugshare.util;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0002\b\u0006\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u0004J&\u0010\b\u001a\u00020\u00042\u0006\u0010\t\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u00042\u0006\u0010\u000b\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\u0004J.\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u00042\u0006\u0010\u0010\u001a\u00020\u00042\u0006\u0010\u0011\u001a\u00020\u00042\u0006\u0010\u0012\u001a\u00020\u00042\u0006\u0010\u0013\u001a\u00020\u0004R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"}, d2 = {"Lcom/pevplugshare/util/LocationUtils;", "", "()V", "EARTH_RADIUS_METERS", "", "formatDistance", "", "meters", "haversineDistance", "lat1", "lon1", "lat2", "lon2", "isWithinRadius", "", "userLat", "userLon", "targetLat", "targetLon", "radiusMeters", "app_debug"})
public final class LocationUtils {
    private static final double EARTH_RADIUS_METERS = 6371000.0;
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.util.LocationUtils INSTANCE = null;
    
    private LocationUtils() {
        super();
    }
    
    public final double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        return 0.0;
    }
    
    public final boolean isWithinRadius(double userLat, double userLon, double targetLat, double targetLon, double radiusMeters) {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String formatDistance(double meters) {
        return null;
    }
}