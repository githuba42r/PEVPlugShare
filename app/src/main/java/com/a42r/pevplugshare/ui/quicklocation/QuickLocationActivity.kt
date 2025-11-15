package com.a42r.pevplugshare.ui.quicklocation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.databinding.ActivityQuickLocationBinding
import com.a42r.pevplugshare.data.model.PhotoMetadata
import com.a42r.pevplugshare.data.remote.TokenManager
import com.a42r.pevplugshare.ui.edit.EditLocationActivity
import com.a42r.pevplugshare.util.PhotoUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Quick location workflow: select/take photos, pick location on map, edit photos, AI analysis, then add location
 */
@AndroidEntryPoint
class QuickLocationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuickLocationBinding
    private val selectedPhotoPaths = mutableListOf<String>()
    private val photoMetadata = mutableMapOf<String, PhotoMetadata>()
    private var currentStep = Step.SELECT_PHOTOS
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var currentPhotoUri: Uri? = null
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var selectedBearing: Float? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var hasMediaLocationPermission = false

    @Inject
    lateinit var tokenManager: TokenManager

    enum class Step {
        SELECT_PHOTOS,
        TAKE_PHOTOS,
        PICK_LOCATION,
        EDIT_PHOTOS,
        AI_ANALYSIS,
        ADD_LOCATION
    }

    private val requestMediaLocationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasMediaLocationPermission = isGranted
        if (isGranted) {
            Log.d("QuickLocationActivity", "ACCESS_MEDIA_LOCATION permission granted")
        } else {
            Log.w("QuickLocationActivity", "ACCESS_MEDIA_LOCATION permission denied - location data from images may not be available")
            Toast.makeText(this, "Media location permission denied - image location data may be unavailable", Toast.LENGTH_LONG).show()
        }
    }

    private val selectPhotos = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            Log.d("QuickLocationActivity", "Selected ${uris.size} photos")
            processPhotoUris(uris)
        }
    }

    private val takeCameraPhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentPhotoUri != null) {
            Log.d("QuickLocationActivity", "Photo taken: $currentPhotoUri")
            processPhotoUris(listOf(currentPhotoUri!!))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuickLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check and request ACCESS_MEDIA_LOCATION permission on Android 10+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                hasMediaLocationPermission = true
            } else {
                requestMediaLocationPermission.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
            }
        } else {
            hasMediaLocationPermission = true // Not needed on Android 9 and below
        }

        binding.selectPhotosButton.setOnClickListener {
            selectPhotos.launch("image/*")
        }
        binding.takePhotosButton.setOnClickListener {
            showCameraWorkflowDialog()
        }
    }

    private fun showCameraWorkflowDialog() {
        AlertDialog.Builder(this)
            .setTitle("Camera")
            .setMessage("Take photos with your camera. You can take multiple photos and choose to continue or finish.")
            .setPositiveButton("Start") { _, _ ->
                launchCamera()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchCamera() {
        currentStep = Step.TAKE_PHOTOS
        val photoFile = createImageFile()
        if (photoFile != null) {
            currentPhotoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            takeCameraPhoto.launch(currentPhotoUri!!)
        } else {
            Toast.makeText(this, "Failed to create photo file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile("QUICK_LOC_${timeStamp}_", ".jpg", storageDir)
        } catch (e: Exception) {
            Log.e("QuickLocationActivity", "Error creating image file", e)
            null
        }
    }

    private fun showPhotoTakenDialog() {
        AlertDialog.Builder(this)
            .setTitle("Photo Saved")
            .setMessage("${selectedPhotoPaths.size} photo(s) saved")
            .setNeutralButton("Take Another") { _, _ ->
                launchCamera()
            }
            .setPositiveButton("Done Taking Photos") { _, _ ->
                if (selectedPhotoPaths.isNotEmpty()) {
                    proceedToLocationPicker()
                } else {
                    finish()
                }
            }
            .setNegativeButton("Cancel All", null)
            .show()
    }

    private fun processPhotoUris(uris: List<Uri>) {
        scope.launch(Dispatchers.Default) {
            uris.forEach { uri ->
                try {
                    Log.d("QuickLocationActivity", "Processing URI: $uri")

                    // On Android 10+, use setRequireOriginal to get the original image with EXIF data
                    val actualUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && uri.scheme == "content") {
                        try {
                            android.provider.MediaStore.setRequireOriginal(uri)
                        } catch (e: Exception) {
                            Log.w("QuickLocationActivity", "setRequireOriginal failed, using original URI", e)
                            uri
                        }
                    } else {
                        uri
                    }

                    // Extract metadata from URI - with ACCESS_MEDIA_LOCATION permission,
                    // this should now include GPS data
                    val metadata = PhotoUtils.getPhotoMetadataFromUri(this@QuickLocationActivity, actualUri)
                    Log.d("QuickLocationActivity", "Extracted metadata - hasGPS: ${metadata.hasGpsData}, location: ${metadata.getLocationString()}")

                    // Copy photo to cache for later use
                    val file = copyPhotoToCache(actualUri)
                    if (file != null) {
                        selectedPhotoPaths.add(file.absolutePath)
                        photoMetadata[file.absolutePath] = metadata
                        Log.d("QuickLocationActivity", "Saved photo: ${file.name}")
                    }
                } catch (e: Exception) {
                    Log.e("QuickLocationActivity", "Error processing photo", e)
                }
            }

            withContext(Dispatchers.Main) {
                if (selectedPhotoPaths.isNotEmpty()) {
                    if (currentStep == Step.TAKE_PHOTOS) {
                        showPhotoTakenDialog()
                    } else {
                        proceedToLocationPicker()
                    }
                } else {
                    Toast.makeText(this@QuickLocationActivity, "Failed to load photos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun copyPhotoToCache(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = "quick_location_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}.jpg"
            val cacheFile = File(cacheDir, fileName)

            inputStream.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            cacheFile
        } catch (e: Exception) {
            Log.e("QuickLocationActivity", "Error copying photo", e)
            null
        }
    }

    private fun proceedToLocationPicker() {
        currentStep = Step.PICK_LOCATION

        // Try to get location from image EXIF data
        var photoLat: Double? = null
        var photoLng: Double? = null
        var photoBearing: Float? = null

        for (path in selectedPhotoPaths) {
            val metadata = photoMetadata[path]
            if (metadata != null && metadata.hasGpsData) {
                photoLat = metadata.latitude
                photoLng = metadata.longitude
                photoBearing = metadata.bearing
                Log.d("QuickLocationActivity", "Using EXIF location from image: ${metadata.getLocationString()}, bearing: ${metadata.getBearingString()}")
                break
            }
        }

        selectedLatitude = photoLat
        selectedLongitude = photoLng
        selectedBearing = photoBearing

        // If no location from images, try device location
        if (selectedLatitude == null || selectedLongitude == null) {
            getCurrentLocation { lat, lng ->
                selectedLatitude = lat
                selectedLongitude = lng
                Log.d("QuickLocationActivity", "Using device location: $lat, $lng")
                showLocationPickerDialog()
            }
        } else {
            showLocationPickerDialog()
        }
    }

    private fun getCurrentLocation(callback: (lat: Double, lng: Double) -> Unit) {
        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // If no permissions, use fallback
            callback(-33.8688, 151.2093) // Sydney, Australia
            return
        }

        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    callback(location.latitude, location.longitude)
                } else {
                    // Try device's last known location (coarse)
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation: Location? ->
                        if (lastLocation != null) {
                            callback(lastLocation.latitude, lastLocation.longitude)
                        } else {
                            callback(-33.8688, 151.2093) // Sydney, Australia
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("QuickLocationActivity", "Error getting current location", e)
            // Try device's last known location (coarse)
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation: Location? ->
                if (lastLocation != null) {
                    callback(lastLocation.latitude, lastLocation.longitude)
                } else {
                    callback(-33.8688, 151.2093) // Sydney, Australia
                }
            }
        }
    }

    private val locationPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultBundle = result.data?.getBundleExtra("result")
            if (resultBundle != null) {
                selectedLatitude = resultBundle.getDouble("latitude")
                selectedLongitude = resultBundle.getDouble("longitude")
                selectedBearing = resultBundle.getFloat("bearing")
                Log.d("QuickLocationActivity", "Location selected: $selectedLatitude, $selectedLongitude")
            }
            proceedAfterLocationPicker()
        }
    }

    private fun showLocationPickerDialog() {
        val intent = Intent(this, LocationPickerMapActivity::class.java).apply {
            putExtra("latitude", selectedLatitude ?: -33.8688)
            putExtra("longitude", selectedLongitude ?: 151.2093)
            putExtra("bearing", selectedBearing ?: 0f)
        }
        locationPickerLauncher.launch(intent)
    }

    private fun proceedAfterLocationPicker() {
        // After location is selected, proceed directly to photo editing
        // (Skip the choice dialog - workflow order is: photos → location → editing → AI)
        // Save selected location as last location
        if (selectedLatitude != null && selectedLongitude != null) {
            com.a42r.pevplugshare.util.QuickLocationPrefs.saveLastLocation(this, selectedLatitude!!, selectedLongitude!!)
        }
        proceedToPhotoEditing()
    }

    private fun showPhotoProgressDialog() {
        AlertDialog.Builder(this)
            .setTitle("Photos Loaded")
            .setMessage("What would you like to do next?")
            .setNegativeButton("Edit Photos") { _, _ ->
                proceedToPhotoEditing()
            }
            .setPositiveButton("AI Analysis") { _, _ ->
                proceedToAiAnalysis()
            }
            .show()
    }

    private val photoEditorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // Photos have been edited, proceed with workflow
            val editedPhotoPaths = result.data?.getStringArrayListExtra("edited_photo_paths")
            if (editedPhotoPaths != null) {
                selectedPhotoPaths.clear()
                selectedPhotoPaths.addAll(editedPhotoPaths)
            }
            proceedAfterPhotoEditing()
        }
    }

    private fun proceedToPhotoEditing() {
        currentStep = Step.EDIT_PHOTOS
        // Filter out deleted/non-existent photo files before launching editor
        val validPhotoPaths = selectedPhotoPaths.filter { path ->
            val file = java.io.File(path)
            file.exists()
        }
        val intent = Intent(this, PhotoGalleryEditorActivity::class.java).apply {
            putStringArrayListExtra("photo_paths", ArrayList(validPhotoPaths))
            // Pass focus areas if any exist
            val focusBundle = Bundle()
            putExtra("focus_areas", focusBundle)
        }
        photoEditorLauncher.launch(intent)
    }

    private fun proceedAfterPhotoEditing() {
        val isAiEnabled = tokenManager.isAiAnalysisEnabled()

        when {
            isAiEnabled -> {
                // Only AI analysis available: go directly to it
                proceedToAiAnalysis()
            }
            else -> {
                // Only location editing available: go to location form
                proceedToAddLocation()
            }
        }
    }

    private fun proceedToAiAnalysis() {
        currentStep = Step.AI_ANALYSIS
        // TODO: Launch AI analysis
        Toast.makeText(this, "AI analysis coming soon", Toast.LENGTH_SHORT).show()
        // For now, proceed to location
        proceedAfterAiAnalysis()
    }

    private fun proceedAfterAiAnalysis() {
        // After AI analysis, always go to location form
        proceedToAddLocation()
    }

    private fun proceedToAddLocation() {
        currentStep = Step.ADD_LOCATION
        val intent = Intent(this, EditLocationActivity::class.java).apply {
            if (selectedLatitude != null && selectedLongitude != null) {
                putExtra("latitude", selectedLatitude!!)
                putExtra("longitude", selectedLongitude!!)
            }
            if (selectedBearing != null) {
                putExtra("bearing", selectedBearing!!)
            }
            putStringArrayListExtra("photo_paths", ArrayList(selectedPhotoPaths))
        }
        startActivity(intent)
        // Removed finish() to keep wizard open
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
