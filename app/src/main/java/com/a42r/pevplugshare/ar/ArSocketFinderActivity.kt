package com.a42r.pevplugshare.ar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.databinding.ActivityArSocketFinderBinding
import com.a42r.pevplugshare.data.model.ArSocketData
import com.a42r.pevplugshare.data.model.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.ar.core.*
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * AR Socket Finder Activity
 * Provides augmented reality view to locate charging sockets with pixel-accurate targeting
 */
@AndroidEntryPoint
class ArSocketFinderActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityArSocketFinderBinding
    private var arCoreHelper: ArCoreHelper? = null
    private var currentMode = Mode.AR
    private var targetLocation: Location? = null
    private var socketAnchor: Anchor? = null
    
    // Location and sensors
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    
    // Sensor data
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var currentAzimuth = 0f
    private var currentPitch = 0f
    private var currentUserLat = 0.0
    private var currentUserLon = 0.0
    private var currentUserAlt = 0.0
    
    companion object {
        private const val TAG = "ArSocketFinder"
        private const val EXTRA_LOCATION = "extra_location"
        
        fun createIntent(context: Context, location: Location): Intent {
            return Intent(context, ArSocketFinderActivity::class.java).apply {
                putExtra(EXTRA_LOCATION, location)
            }
        }
    }
    
    enum class Mode {
        AR,       // ARCore mode
        COMPASS   // Fallback compass mode
    }
    
    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            initializeAR()
        } else {
            showError("Camera permission required for AR mode")
            switchToCompassMode()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArSocketFinderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get target location
        targetLocation = intent.getParcelableExtra(EXTRA_LOCATION)
        if (targetLocation == null) {
            Toast.makeText(this, "No location data provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Initialize services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        
        setupUI()
        checkPermissionsAndStart()
    }
    
    private fun setupUI() {
        targetLocation?.let { location ->
            binding.locationNameText.text = location.name
        }
        
        binding.modeToggleButton.setOnClickListener {
            when (currentMode) {
                Mode.AR -> switchToCompassMode()
                Mode.COMPASS -> switchToARMode()
            }
        }
    }
    
    private fun checkPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED) {
            // Check if ARCore is supported
            if (ArCoreHelper.isArCoreSupported(this)) {
                initializeAR()
            } else {
                showError("AR not supported on this device")
                switchToCompassMode()
            }
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }
    
    private fun initializeAR() {
        binding.loadingText.text = "Initializing AR..."
        
        arCoreHelper = ArCoreHelper(this)
        if (arCoreHelper?.initializeSession() == true) {
            binding.statusText.text = "AR Ready - Waiting for geospatial lock..."
            startARTracking()
        } else {
            showError("Failed to initialize AR")
            switchToCompassMode()
        }
    }
    
    private fun startARTracking() {
        // Get current location
        getCurrentLocation { lat, lon, alt ->
            currentUserLat = lat
            currentUserLon = lon
            currentUserAlt = alt
            
            // Create anchor for socket
            createSocketAnchor()
            
            // Start AR update loop
            startARUpdateLoop()
            
            binding.loadingOverlay.visibility = View.GONE
        }
    }
    
    private fun createSocketAnchor() {
        val location = targetLocation ?: return
        val arData = location.arSocketData
        
        if (arData != null && arData.cameraLatitude != null && arData.cameraLongitude != null) {
            // Use pre-computed AR offset data with valid camera position
            val (socketLat, socketLon, socketAlt) = ArCoordinateUtils.enuToGeographic(
                arData.cameraLatitude,
                arData.cameraLongitude,
                arData.cameraAltitude ?: 0.0,
                arData.offsetEast,
                arData.offsetNorth,
                arData.offsetUp
            )
            
            socketAnchor = arCoreHelper?.createGeospatialAnchor(
                socketLat, socketLon, socketAlt
            )
            Log.d(TAG, "Socket anchor created from AR data: $socketLat, $socketLon, $socketAlt")
        } else {
            // No valid AR data - use location coordinates directly
            // Note: This will be less accurate but still provides basic AR functionality
            socketAnchor = arCoreHelper?.createGeospatialAnchor(
                location.latitude,
                location.longitude,
                0.0
            )
            Log.d(TAG, "Socket anchor created from location coords (no AR data): ${location.latitude}, ${location.longitude}")
        }
        
        if (socketAnchor != null) {
            Log.d(TAG, "Socket anchor created successfully")
        } else {
            Log.w(TAG, "Failed to create socket anchor")
        }
    }
    
    private fun startARUpdateLoop() {
        binding.arSurfaceView.post(object : Runnable {
            override fun run() {
                if (currentMode == Mode.AR && !isFinishing) {
                    updateARFrame()
                    binding.arSurfaceView.postDelayed(this, 33) // ~30 fps
                }
            }
        })
    }
    
    private fun updateARFrame() {
        val frame = arCoreHelper?.update() ?: return
        
        // Check geospatial status
        if (arCoreHelper?.isGeospatialReady() == true) {
            binding.statusText.text = "AR Tracking Active"
            
            // Get accuracy
            arCoreHelper?.getGeospatialAccuracy()?.let { (hAcc, vAcc) ->
                binding.accuracyText.visibility = View.VISIBLE
                binding.accuracyText.text = "Accuracy: ±${hAcc.roundToInt()}m horizontal, ±${vAcc.roundToInt()}m vertical"
            }
            
            // Update overlay with anchor projection
            updateOverlay(frame)
        } else {
            binding.statusText.text = "Acquiring geospatial lock..."
        }
    }
    
    private fun updateOverlay(frame: Frame) {
        val anchor = socketAnchor ?: return
        
        if (anchor.trackingState != TrackingState.TRACKING) {
            binding.arOverlay.targetX = null
            binding.arOverlay.targetY = null
            return
        }
        
        val camera = frame.camera
        val pose = anchor.pose
        
        // Project anchor to screen coordinates
        val viewMatrix = FloatArray(16)
        val projectionMatrix = FloatArray(16)
        camera.getViewMatrix(viewMatrix, 0)
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
        
        // Convert pose to screen position
        val screenPos = projectToScreen(pose, viewMatrix, projectionMatrix)
        
        if (screenPos != null) {
            // Target is on screen
            binding.arOverlay.targetX = screenPos.first
            binding.arOverlay.targetY = screenPos.second
            binding.arOverlay.offScreenDirection = null
        } else {
            // Target is off-screen - calculate direction
            val direction = calculateOffScreenDirection(pose, camera)
            binding.arOverlay.targetX = null
            binding.arOverlay.targetY = null
            binding.arOverlay.offScreenDirection = direction
        }
        
        // Update distance info
        val distance = calculateDistance(camera.pose, pose)
        binding.arOverlay.distance = ArCoordinateUtils.formatDistance(distance)
        
        val height = pose.ty() - camera.pose.ty()
        binding.arOverlay.heightInfo = ArCoordinateUtils.formatHeight(height)
        
        binding.arOverlay.invalidate()
    }
    
    private fun projectToScreen(
        pose: Pose,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray
    ): Pair<Float, Float>? {
        val position = floatArrayOf(pose.tx(), pose.ty(), pose.tz(), 1f)
        val viewPos = FloatArray(4)
        val screenPos = FloatArray(4)
        
        // Apply view transformation
        android.opengl.Matrix.multiplyMV(viewPos, 0, viewMatrix, 0, position, 0)
        
        // Check if behind camera or too close to near plane (0.1m)
        // In ARCore camera space, camera looks down negative Z, so positive Z is behind
        if (viewPos[2] > -0.1f) return null
        
        // Apply projection
        android.opengl.Matrix.multiplyMV(screenPos, 0, projectionMatrix, 0, viewPos, 0)
        
        // Perspective divide
        if (screenPos[3] == 0f) return null
        val ndcX = screenPos[0] / screenPos[3]
        val ndcY = screenPos[1] / screenPos[3]
        
        // Check if on screen
        if (ndcX < -1f || ndcX > 1f || ndcY < -1f || ndcY > 1f) return null
        
        // Convert to screen coordinates
        val screenX = (ndcX + 1f) * 0.5f * binding.arOverlay.width
        val screenY = (1f - ndcY) * 0.5f * binding.arOverlay.height
        
        return Pair(screenX, screenY)
    }
    
    private fun calculateOffScreenDirection(pose: Pose, camera: Camera): Float {
        val dx = pose.tx() - camera.pose.tx()
        val dz = pose.tz() - camera.pose.tz()
        
        val angleRad = atan2(dx.toDouble(), -dz.toDouble())
        return Math.toDegrees(angleRad).toFloat()
    }
    
    private fun calculateDistance(pose1: Pose, pose2: Pose): Float {
        val dx = pose1.tx() - pose2.tx()
        val dy = pose1.ty() - pose2.ty()
        val dz = pose1.tz() - pose2.tz()
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }
    
    private fun switchToCompassMode() {
        currentMode = Mode.COMPASS
        binding.arSurfaceView.visibility = View.GONE
        binding.arOverlay.visibility = View.GONE
        binding.compassModeLayout.visibility = View.VISIBLE
        binding.modeToggleButton.text = "Switch to AR"
        binding.instructionText.text = "Point device towards socket location"
        
        // Start compass updates
        startCompassUpdates()
    }
    
    private fun switchToARMode() {
        if (!ArCoreHelper.isArCoreSupported(this)) {
            Toast.makeText(this, "AR not supported", Toast.LENGTH_SHORT).show()
            return
        }
        
        currentMode = Mode.AR
        binding.arSurfaceView.visibility = View.VISIBLE
        binding.arOverlay.visibility = View.VISIBLE
        binding.compassModeLayout.visibility = View.GONE
        binding.modeToggleButton.text = "Switch to Compass"
        binding.instructionText.text = "Look around to find the glowing target"
        
        if (arCoreHelper == null) {
            initializeAR()
        } else {
            startARUpdateLoop()
        }
    }
    
    private fun startCompassUpdates() {
        getCurrentLocation { lat, lon, alt ->
            currentUserLat = lat
            currentUserLon = lon
            currentUserAlt = alt
            updateCompassDisplay()
        }
    }
    
    private fun updateCompassDisplay() {
        val location = targetLocation ?: return
        
        // Calculate distance
        val distance = ArCoordinateUtils.calculateDistance(
            currentUserLat, currentUserLon,
            location.latitude, location.longitude
        )
        binding.compassDistanceText.text = ArCoordinateUtils.formatDistance(distance)
        
        // Calculate bearing to target
        val bearing = ArCoordinateUtils.calculateBearing(
            currentUserLat, currentUserLon,
            location.latitude, location.longitude
        )
        
        // Rotate arrow to point to target (relative to current heading)
        val rotation = bearing - currentAzimuth
        binding.compassArrow.rotation = rotation
        
        // Update bearing text
        val direction = when {
            bearing < 22.5f || bearing >= 337.5f -> "N"
            bearing < 67.5f -> "NE"
            bearing < 112.5f -> "E"
            bearing < 157.5f -> "SE"
            bearing < 202.5f -> "S"
            bearing < 247.5f -> "SW"
            bearing < 292.5f -> "W"
            bearing < 337.5f -> "NW"
            else -> "N"
        }
        binding.compassBearingText.text = "Direction: $direction (${bearing.roundToInt()}°)"
    }
    
    private fun getCurrentLocation(callback: (Double, Double, Double) -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            showError("Location permission required for AR mode")
            switchToCompassMode()
            return
        }
        
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location ->
            if (location != null) {
                callback(location.latitude, location.longitude, location.altitude)
            } else {
                showError("Could not get current location")
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error getting location", e)
            showError("Location error: ${e.message}")
        }
    }
    
    private fun showError(message: String) {
        binding.statusText.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    override fun onResume() {
        super.onResume()
        arCoreHelper?.resume()
        
        // Register sensors for compass mode
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }
    
    override fun onPause() {
        super.onPause()
        arCoreHelper?.pause()
        sensorManager.unregisterListener(this)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        socketAnchor?.detach()
        arCoreHelper?.destroy()
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravity, 0, gravity.size)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, geomagnetic, 0, geomagnetic.size)
            }
        }
        
        val R = FloatArray(9)
        val I = FloatArray(9)
        
        if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(R, orientation)
            
            currentAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
            if (currentAzimuth < 0) currentAzimuth += 360f
            
            currentPitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
            
            if (currentMode == Mode.COMPASS) {
                updateCompassDisplay()
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not used
    }
}
