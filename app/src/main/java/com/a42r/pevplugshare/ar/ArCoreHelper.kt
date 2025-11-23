package com.a42r.pevplugshare.ar

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.ar.core.*
import com.google.ar.core.exceptions.*

/**
 * Helper class for ARCore session management and geospatial API
 */
class ArCoreHelper(private val activity: Activity) {
    
    private var arSession: Session? = null
    private var isGeospatialAvailable = false
    
    companion object {
        private const val TAG = "ArCoreHelper"
        
        /**
         * Check if ARCore is supported and installed on this device
         */
        fun isArCoreSupported(context: Context): Boolean {
            return try {
                when (ArCoreApk.getInstance().checkAvailability(context)) {
                    ArCoreApk.Availability.SUPPORTED_INSTALLED -> true
                    ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
                    ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                        // ARCore is supported but needs installation/update
                        true
                    }
                    else -> false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking ARCore availability", e)
                false
            }
        }
        
        /**
         * Request ARCore installation if needed
         * @return true if installation requested, false if already installed or not needed
         */
        fun requestInstallIfNeeded(activity: Activity): Boolean {
            return try {
                when (ArCoreApk.getInstance().requestInstall(activity, true)) {
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> true
                    ArCoreApk.InstallStatus.INSTALLED -> false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting ARCore installation", e)
                false
            }
        }
    }
    
    /**
     * Initialize ARCore session with geospatial mode
     */
    fun initializeSession(): Boolean {
        return try {
            // Check if installation is needed
            if (requestInstallIfNeeded(activity)) {
                Log.i(TAG, "ARCore installation requested")
                return false
            }
            
            val session = Session(activity)
            val config = Config(session).apply {
                // Enable geospatial mode for outdoor tracking
                geospatialMode = Config.GeospatialMode.ENABLED
                
                // Optimize for tracking
                planeFindingMode = Config.PlaneFindingMode.DISABLED
                lightEstimationMode = Config.LightEstimationMode.DISABLED
                
                // Enable depth if available (helps with occlusion)
                depthMode = Config.DepthMode.AUTOMATIC
            }
            
            session.configure(config)
            arSession = session
            
            Log.i(TAG, "ARCore session initialized successfully")
            true
        } catch (e: UnavailableArcoreNotInstalledException) {
            Log.e(TAG, "ARCore not installed", e)
            false
        } catch (e: UnavailableApkTooOldException) {
            Log.e(TAG, "ARCore APK too old", e)
            false
        } catch (e: UnavailableSdkTooOldException) {
            Log.e(TAG, "SDK too old for ARCore", e)
            false
        } catch (e: UnavailableDeviceNotCompatibleException) {
            Log.e(TAG, "Device not compatible with ARCore", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ARCore session", e)
            false
        }
    }
    
    /**
     * Resume AR session
     */
    fun resume() {
        arSession?.resume()
    }
    
    /**
     * Pause AR session
     */
    fun pause() {
        arSession?.pause()
    }
    
    /**
     * Update session and get current frame
     */
    fun update(): Frame? {
        return try {
            arSession?.update()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating AR frame", e)
            null
        }
    }
    
    /**
     * Get current geospatial pose
     * @return Earth position or null if not available
     */
    fun getGeospatialPose(): Earth? {
        return try {
            arSession?.earth?.takeIf { it.trackingState == TrackingState.TRACKING }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting geospatial pose", e)
            null
        }
    }
    
    /**
     * Check if geospatial tracking is ready
     */
    fun isGeospatialReady(): Boolean {
        val earth = arSession?.earth ?: return false
        return earth.trackingState == TrackingState.TRACKING &&
               earth.earthState == Earth.EarthState.ENABLED
    }
    
    /**
     * Get geospatial accuracy (horizontal and vertical in meters)
     */
    fun getGeospatialAccuracy(): Pair<Float, Float>? {
        return try {
            val earth = arSession?.earth
            if (earth?.trackingState == TrackingState.TRACKING) {
                val cameraGeospatialPose = earth.cameraGeospatialPose
                Pair(
                    cameraGeospatialPose.horizontalAccuracy.toFloat(),
                    cameraGeospatialPose.verticalAccuracy.toFloat()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting geospatial accuracy", e)
            null
        }
    }
    
    /**
     * Create a geospatial anchor at specific lat/lon/alt
     */
    fun createGeospatialAnchor(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        qx: Float = 0f,
        qy: Float = 0f,
        qz: Float = 0f,
        qw: Float = 1f
    ): Anchor? {
        return try {
            val earth = arSession?.earth
            if (earth?.trackingState == TrackingState.TRACKING) {
                earth.createAnchor(latitude, longitude, altitude, qx, qy, qz, qw)
            } else {
                Log.w(TAG, "Cannot create anchor - geospatial tracking not ready")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating geospatial anchor", e)
            null
        }
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        arSession?.close()
        arSession = null
    }
    
    /**
     * Get the AR session
     */
    fun getSession(): Session? = arSession
}
