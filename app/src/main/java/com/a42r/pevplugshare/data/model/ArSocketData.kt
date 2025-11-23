package com.a42r.pevplugshare.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * AR-specific data for socket location in 3D space
 * Uses East-North-Up (ENU) coordinate system for local offsets
 */
@Parcelize
data class ArSocketData(
    // 3D offset from camera position to socket in ENU coordinates (meters)
    val offsetEast: Float = 0f,
    val offsetNorth: Float = 0f,
    val offsetUp: Float = 0f,
    
    // Original camera data used to compute offsets
    val cameraLatitude: Double? = null,
    val cameraLongitude: Double? = null,
    val cameraAltitude: Double? = null,
    val cameraBearing: Float? = null,
    val cameraPitch: Float? = null,
    
    // Computed socket absolute position (optional, for verification)
    val socketLatitude: Double? = null,
    val socketLongitude: Double? = null,
    val socketAltitude: Double? = null,
    
    // Confidence and metadata
    val confidence: Float = 0f,  // 0.0 to 1.0
    val timestamp: Long = System.currentTimeMillis(),
    val method: ArLocalizationMethod = ArLocalizationMethod.GEOSPATIAL_API
) : Parcelable

enum class ArLocalizationMethod {
    GPS_BEARING,      // Basic GPS + compass bearing
    GEOSPATIAL_API,   // ARCore Geospatial API
    VPS,              // ARCore VPS (Visual Positioning System)
    PHOTO_MATCHING    // On-device photo feature matching
}
