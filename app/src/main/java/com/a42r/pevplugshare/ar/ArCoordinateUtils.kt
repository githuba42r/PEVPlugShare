package com.a42r.pevplugshare.ar

import android.location.Location
import kotlin.math.*

/**
 * Utility functions for AR coordinate calculations
 * Handles conversions between geographic coordinates and local ENU (East-North-Up) coordinate system
 */
object ArCoordinateUtils {
    
    private const val EARTH_RADIUS = 6378137.0 // meters (WGS84)
    
    /**
     * Calculate 3D offset in ENU coordinates from camera position to socket
     * 
     * @param cameraLat Camera latitude in degrees
     * @param cameraLon Camera longitude in degrees
     * @param cameraAlt Camera altitude in meters
     * @param bearing Camera azimuth (compass bearing) in degrees (0-360)
     * @param pitch Camera pitch in degrees (positive = looking up, negative = looking down)
     * @param distance Estimated distance to socket in meters
     * @return Triple of (offsetEast, offsetNorth, offsetUp) in meters
     */
    fun calculateENU(
        cameraLat: Double,
        cameraLon: Double,
        cameraAlt: Double,
        bearing: Float,
        pitch: Float,
        distance: Float
    ): Triple<Float, Float, Float> {
        // Convert angles to radians
        val bearingRad = Math.toRadians(bearing.toDouble())
        val pitchRad = Math.toRadians(pitch.toDouble())
        
        // Calculate horizontal distance (ground plane projection)
        val horizontalDist = distance * cos(pitchRad)
        
        // Calculate ENU offsets
        val offsetEast = (horizontalDist * sin(bearingRad)).toFloat()
        val offsetNorth = (horizontalDist * cos(bearingRad)).toFloat()
        val offsetUp = (distance * sin(pitchRad)).toFloat()
        
        return Triple(offsetEast, offsetNorth, offsetUp)
    }
    
    /**
     * Convert ENU offset to geographic coordinates
     * 
     * @param originLat Origin latitude in degrees
     * @param originLon Origin longitude in degrees
     * @param originAlt Origin altitude in meters
     * @param offsetE Offset east in meters
     * @param offsetN Offset north in meters
     * @param offsetU Offset up in meters
     * @return Triple of (latitude, longitude, altitude)
     */
    fun enuToGeographic(
        originLat: Double,
        originLon: Double,
        originAlt: Double,
        offsetE: Float,
        offsetN: Float,
        offsetU: Float
    ): Triple<Double, Double, Double> {
        val originLatRad = Math.toRadians(originLat)
        
        // Calculate lat/lon deltas
        val deltaLat = offsetN / EARTH_RADIUS
        val deltaLon = offsetE / (EARTH_RADIUS * cos(originLatRad))
        
        // Convert back to degrees
        val targetLat = originLat + Math.toDegrees(deltaLat)
        val targetLon = originLon + Math.toDegrees(deltaLon)
        val targetAlt = originAlt + offsetU
        
        return Triple(targetLat, targetLon, targetAlt)
    }
    
    /**
     * Calculate bearing from one point to another
     * 
     * @param fromLat Starting latitude in degrees
     * @param fromLon Starting longitude in degrees
     * @param toLat Ending latitude in degrees
     * @param toLon Ending longitude in degrees
     * @return Bearing in degrees (0-360)
     */
    fun calculateBearing(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Float {
        val fromLatRad = Math.toRadians(fromLat)
        val fromLonRad = Math.toRadians(fromLon)
        val toLatRad = Math.toRadians(toLat)
        val toLonRad = Math.toRadians(toLon)
        
        val dLon = toLonRad - fromLonRad
        
        val y = sin(dLon) * cos(toLatRad)
        val x = cos(fromLatRad) * sin(toLatRad) - sin(fromLatRad) * cos(toLatRad) * cos(dLon)
        
        val bearingRad = atan2(y, x)
        val bearingDeg = Math.toDegrees(bearingRad)
        
        return ((bearingDeg + 360) % 360).toFloat()
    }
    
    /**
     * Calculate distance between two geographic points
     * Uses Haversine formula
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
    
    /**
     * Calculate 3D distance including altitude difference
     */
    fun calculate3DDistance(
        lat1: Double, lon1: Double, alt1: Double,
        lat2: Double, lon2: Double, alt2: Double
    ): Float {
        val horizontalDist = calculateDistance(lat1, lon1, lat2, lon2)
        val verticalDist = (alt2 - alt1).toFloat()
        return sqrt(horizontalDist * horizontalDist + verticalDist * verticalDist)
    }
    
    /**
     * Format distance for display
     */
    fun formatDistance(meters: Float): String {
        return when {
            meters < 1 -> String.format("%.1f cm", meters * 100)
            meters < 1000 -> String.format("%.1f m", meters)
            else -> String.format("%.2f km", meters / 1000)
        }
    }
    
    /**
     * Format height for display (relative to current position)
     */
    fun formatHeight(meters: Float): String {
        return when {
            abs(meters) < 0.1 -> "same level"
            meters > 0 -> String.format("%.1f m above", meters)
            else -> String.format("%.1f m below", abs(meters))
        }
    }
}
