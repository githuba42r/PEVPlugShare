package com.pevplugshare.util;

/**
 * Utility object for calculating distances between GPS coordinates using Haversine formula
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0002\b\u0006\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J&\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\u00042\u0006\u0010\b\u001a\u00020\u00042\u0006\u0010\t\u001a\u00020\u0004J\u000e\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u0004J.\u0010\r\u001a\u00020\u000e2\u0006\u0010\u0006\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\u00042\u0006\u0010\b\u001a\u00020\u00042\u0006\u0010\t\u001a\u00020\u00042\u0006\u0010\u000f\u001a\u00020\u0004R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0010"}, d2 = {"Lcom/pevplugshare/util/DistanceCalculator;", "", "()V", "EARTH_RADIUS_METERS", "", "calculateDistance", "lat1", "lon1", "lat2", "lon2", "formatDistance", "", "distanceMeters", "isWithinRadius", "", "radiusMeters", "app_release"})
public final class DistanceCalculator {
    private static final double EARTH_RADIUS_METERS = 6371000.0;
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.util.DistanceCalculator INSTANCE = null;
    
    private DistanceCalculator() {
        super();
    }
    
    /**
     * Calculate line-of-sight distance between two GPS coordinates using Haversine formula
     *
     * @param lat1 Latitude of first point in degrees
     * @param lon1 Longitude of first point in degrees
     * @param lat2 Latitude of second point in degrees
     * @param lon2 Longitude of second point in degrees
     * @return Distance in meters
     */
    public final double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        return 0.0;
    }
    
    /**
     * Check if two locations are within a given radius
     *
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @param radiusMeters Radius in meters
     * @return true if locations are within radius
     */
    public final boolean isWithinRadius(double lat1, double lon1, double lat2, double lon2, double radiusMeters) {
        return false;
    }
    
    /**
     * Format distance for display
     *
     * @param distanceMeters Distance in meters
     * @return Formatted string (e.g., "2.3 km" or "150 m")
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String formatDistance(double distanceMeters) {
        return null;
    }
}