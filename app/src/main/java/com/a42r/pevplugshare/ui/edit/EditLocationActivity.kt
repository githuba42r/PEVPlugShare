package com.a42r.pevplugshare.ui.edit

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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

    @Inject
    lateinit var descriptionGenerator: com.a42r.pevplugshare.ai.LocationDescriptionGenerator

    @Inject
    lateinit var savedForLaterRepository: com.a42r.pevplugshare.domain.repository.SavedForLaterRepository

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
    private var savedForLaterId: String? = null  // ID of saved for later entry to delete on success
    private var isProcessingQuickAddPhotos = false  // Flag to track if we're processing photos from QuickLocationActivity
    private var waitingForSyncVerification = false  // Flag to prevent auto-close when showing verification dialog
    private var pendingSyncWithVerification = false  // Flag to trigger sync with verification after DB update
    private var pendingSyncWithoutVerification = false  // Flag to trigger sync without verification after DB update
    private var isInitializingUI = false  // Flag to prevent spinner listeners from firing during initial setup

    private val syncResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Security: Only process if we're actually waiting for a sync result
            if (!waitingForSyncVerification) {
                Log.w("EditLocationActivity", "Ignoring sync result - not waiting for sync")
                return
            }

            // Validate intent and data
            if (intent == null) {
                Log.w("EditLocationActivity", "Received null intent in sync result")
                return
            }

            // Validate the intent action matches our expected action
            if (intent.action != com.a42r.pevplugshare.services.OsmandBackgroundSyncService.ACTION_SYNC_RESULT) {
                Log.w("EditLocationActivity", "Ignoring intent with wrong action: ${intent.action}")
                return
            }

            // Extract and validate data
            if (!intent.hasExtra(com.a42r.pevplugshare.services.OsmandBackgroundSyncService.EXTRA_SYNC_SUCCESS)) {
                Log.w("EditLocationActivity", "Sync result missing success flag")
                return
            }

            val success = intent.getBooleanExtra(com.a42r.pevplugshare.services.OsmandBackgroundSyncService.EXTRA_SYNC_SUCCESS, false)
            val message = intent.getStringExtra(com.a42r.pevplugshare.services.OsmandBackgroundSyncService.EXTRA_SYNC_MESSAGE)

            // Validate message is not empty
            if (message.isNullOrBlank()) {
                Log.w("EditLocationActivity", "Sync result missing or empty message")
                return
            }

            Log.d("EditLocationActivity", "Valid sync result received: success=$success, message=$message")

            Toast.makeText(this@EditLocationActivity, message, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()

            // Clear waiting flag and close activity
            waitingForSyncVerification = false
            setResult(RESULT_OK)
            finish()
        }
    }

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

        // Register broadcast receiver for sync results
        LocalBroadcastManager.getInstance(this).registerReceiver(
            syncResultReceiver,
            IntentFilter(com.a42r.pevplugshare.services.OsmandBackgroundSyncService.ACTION_SYNC_RESULT)
        )

        // Check if we'll be processing QuickAdd photos - show overlay IMMEDIATELY
        savedForLaterId = intent.getStringExtra("saved_for_later_id")
        val incomingPhotoPaths = intent.getStringArrayListExtra("photo_paths")
        Log.d("EditLocationActivity", "onCreate: isAddMode=$isAddMode, incomingPhotoPaths=${incomingPhotoPaths?.size}, savedForLaterId=$savedForLaterId")
        if (isAddMode && incomingPhotoPaths != null && savedForLaterId == null) {
            // We'll be processing photos from QuickLocationActivity
            isProcessingQuickAddPhotos = true
            binding.photoLoadingOverlay.visibility = android.view.View.VISIBLE
            binding.formContainer.visibility = android.view.View.GONE
            Log.d("EditLocationActivity", "SHOWING LOADING OVERLAY - overlay visible, form hidden")
        }

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isAddMode) "Add Location" else "Edit Location"
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize sensor manager for compass
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        setupUI()
        setupPhotoSection()
        setupObservers()

        // Retrieve photo paths from intent and process them
        incomingPhotoPaths?.let {
            // In add mode, check if these are cache paths that need processing
            if (isAddMode) {
                // Check if coming from saved location - if so, use photos directly without processing
                // This prevents deleting the original cache files from saved location
                if (savedForLaterId != null) {
                    Log.d("EditLocationActivity", "Loading photos from saved location - using original paths")
                    // Validate and filter photos to ensure they exist
                    val validPhotos = it.filter { path ->
                        val file = File(path)
                        val exists = file.exists()
                        if (!exists) {
                            Log.e("EditLocationActivity", "Skipping non-existent saved photo: $path")
                        }
                        exists
                    }

                    if (validPhotos.isEmpty() && it.isNotEmpty()) {
                        Log.e("EditLocationActivity", "All ${it.size} saved photos are invalid/missing!")
                        Toast.makeText(this, "Warning: Saved photos not found", Toast.LENGTH_LONG).show()
                    } else if (validPhotos.size < it.size) {
                        Log.w("EditLocationActivity", "${it.size - validPhotos.size} saved photos were invalid/missing")
                        Toast.makeText(this, "${validPhotos.size}/${it.size} photos loaded", Toast.LENGTH_SHORT).show()
                    }

                    // Use photos directly without processing - they'll be moved to permanent storage on submit
                    photoUris.addAll(validPhotos)
                    refreshPhotoThumbnails()
                    viewModel.updatePhotoPaths(photoUris)

                    // Trigger auto-analysis for saved locations too
                    autoAnalyzePhotosIfReady()

                    // Don't set currentEditLocationId yet - it will be generated on submit
                    // This ensures we don't create temp folders unnecessarily
                } else {
                    // If photos are from QuickLocationActivity workflow (not saved location), process them now
                    // Process them now before adding to photoUris
                    // Note: currentEditLocationId will be set after this block, so we generate temp ID now
                    // and will use it throughout add mode for consistency
                    if (currentEditLocationId == null) {
                        currentEditLocationId = UUID.randomUUID().toString()
                    }

                    // Loading overlay was already shown before setupObservers()
                    lifecycleScope.launch {
                        val processedPaths = withContext(Dispatchers.IO) {
                            processCachePhotos(it, currentEditLocationId!!)
                        }

                        photoUris.addAll(processedPaths)
                        refreshPhotoThumbnails()
                        viewModel.updatePhotoPaths(photoUris)

                        // Trigger auto-analysis after photos are loaded
                        autoAnalyzePhotosIfReady()

                        // Hide loading overlay after processing
                        isProcessingQuickAddPhotos = false
                        binding.photoLoadingOverlay.visibility = android.view.View.GONE
                        binding.formContainer.visibility = android.view.View.VISIBLE
                    }
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
                // Automatically perform reverse geocoding to set location name
                autoPopulateLocationName(intentLatitude, intentLongitude)
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
                    // Automatically perform reverse geocoding to set location name
                    autoPopulateLocationName(it.latitude, it.longitude)
                }
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
            buttonCancel.setOnClickListener { handleCancel() }
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
            buttonParseDescription.visibility = if (hasApiKey) android.view.View.VISIBLE else android.view.View.GONE

            buttonParseDescription.setOnClickListener { parseDescriptionToFields() }
            // Parse button visible in both add and edit modes

            // Reverse geocoding button
            reverseGeocodeButton.setOnClickListener { performReverseGeocoding() }

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

            // Add text watchers for all other input fields
            businessNameInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    viewModel.updateBusinessName(s?.toString() ?: "")
                }
            })

            streetAddressInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    viewModel.updateStreetAddress(s?.toString() ?: "")
                }
            })

            suburbInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    viewModel.updateSuburb(s?.toString() ?: "")
                }
            })

            stateInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    viewModel.updateState(s?.toString() ?: "")
                }
            })

            postcodeInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    viewModel.updatePostcode(s?.toString() ?: "")
                }
            })

            descriptionInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    viewModel.updateDescription(s?.toString() ?: "")
                }
            })

            additionalInfoInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    viewModel.updateAdditionalInfo(s?.toString() ?: "")
                }
            })

            providerNameInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    viewModel.updateProviderName(s?.toString() ?: "")
                }
            })

            chargingCostsInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    viewModel.updateChargingCosts(s?.toString() ?: "")
                }
            })

            providerWebsiteInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    viewModel.updateProviderWebsite(s?.toString() ?: "")
                }
            })

            // Setup checkbox listener
            feeCheckbox.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateHasFee(isChecked)
                feeDetailsSection.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
                updateFeeIndicator(isChecked)
            }

            // Setup opening hours spinner listener
            openingHoursSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    if (isInitializingUI) {
                        Log.d("EditLocationActivity", "openingHoursSpinner.onItemSelected: Skipping (initializing UI)")
                        return
                    }
                    val selectedHours = parent?.getItemAtPosition(position).toString()
                    Log.d("EditLocationActivity", "openingHoursSpinner.onItemSelected: $selectedHours")
                    viewModel.updateOpeningHours(selectedHours)
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

            // Setup socket type spinner listener
            socketTypeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    if (isInitializingUI) {
                        Log.d("EditLocationActivity", "socketTypeSpinner.onItemSelected: Skipping (initializing UI)")
                        return
                    }
                    val selectedType = parent?.getItemAtPosition(position).toString()
                    Log.d("EditLocationActivity", "socketTypeSpinner.onItemSelected: $selectedType")
                    viewModel.updateSocketTypes(selectedType)
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

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

            // NOTE: Socket type and socket count are independent properties
            // Do NOT automatically change socket count when socket type changes
            // The user should independently select both the type and count
            // (Previously this listener was overwriting the socket count, causing data loss)

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
                        // Additional Location Info toggle logic
                        var additionalInfoVisible = false
                        additionalLocationInfoSection.visibility = android.view.View.GONE
                        reverseGeocodeButton.visibility = android.view.View.GONE
                        toggleAdditionalLocationInfoButton.setImageResource(R.drawable.ic_expand_more)
                        toggleAdditionalLocationInfoButton.setOnClickListener {
                            additionalInfoVisible = !additionalInfoVisible
                            if (additionalInfoVisible) {
                                additionalLocationInfoSection.visibility = android.view.View.VISIBLE
                                reverseGeocodeButton.visibility = android.view.View.VISIBLE
                                toggleAdditionalLocationInfoButton.setImageResource(R.drawable.ic_expand_less)
                            } else {
                                additionalLocationInfoSection.visibility = android.view.View.GONE
                                reverseGeocodeButton.visibility = android.view.View.GONE
                                toggleAdditionalLocationInfoButton.setImageResource(R.drawable.ic_expand_more)
                            }
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



    private fun showMapModal() {
        // Use a DialogFragment for proper modal behavior that blocks all background interaction
        val currentLat = viewModel.formData.value.latitude
        val currentLon = viewModel.formData.value.longitude

        val mapDialog = MapLocationPickerDialogFragment.newInstance(currentLat, currentLon).apply {
            setOnLocationSelectedListener { latitude, longitude ->
                viewModel.updateLocation(latitude, longitude)
                Toast.makeText(this@EditLocationActivity, "Location updated", Toast.LENGTH_SHORT).show()
            }
        }
        mapDialog.show(supportFragmentManager, "map_picker")
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
                        Log.d("EditLocationActivity", "State.Ready: isProcessingQuickAddPhotos=$isProcessingQuickAddPhotos")
                        binding.progressBar.visibility = android.view.View.GONE

                        // Don't show form if we're still processing QuickAdd photos
                        if (!isProcessingQuickAddPhotos) {
                            binding.formContainer.visibility = android.view.View.VISIBLE
                            Log.d("EditLocationActivity", "State.Ready: Showing form (not processing photos)")
                        } else {
                            Log.d("EditLocationActivity", "State.Ready: Keeping form hidden (processing photos)")
                        }

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

                        // Always show sync button in edit mode (not add mode)
                        binding.buttonSyncToOsmand.visibility = android.view.View.VISIBLE

                        // Initialize sync button state (should be enabled initially with no changes)
                        updateSyncButtonState()
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

                        // Delete the saved for later entry if this was from a saved location
                        savedForLaterId?.let { id ->
                            lifecycleScope.launch {
                                try {
                                    savedForLaterRepository.deleteSavedForLater(id)
                                    Log.d("EditLocationActivity", "Deleted saved for later entry: $id")
                                } catch (e: Exception) {
                                    Log.e("EditLocationActivity", "Error deleting saved for later entry", e)
                                }
                            }
                        }

                        // Check if we need to perform OsmAnd sync after update
                        val locId = locationId
                        val shouldSync = pendingSyncWithVerification || pendingSyncWithoutVerification

                        if (pendingSyncWithVerification && locId != null) {
                            // Clear flag and trigger sync with verification
                            pendingSyncWithVerification = false
                            Log.d("EditLocationActivity", "━━━ UpdateSuccess: Starting sync WITH verification ━━━")
                            Log.d("EditLocationActivity", "Location ID: $locId")
                            showDescriptionVerificationAndSync(locId)
                        } else if (pendingSyncWithoutVerification && locId != null) {
                            // Clear flag and trigger sync without verification
                            pendingSyncWithoutVerification = false
                            Log.d("EditLocationActivity", "━━━ UpdateSuccess: Starting sync WITHOUT verification ━━━")
                            Log.d("EditLocationActivity", "Location ID: $locId")
                            generateAndSyncInBackground(locId)
                        }

                        // Only finish if NOT syncing AND not waiting for sync verification
                        if (!shouldSync && !waitingForSyncVerification) {
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            // Re-enable sync button after successful update (no pending changes now)
                            updateSyncButtonState()
                        }
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
                        // Re-enable sync button based on current state
                        updateSyncButtonState()
                        Toast.makeText(this@EditLocationActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.formData.collect { data ->
                updateUIFromFormData(data)

                // Update Sync to OsmAnd button state based on pending changes
                updateSyncButtonState()
            }
        }
    }

    private fun populateFormFromLocation(location: com.a42r.pevplugshare.data.model.Location) {
        isInitializingUI = true  // Prevent spinner listeners from firing
        
        binding.apply {
            nameInput.setText(location.name)
            businessNameInput.setText(location.businessName ?: "")
            streetAddressInput.setText(location.streetAddress ?: "")
            suburbInput.setText(location.suburb ?: "")
            stateInput.setText(location.state ?: "")
            postcodeInput.setText(location.postcode ?: "")
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

            // Now set the actual socket count from the database
            selectSocketCount(location.gpoCount)

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
        viewModel.updateBusinessName(location.businessName ?: "")
        viewModel.updateStreetAddress(location.streetAddress ?: "")
        viewModel.updateSuburb(location.suburb ?: "")
        viewModel.updateState(location.state ?: "")
        viewModel.updatePostcode(location.postcode ?: "")
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
        
        isInitializingUI = false  // Re-enable spinner listeners
    }

    private fun initializeUIForAddMode() {
        isInitializingUI = true  // Prevent spinner listeners from firing
        
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
        
        isInitializingUI = false  // Re-enable spinner listeners

        // Automatically trigger AI photo analysis if conditions are met
        // This handles the case where photos are already loaded synchronously (from saved location)
        autoAnalyzePhotosIfReady()
    }

    private fun autoAnalyzePhotosIfReady() {
        // Check if all conditions are met for automatic AI analysis
        val hasPhotos = photoUris.isNotEmpty()
        val descriptionEmpty = binding.descriptionInput.text.toString().isBlank()
        val hasApiKey = tokenManager.getOpenRouterApiKey()?.isNotBlank() == true
        val hasValidModel = tokenManager.getSelectedAiModel()?.isNotBlank() == true

        Log.d("EditLocationActivity", "Auto-analyze check: hasPhotos=$hasPhotos, descriptionEmpty=$descriptionEmpty, hasApiKey=$hasApiKey, hasValidModel=$hasValidModel")

        if (isAddMode && hasPhotos && descriptionEmpty && hasApiKey && hasValidModel) {
            Log.d("EditLocationActivity", "Auto-triggering AI photo analysis for ${photoUris.size} photos")
            // Post this to the message queue to ensure UI is fully initialized
            binding.root.post {
                analyzePhotosWithAI(autoTriggered = true)
            }
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
        Log.d("EditLocationActivity", "selectSocketCount($count) called")
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

            Log.d("EditLocationActivity", "  Calling viewModel.updateGpoCount($count)...")
            viewModel.updateGpoCount(count)
            Log.d("EditLocationActivity", "  ✓ selectSocketCount($count) completed")
        }
    }

    private fun updateUIFromFormData(data: EditLocationFormData) {
        Log.d("EditLocationActivity", "Updating UI with form data (preserving focused fields)")
        binding.apply {
            // Only update text fields if they don't have focus (not being edited by user)
            if (!nameInput.hasFocus()) nameInput.setText(data.name)
            if (!businessNameInput.hasFocus()) businessNameInput.setText(data.businessName)
            if (!streetAddressInput.hasFocus()) streetAddressInput.setText(data.streetAddress)
            if (!suburbInput.hasFocus()) suburbInput.setText(data.suburb)
            if (!stateInput.hasFocus()) stateInput.setText(data.state)
            if (!postcodeInput.hasFocus()) postcodeInput.setText(data.postcode)
            if (!descriptionInput.hasFocus()) descriptionInput.setText(data.description)
            if (!additionalInfoInput.hasFocus()) additionalInfoInput.setText(data.additionalInfo)
            if (!providerNameInput.hasFocus()) providerNameInput.setText(data.providerName)
            if (!chargingCostsInput.hasFocus()) chargingCostsInput.setText(data.chargingCosts)
            if (!providerWebsiteInput.hasFocus()) providerWebsiteInput.setText(data.providerWebsite)

            // Update checkbox (always safe to update)
            feeCheckbox.isChecked = data.hasFee
            feeDetailsSection.visibility = if (data.hasFee) android.view.View.VISIBLE else android.view.View.GONE

            // NOTE: Do NOT call selectSocketCount here - it causes a feedback loop
            // where the button click calls updateGpoCount -> updates formData ->
            // calls updateUIFromFormData -> calls selectSocketCount again,
            // which can overwrite the user's selection with stale data
        }
    }

    private fun updateSyncButtonState() {
        // Only update sync button state in edit mode (not add mode)
        if (isAddMode) {
            Log.d("EditLocationActivity", "updateSyncButtonState: Skipping (add mode)")
            return
        }

        val hasPendingChanges = viewModel.hasChanges()

        binding.buttonSyncToOsmand.isEnabled = !hasPendingChanges

        // Update visual appearance - gray out when disabled
        binding.buttonSyncToOsmand.alpha = if (hasPendingChanges) 0.5f else 1.0f

        Log.d("EditLocationActivity", "updateSyncButtonState: hasPendingChanges=$hasPendingChanges, isEnabled=${!hasPendingChanges}, alpha=${if (hasPendingChanges) 0.5f else 1.0f}")
    }

    private fun submitUpdates() {
        if (photosBeingProcessed > 0) {
            Toast.makeText(this, "Please wait for photos to finish processing", Toast.LENGTH_SHORT).show()
            return
        }

        // In edit mode, prompt for OsmAnd sync if enabled
        if (!isAddMode && tokenManager.getSyncToOsmandOnUpdate()) {
            showOsmandSyncPrompt()
            return
        }

        // Continue with normal submit
        performActualSubmit(syncToOsmand = false, verifyDescription = false)
    }

    private fun showOsmandSyncPrompt() {
        val dialogView = layoutInflater.inflate(com.a42r.pevplugshare.R.layout.dialog_osmand_sync_prompt, null)

        val checkboxVerify = dialogView.findViewById<android.widget.CheckBox>(com.a42r.pevplugshare.R.id.checkboxVerifyDescription)
        val buttonCancel = dialogView.findViewById<android.widget.Button>(com.a42r.pevplugshare.R.id.buttonCancel)
        val buttonSync = dialogView.findViewById<android.widget.Button>(com.a42r.pevplugshare.R.id.buttonSyncToOsmand)

        // Set initial state from saved preferences
        checkboxVerify.isChecked = tokenManager.getVerifyDescriptionBeforeSync()

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Sync to OsmAnd?")
            .setView(dialogView)
            .setCancelable(true)
            .create()

        buttonCancel.setOnClickListener {
            // Save verify preference state
            tokenManager.setVerifyDescriptionBeforeSync(checkboxVerify.isChecked)

            dialog.dismiss()
            // Continue with update without syncing
            performActualSubmit(syncToOsmand = false, verifyDescription = false)
        }

        buttonSync.setOnClickListener {
            val verifyDescription = checkboxVerify.isChecked

            // Save verify preference state
            tokenManager.setVerifyDescriptionBeforeSync(verifyDescription)

            dialog.dismiss()
            // Continue with update and sync to OsmAnd
            performActualSubmit(syncToOsmand = true, verifyDescription = verifyDescription)
        }

        dialog.show()
    }

    private fun performActualSubmit(syncToOsmand: Boolean, verifyDescription: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            Log.d("EditLocationActivity", "")
            Log.d("EditLocationActivity", "💾 SUBMITTING LOCATION UPDATE")

            val finalLocationId = if (isAddMode) currentEditLocationId ?: UUID.randomUUID().toString() else locationId!!
            Log.d("EditLocationActivity", "   Final location ID: $finalLocationId")
            Log.d("EditLocationActivity", "   Is add mode: $isAddMode")
            Log.d("EditLocationActivity", "   Current photoUris: $photoUris")

            // Move temporary images to permanent storage
            val newPermanentPaths: List<String> = if (isAddMode) {
                // Check if photos are from saved location (in cache) or already processed (in temp)
                val hasProcessedPhotos = currentEditLocationId != null && photoUris.any { it.contains("/temp_") }

                if (hasProcessedPhotos) {
                    // Photos were already processed into temp storage (from QuickLocationActivity workflow)
                    // Move from temp directory structure
                    ImageUtils.moveTempImagesToPermanent(
                        this@EditLocationActivity,
                        currentEditLocationId ?: "temp_fallback",
                        finalLocationId
                    )
                } else {
                    // Photos are from saved location (still in cache) - need to process and move
                    Log.d("EditLocationActivity", "Processing cache photos from saved location")
                    val processedPaths = mutableListOf<String>()

                    photoUris.forEachIndexed { index, cachePath ->
                        try {
                            val cacheFile = File(cachePath)
                            if (cacheFile.exists()) {
                                // Resize and save to permanent storage
                                val resizedFile = ImageUtils.resizeAndSaveImage(
                                    this@EditLocationActivity,
                                    cacheFile,
                                    finalLocationId,
                                    index,
                                    isTemporary = false // Save directly to permanent
                                )

                                resizedFile?.let {
                                    processedPaths.add(it.absolutePath)
                                    Log.d("EditLocationActivity", "   ✓ Processed cache photo to permanent: ${it.absolutePath}")
                                    // Don't delete cache file - it belongs to saved location
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("EditLocationActivity", "Error processing cache photo $index", e)
                        }
                    }
                    processedPaths
                }
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
                // Sync all current UI field values to ViewModel before submitting
                Log.d("EditLocationActivity", "")
                Log.d("EditLocationActivity", "📝 SYNCING UI FIELDS TO VIEWMODEL BEFORE SUBMIT")
                Log.d("EditLocationActivity", "  Description from UI: '${binding.descriptionInput.text.toString()}'")
                Log.d("EditLocationActivity", "  Additional Info from UI: '${binding.additionalInfoInput.text.toString()}'")

                viewModel.updateName(binding.nameInput.text.toString())
                viewModel.updateBusinessName(binding.businessNameInput.text.toString())
                viewModel.updateStreetAddress(binding.streetAddressInput.text.toString())
                viewModel.updateSuburb(binding.suburbInput.text.toString())
                viewModel.updateState(binding.stateInput.text.toString())
                viewModel.updatePostcode(binding.postcodeInput.text.toString())
                viewModel.updateDescription(binding.descriptionInput.text.toString())
                viewModel.updateAdditionalInfo(binding.additionalInfoInput.text.toString())
                viewModel.updateProviderName(binding.providerNameInput.text.toString())
                viewModel.updateChargingCosts(binding.chargingCostsInput.text.toString())
                viewModel.updateProviderWebsite(binding.providerWebsiteInput.text.toString())

                Log.d("EditLocationActivity", "✓ UI fields synced to ViewModel")

                // Sync spinner and checkbox selections to ensure they're captured
                val selectedSocketType = binding.socketTypeSpinner.selectedItem?.toString() ?: "AU GPO (AS3112)"
                val socketTypeEnumValue = when {
                    selectedSocketType.contains("Type 2") -> "TYPE2"
                    selectedSocketType.contains("Tesla") -> "TESLA"
                    selectedSocketType.contains("CCS") -> "CCS2"
                    selectedSocketType.contains("CHAdeMO") -> "CHADEMO"
                    selectedSocketType.contains("Type 1") -> "TYPE1"
                    selectedSocketType.contains("Wall") -> "WALL_SOCKET"
                    selectedSocketType.contains("Caravan") -> "CARAVAN"
                    selectedSocketType.contains("Other") -> "OTHER"
                    else -> "AS3112"
                }
                viewModel.updateSocketTypes(socketTypeEnumValue)

                val selectedHours = binding.openingHoursSpinner.selectedItem?.toString() ?: "24/7"
                viewModel.updateOpeningHours(selectedHours)

                viewModel.updateHasFee(binding.feeCheckbox.isChecked)

                // Extract GPO count from ViewModel (already updated by selectSocketCount calls)
                val gpoCount = viewModel.formData.value.gpoCount
                Log.d("EditLocationActivity", "━━━ CRITICAL CHECK BEFORE SUBMIT ━━━")
                Log.d("EditLocationActivity", "   Current formData.gpoCount: $gpoCount")
                Log.d("EditLocationActivity", "   About to call submitUpdates() with this gpoCount")
                viewModel.updateGpoCount(gpoCount)

                val isPublic = binding.accessTypeText.text == "Public"
                viewModel.updateAccessType(if (isPublic) "public" else "private")

                val isFavorite = binding.favoriteIndicator.tag == "favorite"
                viewModel.updateFavorite(isFavorite)

                // Update locationId for all photos associated with this session
                if (isAddMode && currentEditLocationId != null) {
                    photoRepository.updatePhotoLocationId(currentEditLocationId!!, finalLocationId)
                }

                if (isAddMode) {
                    viewModel.addLocation(finalLocationId)
                    submittedSuccessfully = true
                } else {
                    // Handle OsmAnd sync if requested - set flags to trigger after DB update
                    if (syncToOsmand && locationId != null) {
                        // Set flag to prevent auto-close for ANY sync operation
                        waitingForSyncVerification = true

                        if (verifyDescription) {
                            // Set flag to trigger sync with verification after DB update completes
                            pendingSyncWithVerification = true
                            Log.d("EditLocationActivity", "Set pendingSyncWithVerification=true, will sync after DB update")
                        } else {
                            // Set flag to trigger sync without verification after DB update completes
                            pendingSyncWithoutVerification = true
                            Log.d("EditLocationActivity", "Set pendingSyncWithoutVerification=true, will sync after DB update")
                        }
                    }

                    // Submit the database update - sync will be triggered in UpdateSuccess handler
                    viewModel.submitUpdates()
                }
            }
        }
    }

    private fun showDescriptionVerificationAndSync(locationId: String) {
        // ALWAYS generate a fresh description for OsmAnd sync
        // The local DB description (from the form) has already been saved
        // The generated description is ONLY for OsmAnd, not saved to local DB
        generateDescriptionForVerification(locationId)
    }

    private fun generateDescriptionForVerification(locationId: String) {
        // Show loading modal
        showDescriptionGenerationModal()

        lifecycleScope.launch {
            val result = viewModel.generateDescriptionFromLocation(locationId)

            result.onSuccess { generatedDescription ->
                withContext(Dispatchers.Main) {
                    hideDescriptionGenerationModal()
                    showGeneratedDescriptionConfirmation(locationId, generatedDescription)
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    hideDescriptionGenerationModal()
                    Toast.makeText(this@EditLocationActivity, "Failed to generate description: ${error.message}", Toast.LENGTH_SHORT).show()
                    submittedSuccessfully = true
                    finish()
                }
            }
        }
    }

    private fun showGeneratedDescriptionConfirmation(locationId: String, generatedDescription: String) {
        val dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_1, null)
        val textView = dialogView.findViewById<TextView>(android.R.id.text1)
        textView.text = "Generated description for OsmAnd:\n\n$generatedDescription"
        textView.textSize = 16f
        textView.setPadding(48, 48, 48, 48)

        android.app.AlertDialog.Builder(this)
            .setTitle("Verify Generated Description")
            .setView(dialogView)
            .setCancelable(false) // Force user to make a choice
            .setPositiveButton("Sync", android.content.DialogInterface.OnClickListener { _, _ ->
                // Start sync in background and wait for result
                performSyncAndWaitForResult(locationId, generatedDescription)
            })
            .setNegativeButton("Cancel", android.content.DialogInterface.OnClickListener { _, _ ->
                // Close activity without syncing
                waitingForSyncVerification = false
                setResult(RESULT_OK)
                finish()
            })
            .show()
    }

    private fun performSyncAndWaitForResult(locationId: String, description: String) {
        // Show progress
        Toast.makeText(this, "Syncing to OsmAnd...", Toast.LENGTH_SHORT).show()

        Log.d("EditLocationActivity", "━━━ performSyncAndWaitForResult CALLED ━━━")
        Log.d("EditLocationActivity", "Location ID: $locationId")
        Log.d("EditLocationActivity", "Description length: ${description.length}")

        // Start the background service to perform sync
        val syncIntent = Intent(this, com.a42r.pevplugshare.services.OsmandBackgroundSyncService::class.java).apply {
            putExtra(com.a42r.pevplugshare.services.OsmandBackgroundSyncService.EXTRA_LOCATION_ID, locationId)
            putExtra(com.a42r.pevplugshare.services.OsmandBackgroundSyncService.EXTRA_DESCRIPTION, description)
            putExtra(com.a42r.pevplugshare.services.OsmandBackgroundSyncService.EXTRA_GENERATE_DESCRIPTION, false)
        }
        startService(syncIntent)

        Log.d("EditLocationActivity", "Started OsmandBackgroundSyncService with provided description")
    }

    private fun generateAndSyncInBackground(locationId: String) {
        // Let the background service generate and sync
        Toast.makeText(this, "Generating description and syncing to OsmAnd...", Toast.LENGTH_SHORT).show()

        Log.d("EditLocationActivity", "━━━ generateAndSyncInBackground CALLED ━━━")
        Log.d("EditLocationActivity", "Location ID: $locationId")

        // Start the background service - let it generate the description from DB
        val syncIntent = Intent(this, com.a42r.pevplugshare.services.OsmandBackgroundSyncService::class.java).apply {
            putExtra(com.a42r.pevplugshare.services.OsmandBackgroundSyncService.EXTRA_LOCATION_ID, locationId)
            putExtra(com.a42r.pevplugshare.services.OsmandBackgroundSyncService.EXTRA_GENERATE_DESCRIPTION, true)
        }
        startService(syncIntent)

        Log.d("EditLocationActivity", "Started OsmandBackgroundSyncService with auto-generation")
    }

    private suspend fun generateDescriptionFromDB(locationId: String): String? {
        val result = viewModel.generateDescriptionFromLocation(locationId)
        return result.getOrNull()
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
        // Check for pending changes
        if (viewModel.hasChanges()) {
            Toast.makeText(this, "Please save changes before syncing to OsmAnd", Toast.LENGTH_LONG).show()
            return
        }

        // First generate AI description
        generateDescriptionForSync()
    }

    private fun generateDescriptionForSync() {
        val locId = locationId
        if (locId == null) {
            Toast.makeText(this, "Location ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading modal
        showDescriptionGenerationModal()

        lifecycleScope.launch {
            val result = viewModel.generateDescriptionFromLocation(locId)

            result.onSuccess { generatedDescription ->
                withContext(Dispatchers.Main) {
                    hideDescriptionGenerationModal()
                    showSyncDescriptionModal(generatedDescription)
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    hideDescriptionGenerationModal()
                    Toast.makeText(this@EditLocationActivity, "Failed to generate description: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private var descriptionGenerationDialog: androidx.appcompat.app.AlertDialog? = null

    private fun showDescriptionGenerationModal() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_loading, null)
        val messageText = dialogView.findViewById<TextView>(R.id.loadingMessage)
        messageText?.text = "Generating description for OSMAnd sync..."

        descriptionGenerationDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        descriptionGenerationDialog?.show()
    }

    private fun hideDescriptionGenerationModal() {
        descriptionGenerationDialog?.dismiss()
        descriptionGenerationDialog = null
    }

    private fun showSyncDescriptionModal(generatedDescription: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Generated Description for OSMAnd Sync")
            .setMessage("Generated Description:\n\n$generatedDescription\n\nProceed with OSMAnd sync?")
            .setPositiveButton("Sync") { _, _ ->
                syncToOsmandWithDescription(generatedDescription)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun syncToOsmandWithDescription(generatedDescription: String) {
        // Sync to OSMAnd with the generated description (without modifying form fields)
        locationId?.let { viewModel.syncToOsmandWithDescription(it, generatedDescription) }
        Toast.makeText(this, "Syncing to OSMAnd with generated description", Toast.LENGTH_SHORT).show()
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

    private fun runAutoDescriptionGeneration() {
        val name = binding.nameInput.text.toString()
        if (name.isBlank()) {
            Log.d("EditLocationActivity", "Cannot run auto description generation: location name is blank")
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

        Log.d("EditLocationActivity", "Running auto description generation for location: $name")

        lifecycleScope.launch {
            val result = descriptionGenerator.generateDescription(
                locationName = "",  // Do not include location name in generated description
                accessType = accessType,
                hasFee = hasFee,
                chargingPoints = gpoCount,
                socketTypes = listOf(socketType),
                openingHours = openingHours,
                capacity = capacity,
                additionalInfo = additionalInfo,
                description = currentDescription,  // Pass the analysis-generated description
                conversationHistory = emptyList()
            )

            result.onSuccess { generatedDescription ->
                Log.d("EditLocationActivity", "Auto description generation successful: $generatedDescription")
                binding.descriptionInput.setText(generatedDescription as CharSequence)
                viewModel.updateDescription(generatedDescription)
                Toast.makeText(this@EditLocationActivity as android.content.Context, "Description auto-generated and updated", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Log.e("EditLocationActivity", "Auto description generation failed: ${error.message}")
                Toast.makeText(this@EditLocationActivity as android.content.Context, "Failed to auto-generate description: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
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

    private fun performReverseGeocoding() {
        Log.d("EditLocationActivity", "performReverseGeocoding called")
        // Get current coordinates from the form
        val latitude = viewModel.formData.value.latitude
        val longitude = viewModel.formData.value.longitude

        Log.d("EditLocationActivity", "Coordinates: lat=$latitude, lon=$longitude")

        if (latitude == 0.0 && longitude == 0.0) {
            Toast.makeText(this, "No location coordinates available", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading indicator
        binding.reverseGeocodeButton.isEnabled = false
        binding.reverseGeocodeButton.alpha = 0.5f

        // Perform geocoding in a coroutine
        lifecycleScope.launch {
            try {
                Log.d("EditLocationActivity", "Starting geocoding request")
                val result = viewModel.performReverseGeocoding(latitude, longitude)
                Log.d("EditLocationActivity", "Geocoding result: $result")
                when (result) {
                    is com.a42r.pevplugshare.data.remote.GeocodingResult.Success -> {
                        Log.d("EditLocationActivity", "Geocoding success, showing confirmation dialog")
                        showGeocodingConfirmationDialog(result)
                    }
                    is com.a42r.pevplugshare.data.remote.GeocodingResult.Error -> {
                        Log.d("EditLocationActivity", "Geocoding error: ${result.message}")
                        Toast.makeText(this@EditLocationActivity, "Geocoding failed: ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("EditLocationActivity", "Error during reverse geocoding", e)
                Toast.makeText(this@EditLocationActivity, "Error during geocoding", Toast.LENGTH_SHORT).show()
            } finally {
                // Re-enable button
                binding.reverseGeocodeButton.isEnabled = true
                binding.reverseGeocodeButton.alpha = 1.0f
            }
        }
    }

    /**
     * Automatically performs reverse geocoding and populates the location name field
     * when adding a new location. Uses displayName if available, otherwise suburb.
     */
    private fun autoPopulateLocationName(latitude: Double, longitude: Double) {
        if (!isAddMode) {
            return // Only auto-populate in add mode
        }

        Log.d("EditLocationActivity", "autoPopulateLocationName: lat=$latitude, lon=$longitude")

        lifecycleScope.launch {
            try {
                val result = viewModel.performReverseGeocoding(latitude, longitude)
                when (result) {
                    is com.a42r.pevplugshare.data.remote.GeocodingResult.Success -> {
                        // Set location name to business name first, then suburb as fallback
                        val locationName = result.businessName ?: result.suburb
                        if (!locationName.isNullOrBlank()) {
                            Log.d("EditLocationActivity", "Auto-setting location name to: $locationName")
                            viewModel.updateName(locationName)
                            binding.nameInput.setText(locationName)
                        } else {
                            Log.w("EditLocationActivity", "No businessName or suburb available from geocoding")
                        }
                    }
                    is com.a42r.pevplugshare.data.remote.GeocodingResult.Error -> {
                        Log.w("EditLocationActivity", "Auto-geocoding failed: ${result.message}")
                        // Don't show error toast for automatic geocoding - it's not critical
                    }
                }
            } catch (e: Exception) {
                Log.e("EditLocationActivity", "Error during auto-geocoding", e)
                // Don't show error toast for automatic geocoding - it's not critical
            }
        }
    }

    private fun showGeocodingConfirmationDialog(geocodingResult: com.a42r.pevplugshare.data.remote.GeocodingResult.Success) {
        Log.d("EditLocationActivity", "showGeocodingConfirmationDialog called with result: $geocodingResult")
        // Get current form values
        val currentFormData = viewModel.formData.value
        Log.d("EditLocationActivity", "Current form data: $currentFormData")

        // Build the changes message
        val changes = StringBuilder()
        changes.append("The following information will be updated:\n\n")

        // Check location name
        if (currentFormData.name.isBlank() && geocodingResult.displayName != null) {
            changes.append("📍 Location Name: \"${geocodingResult.displayName}\"\n")
        }

        // Check business name
        if (currentFormData.businessName.isBlank() && geocodingResult.displayName != null) {
            changes.append("🏢 Business Name: \"${geocodingResult.displayName}\"\n")
        }

        // Check address fields
        if (currentFormData.streetAddress.isBlank() && geocodingResult.streetAddress != null) {
            changes.append("🏠 Street Address: \"${geocodingResult.streetAddress}\"\n")
        }
        if (currentFormData.suburb.isBlank() && geocodingResult.suburb != null) {
            changes.append("🏙️ Suburb: \"${geocodingResult.suburb}\"\n")
        }
        if (currentFormData.state.isBlank() && geocodingResult.state != null) {
            changes.append("🗺️ State: \"${geocodingResult.state}\"\n")
        }
        if (currentFormData.postcode.isBlank() && geocodingResult.postcode != null) {
            changes.append("📮 Postcode: \"${geocodingResult.postcode}\"\n")
        }

        val changesMessage = changes.toString()
        Log.d("EditLocationActivity", "Changes message: $changesMessage")

        if (changesMessage == "The following information will be updated:\n\n") {
            // No changes to show
            Log.d("EditLocationActivity", "No changes to show, showing toast")
            Toast.makeText(this, "No new information found at this location", Toast.LENGTH_SHORT).show()
            return
        }

        // Show confirmation dialog
        Log.d("EditLocationActivity", "Showing confirmation dialog")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Confirm Address Update")
            .setMessage(changesMessage)
            .setPositiveButton("Apply Changes") { dialog, _ ->
                Log.d("EditLocationActivity", "User confirmed changes, applying results")
                dialog.dismiss()
                applyGeocodingResults(geocodingResult)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun applyGeocodingResults(geocodingResult: com.a42r.pevplugshare.data.remote.GeocodingResult.Success) {
        Log.d("EditLocationActivity", "applyGeocodingResults called with result: $geocodingResult")
        val currentFormData = viewModel.formData.value
        Log.d("EditLocationActivity", "Current form data before update: $currentFormData")

        // Update form data with geocoding results (only if fields are currently empty)
        if (currentFormData.name.isBlank() && geocodingResult.displayName != null) {
            Log.d("EditLocationActivity", "Updating name to: ${geocodingResult.displayName}")
            viewModel.updateName(geocodingResult.displayName)
        }

        if (currentFormData.businessName.isBlank() && geocodingResult.displayName != null) {
            Log.d("EditLocationActivity", "Updating businessName to: ${geocodingResult.displayName}")
            viewModel.updateBusinessName(geocodingResult.displayName)
        }

        if (currentFormData.streetAddress.isBlank() && geocodingResult.streetAddress != null) {
            Log.d("EditLocationActivity", "Updating streetAddress to: ${geocodingResult.streetAddress}")
            viewModel.updateStreetAddress(geocodingResult.streetAddress)
        }

        if (currentFormData.suburb.isBlank() && geocodingResult.suburb != null) {
            Log.d("EditLocationActivity", "Updating suburb to: ${geocodingResult.suburb}")
            viewModel.updateSuburb(geocodingResult.suburb)
        }

        if (currentFormData.state.isBlank() && geocodingResult.state != null) {
            Log.d("EditLocationActivity", "Updating state to: ${geocodingResult.state}")
            viewModel.updateState(geocodingResult.state)
        }

        if (currentFormData.postcode.isBlank() && geocodingResult.postcode != null) {
            Log.d("EditLocationActivity", "Updating postcode to: ${geocodingResult.postcode}")
            viewModel.updatePostcode(geocodingResult.postcode)
        }

        Log.d("EditLocationActivity", "Form data after update: ${viewModel.formData.value}")
        Toast.makeText(this, "Address information updated", Toast.LENGTH_SHORT).show()
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
                analyzePhotosWithAI(autoTriggered = false)
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
            Log.d("EditLocationActivity", "   File exists: ${file.exists()}")
            Log.d("EditLocationActivity", "   File readable: ${file.canRead()}")

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
                    .override(800, 800) // Downsample large images to prevent Canvas bitmap size exceptions
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
            viewModel.formData.value.latitude, viewModel.formData.value.longitude,
            photoLat, photoLon
        )

        // If more than 50 meters different, ask user
        if (distance > 50) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Update Location from Photo GPS?")
                .setMessage(
                    "Photo GPS location differs by ${String.format("%.0f", distance)}m:\n" +
                    "Current: ${viewModel.formData.value.latitude}, ${viewModel.formData.value.longitude}\n" +
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
        viewModel.updateLocation(photoLat, photoLon)

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

    private fun analyzePhotosWithAI(autoTriggered: Boolean = false) {
        val apiKey = tokenManager.getOpenRouterApiKey()
        if (apiKey.isNullOrBlank()) {
            Toast.makeText(this, "API key not configured", Toast.LENGTH_SHORT).show()
            return
        }

        if (photoUris.isEmpty()) {
            Toast.makeText(this, "No photos to analyze", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate that photo files exist before attempting analysis
        val validPhotoFiles = photoUris.mapNotNull { path ->
            val file = File(path)
            if (file.exists() && file.canRead()) {
                file
            } else {
                Log.e("EditLocationActivity", "Skipping non-existent/unreadable photo: $path")
                null
            }
        }

        if (validPhotoFiles.isEmpty()) {
            Toast.makeText(this, "No valid photos found to analyze", Toast.LENGTH_SHORT).show()
            return
        }

        if (validPhotoFiles.size < photoUris.size) {
            Log.w("EditLocationActivity", "Some photos were skipped: ${photoUris.size - validPhotoFiles.size} invalid out of ${photoUris.size}")
        }

        // Show progress dialog
        val progressDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Analyzing Photos")
            .setMessage("AI is analyzing ${validPhotoFiles.size} photo(s)...\nThis may take a moment.")
            .setCancelable(false)
            .create()
        progressDialog.show()

        lifecycleScope.launch {
            try {
                val focusAreasMap = mutableMapOf<String, android.graphics.RectF>()
                for (path in photoUris) {
                    val photoEntity = photoRepository.getPhotoByFilePath(path)
                    photoEntity?.focusRectString?.let { focusRectString ->
                        com.a42r.pevplugshare.util.PhotoUtils.parseFocusRectString(focusRectString)?.let { rectF ->
                            focusAreasMap[path] = rectF
                        }
                    }
                }
                Log.d("EditLocationActivity", "Starting AI photo analysis request for ${validPhotoFiles.size} photos")
                val result = photoAnalyzer.analyzePhotos(validPhotoFiles, apiKey, focusAreasMap)

                progressDialog.dismiss()

                result.fold(
                    onSuccess = { analysis ->
                        Log.d("EditLocationActivity", "AI photo analysis request successful, received analysis result")
                        if (autoTriggered) {
                            // Auto-triggered: apply directly without confirmation modal
                            applyAnalysisToForm(analysis)
                            Toast.makeText(this@EditLocationActivity, "AI analysis applied to form", Toast.LENGTH_SHORT).show()
                        } else {
                            // Manual trigger: show confirmation modal
                            showAnalysisResults(analysis)
                        }
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

            analysis.socketLocationDescription?.let {
                append("\n📍 Description:\n$it\n")
            }

            analysis.locationDetails?.let {
                append("\n🏞️ Location Details:\n$it\n")
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
                // Apply the analysis to the form only when user confirms
                applyAnalysisToForm(analysis)
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

            // Handle socket location description - use for main description (helps find sockets)
            analysis.socketLocationDescription?.let { socketLocation ->
                descriptionInput.setText(socketLocation)
                viewModel.updateDescription(socketLocation)
            }

            // Handle location details - add to additional info (descriptive details)
            analysis.locationDetails?.let { locationDetails ->
                val existing = additionalInfoInput.text.toString()
                val updated = if (existing.isBlank()) {
                    locationDetails
                } else {
                    "$existing\n\n$locationDetails"
                }
                additionalInfoInput.setText(updated)
                viewModel.updateAdditionalInfo(updated)
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

    override fun onBackPressed() {
        handleCancel()
    }

    private fun handleCancel() {
        // Check if there are unsaved changes
        if (viewModel.hasChanges()) {
            AlertDialog.Builder(this)
                .setTitle("Discard Changes")
                .setMessage("You have unsaved changes. Are you sure you want to cancel and lose your changes?")
                .setPositiveButton("Discard") { _, _ ->
                    finish()
                }
                .setNegativeButton("Keep Editing", null)
                .show()
        } else {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Start listening to compass sensor in add mode
        if (isAddMode) {
            rotationSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    override fun onPause() {
        super.onPause()
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

        // Unregister broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(syncResultReceiver)

        if (!submittedSuccessfully) {
            // If the activity is destroyed without successful submission, delete temporary images
            currentEditLocationId?.let { sessionId ->
                ImageUtils.deleteTempImages(this, sessionId)
                Log.d("EditLocationActivity", "Deleted temporary images for session: $sessionId")
            }
        }
    }
}
