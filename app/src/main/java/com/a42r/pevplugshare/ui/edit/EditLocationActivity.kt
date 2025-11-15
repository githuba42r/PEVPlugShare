package com.a42r.pevplugshare.ui.edit

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.databinding.ActivityEditLocationBinding
import com.a42r.pevplugshare.util.ImageUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.runBlocking
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt
import com.a42r.pevplugshare.domain.repository.PhotoRepository
import com.a42r.pevplugshare.data.db.entity.PhotoEntity

@AndroidEntryPoint
class EditLocationActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityEditLocationBinding
    private val viewModel: EditLocationViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @Inject
    lateinit var tokenManager: com.a42r.pevplugshare.data.remote.TokenManager

    @Inject
    lateinit var photoAnalyzer: com.a42r.pevplugshare.ai.PhotoAnalyzer

    @Inject
    lateinit var photoRepository: PhotoRepository

    private var mapView: MapView? = null
    private var locationMarker: Marker? = null
    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0

    // Sensor related fields
    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private var currentAzimuth: Float = 0f

    // Photo related fields
    private val photoUris = mutableListOf<String>()
    private val photoMetadata = mutableMapOf<String, com.a42r.pevplugshare.data.model.PhotoMetadata>() // filePath -> metadata
    private val maxPhotos = 5
    private var currentEditLocationId: String? = null
    private var currentPhotoUri: Uri? = null
    private var tempPhotoFile: File? = null
    private var editingPhotoIndex: Int = -1
    private val photoSaveMutex = Mutex()  // Mutex for synchronized photo saving
    private var photosBeingProcessed = 0  // Count of photos currently being processed/saved
    private var submittedSuccessfully: Boolean = false

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            currentPhotoUri?.let {
                processAndSavePhoto()
            }
        }
    }

    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        Log.d("EditLocationActivity", "=== pickImages called with ${uris.size} uris ===")
        uris.forEachIndexed { index, uri ->
            Log.d("EditLocationActivity", "  uri[$index]: $uri")
        }

        if (uris.isNotEmpty()) {
            val remainingSlots = maxPhotos - photoUris.size
            val urisToProcess = uris.take(remainingSlots)

            Log.d("EditLocationActivity", "photoUris.size=${photoUris.size}, remainingSlots=$remainingSlots, urisToProcess.size=${urisToProcess.size}")

            if (uris.size > remainingSlots) {
                Toast.makeText(
                    this,
                    "Only adding $remainingSlots photo(s). Maximum is $maxPhotos total.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            urisToProcess.forEach { uri ->
                Log.d("EditLocationActivity", "Processing URI: $uri")
                processAndSaveGalleryPhoto(uri)
            }

            Log.d("EditLocationActivity", "After loop: photosBeingProcessed=$photosBeingProcessed")
        }
    }

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private val imageEditor = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val editedPhotoPath = result.data?.getStringExtra(ImageEditorActivity.EXTRA_OUTPUT_PATH)
            val focusRectString = result.data?.getStringExtra(ImageEditorActivity.EXTRA_FOCUS_RECT)
            val metadata = result.data?.getParcelableExtra<com.a42r.pevplugshare.data.model.PhotoMetadata>(ImageEditorActivity.EXTRA_METADATA)
            val isSaveNew = result.data?.getBooleanExtra(ImageEditorActivity.EXTRA_IS_SAVE_NEW, false) ?: false

            Log.d("EditLocationActivity", "")
            Log.d("EditLocationActivity", "📥 IMAGE EDITOR RESULT RECEIVED")
            Log.d("EditLocationActivity", "   Edited path: $editedPhotoPath")
            Log.d("EditLocationActivity", "   Focus string from editor: $focusRectString")
            Log.d("EditLocationActivity", "   Is save new: $isSaveNew")
            Log.d("EditLocationActivity", "   editingPhotoIndex: $editingPhotoIndex")
            Log.d("EditLocationActivity", "   photoUris.size: ${photoUris.size}")

            if (editedPhotoPath != null) {
                // Simply update the in-memory list and UI
                // Focus is now stored in SharedPreferences and will be persisted to DB when location is saved
                if (isSaveNew) {
                    photoUris.add(editedPhotoPath)
                    Toast.makeText(this, "New photo added", Toast.LENGTH_SHORT).show()
                } else if (editingPhotoIndex >= 0 && editingPhotoIndex < photoUris.size) {
                    val originalPhotoPath = photoUris[editingPhotoIndex]
                    // Delete the original file if it's different from the edited path
                    if (originalPhotoPath != editedPhotoPath) {
                        File(originalPhotoPath).delete()
                    }
                    photoUris[editingPhotoIndex] = editedPhotoPath
                    Toast.makeText(this, "Photo updated", Toast.LENGTH_SHORT).show()
                }

                photoMetadata[editedPhotoPath] = metadata ?: com.a42r.pevplugshare.data.model.PhotoMetadata()
                refreshPhotoThumbnails()
                viewModel.updatePhotoPaths(photoUris)
            }
        }
        editingPhotoIndex = -1
    }

    private val locationId: String? by lazy {
        intent.getStringExtra("location_id")
    }

    private val isAddMode: Boolean by lazy {
        val id = locationId
        id == null || id.isEmpty()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize osmdroid configuration
        Configuration.getInstance().load(
            this,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        )

        binding = ActivityEditLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isAddMode) "Add Location" else "Edit Location"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize sensor manager for compass
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        setupUI()
        setupMapModal()
        setupPhotoSection()
        setupObservers()

        // Retrieve photo paths from intent
        val incomingPhotoPaths = intent.getStringArrayListExtra("photo_paths")
        incomingPhotoPaths?.let {
            // In add mode, check if these are cache paths that need processing
            if (isAddMode) {
                // If photos are from QuickLocationActivity, they're in cache and need processing
                // Process them now before adding to photoUris
                // Note: currentEditLocationId will be set after this block, so we generate temp ID now
                // and will use it throughout add mode for consistency
                if (currentEditLocationId == null) {
                    currentEditLocationId = UUID.randomUUID().toString()
                }

                lifecycleScope.launch {
                    val processedPaths = withContext(Dispatchers.IO) {
                        processCachePhotos(it, currentEditLocationId!!)
                    }

                    photoUris.addAll(processedPaths)
                    refreshPhotoThumbnails()
                    viewModel.updatePhotoPaths(photoUris)
                }
            } else {
                // Edit mode: photos already have stable paths (stored in database)
                photoUris.addAll(it)
                refreshPhotoThumbnails()
                viewModel.updatePhotoPaths(photoUris)
            }
        }

        // Retrieve focus areas from intent and populate SharedPreferences
        val incomingFocusAreas = intent.getBundleExtra("focus_areas")
        incomingFocusAreas?.let { bundle ->
            val sharedPref = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
            bundle.keySet().forEach { key ->
                val focusRectString = bundle.getString(key)
                if (focusRectString != null) {
                    sharedPref.edit().putString("focus_$key", focusRectString).apply()
                    Log.d("EditLocationActivity", "Loaded focus from Intent for $key: $focusRectString")
                }
            }
        }

        // Retrieve location from intent
        val intentLatitude = intent.getDoubleExtra("latitude", 0.0)
        val intentLongitude = intent.getDoubleExtra("longitude", 0.0)

        if (isAddMode) {
            // Only generate if not already set (may have been set when processing incoming photos)
            if (currentEditLocationId == null) {
                currentEditLocationId = UUID.randomUUID().toString()
            }
            // Add mode: Initialize for new location entry
            viewModel.initializeForAddMode()
            if (intentLatitude != 0.0 || intentLongitude != 0.0) {
                viewModel.updateLocation(intentLatitude, intentLongitude)
                selectedLatitude = intentLatitude
                selectedLongitude = intentLongitude
                binding.locationCoordinatesTextView.text =
                    "📍 ${String.format("%.4f", intentLatitude)}, ${String.format("%.4f", intentLongitude)}"
            } else {
                requestLocationAndPopulate()
            }
        } else {
            // Edit mode: Load existing location
            locationId?.let { viewModel.loadLocation(it) }
        }
    }

    private fun requestLocationAndPopulate() {
        // Check for location permission
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            getLocationAndPopulate()
        } else {
            // Request permission
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        }
    }

    private fun getLocationAndPopulate() {
        try {
            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                com.google.android.gms.tasks.CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                location?.let {
                                    viewModel.updateLocation(it.latitude, it.longitude)
                                    selectedLatitude = it.latitude
                                    selectedLongitude = it.longitude
                                    binding.locationCoordinatesTextView.text =
                                        "📍 ${String.format("%.4f", it.latitude)}, ${String.format("%.4f", it.longitude)}"                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCompassDirection(azimuth: Float): String {
        val degrees = (azimuth + 360) % 360
        return when (degrees.roundToInt()) {
            in 0..22 -> "North"
            in 23..67 -> "North-East"
            in 68..112 -> "East"
            in 113..157 -> "South-East"
            in 158..202 -> "South"
            in 203..247 -> "South-West"
            in 248..292 -> "West"
            in 293..337 -> "North-West"
            else -> "North"
        }
    }

    private fun setupUI() {
        binding.apply {
            // Map location button
            mapLocationButton.setOnClickListener { showMapModal() }

            buttonSubmit.setOnClickListener { submitUpdates() }
            buttonSubmit.text = if (isAddMode) "Add Location" else "Update"
            // Disable submit button initially in add mode until name is filled
            if (isAddMode) {
                buttonSubmit.isEnabled = false
            }
            buttonCancel.setOnClickListener { finish() }
            buttonSyncToOsmand.setOnClickListener { syncToOsmand() }
            // Hide sync button in add mode
            buttonSyncToOsmand.visibility = if (isAddMode) android.view.View.GONE else android.view.View.VISIBLE

            // Delete button visibility and click listener
            binding.buttonDelete.visibility = if (isAddMode) android.view.View.GONE else android.view.View.VISIBLE
            binding.buttonDelete.setOnClickListener {
                showDeleteConfirmationDialog()
            }

            // Setup AI buttons - hide if no API key
            val hasApiKey = tokenManager.getOpenRouterApiKey()?.isNotBlank() == true
            buttonGenerateDescription.visibility = if (hasApiKey) android.view.View.VISIBLE else android.view.View.GONE
            buttonParseDescription.visibility = if (hasApiKey) android.view.View.VISIBLE else android.view.View.GONE

            buttonGenerateDescription.setOnClickListener { showAiDescriptionDialog() }
            buttonParseDescription.setOnClickListener { parseDescriptionToFields() }
            // Parse button visible in both add and edit modes

            // Add name field text watcher to enable/disable submit button
            nameInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    if (isAddMode) {
                        // Check both name is not blank AND photos are not being processed
                        val nameNotBlank = !s.isNullOrBlank()
                        val isProcessingPhotos = photosBeingProcessed > 0
                        binding.buttonSubmit.isEnabled = nameNotBlank && !isProcessingPhotos
                        Log.d("EditLocationActivity", "TextWatcher: nameNotBlank=$nameNotBlank, isProcessingPhotos=$isProcessingPhotos, enabled=${binding.buttonSubmit.isEnabled}")
                    }
                    viewModel.updateName(s?.toString() ?: "")
                }
            })

            // Setup socket number buttons
            socket1Button.setOnClickListener { selectSocketCount(1) }
            socket2Button.setOnClickListener { selectSocketCount(2) }
            socket4Button.setOnClickListener { selectSocketCount(4) }
            socket10Button.setOnClickListener { selectSocketCount(10) }
            socket10PlusButton.setOnClickListener { selectSocketCount(11) }

            // Setup socket type spinner with common types first
            val socketTypes = arrayOf(
                "AU GPO (AS3112)",
                "EV Charger (Type 2)",
                "Tesla Charger",
                "CCS2/Combo",
                "CHAdeMO",
                "Type 1 (J1772)",
                "Wall Socket (15A)",
                "Caravan Plug",
                "Other"
            )
            val socketAdapter = android.widget.ArrayAdapter(
                this@EditLocationActivity,
                android.R.layout.simple_spinner_item,
                socketTypes
            )
            socketAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            socketTypeSpinner.adapter = socketAdapter
            socketTypeSpinner.setSelection(0)

            // Setup opening hours spinner with common options
            val openingHoursList = arrayOf(
                "24/7",
                "9AM-5PM (Mon-Sun)",
                "9AM-5PM (Weekdays)",
                "24/7 (Weekdays only)",
                "8AM-6PM (Mon-Fri)",
                "8AM-6PM (Mon-Sat)",
                "10AM-4PM (Weekends)",
                "By Appointment",
                "Daylight Hours",
                "Custom"
            )
            val hoursAdapter = android.widget.ArrayAdapter(
                this@EditLocationActivity,
                android.R.layout.simple_spinner_item,
                openingHoursList
            )
            hoursAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            openingHoursSpinner.adapter = hoursAdapter
            openingHoursSpinner.setSelection(0)

            // Setup fee indicator click
            feeIndicator.setOnClickListener {
                val hasFee = !feeCheckbox.isChecked
                feeCheckbox.isChecked = hasFee
                updateFeeIndicator(hasFee)
                feeDetailsSection.visibility = if (hasFee) android.view.View.VISIBLE else android.view.View.GONE
            }

            // Setup access type toggle click
            accessTypeToggleLayout.setOnClickListener {
                val isPublic = accessTypeText.text != "Public"
                updateAccessTypeIndicator(isPublic)
                viewModel.updateAccessType(if (isPublic) "public" else "private")
            }

            // Setup favorite toggle click
            favoriteIndicator.setOnClickListener {
                val isFavorite = favoriteIndicator.tag == "favorite"
                updateFavoriteIndicator(!isFavorite)
                viewModel.updateFavorite(!isFavorite)
            }
        }
    }

    private fun setupMapModal() {
        // Find modal components
        val mapModalOverlay = findViewById<android.view.View>(com.a42r.pevplugshare.R.id.mapModalOverlay)
        mapView = findViewById(com.a42r.pevplugshare.R.id.mapView)
        val buttonAccept = findViewById<com.google.android.material.button.MaterialButton>(com.a42r.pevplugshare.R.id.buttonAcceptLocation)
        val buttonCancel = findViewById<com.google.android.material.button.MaterialButton>(com.a42r.pevplugshare.R.id.buttonCancelLocation)

        // Setup map
        mapView?.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(tokenManager.getMapZoomLevel().toDouble())

            // Add tap listener to update marker position
            setOnClickListener {
                // Map click will be handled by the map's touch events
            }
        }

        // Accept button
        buttonAccept.setOnClickListener {
            // Update the location with selected coordinates
            viewModel.updateLocation(selectedLatitude, selectedLongitude)
            Toast.makeText(this, "Location updated", Toast.LENGTH_SHORT).show()
            hideMapModal()
        }

        // Cancel button
        buttonCancel.setOnClickListener {
            hideMapModal()
        }

        // Setup map tap listener
        mapView?.overlays?.add(object : org.osmdroid.views.overlay.Overlay() {
            override fun onSingleTapConfirmed(e: android.view.MotionEvent, mapView: MapView): Boolean {
                val projection = mapView.projection
                val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                updateMapMarker(geoPoint.latitude, geoPoint.longitude)
                return true
            }
        })
    }

    private fun showMapModal() {
        val mapModalOverlay = findViewById<android.view.View>(com.a42r.pevplugshare.R.id.mapModalOverlay)
        mapModalOverlay.visibility = android.view.View.VISIBLE

        // Initialize map with current location
        val currentLat = viewModel.formData.value.latitude
        val currentLon = viewModel.formData.value.longitude

        if (currentLat != 0.0 && currentLon != 0.0) {
            updateMapMarker(currentLat, currentLon)
            mapView?.controller?.setCenter(GeoPoint(currentLat, currentLon))
        }
    }

    private fun hideMapModal() {
        val mapModalOverlay = findViewById<android.view.View>(com.a42r.pevplugshare.R.id.mapModalOverlay)
        mapModalOverlay.visibility = android.view.View.GONE
    }

    private fun updateMapMarker(latitude: Double, longitude: Double) {
        selectedLatitude = latitude
        selectedLongitude = longitude

        // Remove existing marker
        locationMarker?.let { mapView?.overlays?.remove(it) }

        // Add new marker
        locationMarker = Marker(mapView).apply {
            position = GeoPoint(latitude, longitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Selected Location"
        }
        mapView?.overlays?.add(locationMarker)
        mapView?.invalidate()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.editState.collect { state ->
                when (state) {
                    is EditLocationState.Loading -> {
                        binding.progressBar.visibility = android.view.View.VISIBLE
                        binding.formContainer.visibility = android.view.View.GONE
                        binding.buttonSubmit.isEnabled = false
                    }
                    is EditLocationState.Ready -> {
                        // Add mode - form is ready for input
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.formContainer.visibility = android.view.View.VISIBLE
                        binding.buttonSubmit.isEnabled = true

                        // Initialize UI with default values
                        initializeUIForAddMode()
                    }
                    is EditLocationState.Loaded -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.formContainer.visibility = android.view.View.VISIBLE
                        // Only enable button if not processing photos
                        binding.buttonSubmit.isEnabled = photosBeingProcessed == 0
                        Log.d("EditLocationActivity", "EditLocationState.Loaded: photosBeingProcessed=$photosBeingProcessed, buttonSubmit.isEnabled=${binding.buttonSubmit.isEnabled}")
                        populateFormFromLocation(state.location)

                        // Show sync button if pending
                        binding.buttonSyncToOsmand.visibility =
                            if (state.location.syncStatus.value == "PENDING") android.view.View.VISIBLE
                            else android.view.View.GONE
                    }
                    is EditLocationState.Updating -> {
                        binding.progressBar.visibility = android.view.View.VISIBLE
                        binding.buttonSubmit.isEnabled = false
                        binding.buttonSyncToOsmand.isEnabled = false
                    }
                    is EditLocationState.UpdateSuccess -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        val message = if (isAddMode) "Location added!" else "Location updated!"
                        Toast.makeText(this@EditLocationActivity, message, Toast.LENGTH_SHORT).show()
                        submittedSuccessfully = true // Set flag on successful submission
                        setResult(RESULT_OK)
                        finish()
                    }
                    is EditLocationState.DeleteSuccess -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        Toast.makeText(this@EditLocationActivity, "Location deleted!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK) // Indicate success to previous activity
                        finish()
                    }
                    is EditLocationState.Error -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.buttonSubmit.isEnabled = true
                        Toast.makeText(this@EditLocationActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.formData.collect { data ->
                updateUIFromFormData(data)
            }
        }
    }

    private fun populateFormFromLocation(location: com.a42r.pevplugshare.data.model.Location) {
        binding.apply {
            nameInput.setText(location.name)
            selectSocketCount(location.gpoCount)
            descriptionInput.setText(location.description ?: "")
            additionalInfoInput.setText(location.additionalInfo ?: "")

            // Set opening hours spinner
            val openingHours = location.openingHours ?: "24/7"
            val hoursIndex = when {
                openingHours.contains("24/7", ignoreCase = true) && !openingHours.contains("weekday", ignoreCase = true) -> 0
                openingHours.contains("9AM-5PM", ignoreCase = true) && openingHours.contains("Mon-Sun", ignoreCase = true) -> 1
                openingHours.contains("9AM-5PM", ignoreCase = true) && openingHours.contains("Weekday", ignoreCase = true) -> 2
                openingHours.contains("24/7", ignoreCase = true) && openingHours.contains("weekday", ignoreCase = true) -> 3
                openingHours.contains("8AM-6PM", ignoreCase = true) && openingHours.contains("Mon-Fri", ignoreCase = true) -> 4
                openingHours.contains("8AM-6PM", ignoreCase = true) && openingHours.contains("Mon-Sat", ignoreCase = true) -> 5
                openingHours.contains("10AM-4PM", ignoreCase = true) && openingHours.contains("Weekend", ignoreCase = true) -> 6
                openingHours.contains("Appointment", ignoreCase = true) -> 7
                openingHours.contains("Daylight", ignoreCase = true) -> 8
                else -> 9 // Custom
            }
            openingHoursSpinner.setSelection(hoursIndex)

            val socketTypeStr = location.socketTypes.firstOrNull()?.name ?: "AS3112"
            socketTypeSpinner.setSelection(getSocketTypeIndex(socketTypeStr))

            val isPublic = location.publicPrivate == com.a42r.pevplugshare.data.model.LocationAccessType.PUBLIC
            updateAccessTypeIndicator(isPublic)

            feeCheckbox.isChecked = location.hasFee
            updateFeeIndicator(location.hasFee)
            if (location.hasFee) {
                feeDetailsSection.visibility = android.view.View.VISIBLE
                providerNameInput.setText(location.providerName ?: "")
                chargingCostsInput.setText(location.chargingCosts ?: "")
                providerWebsiteInput.setText(location.providerWebsite ?: "")
            }

            viewModel.updateLocation(location.latitude, location.longitude)
        }

        viewModel.updateName(location.name)
        viewModel.updateGpoCount(location.gpoCount)
        viewModel.updateDescription(location.description ?: "")
        viewModel.updateOpeningHours(location.openingHours ?: "24/7")
        viewModel.updateAdditionalInfo(location.additionalInfo ?: "")
        val isPublic = location.publicPrivate == com.a42r.pevplugshare.data.model.LocationAccessType.PUBLIC
        viewModel.updateAccessType(if (isPublic) "public" else "private")
        viewModel.updateHasFee(location.hasFee)
        viewModel.updateProviderName(location.providerName ?: "")
        viewModel.updateChargingCosts(location.chargingCosts ?: "")
        viewModel.updateProviderWebsite(location.providerWebsite ?: "")

        // Set favorite state
        updateFavoriteIndicator(location.isFavourite)
        viewModel.updateFavorite(location.isFavourite)
    }

    private fun initializeUIForAddMode() {
        binding.apply {
            // Set socket count to 1
            selectSocketCount(1)

            // Set opening hours to 24/7 (index 0)
            openingHoursSpinner.setSelection(0)

            // Set socket type to AU GPO (index 0)
            socketTypeSpinner.setSelection(0)

            // Set as Public
            updateAccessTypeIndicator(true)

            // Set as Free (no fee)
            feeCheckbox.isChecked = false
            updateFeeIndicator(false)
            feeDetailsSection.visibility = android.view.View.GONE

            // Set favorite to false
            updateFavoriteIndicator(false)
        }
    }

    private fun updateFavoriteIndicator(isFavorite: Boolean) {
        binding.favoriteIndicator.apply {
            if (isFavorite) {
                setImageResource(com.a42r.pevplugshare.R.drawable.ic_favorite)
                setColorFilter(ContextCompat.getColor(context, com.google.android.material.R.color.design_default_color_primary))
                tag = "favorite"
            } else {
                setImageResource(com.a42r.pevplugshare.R.drawable.ic_favorite_border)
                setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray))
                tag = "not_favorite"
            }
        }
    }

    private fun updateFeeIndicator(hasFee: Boolean) {
        binding.feeIndicator.apply {
            if (hasFee) {
                setColorFilter(ContextCompat.getColor(context, com.google.android.material.R.color.design_default_color_primary))
            } else {
                setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray))
            }
        }
    }

    private fun updateAccessTypeIndicator(isPublic: Boolean) {
        binding.apply {
            if (isPublic) {
                accessTypeIcon.setImageResource(com.a42r.pevplugshare.R.drawable.ic_public)
                accessTypeIcon.setColorFilter(ContextCompat.getColor(this@EditLocationActivity, com.google.android.material.R.color.design_default_color_primary))
                accessTypeText.text = "Public"
            } else {
                accessTypeIcon.setImageResource(com.a42r.pevplugshare.R.drawable.ic_private)
                accessTypeIcon.setColorFilter(ContextCompat.getColor(this@EditLocationActivity, android.R.color.darker_gray))
                accessTypeText.text = "Private"
            }
        }
    }

    private fun selectSocketCount(count: Int) {
        binding.apply {
            // Reset all buttons to default outlined style
            val defaultColor = ContextCompat.getColor(this@EditLocationActivity, android.R.color.darker_gray)
            val selectedColor = ContextCompat.getColor(this@EditLocationActivity, com.google.android.material.R.color.design_default_color_primary)
            val defaultWidth = 1
            val selectedWidth = 3

            socket1Button.icon = null
            socket1Button.strokeColor = android.content.res.ColorStateList.valueOf(defaultColor)
            socket1Button.strokeWidth = defaultWidth

            socket2Button.icon = null
            socket2Button.strokeColor = android.content.res.ColorStateList.valueOf(defaultColor)
            socket2Button.strokeWidth = defaultWidth

            socket4Button.icon = null
            socket4Button.strokeColor = android.content.res.ColorStateList.valueOf(defaultColor)
            socket4Button.strokeWidth = defaultWidth

            socket10Button.icon = null
            socket10Button.strokeColor = android.content.res.ColorStateList.valueOf(defaultColor)
            socket10Button.strokeWidth = defaultWidth

            socket10PlusButton.icon = null
            socket10PlusButton.strokeColor = android.content.res.ColorStateList.valueOf(defaultColor)
            socket10PlusButton.strokeWidth = defaultWidth

            // Highlight selected button with thicker, colored border
            val selectedButton = when (count) {
                1 -> socket1Button
                2 -> socket2Button
                4 -> socket4Button
                10 -> socket10Button
                else -> socket10PlusButton
            }
            selectedButton.strokeColor = android.content.res.ColorStateList.valueOf(selectedColor)
            selectedButton.strokeWidth = selectedWidth

            viewModel.updateGpoCount(count)
        }
    }

    private fun updateUIFromFormData(data: EditLocationFormData) {
        binding.apply {
            nameInput.setOnFocusChangeListener { _, _ ->
                viewModel.updateName(nameInput.text.toString())
            }

            descriptionInput.setOnFocusChangeListener { _, _ ->
                viewModel.updateDescription(descriptionInput.text.toString())
            }

            openingHoursSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    val selectedHours = parent?.getItemAtPosition(position).toString()
                    viewModel.updateOpeningHours(selectedHours)
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

            additionalInfoInput.setOnFocusChangeListener { _, _ ->
                viewModel.updateAdditionalInfo(additionalInfoInput.text.toString())
            }

            feeCheckbox.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateHasFee(isChecked)
                feeDetailsSection.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
            }

            providerNameInput.setOnFocusChangeListener { _, _ ->
                viewModel.updateProviderName(providerNameInput.text.toString())
            }

            chargingCostsInput.setOnFocusChangeListener { _, _ ->
                viewModel.updateChargingCosts(chargingCostsInput.text.toString())
            }

            providerWebsiteInput.setOnFocusChangeListener { _, _ ->
                viewModel.updateProviderWebsite(providerWebsiteInput.text.toString())
            }
        }
    }

    private fun submitUpdates() {
        if (photosBeingProcessed > 0) {
            Toast.makeText(this, "Please wait for photos to finish processing", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            Log.d("EditLocationActivity", "")
            Log.d("EditLocationActivity", "💾 SUBMITTING LOCATION UPDATE")

            val finalLocationId = if (isAddMode) currentEditLocationId ?: UUID.randomUUID().toString() else locationId!!
            Log.d("EditLocationActivity", "   Final location ID: $finalLocationId")
            Log.d("EditLocationActivity", "   Is add mode: $isAddMode")
            Log.d("EditLocationActivity", "   Current photoUris: $photoUris")

            // Move temporary images to permanent storage
            val newPermanentPaths: List<String> = if (isAddMode) {
                // Add mode: Move from temp directory structure
                ImageUtils.moveTempImagesToPermanent(
                    this@EditLocationActivity,
                    currentEditLocationId ?: "temp_fallback",
                    finalLocationId
                )
            } else {
                // Edit mode: Move images from photoUris that are not in permanent storage
                val permanentDir = ImageUtils.getLocationImageDirectory(this@EditLocationActivity, finalLocationId)
                val migratedPaths = mutableListOf<String>()

                photoUris.forEach { photoPath ->
                    Log.d("EditLocationActivity", "   Checking photo: $photoPath")
                    val photoFile = File(photoPath)

                    // If the image is in cache (edited/temp), move it to permanent storage
                    if (photoPath.contains("cache")) {
                        // Calculate next index for permanent storage
                        val existingImages = permanentDir.listFiles()?.filter { it.extension == "jpg" } ?: emptyList()
                        val nextIndex = if (existingImages.isEmpty()) 0 else {
                            existingImages.mapNotNull { file ->
                                file.name.removePrefix("image_").removeSuffix(".jpg").toIntOrNull()
                            }.maxOrNull()?.plus(1) ?: 0
                        }

                        val permanentFile = File(permanentDir, "image_$nextIndex.jpg")
                        try {
                            photoFile.renameTo(permanentFile)
                            migratedPaths.add(permanentFile.absolutePath)
                            Log.d("EditLocationActivity", "   ✓ Moved to permanent: ${permanentFile.absolutePath}")
                        } catch (e: Exception) {
                            Log.e("EditLocationActivity", "   ✗ Error moving photo: ${e.message}", e)
                        }
                    } else {
                        // Already in permanent storage, keep as is
                        migratedPaths.add(photoPath)
                        Log.d("EditLocationActivity", "   ✓ Already permanent: $photoPath")
                    }
                }
                migratedPaths
            }

            Log.d("EditLocationActivity", "📸 PHOTO MIGRATION COMPLETE")
            Log.d("EditLocationActivity", "   Total paths: ${newPermanentPaths.size}")
            newPermanentPaths.forEachIndexed { index, path ->
                Log.d("EditLocationActivity", "   [$index] $path")
            }

            // Update photoUris with the new permanent paths
            withContext(Dispatchers.Main) {
                photoUris.clear()
                photoUris.addAll(newPermanentPaths)
                viewModel.updatePhotoPaths(photoUris)
            }

            // Persist focus regions from SharedPreferences to database
            val sharedPref = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this@EditLocationActivity)
            Log.d("EditLocationActivity", "")
            Log.d("EditLocationActivity", "🔍 SEARCHING FOR FOCUS IN SharedPreferences")
            Log.d("EditLocationActivity", "   Total SharedPref keys: ${sharedPref.all.size}")

            for (permanentPath in newPermanentPaths) {
                // Try to find focus for this path in SharedPreferences
                // It might be stored under either the permanent path or a temp/edited path
                var focusString: String? = sharedPref.getString("focus_$permanentPath", null)

                if (focusString == null) {
                    // If not found, it might have been stored under an old path
                    // Check all focus_ keys in SharedPreferences
                    Log.d("EditLocationActivity", "   Not found for $permanentPath, searching all keys...")
                    for ((key, value) in sharedPref.all) {
                        if (key.startsWith("focus_") && value is String) {
                            Log.d("EditLocationActivity", "   Found: key=$key")
                            // This is a focus entry, check if it should apply to this path
                            focusString = value as String
                            break // Use the first one found
                        }
                    }
                }

                if (focusString != null) {
                    Log.d("EditLocationActivity", "💾 SAVING PHOTO WITH FOCUS")
                    Log.d("EditLocationActivity", "   Path: $permanentPath")
                    Log.d("EditLocationActivity", "   Focus: $focusString")

                    try {
                        // Create PhotoEntity with the permanent path and focus
                        val photoId = UUID.randomUUID().toString()
                        val photoEntity = PhotoEntity(
                            id = photoId,
                            locationId = finalLocationId,
                            filePath = permanentPath,
                            timestamp = System.currentTimeMillis(),
                            exifMetadata = null,
                            gpxData = null,
                            focusRectString = focusString
                        )
                        photoRepository.insertPhoto(photoEntity)
                        Log.d("EditLocationActivity", "   ✅ Saved to database")
                    } catch (e: Exception) {
                        Log.e("EditLocationActivity", "   ❌ ERROR saving photo: ${e.message}", e)
                    }
                } else {
                    Log.d("EditLocationActivity", "📸 SAVING PHOTO WITHOUT FOCUS")
                    Log.d("EditLocationActivity", "   Path: $permanentPath")

                    try {
                        // Insert photo without focus
                        val photoId = UUID.randomUUID().toString()
                        val photoEntity = PhotoEntity(
                            id = photoId,
                            locationId = finalLocationId,
                            filePath = permanentPath,
                            timestamp = System.currentTimeMillis(),
                            exifMetadata = null,
                            gpxData = null,
                            focusRectString = null
                        )
                        photoRepository.insertPhoto(photoEntity)
                        Log.d("EditLocationActivity", "   ✅ Saved to database")
                    } catch (e: Exception) {
                        Log.e("EditLocationActivity", "   ❌ ERROR saving photo: ${e.message}", e)
                    }
                }
            }

            // Clear focus SharedPreferences after saving
            sharedPref.edit().apply {
                for ((key, _) in sharedPref.all) {
                    if (key.startsWith("focus_")) {
                        remove(key)
                    }
                }
            }.apply()
            Log.d("EditLocationActivity", "🧹 Cleared focus from SharedPreferences")

            // Continue with location save on Main dispatcher
            withContext(Dispatchers.Main) {
                // Update locationId for all photos associated with this session
                if (isAddMode && currentEditLocationId != null) {
                    photoRepository.updatePhotoLocationId(currentEditLocationId!!, finalLocationId)
                }

                if (isAddMode) {
                    viewModel.addLocation(finalLocationId)
                } else {
                    viewModel.submitUpdates(finalLocationId)
                }
                submittedSuccessfully = true
            }
        }
    }

    private fun updateSubmitButtonState() {

        runOnUiThread {
            binding.apply {
                val isProcessingPhotos = photosBeingProcessed > 0

                // In add mode, also check that name is not blank
                val shouldBeEnabled = if (isAddMode) {
                    !isProcessingPhotos && !nameInput.text.isNullOrBlank()
                } else {
                    // In edit mode, only disable while processing photos
                    !isProcessingPhotos
                }

                Log.d("EditLocationActivity", "  isProcessingPhotos=$isProcessingPhotos, shouldBeEnabled=$shouldBeEnabled")
                buttonSubmit.isEnabled = shouldBeEnabled

                if (isProcessingPhotos) {
                    buttonSubmit.text = "Processing $photosBeingProcessed photo(s)..."
                    Log.d("EditLocationActivity", "  Button text: '${buttonSubmit.text}' DISABLED")
                } else {
                    buttonSubmit.text = if (isAddMode) "Add Location" else "Update"
                    Log.d("EditLocationActivity", "  Button text: '${buttonSubmit.text}' ENABLED")
                }
            }
        }
    }

    private fun syncToOsmand() {
        locationId?.let { viewModel.syncToOsmand(it) }
    }

    private fun showAiDescriptionDialog() {
        val name = binding.nameInput.text.toString()
        if (name.isBlank()) {
            Toast.makeText(this, "Please enter a location name first", Toast.LENGTH_SHORT).show()
            return
        }

        val gpoCount = viewModel.formData.value.gpoCount
        val openingHours = binding.openingHoursSpinner.selectedItem?.toString()
        val additionalInfo = binding.additionalInfoInput.text.toString().takeIf { it.isNotBlank() }
        val currentDescription = binding.descriptionInput.text.toString()

        // Get socket type from spinner
        val socketTypeIndex = binding.socketTypeSpinner.selectedItemPosition
        val socketType = when (socketTypeIndex) {
            1 -> "TYPE2"
            2 -> "TESLA"
            3 -> "CCS2"
            4 -> "CHADEMO"
            5 -> "TYPE1"
            6 -> "WALL_SOCKET"
            7 -> "CARAVAN"
            8 -> "OTHER"
            else -> "AS3112"
        }

        // Get access type from indicator
        val accessType = if (binding.accessTypeText.text == "Public") {
            com.a42r.pevplugshare.data.model.LocationAccessType.PUBLIC
        } else {
            com.a42r.pevplugshare.data.model.LocationAccessType.PRIVATE
        }

        // Get fee status
        val hasFee = binding.feeCheckbox.isChecked

        // Get capacity if available
        val capacity = binding.chargingCostsInput.text.toString().takeIf { it.isNotBlank() }

        val dialog = AiDescriptionDialogFragment.newInstance(
            locationName = name,
            accessType = accessType,
            hasFee = hasFee,
            chargingPoints = gpoCount,
            socketTypes = listOf(socketType),
            openingHours = openingHours,
            capacity = capacity,
            additionalInfo = additionalInfo,
            originalDescription = currentDescription.takeIf { it.isNotBlank() }
        ) { generatedDescription ->
            binding.descriptionInput.setText(generatedDescription)
            viewModel.updateDescription(generatedDescription)
            Toast.makeText(this, "Description updated", Toast.LENGTH_SHORT).show()
        }

        dialog.show(supportFragmentManager, "AiDescriptionDialog")
    }

    private fun parseDescriptionToFields() {
        val description = binding.descriptionInput.text.toString()
        if (description.isBlank()) {
            Toast.makeText(this, "No description to parse", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.buttonParseDescription.isEnabled = false

        viewModel.parseDescription(description) { parsed, error ->
            binding.progressBar.visibility = android.view.View.GONE
            binding.buttonParseDescription.isEnabled = true

            if (parsed != null) {
                val extractedInfo = mutableListOf<String>()

                // Update opening hours - default to 24/7 if not found
                val hours = parsed.openingHours ?: "24/7"
                extractedInfo.add("Opening hours: $hours${if (parsed.openingHours == null) " (default)" else ""}")

                // Try to match with spinner options
                val hoursAdapter = binding.openingHoursSpinner.adapter as android.widget.ArrayAdapter<String>
                var matchedHours = false
                for (i in 0 until hoursAdapter.count) {
                    val option = hoursAdapter.getItem(i)
                    if (option != null && hours.contains(option, ignoreCase = true)) {
                        binding.openingHoursSpinner.setSelection(i)
                        matchedHours = true
                        break
                    }
                }
                // If no match found, default to 24/7 (index 0)
                if (!matchedHours) {
                    binding.openingHoursSpinner.setSelection(0)
                }
                viewModel.updateOpeningHours(hours)

                // Update charging points if found
                if (parsed.chargingPoints != null) {
                    extractedInfo.add("Number of sockets: ${parsed.chargingPoints}")
                    selectSocketCount(parsed.chargingPoints)
                }

                // Update capacity/costs if found
                if (parsed.capacity != null) {
                    extractedInfo.add("Capacity: ${parsed.capacity}")
                    binding.chargingCostsInput.setText(parsed.capacity)
                    viewModel.updateChargingCosts(parsed.capacity)
                }

                // Update fee status
                val feeInfo = if (parsed.isFree) "Free to use" else "Has charging fees"
                extractedInfo.add(feeInfo)
                binding.feeCheckbox.isChecked = parsed.hasFee
                updateFeeIndicator(parsed.hasFee)
                binding.feeDetailsSection.visibility = if (parsed.hasFee) android.view.View.VISIBLE else android.view.View.GONE
                viewModel.updateHasFee(parsed.hasFee)

                // Update public/private status if found
                if (parsed.isPublic != null) {
                    val accessInfo = if (parsed.isPublic) "Public access" else "Private access"
                    extractedInfo.add(accessInfo)
                    updateAccessTypeIndicator(parsed.isPublic)
                    viewModel.updateAccessType(if (parsed.isPublic) "public" else "private")
                }

                // Add additional info if found
                if (parsed.additionalInfo != null) {
                    extractedInfo.add("Additional details: ${parsed.additionalInfo}")
                    binding.additionalInfoInput.setText(parsed.additionalInfo)
                }

                val message = if (extractedInfo.isNotEmpty()) {
                    "✅ Extracted:\n" + extractedInfo.joinToString("\n")
                } else {
                    "No additional information could be extracted from the description"
                }

                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            } else {
                val errorMsg = error ?: "Unknown error"
                Log.e("EditLocationActivity", "Parse error: $errorMsg")
                Toast.makeText(this, "❌ Failed to parse:\n$errorMsg", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getSocketTypeIndex(socketType: String): Int {
        return when (socketType.uppercase()) {
            "TYPE2_EV", "TYPE2" -> 1
            "TESLA" -> 2
            "CCS2", "COMBO" -> 3
            "CHADEMO" -> 4
            "TYPE1", "J1772" -> 5
            "WALL_SOCKET", "15A" -> 6
            "CARAVAN" -> 7
            "OTHER_EV", "OTHER" -> 8
            else -> 0 // AS3112
        }
    }

    private fun showDeleteConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Location")
            .setMessage("Are you sure you want to delete this location and all its associated images? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                dialog.dismiss()
                locationId?.let { id ->
                    viewModel.deleteLocation(id)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupPhotoSection() {
        binding.addPhotoButton.setOnClickListener {
            if (photoUris.size < maxPhotos) {
                showPhotoSourceDialog()
            } else {
                Toast.makeText(this, "Maximum 5 photos allowed", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup analyze photos button
        binding.analyzePhotosButton.setOnClickListener {
            if (photoUris.isNotEmpty()) {
                analyzePhotosWithAI()
            } else {
                Toast.makeText(this, "Add photos first to analyze", Toast.LENGTH_SHORT).show()
            }
        }

        // Show/hide analyze button based on photo count
        updateAnalyzeButtonVisibility()

        // Load existing photos if in edit mode
        if (!isAddMode) {
            loadExistingPhotos()
        }
    }

    private fun updateAnalyzeButtonVisibility() {
        val hasApiKey = tokenManager.getOpenRouterApiKey()?.isNotBlank() == true
        val hasPhotos = photoUris.isNotEmpty()
        binding.analyzePhotosButton.visibility = if (hasApiKey && hasPhotos) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }

    private fun showPhotoSourceDialog() {
        val options = arrayOf("Take Photo", "Choose Multiple Photos")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Add Photos")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> {
                        // Add small delay to ensure photo picker is ready
                        binding.root.postDelayed({
                            pickImages.launch("image/*")
                        }, 300)
                    }
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                cameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        val photoFile = createImageFile()
        photoFile?.let { file ->
            currentPhotoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )
            tempPhotoFile = file
            takePicture.launch(currentPhotoUri)
        }
    }

    private fun createImageFile(): File? {
        return try {
            val timestamp = System.currentTimeMillis()
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile("JPEG_${timestamp}_", ".jpg", storageDir)
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Process cache photos from QuickLocationActivity into permanent storage
     * This is called during onCreate for add mode when photos are passed via Intent
     */
    private suspend fun processCachePhotos(cachePaths: List<String>, sessionId: String): List<String> {
        val temporaryPaths = mutableListOf<String>()

        cachePaths.forEachIndexed { index, cachePath ->
            try {
                val cacheFile = File(cachePath)

                if (cacheFile.exists()) {
                    photoSaveMutex.lock()
                    try {
                        val resizedFile = ImageUtils.resizeAndSaveImage(
                            this@EditLocationActivity,
                            cacheFile,
                            sessionId,
                            index,
                            isTemporary = true
                        )

                        resizedFile?.let {
                            temporaryPaths.add(it.absolutePath)
                            // Delete cache file after successful save
                            cacheFile.delete()
                        }
                    } finally {
                        photoSaveMutex.unlock()
                    }
                }
            } catch (e: Exception) {
                Log.e("EditLocationActivity", "Error processing cache image $index", e)
            }
        }

        return temporaryPaths
    }

    private fun processAndSavePhoto() {
        photosBeingProcessed++
        updateSubmitButtonState()
        Log.d("EditLocationActivity", "=== START processAndSavePhoto: photosBeingProcessed=$photosBeingProcessed ===")

        lifecycleScope.launch(Dispatchers.IO) {
            tempPhotoFile?.let { sourceFile ->
                val currentSessionId = currentEditLocationId ?: "temp_fallback" // Use currentEditLocationId as session ID
                Log.d("EditLocationActivity", "Processing camera photo - sessionId: $currentSessionId")

                photoSaveMutex.lock()
                try {
                    // Find next available index INSIDE the mutex to prevent race condition
                    val nextImageIndex = findNextImageIndex(currentSessionId, isTemporary = true)
                    Log.d("EditLocationActivity", "Got nextImageIndex: $nextImageIndex (under mutex lock)")

                    val resizedFile = ImageUtils.resizeAndSaveImage(
                        this@EditLocationActivity,
                        sourceFile,
                        currentSessionId,
                        nextImageIndex,
                        isTemporary = true
                    )
                    Log.d("EditLocationActivity", "After resizeAndSaveImage: file=${ resizedFile?.absolutePath ?: "NULL"}")

                    withContext(Dispatchers.Main) {
                        resizedFile?.let {
                            Log.d("EditLocationActivity", "✓ Camera photo saved successfully to temp: ${it.absolutePath}")
                            photoUris.add(it.absolutePath)
                            addPhotoThumbnail(it)
                            viewModel.updatePhotoPaths(photoUris)
                            // Delete temp file
                            sourceFile.delete()
                            tempPhotoFile = null
                            Toast.makeText(
                                this@EditLocationActivity,
                                "Photo added (${photoUris.size}/$maxPhotos)",
                                Toast.LENGTH_SHORT
                            ).show()
                        } ?: run {
                            Log.e("EditLocationActivity", "✗ Failed to save camera photo")
                            Toast.makeText(
                                this@EditLocationActivity,
                                "Failed to save photo",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        photosBeingProcessed--
                        Log.d("EditLocationActivity", "✓ Camera photo complete: photosBeingProcessed=$photosBeingProcessed")
                        updateSubmitButtonState()
                    }
                } catch (e: Exception) {
                    Log.e("EditLocationActivity", "Exception in processAndSavePhoto", e)
                    withContext(Dispatchers.Main) {
                        photosBeingProcessed--
                        updateSubmitButtonState()
                    }
                } finally {
                    photoSaveMutex.unlock()
                    Log.d("EditLocationActivity", "=== END processAndSavePhoto: mutex unlocked ===")
                }
            }
        }
    }

    private fun processAndSaveGalleryPhoto(uri: Uri) {
        photosBeingProcessed++
        updateSubmitButtonState()
        Log.d("EditLocationActivity", "=== START processAndSaveGalleryPhoto: photosBeingProcessed=$photosBeingProcessed ===")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Copy URI content to temp file
                val tempFile = File.createTempFile("gallery_", ".jpg", cacheDir)
                Log.d("EditLocationActivity", "Created temp file: ${tempFile.absolutePath}")

                contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d("EditLocationActivity", "Copied gallery photo to temp file: ${tempFile.length()} bytes")

                val currentSessionId = currentEditLocationId ?: "temp_fallback" // Use currentEditLocationId as session ID
                Log.d("EditLocationActivity", "Processing gallery photo - sessionId: $currentSessionId")

                photoSaveMutex.lock()
                try {
                    // Find next available index INSIDE the mutex to prevent race condition
                    val nextImageIndex = findNextImageIndex(currentSessionId, isTemporary = true)
                    Log.d("EditLocationActivity", "Got nextImageIndex: $nextImageIndex (under mutex lock)")

                    val resizedFile = ImageUtils.resizeAndSaveImage(
                        this@EditLocationActivity,
                        tempFile,
                        currentSessionId,
                        nextImageIndex,
                        isTemporary = true
                    )
                    Log.d("EditLocationActivity", "After resizeAndSaveImage: file=${resizedFile?.absolutePath ?: "NULL"}")

                    withContext(Dispatchers.Main) {
                        resizedFile?.let { file ->
                            Log.d("EditLocationActivity", "✓ Gallery photo saved successfully to temp: ${file.absolutePath}")
                            photoUris.add(file.absolutePath)
                            addPhotoThumbnail(file)
                            viewModel.updatePhotoPaths(photoUris)
                            Toast.makeText(
                                this@EditLocationActivity,
                                "Photo added (${photoUris.size}/$maxPhotos)",
                                Toast.LENGTH_SHORT
                            ).show()
                        } ?: run {
                            Log.e("EditLocationActivity", "✗ Failed to save gallery photo")
                            Toast.makeText(
                                this@EditLocationActivity,
                                "Failed to save photo",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        photosBeingProcessed--
                        Log.d("EditLocationActivity", "✓ Gallery photo complete: photosBeingProcessed=$photosBeingProcessed")
                        updateSubmitButtonState()
                    }
                } catch (e: Exception) {
                    Log.e("EditLocationActivity", "Exception while holding mutex in processAndSaveGalleryPhoto", e)
                    withContext(Dispatchers.Main) {
                        photosBeingProcessed--
                        updateSubmitButtonState()
                    }
                } finally {
                    photoSaveMutex.unlock()
                    Log.d("EditLocationActivity", "=== END processAndSaveGalleryPhoto: mutex unlocked ===")
                }

                // Delete temp file
                tempFile.delete()
                Log.d("EditLocationActivity", "Deleted temp file")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("EditLocationActivity", "Exception processing gallery photo", e)
                    photosBeingProcessed--
                    updateSubmitButtonState()
                    Toast.makeText(
                        this@EditLocationActivity,
                        "Failed to process photo: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun addPhotoThumbnail(file: File) {
        val thumbnailView = LayoutInflater.from(this)
            .inflate(R.layout.item_photo_thumbnail, binding.photosContainer, false)
        val imageView = thumbnailView.findViewById<ImageView>(R.id.thumbnailImage)
        val removeButton = thumbnailView.findViewById<ImageView>(R.id.removePhotoButton)
        val focusIndicator = thumbnailView.findViewById<ImageView>(R.id.focusIndicator)

        val filePath = file.absolutePath
        // Store current path in tag so it can be updated later
        imageView.tag = filePath
        Log.d("EditLocationActivity", "   Setting imageView tag: $filePath")

        lifecycleScope.launch(Dispatchers.IO) {
            Log.d("EditLocationActivity", "")
            Log.d("EditLocationActivity", "🖼️  THUMBNAIL LOADING STARTED")
            Log.d("EditLocationActivity", "   File: $filePath")

            // Check for focus in SharedPreferences first (during editing)
            val sharedPref = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this@EditLocationActivity)
            var focusRectString: String? = sharedPref.getString("focus_$filePath", null)

            Log.d("EditLocationActivity", "📋 FOCUS LOOKUP")
            Log.d("EditLocationActivity", "   Checking SharedPreferences: $filePath")
            Log.d("EditLocationActivity", "   Focus from SharedPreferences: $focusRectString")

            // Denormalize focus rect from normalized coordinates
            val focusRect = focusRectString?.let { rectString ->
                Log.d("EditLocationActivity", "🔄 DENORMALIZATION PROCESS")
                // First, we need to get the actual image dimensions to denormalize
                val file = File(filePath)
                if (file.exists()) {
                    val options = android.graphics.BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    android.graphics.BitmapFactory.decodeFile(filePath, options)
                    val imageWidth = options.outWidth
                    val imageHeight = options.outHeight

                    Log.d("EditLocationActivity", "   Original image size: $imageWidth x $imageHeight")
                    Log.d("EditLocationActivity", "   Normalized string: $rectString")

                    // Denormalize the focus rectangle using actual image dimensions
                    val denormalized = com.a42r.pevplugshare.util.PhotoUtils.normalizedStringToFocusRect(rectString, imageWidth, imageHeight)
                    Log.d("EditLocationActivity", "   Denormalized: ${denormalized?.let { "L=${it.left} T=${it.top} R=${it.right} B=${it.bottom}" } ?: "null"}")
                    denormalized
                } else {
                    Log.e("EditLocationActivity", "❌ Image file not found: $filePath")
                    null
                }
            }
            Log.d("EditLocationActivity", "🖼️  THUMBNAIL LOADING COMPLETE - Focus ready: ${focusRect != null}")



            withContext(Dispatchers.Main) {
                Glide.with(this@EditLocationActivity)
                    .asBitmap()
                    .load(file)
                    .fitCenter() // Changed from centerCrop() to fitCenter()
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE) // Bypass disk cache
                    .skipMemoryCache(true) // Bypass memory cache
                    .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                        override fun onResourceReady(bitmap: android.graphics.Bitmap, transition: com.bumptech.glide.request.transition.Transition<in android.graphics.Bitmap>?) {
                            Log.d("EditLocationActivity", "Focus rect for $filePath: ${focusRect?.let { "${it.left},${it.top},${it.right},${it.bottom}" } ?: "null"}")

                            if (focusRect != null) {
                                Log.d("EditLocationActivity", "Drawing yellow rectangle for $filePath")
                                val mutableBitmap = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
                                val canvas = android.graphics.Canvas(mutableBitmap)
                                val paint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.YELLOW
                                    style = android.graphics.Paint.Style.STROKE
                                    strokeWidth = 5f
                                }
                                // Scale the focusRect to the thumbnail size
                                val scaleX = mutableBitmap.width.toFloat() / bitmap.width
                                val scaleY = mutableBitmap.height.toFloat() / bitmap.height
                                val scaledRect = android.graphics.RectF(
                                    focusRect.left * scaleX,
                                    focusRect.top * scaleY,
                                    focusRect.right * scaleX,
                                    focusRect.bottom * scaleY
                                )
                                Log.d("EditLocationActivity", "Scaled rect: ${scaledRect.left},${scaledRect.top},${scaledRect.right},${scaledRect.bottom} (scale: $scaleX x $scaleY)")
                                canvas.drawRect(scaledRect, paint)
                                imageView.setImageBitmap(mutableBitmap)
                            } else {
                                Log.d("EditLocationActivity", "No focus rect for $filePath, displaying plain bitmap")
                                imageView.setImageBitmap(bitmap)
                            }
                            focusIndicator.visibility = android.view.View.GONE // Hide since we draw directly
                        }

                        override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                            // Do nothing
                        }
                    })
            }
        }

        // Click to edit image
        imageView.setOnClickListener {
            // Get the current path from the tag (which may have been updated)
            val currentPath = imageView.tag as? String ?: file.absolutePath
            Log.d("EditLocationActivity", "   Click listener - imageView.tag=${imageView.tag}, currentPath=$currentPath")
            Log.d("EditLocationActivity", "   photoUris=$photoUris")
            val currentIndex = photoUris.indexOfFirst { it == currentPath }
            if (currentIndex >= 0) {
                Log.d("EditLocationActivity", "   Click to edit - found index=$currentIndex for path=$currentPath")
                editPhoto(currentIndex, File(photoUris[currentIndex]))
            } else {
                Log.e("EditLocationActivity", "❌ Could not find photo in photoUris: $currentPath")
            }
        }

        // Long click shows info
        imageView.setOnLongClickListener {
            val currentPath = imageView.tag as? String ?: file.absolutePath
            lifecycleScope.launch(Dispatchers.IO) {
                val photoEntity = photoRepository.getPhotoByFilePath(currentPath)
                val hasFocus = photoEntity?.focusRectString != null
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@EditLocationActivity,
                        if (hasFocus) "Focus area set - tap to edit" else "Tap to edit, long press for info",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            true
        }

        removeButton.setOnClickListener {
            val currentPath = imageView.tag as? String ?: file.absolutePath
            val removeIndex = photoUris.indexOfFirst { it == currentPath }
            if (removeIndex >= 0) {
                removePhoto(removeIndex)
            } else {
                Log.e("EditLocationActivity", "❌ Could not find photo index for removal: $currentPath")
            }
        }

        binding.photosContainer.addView(thumbnailView, binding.photosContainer.childCount)
        updateAddButtonVisibility()
        updateAnalyzeButtonVisibility()
    }

    private fun removePhoto(index: Int) {
        if (index < photoUris.size) {
            val path = photoUris[index]
            val sessionId = currentEditLocationId ?: locationId ?: return // Use currentEditLocationId for temp, or locationId for permanent

            // Delete the actual file
            File(path).delete()

            photoMetadata.remove(path)
            photoUris.removeAt(index)
            viewModel.updatePhotoPaths(photoUris)

            // Delete from database
            lifecycleScope.launch(Dispatchers.IO) {
                photoRepository.deletePhotoByFilePath(path)
            }

            // Renumber remaining images to ensure continuous indexing
            renumberPhotos(sessionId, isTemporary = isAddMode) // Renumber in temp if add mode, else permanent

            refreshPhotoThumbnails()
            updateAnalyzeButtonVisibility()
            Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renumberPhotos(id: String, isTemporary: Boolean) {
        val imageFiles = if (isTemporary) {
            ImageUtils.getTempImageDirectory(this, id).listFiles()?.filter { it.extension == "jpg" }?.sortedBy { it.name } ?: emptyList()
        } else {
            ImageUtils.getLocationImages(this, id)
        }

        imageFiles.forEachIndexed { index, file ->
            val expectedName = "image_$index.jpg"
            if (file.name != expectedName) {
                val newFile = File(file.parent, expectedName)
                file.renameTo(newFile)
                // Update photoUris with new path
                val oldPath = file.absolutePath
                val newPath = newFile.absolutePath
                photoUris.replaceAll { if (it == oldPath) newPath else it }

                // Update database with new file path
                lifecycleScope.launch(Dispatchers.IO) {
                    photoRepository.updatePhotoFilePath(oldPath, newPath)
                }
            }
        }
    }

    private fun editPhoto(index: Int, file: File) {
        try {
            Log.d("EditLocationActivity", "editPhoto called: index=$index, file=${file.absolutePath}")

            if (!file.exists()) {
                Toast.makeText(this, "Photo file no longer exists", Toast.LENGTH_SHORT).show()
                Log.e("EditLocationActivity", "Photo file does not exist: ${file.absolutePath}")
                return
            }

            // Create temp file first
            val editOutputFile = try {
                File.createTempFile("edited_", ".jpg", cacheDir)
            } catch (e: Exception) {
                Log.e("EditLocationActivity", "Error creating temp file", e)
                Toast.makeText(this, "Error creating temp file: ${e.message}", Toast.LENGTH_SHORT).show()
                return
            }

            editingPhotoIndex = index

            // Load focus from database if this is an existing image
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val filePath = file.absolutePath
                    Log.d("EditLocationActivity", "📌 Loading focus from database for: $filePath")

                    // Query database for focus information
                    val photoEntity = photoRepository.getPhotoByFilePath(filePath)

                    if (photoEntity != null && !photoEntity.focusRectString.isNullOrEmpty()) {
                        Log.d("EditLocationActivity", "✓ Found focus in database: ${photoEntity.focusRectString}")

                        // Store focus in SharedPreferences using the output file path as key
                        val sharedPref = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this@EditLocationActivity)
                        sharedPref.edit().putString("focus_${editOutputFile.absolutePath}", photoEntity.focusRectString).apply()
                        Log.d("EditLocationActivity", "✓ Stored focus in SharedPreferences for editing")
                    } else {
                        Log.d("EditLocationActivity", "- No focus in database for this image")
                    }
                } catch (e: Exception) {
                    Log.e("EditLocationActivity", "Error loading focus from database: ${e.message}", e)
                }

                // Launch image editor on main thread
                withContext(Dispatchers.Main) {
                    val intent = android.content.Intent(this@EditLocationActivity, ImageEditorActivity::class.java).apply {
                        putExtra(ImageEditorActivity.EXTRA_IMAGE_PATH, file.absolutePath)
                        putExtra(ImageEditorActivity.EXTRA_OUTPUT_PATH, editOutputFile.absolutePath)
                    }

                    Log.d("EditLocationActivity", "Opening image editor with output: ${editOutputFile.absolutePath}")

                    try {
                        imageEditor.launch(intent)
                        Log.d("EditLocationActivity", "Image editor launched successfully")
                    } catch (e: Exception) {
                        Log.e("EditLocationActivity", "Error launching image editor", e)
                        Toast.makeText(this@EditLocationActivity, "Error opening image editor: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening image editor: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("EditLocationActivity", "Error in editPhoto", e)
        }
    }

    private fun checkLocationUpdate(metadata: com.a42r.pevplugshare.data.model.PhotoMetadata) {
        // Check if photo location differs significantly from current location
        if (metadata.latitude == null || metadata.longitude == null) {
            return
        }

        val photoLat = metadata.latitude!!
        val photoLon = metadata.longitude!!

        // Calculate distance from current location
        val distance = com.a42r.pevplugshare.util.DistanceCalculator.calculateDistance(
            selectedLatitude, selectedLongitude,
            photoLat, photoLon
        )

        // If more than 50 meters different, ask user
        if (distance > 50) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Update Location from Photo GPS?")
                .setMessage(
                    "Photo GPS location differs by ${String.format("%.0f", distance)}m:\n" +
                    "Current: $selectedLatitude, $selectedLongitude\n" +
                    "Photo: $photoLat, $photoLon\n\n" +
                    "Update location to photo GPS coordinates?"
                )
                .setPositiveButton("Update") { _, _ ->
                    updateLocationFromPhoto(photoLat, photoLon, metadata)
                }
                .setNegativeButton("Keep Current", null)
                .show()
        }
    }

    private fun updateLocationFromPhoto(
        photoLat: Double,
        photoLon: Double,
        metadata: com.a42r.pevplugshare.data.model.PhotoMetadata
    ) {
        selectedLatitude = photoLat
        selectedLongitude = photoLon

        // Update UI fields if they exist
        try {
            // Try to update latitude/longitude fields if visible
            val latField = binding.root.findViewWithTag<android.widget.EditText>("latitude_field")
            val lonField = binding.root.findViewWithTag<android.widget.EditText>("longitude_field")
            latField?.setText(String.format("%.6f", photoLat))
            lonField?.setText(String.format("%.6f", photoLon))
        } catch (e: Exception) {
            Log.d("EditLocationActivity", "Could not update coordinate fields: ${e.message}")
        }

        // Add bearing/direction to additional notes if available
        if (metadata.hasBearingData && metadata.bearing != null) {
            try {
                val notesField = binding.root.findViewWithTag<android.widget.EditText>("notes_field")
                val currentNotes = notesField?.text?.toString() ?: ""
                val bearingNote = "Direction when photo taken: ${metadata.getBearingString()}"
                val updatedNotes = if (currentNotes.isEmpty()) {
                    bearingNote
                } else {
                    "$currentNotes\n$bearingNote"
                }
                notesField?.setText(updatedNotes)
            } catch (e: Exception) {
                Log.d("EditLocationActivity", "Could not update notes field: ${e.message}")
            }
        }

        Toast.makeText(
            this,
            "Location updated to GPS coordinates from photo",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun refreshPhotoThumbnails() {
        // Remove all thumbnails except the add button
        while (binding.photosContainer.childCount > 1) {
            binding.photosContainer.removeViewAt(1)
        }

        // Re-add thumbnails
        photoUris.forEachIndexed { index, path ->
            addPhotoThumbnail(File(path))
        }

        updateAddButtonVisibility()
    }

    private fun updateAddButtonVisibility() {
        binding.addPhotoButton.visibility = if (photoUris.size >= maxPhotos) {
            android.view.View.GONE
        } else {
            android.view.View.VISIBLE
        }
    }

    private fun loadExistingPhotos() {
        locationId?.let { id ->
            val existingPhotos = ImageUtils.getLocationImages(this, id)
            Log.d("EditLocationActivity", "Loading existing photos for location $id: found ${existingPhotos.size} photos")

            // Load focus information from database into SharedPreferences SYNCHRONOUSLY
            Log.d("EditLocationActivity", "📌 Loading focus from database for all existing photos")
            val sharedPref = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this@EditLocationActivity)

            // Use runBlocking to perform the database queries synchronously on IO thread
            runBlocking(Dispatchers.IO) {
                existingPhotos.forEach { file ->
                    try {
                        val photoEntity = photoRepository.getPhotoByFilePath(file.absolutePath)
                        if (photoEntity != null && !photoEntity.focusRectString.isNullOrEmpty()) {
                            sharedPref.edit().putString("focus_${file.absolutePath}", photoEntity.focusRectString).apply()
                            Log.d("EditLocationActivity", "✓ Loaded focus from DB for ${file.name}: ${photoEntity.focusRectString}")
                        } else {
                            Log.d("EditLocationActivity", "- No focus in DB for ${file.name}")
                        }
                    } catch (e: Exception) {
                        Log.e("EditLocationActivity", "Error loading focus from database: ${e.message}", e)
                    }
                }
            }

            // Now add photos to UI after focus is loaded
            existingPhotos.forEach { file ->
                Log.d("EditLocationActivity", "  - ${file.absolutePath}")
                photoUris.add(file.absolutePath)
                addPhotoThumbnail(file)
            }
        }
    }

    private fun findNextImageIndex(id: String, isTemporary: Boolean): Int {
        val existingImages = if (isTemporary) {
            ImageUtils.getTempImageDirectory(this, id).listFiles()?.filter { it.extension == "jpg" }?.sortedBy { it.name } ?: emptyList()
        } else {
            ImageUtils.getLocationImages(this, id)
        }

        Log.d("EditLocationActivity", "findNextImageIndex for $id (isTemporary=$isTemporary): found ${existingImages.size} existing images")
        existingImages.forEach { img ->
            Log.d("EditLocationActivity", "    existing: ${img.name}")
        }
        if (existingImages.isEmpty()) {
            Log.d("EditLocationActivity", "  -> returning index 0 (no existing images)")
            return 0
        }
        // Find the highest index and return next one
        val highestIndex = existingImages.mapNotNull { file ->
            file.name.removePrefix("image_").removeSuffix(".jpg").toIntOrNull()
        }.maxOrNull() ?: -1
        Log.d("EditLocationActivity", "  -> highestIndex: $highestIndex, returning: ${highestIndex + 1}")
        return highestIndex + 1
    }

    private fun analyzePhotosWithAI() {
        val apiKey = tokenManager.getOpenRouterApiKey()
        if (apiKey.isNullOrBlank()) {
            Toast.makeText(this, "API key not configured", Toast.LENGTH_SHORT).show()
            return
        }

        if (photoUris.isEmpty()) {
            Toast.makeText(this, "No photos to analyze", Toast.LENGTH_SHORT).show()
            return
        }

        // Show progress dialog
        val progressDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Analyzing Photos")
            .setMessage("AI is analyzing ${photoUris.size} photo(s)...\nThis may take a moment.")
            .setCancelable(false)
            .create()
        progressDialog.show()

        lifecycleScope.launch {
            try {
                val photoFiles = photoUris.map { File(it) }
                val focusAreasMap = mutableMapOf<String, android.graphics.RectF>()
                for (path in photoUris) {
                    val photoEntity = photoRepository.getPhotoByFilePath(path)
                    photoEntity?.focusRectString?.let { focusRectString ->
                        com.a42r.pevplugshare.util.PhotoUtils.parseFocusRectString(focusRectString)?.let { rectF ->
                            focusAreasMap[path] = rectF
                        }
                    }
                }
                Log.d("EditLocationActivity", "Starting AI photo analysis request for ${photoUris.size} photos")
                val result = photoAnalyzer.analyzePhotos(photoFiles, apiKey, focusAreasMap)

                progressDialog.dismiss()

                result.fold(
                    onSuccess = { analysis ->
                        Log.d("EditLocationActivity", "AI photo analysis request successful, received analysis result")
                        showAnalysisResults(analysis)
                        applyAnalysisToForm(analysis)
                    },
                    onFailure = { error ->
                        Log.d("EditLocationActivity", "AI photo analysis request failed: ${error.message}")
                        Toast.makeText(
                            this@EditLocationActivity,
                            "Analysis failed: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } catch (e: Exception) {
                progressDialog.dismiss()
                Log.d("EditLocationActivity", "AI photo analysis request error: ${e.message}")
                Toast.makeText(
                    this@EditLocationActivity,
                    "Analysis error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showAnalysisResults(analysis: com.a42r.pevplugshare.ai.PhotoAnalysisResult) {
        val message = buildString {
            append("📸 Photo Analysis Results:\n\n")

            analysis.socketCount?.let {
                append("🔌 Socket Count: $it\n")
            }

            if (analysis.socketTypes.isNotEmpty()) {
                append("\n🔋 Socket Types:\n")
                analysis.socketTypes.forEach { type ->
                    append("  • $type\n")
                }
            }

            analysis.locationDescription?.let {
                append("\n📍 Location:\n$it\n")
            }

            analysis.powerRating?.let {
                append("\n⚡ Power Rating: $it\n")
            }

            if (analysis.warnings.isNotEmpty()) {
                append("\n⚠️ Notes:\n")
                analysis.warnings.forEach { warning ->
                    append("  • $warning\n")
                }
            }

            append("\nApply to form?")
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("AI Analysis Complete")
            .setMessage(message)
            .setPositiveButton("Apply") { _, _ ->
                // Already applied
                Toast.makeText(this, "Analysis applied to form", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun applyAnalysisToForm(analysis: com.a42r.pevplugshare.ai.PhotoAnalysisResult) {
        binding.apply {
            // Update socket count
            analysis.socketCount?.let { count ->
                when {
                    count <= 1 -> selectSocketCount(1)
                    count == 2 -> selectSocketCount(2)
                    count in 3..4 -> selectSocketCount(4)
                    count in 5..10 -> selectSocketCount(10)
                    else -> selectSocketCount(11) // 10+
                }
            }

            // Handle location description - use for main description if empty, otherwise additional info
            analysis.locationDescription?.let { location ->
                if (descriptionInput.text.toString().isBlank()) {
                    // Use for main description if it's empty
                    descriptionInput.setText(location)
                    viewModel.updateDescription(location)
                } else {
                    // Append to additional info if main description already exists
                    val existing = additionalInfoInput.text.toString()
                    val updated = if (existing.isBlank()) {
                        "Location: $location"
                    } else {
                        "$existing\n\nLocation: $location"
                    }
                    additionalInfoInput.setText(updated)
                    viewModel.updateAdditionalInfo(updated)
                }
            }

            // Append power rating and warnings to additional info
            val additionalNotes = buildString {
                analysis.powerRating?.let {
                    if (isNotBlank()) append("\n")
                    append("Power: $it")
                }
                if (analysis.warnings.isNotEmpty()) {
                    if (isNotBlank()) append("\n")
                    append("Notes: ${analysis.warnings.joinToString(", ")}")
                }
            }

            if (additionalNotes.isNotBlank()) {
                val existing = additionalInfoInput.text.toString()
                val updated = if (existing.isBlank()) {
                    additionalNotes.trim()
                } else {
                    "$existing\n$additionalNotes"
                }
                additionalInfoInput.setText(updated)
                viewModel.updateAdditionalInfo(updated)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        // Start listening to compass sensor in add mode
        if (isAddMode) {
            rotationSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
        // Stop listening to compass sensor
        if (isAddMode) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, it.values)

                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)

                // Convert radians to degrees
                currentAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this use case
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!submittedSuccessfully) {
            // If the activity is destroyed without successful submission, delete temporary images
            currentEditLocationId?.let { sessionId ->
                ImageUtils.deleteTempImages(this, sessionId)
                Log.d("EditLocationActivity", "Deleted temporary images for session: $sessionId")
            }
        }
    }
}
