package com.a42r.pevplugshare.util

import kotlin.math.*

/**
 * Utility object for calculating distances between GPS coordinates using Haversine formula
 */
object DistanceCalculator {
    
    private const val EARTH_RADIUS_METERS = 6371000.0 // Earth's radius in meters
    
    /**
     * Calculate line-of-sight distance between two GPS coordinates using Haversine formula
     * 
     * @param lat1 Latitude of first point in degrees
     * @param lon1 Longitude of first point in degrees
     * @param lat2 Latitude of second point in degrees
     * @param lon2 Longitude of second point in degrees
     * @return Distance in meters
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLat = Math.toRadians(lat2 - lat1)
        val deltaLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(deltaLat / 2).pow(2.0) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLon / 2).pow(2.0)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return EARTH_RADIUS_METERS * c
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
    fun isWithinRadius(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
        radiusMeters: Double
    ): Boolean {
        return calculateDistance(lat1, lon1, lat2, lon2) <= radiusMeters
    }
    
    /**
     * Format distance for display
     * 
     * @param distanceMeters Distance in meters
     * @return Formatted string (e.g., "2.3 km" or "150 m")
     */
    fun formatDistance(distanceMeters: Double): String {
        return when {
            distanceMeters >= 1000 -> "%.1f km".format(distanceMeters / 1000)
            else -> "%.0f m".format(distanceMeters)
        }
    }
}
