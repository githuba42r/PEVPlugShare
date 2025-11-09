package com.pevplugshare.ui.edit

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
import com.pevplugshare.R
import com.pevplugshare.databinding.ActivityEditLocationBinding
import com.pevplugshare.util.ImageUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class EditLocationActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityEditLocationBinding
    private val viewModel: EditLocationViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    @Inject
    lateinit var tokenManager: com.pevplugshare.data.remote.TokenManager
    
    @Inject
    lateinit var photoAnalyzer: com.pevplugshare.ai.PhotoAnalyzer
    
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
    private val maxPhotos = 5
    private var currentPhotoUri: Uri? = null
    private var tempPhotoFile: File? = null
    
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            currentPhotoUri?.let {
                processAndSavePhoto()
            }
        }
    }
    
    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            val remainingSlots = maxPhotos - photoUris.size
            val urisToProcess = uris.take(remainingSlots)
            
            if (uris.size > remainingSlots) {
                Toast.makeText(
                    this,
                    "Only adding $remainingSlots photo(s). Maximum is $maxPhotos total.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            
            urisToProcess.forEach { uri ->
                processAndSaveGalleryPhoto(uri)
            }
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

        if (isAddMode) {
            // Add mode: Initialize for new location entry
            viewModel.initializeForAddMode()
            requestLocationAndPopulate()
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
                    
                    // Add direction info to additional info field
                    if (isAddMode) {
                        val direction = getCompassDirection(currentAzimuth)
                        val directionInfo = "Device facing: $direction"
                        val existingInfo = binding.additionalInfoInput.text.toString()
                        val newInfo = if (existingInfo.isBlank()) {
                            directionInfo
                        } else {
                            "$existingInfo\n$directionInfo"
                        }
                        binding.additionalInfoInput.setText(newInfo)
                        viewModel.updateAdditionalInfo(newInfo)
                    }
                    
                    Toast.makeText(this, "Location acquired", Toast.LENGTH_SHORT).show()
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
            buttonCancel.setOnClickListener { finish() }
            buttonSyncToOsmand.setOnClickListener { syncToOsmand() }
            // Hide sync button in add mode
            buttonSyncToOsmand.visibility = if (isAddMode) android.view.View.GONE else android.view.View.VISIBLE
            buttonGenerateDescription.setOnClickListener { showAiDescriptionDialog() }
            buttonParseDescription.setOnClickListener { parseDescriptionToFields() }
            // Parse button visible in both add and edit modes
            
            // Add name field text watcher to enable/disable submit button
            nameInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    if (isAddMode) {
                        buttonSubmit.isEnabled = !s.isNullOrBlank()
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
        val mapModalOverlay = findViewById<android.view.View>(com.pevplugshare.R.id.mapModalOverlay)
        mapView = findViewById(com.pevplugshare.R.id.mapView)
        val buttonAccept = findViewById<com.google.android.material.button.MaterialButton>(com.pevplugshare.R.id.buttonAcceptLocation)
        val buttonCancel = findViewById<com.google.android.material.button.MaterialButton>(com.pevplugshare.R.id.buttonCancelLocation)
        
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
        val mapModalOverlay = findViewById<android.view.View>(com.pevplugshare.R.id.mapModalOverlay)
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
        val mapModalOverlay = findViewById<android.view.View>(com.pevplugshare.R.id.mapModalOverlay)
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
                        binding.buttonSubmit.isEnabled = true
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
                        setResult(RESULT_OK)
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

    private fun populateFormFromLocation(location: com.pevplugshare.data.model.Location) {
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

            val isPublic = location.publicPrivate == com.pevplugshare.data.model.LocationAccessType.PUBLIC
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
        val isPublic = location.publicPrivate == com.pevplugshare.data.model.LocationAccessType.PUBLIC
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
                setImageResource(com.pevplugshare.R.drawable.ic_favorite)
                setColorFilter(ContextCompat.getColor(context, com.google.android.material.R.color.design_default_color_primary))
                tag = "favorite"
            } else {
                setImageResource(com.pevplugshare.R.drawable.ic_favorite_border)
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
                accessTypeIcon.setImageResource(com.pevplugshare.R.drawable.ic_public)
                accessTypeIcon.setColorFilter(ContextCompat.getColor(this@EditLocationActivity, com.google.android.material.R.color.design_default_color_primary))
                accessTypeText.text = "Public"
            } else {
                accessTypeIcon.setImageResource(com.pevplugshare.R.drawable.ic_private)
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
        if (isAddMode) {
            viewModel.addLocation()
        } else {
            viewModel.submitUpdates(locationId!!)
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
            com.pevplugshare.data.model.LocationAccessType.PUBLIC
        } else {
            com.pevplugshare.data.model.LocationAccessType.PRIVATE
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
        
        viewModel.parseDescription(description) { parsed ->
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
                
                val message = if (extractedInfo.isNotEmpty()) {
                    "✅ Extracted:\n" + extractedInfo.joinToString("\n")
                } else {
                    "No additional information could be extracted from the description"
                }
                
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "❌ Failed to parse description. Check your AI settings.", Toast.LENGTH_LONG).show()
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
        binding.analyzePhotosButton.visibility = if (photoUris.isNotEmpty()) {
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
                    1 -> pickImages.launch("image/*")
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
    
    private fun processAndSavePhoto() {
        lifecycleScope.launch(Dispatchers.IO) {
            tempPhotoFile?.let { sourceFile ->
                val currentLocationId = locationId ?: "temp_${System.currentTimeMillis()}"
                val resizedFile = ImageUtils.resizeAndSaveImage(
                    this@EditLocationActivity,
                    sourceFile,
                    currentLocationId,
                    photoUris.size
                )
                
                withContext(Dispatchers.Main) {
                    resizedFile?.let {
                        photoUris.add(it.absolutePath)
                        addPhotoThumbnail(it)
                        // Delete temp file
                        sourceFile.delete()
                        tempPhotoFile = null
                        Toast.makeText(
                            this@EditLocationActivity,
                            "Photo added (${photoUris.size}/$maxPhotos)",
                            Toast.LENGTH_SHORT
                        ).show()
                    } ?: run {
                        Toast.makeText(
                            this@EditLocationActivity,
                            "Failed to save photo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
    
    private fun processAndSaveGalleryPhoto(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Copy URI content to temp file
                val tempFile = File.createTempFile("gallery_", ".jpg", cacheDir)
                contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                val currentLocationId = locationId ?: "temp_${System.currentTimeMillis()}"
                val resizedFile = ImageUtils.resizeAndSaveImage(
                    this@EditLocationActivity,
                    tempFile,
                    currentLocationId,
                    photoUris.size
                )
                
                withContext(Dispatchers.Main) {
                    resizedFile?.let {
                        photoUris.add(it.absolutePath)
                        addPhotoThumbnail(it)
                        Toast.makeText(
                            this@EditLocationActivity,
                            "Photo added (${photoUris.size}/$maxPhotos)",
                            Toast.LENGTH_SHORT
                        ).show()
                    } ?: run {
                        Toast.makeText(
                            this@EditLocationActivity,
                            "Failed to save photo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                
                // Delete temp file
                tempFile.delete()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
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
        
        Glide.with(this)
            .load(file)
            .centerCrop()
            .into(imageView)
        
        val index = photoUris.size - 1
        removeButton.setOnClickListener {
            removePhoto(index)
        }
        
        binding.photosContainer.addView(thumbnailView, binding.photosContainer.childCount)
        updateAddButtonVisibility()
        updateAnalyzeButtonVisibility()
    }
    
    private fun removePhoto(index: Int) {
        if (index < photoUris.size) {
            val path = photoUris[index]
            File(path).delete()
            photoUris.removeAt(index)
            refreshPhotoThumbnails()
            updateAnalyzeButtonVisibility()
            Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show()
        }
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
            existingPhotos.forEach { file ->
                photoUris.add(file.absolutePath)
                addPhotoThumbnail(file)
            }
        }
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
                val result = photoAnalyzer.analyzePhotos(photoFiles, apiKey)
                
                progressDialog.dismiss()
                
                result.fold(
                    onSuccess = { analysis ->
                        showAnalysisResults(analysis)
                        applyAnalysisToForm(analysis)
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            this@EditLocationActivity,
                            "Analysis failed: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(
                    this@EditLocationActivity,
                    "Analysis error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun showAnalysisResults(analysis: com.pevplugshare.ai.PhotoAnalysisResult) {
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
    
    private fun applyAnalysisToForm(analysis: com.pevplugshare.ai.PhotoAnalysisResult) {
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
            
            // Append location description to additional info
            analysis.locationDescription?.let { location ->
                val existing = additionalInfoInput.text.toString()
                val updated = if (existing.isBlank()) {
                    "Location: $location"
                } else {
                    "$existing\n\nLocation: $location"
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
}
