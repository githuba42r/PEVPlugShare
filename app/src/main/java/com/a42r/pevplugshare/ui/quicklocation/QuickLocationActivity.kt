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
import com.a42r.pevplugshare.domain.repository.SavedForLaterRepository
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

    @Inject
    lateinit var savedForLaterRepository: SavedForLaterRepository

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

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            Log.d("QuickLocationActivity", "CAMERA permission granted")
            launchCamera()
        } else {
            Log.w("QuickLocationActivity", "CAMERA permission denied")
            Toast.makeText(this, "Camera permission denied. Cannot take photos.", Toast.LENGTH_LONG).show()
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

        // Restore state if activity was recreated (e.g., orientation change)
        savedInstanceState?.let {
            currentPhotoUri = it.getParcelable(KEY_CURRENT_PHOTO_URI)
            selectedPhotoPaths.clear()
            selectedPhotoPaths.addAll(it.getStringArrayList(KEY_SELECTED_PHOTO_PATHS) ?: emptyList())
            currentStep = Step.valueOf(it.getString(KEY_CURRENT_STEP, Step.SELECT_PHOTOS.name))
            selectedLatitude = if (it.containsKey(KEY_SELECTED_LATITUDE)) it.getDouble(KEY_SELECTED_LATITUDE) else null
            selectedLongitude = if (it.containsKey(KEY_SELECTED_LONGITUDE)) it.getDouble(KEY_SELECTED_LONGITUDE) else null
            selectedBearing = if (it.containsKey(KEY_SELECTED_BEARING)) it.getFloat(KEY_SELECTED_BEARING) else null

            // Restore photo metadata
            val metadataKeys = it.getStringArrayList(KEY_METADATA_KEYS) ?: emptyList()
            photoMetadata.clear()
            for (key in metadataKeys) {
                val metadata = it.getParcelable<PhotoMetadata>("${KEY_METADATA_PREFIX}$key")
                if (metadata != null) {
                    photoMetadata[key] = metadata
                }
            }

            Log.d("QuickLocationActivity", "Restored state: ${selectedPhotoPaths.size} photos, currentPhotoUri=$currentPhotoUri, step=$currentStep")
        }

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
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
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
            .setMessage("${selectedPhotoPaths.size} photo(s) saved. What would you like to do?")
            .setNeutralButton("Add Photo") { _, _ ->
                launchCamera()
            }
            .setPositiveButton("Continue") { _, _ ->
                if (selectedPhotoPaths.isNotEmpty()) {
                    proceedToLocationPicker()
                } else {
                    finish()
                }
            }
            .setNegativeButton("Save for Later") { _, _ ->
                saveForLater()
            }
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
                proceedAfterLocationPicker()
            }
        } else {
            proceedAfterLocationPicker()
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
            val focusAreasBundle = result.data?.getBundleExtra("focus_areas")

            if (editedPhotoPaths != null) {
                selectedPhotoPaths.clear()
                selectedPhotoPaths.addAll(editedPhotoPaths)
            }

            focusAreasBundle?.let { bundle ->
                for (path in selectedPhotoPaths) {
                    val focusRectString = bundle.getString(path)
                    if (focusRectString != null) {
                        photoMetadata[path]?.focusRect = PhotoMetadata.deserializeRectF(focusRectString)
                    }
                }
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
            validPhotoPaths.forEach { path ->
                photoMetadata[path]?.focusRect?.let { rectF ->
                    // Serialize RectF to a string
                    val focusRectString = "${rectF.left},${rectF.top},${rectF.right},${rectF.bottom}"
                    focusBundle.putString(path, focusRectString)
                }
            }
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

        val focusAreasBundle = Bundle()
        selectedPhotoPaths.forEach { path ->
            photoMetadata[path]?.focusRect?.let { rectF ->
                val focusRectString = "${rectF.left},${rectF.top},${rectF.right},${rectF.bottom}"
                focusAreasBundle.putString(path, focusRectString)
            }
        }

        val intent = Intent(this, EditLocationActivity::class.java).apply {
            if (selectedLatitude != null && selectedLongitude != null) {
                putExtra("latitude", selectedLatitude!!)
                putExtra("longitude", selectedLongitude!!)
            }
            if (selectedBearing != null) {
                putExtra("bearing", selectedBearing!!)
            }
            putStringArrayListExtra("photo_paths", ArrayList(selectedPhotoPaths))
            putExtra("focus_areas", focusAreasBundle)
        }
        startActivity(intent)
        finish() // Finish QuickLocationActivity after launching EditLocationActivity
    }

    private fun saveForLater() {
        scope.launch {
            try {
                // Get current location
                getCurrentLocation { lat, lng ->
                    scope.launch {
                        saveForLaterWithLocation(lat, lng)
                    }
                }
            } catch (e: Exception) {
                Log.e("QuickLocationActivity", "Error saving for later", e)
                Toast.makeText(this@QuickLocationActivity, "Failed to save location for later", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun saveForLaterWithLocation(lat: Double, lng: Double) {
        withContext(Dispatchers.IO) {
            try {
                // Create the entity
                val id = "saved_${System.currentTimeMillis()}"

                // Serialize photo paths
                val photoPaths = selectedPhotoPaths.joinToString(",")

                // Serialize photo metadata to JSON
                val metadataJson = serializePhotoMetadata(photoMetadata)

                val savedForLater = com.a42r.pevplugshare.data.db.entity.SavedForLaterEntity(
                    id = id,
                    latitude = lat,
                    longitude = lng,
                    bearing = selectedBearing,
                    photoPaths = photoPaths,
                    photoMetadata = metadataJson,
                    createdAt = System.currentTimeMillis()
                )

                savedForLaterRepository.insertSavedForLater(savedForLater)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@QuickLocationActivity,
                        "Location saved for later with ${selectedPhotoPaths.size} photo(s)",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("QuickLocationActivity", "Error saving for later", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@QuickLocationActivity,
                        "Failed to save location for later",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun serializePhotoMetadata(metadata: Map<String, PhotoMetadata>): String {
        // Simple JSON serialization
        val jsonBuilder = StringBuilder("{")
        metadata.entries.forEachIndexed { index, entry ->
            if (index > 0) jsonBuilder.append(",")
            jsonBuilder.append("\"${entry.key}\":{")
            jsonBuilder.append("\"hasGpsData\":${entry.value.hasGpsData},")
            jsonBuilder.append("\"latitude\":${entry.value.latitude},")
            jsonBuilder.append("\"longitude\":${entry.value.longitude},")
            jsonBuilder.append("\"bearing\":${entry.value.bearing}")

            entry.value.focusRect?.let { rect ->
                jsonBuilder.append(",\"focusRect\":\"${rect.left},${rect.top},${rect.right},${rect.bottom}\"")
            }

            jsonBuilder.append("}")
        }
        jsonBuilder.append("}")
        return jsonBuilder.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save current state to survive configuration changes (e.g., orientation change)
        currentPhotoUri?.let { outState.putParcelable(KEY_CURRENT_PHOTO_URI, it) }
        outState.putStringArrayList(KEY_SELECTED_PHOTO_PATHS, ArrayList(selectedPhotoPaths))
        outState.putString(KEY_CURRENT_STEP, currentStep.name)

        selectedLatitude?.let { outState.putDouble(KEY_SELECTED_LATITUDE, it) }
        selectedLongitude?.let { outState.putDouble(KEY_SELECTED_LONGITUDE, it) }
        selectedBearing?.let { outState.putFloat(KEY_SELECTED_BEARING, it) }

        // Save photo metadata
        val metadataKeys = ArrayList(photoMetadata.keys)
        outState.putStringArrayList(KEY_METADATA_KEYS, metadataKeys)
        for ((key, metadata) in photoMetadata) {
            outState.putParcelable("${KEY_METADATA_PREFIX}$key", metadata)
        }

        Log.d("QuickLocationActivity", "Saved state: ${selectedPhotoPaths.size} photos, currentPhotoUri=$currentPhotoUri, step=$currentStep")
    }

    companion object {
        private const val KEY_CURRENT_PHOTO_URI = "current_photo_uri"
        private const val KEY_SELECTED_PHOTO_PATHS = "selected_photo_paths"
        private const val KEY_CURRENT_STEP = "current_step"
        private const val KEY_SELECTED_LATITUDE = "selected_latitude"
        private const val KEY_SELECTED_LONGITUDE = "selected_longitude"
        private const val KEY_SELECTED_BEARING = "selected_bearing"
        private const val KEY_METADATA_KEYS = "metadata_keys"
        private const val KEY_METADATA_PREFIX = "metadata_"
    }
}
