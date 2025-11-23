package com.a42r.pevplugshare.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.data.model.Location
import com.a42r.pevplugshare.databinding.ActivityMainBinding
import com.a42r.pevplugshare.ui.edit.EditLocationActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var adapter: LocationAdapter? = null
    private var currentGalleryLocation: Location? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            updateUserLocation()
        }
    }

    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // Close the image gallery modal if it's open
            if (binding.imageGalleryOverlay.visibility == android.view.View.VISIBLE) {
                binding.imageGalleryOverlay.visibility = android.view.View.GONE
                binding.addLocationFab.show()
                binding.quickLocationButton.show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Register back button handler for image gallery modal
        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        setupUI()
        setupObservers()
        requestLocationPermission()

        // Validate session token in background and load data (if OSMand is connected)
        lifecycleScope.launch {
            val isValid = viewModel.validateSessionAndLoadData()
            if (!isValid) {
                // OSMand not connected or session invalid - still allow local database usage
                android.util.Log.d("MainActivity", "OSMand not connected, using local database only")
                // Load categories/locations from local database
                viewModel.loadFromLocalDatabase()
            }
        }

        // Handle highlight intent if coming from image viewer
        handleHighlightIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleHighlightIntent(it) }
    }

    override fun onResume() {
        super.onResume()
        // Refresh location list when returning from edit/add activities
        // This ensures gallery icons and photo changes are reflected
        viewModel.syncLocations()

        // Update category badge visibility based on OsmAnd connection status
        updateCategoryBadgeVisibility()
    }

    private fun updateCategoryBadgeVisibility() {
        val category = viewModel.selectedCategory.value
        val isOsmandConnected = viewModel.isOsmandConnected()

        if (category != null && isOsmandConnected) {
            binding.categoryBadge.text = category
            binding.categoryBadge.visibility = android.view.View.VISIBLE
        } else {
            binding.categoryBadge.visibility = android.view.View.GONE
        }
    }

    private fun setupUI() {
        binding.apply {
            menuButton.setOnClickListener { showMenu() }
            addLocationFab.setOnClickListener {
                // Launch EditLocationActivity in add mode (no location_id)
                startActivity(Intent(this@MainActivity, EditLocationActivity::class.java))
            }
            quickLocationButton.setOnClickListener {
                // Launch QuickLocationActivity
                startActivity(Intent(this@MainActivity, com.a42r.pevplugshare.ui.quicklocation.QuickLocationActivity::class.java))
            }
            toggleFilterButton.setOnClickListener {
                showFilterDialog()
            }
            filterFavoritesButton.setOnClickListener {
                viewModel.toggleFavoritesFilter()
            }
            swipeRefreshLayout.setOnRefreshListener {
                viewModel.syncLocations()
            }
            locationRecyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.filterState.collect { filterState ->
                updateFilterButtonsAppearance(filterState)
            }
        }

        lifecycleScope.launch {
            viewModel.locationListState.collect { state ->
                when (state) {
                    is LocationListState.Loading -> {
                        binding.loadingCard.visibility = android.view.View.VISIBLE
                        binding.loadingMessage.text = "Loading locations..."
                        binding.locationRecyclerView.visibility = android.view.View.GONE
                        binding.emptyState.visibility = android.view.View.GONE
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                    is LocationListState.Success -> {
                        binding.loadingCard.visibility = android.view.View.GONE
                        binding.locationRecyclerView.visibility = android.view.View.VISIBLE
                        binding.emptyState.visibility = android.view.View.GONE
                        binding.swipeRefreshLayout.isRefreshing = false

                        val userLoc = viewModel.userLocation.value ?: Pair(0.0, 0.0)

                        // Always recreate adapter to use current user location
                        adapter = LocationAdapter(
                            onItemClick = { location -> showLocationDetails(location) },
                            onNavigateClick = { location -> navigateToLocation(location) },
                            onFavouriteClick = { location -> viewModel.toggleFavourite(location) },
                            onEditClick = { location -> editLocation(location) },
                            onInfoClick = { location -> /* Info button handled internally */ },
                            onMapClick = { location -> loadLocationMap(location) },
                            onPhotoClick = { location -> showPhotoGallery(location) },
                            onArFinderClick = { location -> launchArFinder(location) },
                            userLatitude = userLoc.first,
                            userLongitude = userLoc.second
                        )
                        binding.locationRecyclerView.adapter = adapter
                        adapter?.submitList(state.locations)
                    }
                    is LocationListState.Empty -> {
                        binding.loadingCard.visibility = android.view.View.GONE
                        binding.locationRecyclerView.visibility = android.view.View.GONE
                        binding.emptyState.visibility = android.view.View.VISIBLE
                        binding.emptyState.text = "No charging locations found.\n\nLog in to OSMAnd to sync your favorites, or tap + to add locations manually."
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                    is LocationListState.Error -> {
                        binding.loadingCard.visibility = android.view.View.GONE
                        binding.locationRecyclerView.visibility = android.view.View.GONE
                        binding.emptyState.visibility = android.view.View.VISIBLE
                        binding.emptyState.text = "Error: ${state.message}"
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.userLocation.collect { userLoc ->
                // When user location is updated, refresh the adapter if we have locations displayed
                if (adapter != null && userLoc != null) {
                    val state = viewModel.locationListState.value
                    if (state is LocationListState.Success) {
                        adapter = LocationAdapter(
                            onItemClick = { location -> showLocationDetails(location) },
                            onNavigateClick = { location -> navigateToLocation(location) },
                            onFavouriteClick = { location -> viewModel.toggleFavourite(location) },
                            onEditClick = { location -> editLocation(location) },
                            onInfoClick = { location -> /* Info button handled internally */ },
                            onMapClick = { location -> loadLocationMap(location) },
                            onPhotoClick = { location -> showPhotoGallery(location) },
                            userLatitude = userLoc.first,
                            userLongitude = userLoc.second
                        )
                        binding.locationRecyclerView.adapter = adapter
                        adapter?.submitList(state.locations)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.selectedCategory.collect { category ->
                updateCategoryBadgeVisibility()
            }
        }

        // Observe sync progress
        lifecycleScope.launch {
            viewModel.syncProgress.collect { progress ->
                if (progress != null) {
                    binding.loadingMessage.text = "Syncing ${progress.categoryName}...\n${progress.current} of ${progress.total} locations"
                } else {
                    binding.loadingMessage.text = "Loading locations..."
                }
            }
        }

        // Add long press handler to category badge
        binding.categoryBadge.setOnLongClickListener {
            showCategorySelectionDialog()
            true
        }

        lifecycleScope.launch {
            viewModel.needsCategorySelection.collect { needsSelection ->
                if (needsSelection) {
                    showCategorySelectionDialog(mandatory = true)
                }
            }
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            updateUserLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun updateUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location ->
            location?.let {
                viewModel.updateUserLocation(it.latitude, it.longitude)
            }
        }
    }

    private fun showMenu() {
        val popup = PopupMenu(this, binding.menuButton)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)

        // Update menu items based on OSMand connection status
        lifecycleScope.launch {
            val isOsmandConnected = viewModel.isOsmandConnected()

            // Show/hide OSMand connect menu item based on connection status
            val osmandConnectItem = popup.menu.findItem(R.id.menu_osmand_connect)
            osmandConnectItem?.isVisible = !isOsmandConnected

            // Show/hide OSMand logout menu item based on connection status
            val osmandLogoutItem = popup.menu.findItem(R.id.menu_osmand_logout)
            osmandLogoutItem?.isVisible = isOsmandConnected

            // Disable category selection if not connected to OSMand
            val categoryItem = popup.menu.findItem(R.id.menu_select_category)
            categoryItem?.isEnabled = isOsmandConnected
        }

        // Update the saved for later menu item with badge count
        val savedForLaterItem = popup.menu.findItem(R.id.menu_saved_for_later)
        val count = viewModel.savedForLaterCount.value

        savedForLaterItem.isEnabled = count > 0
        savedForLaterItem.title = if (count > 0) {
            "Saved ($count)"
        } else {
            "Saved"
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_osmand_connect -> {
                    showOsmandConnectionFlow()
                    true
                }
                R.id.menu_select_category -> {
                    showCategorySelectionDialog()
                    true
                }
                R.id.menu_saved_for_later -> {
                    startActivity(Intent(this, com.a42r.pevplugshare.ui.savedforlater.SavedForLaterActivity::class.java))
                    true
                }
                R.id.menu_image_gallery -> {
                    startActivity(Intent(this, com.a42r.pevplugshare.ui.gallery.AllImagesGalleryActivity::class.java))
                    true
                }
                R.id.menu_openrouter_auth -> {
                    startActivity(Intent(this, com.a42r.pevplugshare.ui.oauth.OAuthActivity::class.java))
                    true
                }
                R.id.menu_settings -> {
                    startActivity(Intent(this, com.a42r.pevplugshare.ui.settings.SettingsActivity::class.java))
                    true
                }
                R.id.menu_osmand_logout -> {
                    startActivity(Intent(this, com.a42r.pevplugshare.ui.osmand.OsmandLogoutActivity::class.java))
                    true
                }
                R.id.menu_about -> {
                    showAboutDialog()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showOsmandConnectionFlow() {
        // Launch the LoginActivity to connect to OSMand
        val intent = Intent(this, com.a42r.pevplugshare.ui.auth.LoginActivity::class.java)
        intent.putExtra("isReconnecting", true) // Flag to indicate this is a reconnection
        startActivity(intent)
        // When user returns, they will either be connected or have skipped again
    }

    private fun showLogoutConfirmation() {
        lifecycleScope.launch {
            // Check if credentials are stored
            val hasStoredCredentials = viewModel.hasStoredCredentials()

            val message = buildString {
                append("Are you sure you want to logout?\n\n")
                append("This will:\n")
                append("• End your current session\n")
                append("• Remove session cookie\n")
                append("• Cancel automatic session refresh\n")
                if (hasStoredCredentials) {
                    append("\n⚠️ IMPORTANT:\n")
                    append("• Delete your securely stored login credentials\n")
                    append("• Disable automatic login\n")
                    append("\nYou will need to manually login again next time.")
                }
            }

            AlertDialog.Builder(this@MainActivity)
                .setTitle("Logout")
                .setMessage(message)
                .setPositiveButton("Logout") { _, _ ->
                    performLogout()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(true)
                .show()
        }
    }

    private fun performLogout() {
        lifecycleScope.launch {
            viewModel.logout()
            Toast.makeText(this@MainActivity, "Logged out successfully", Toast.LENGTH_SHORT).show()

            // Navigate back to login screen
            val intent = Intent(this@MainActivity, com.a42r.pevplugshare.ui.auth.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showCategorySelectionDialog(mandatory: Boolean = false) {
        lifecycleScope.launch {
            binding.loadingProgressBar.visibility = android.view.View.VISIBLE

            // Refresh categories from server if not mandatory
            if (!mandatory) {
                viewModel.refreshCategories()
            }

            // Wait for non-empty categories list with a timeout of 15 seconds
            // This replaces the polling mechanism and waits for the Flow to emit non-empty categories
            val categories = withTimeoutOrNull(15.seconds) {
                viewModel.categories.first { it.isNotEmpty() }
            } ?: emptyList()

            binding.loadingProgressBar.visibility = android.view.View.GONE

            android.util.Log.d("MainActivity", "Final categories count: ${categories.size}, list: ${categories.joinToString()}")

            if (categories.isEmpty()) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("No Categories Found")
                    .setMessage("No OsmAnd favorites categories found. Please ensure you are logged in and have favorites saved in OsmAnd.")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        if (mandatory) {
                            viewModel.dismissCategorySelectionPrompt()
                        }
                    }
                    .setCancelable(!mandatory)
                    .show()
                return@launch
            }

            android.util.Log.d("MainActivity", "Showing dialog with ${categories.size} categories: ${categories.joinToString()}")

            val categoryNames = categories.toTypedArray()
            val currentCategory = viewModel.selectedCategory.value
            val selectedIndex = categoryNames.indexOf(currentCategory).takeIf { it >= 0 } ?: -1

            android.util.Log.d("MainActivity", "Current category: $currentCategory, selected index: $selectedIndex")

            var tempSelectedIndex = selectedIndex

            val dialogBuilder = AlertDialog.Builder(this@MainActivity)
                .setTitle("Select Favorites Category")
                .setSingleChoiceItems(categoryNames, selectedIndex) { _, which ->
                    tempSelectedIndex = which
                    android.util.Log.d("MainActivity", "User selected index: $which (${categoryNames[which]})")
                }
                .setPositiveButton("Select") { dialog, _ ->
                    if (tempSelectedIndex >= 0) {
                        val selectedCategory = categoryNames[tempSelectedIndex]
                        android.util.Log.d("MainActivity", "Saving selected category: $selectedCategory")
                        viewModel.selectCategory(selectedCategory)
                        Toast.makeText(this@MainActivity, "Selected: $selectedCategory", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Please select a category", Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                }
                .setCancelable(!mandatory)

            if (!mandatory) {
                dialogBuilder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            }

            dialogBuilder.show()
            android.util.Log.d("MainActivity", "Dialog shown successfully")
        }
    }

    private fun showAboutDialog() {
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "1.0"
        }

        // Calculate image statistics
        lifecycleScope.launch {
            val (imageCount, totalSizeBytes) = withContext(kotlinx.coroutines.Dispatchers.IO) {
                calculateImageStatistics()
            }

            val totalSizeMB = totalSizeBytes / (1024.0 * 1024.0)
            val sizeString = if (totalSizeMB < 1.0) {
                String.format("%.2f KB", totalSizeBytes / 1024.0)
            } else {
                String.format("%.2f MB", totalSizeMB)
            }

            val message = "A charging location finder for Personal Electric Vehicles.\n\n" +
                    "Version: $versionName\n" +
                    "Integration: OSMAnd (via Intent API)\n" +
                    "Storage: Local database\n\n" +
                    "Image Storage:\n" +
                    "• Images stored: $imageCount\n" +
                    "• Total size: $sizeString\n\n" +
                    "Features:\n" +
                    "• Find nearby charging locations\n" +
                    "• Add new charging spots\n" +
                    "• Navigate with OSMAnd\n" +
                    "• Offline support"

            AlertDialog.Builder(this@MainActivity)
                .setTitle("PEVPlugShare")
                .setIcon(R.mipmap.ic_launcher)
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    private fun calculateImageStatistics(): Pair<Int, Long> {
        val imagesDir = com.a42r.pevplugshare.util.ImageUtils.getAppImagesDirectory(this)
        var totalCount = 0
        var totalSize = 0L

        if (imagesDir.exists() && imagesDir.isDirectory) {
            imagesDir.listFiles()?.forEach { locationDir ->
                if (locationDir.isDirectory) {
                    locationDir.listFiles()?.filter { it.extension == "jpg" }?.forEach { imageFile ->
                        totalCount++
                        totalSize += imageFile.length()
                    }
                }
            }
        }

        return Pair(totalCount, totalSize)
    }

    private fun navigateToLocation(location: Location) {
        val success = com.a42r.pevplugshare.util.OsmandIntentHelper.navigateToLocation(
            this,
            location,
            "bicycle" // Default profile for PEV
        )

        if (!success) {
            Toast.makeText(this, R.string.osmand_not_installed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun editLocation(location: Location) {
        val intent = Intent(this, com.a42r.pevplugshare.ui.edit.EditLocationActivity::class.java)
        intent.putExtra("location_id", location.id)
        startActivity(intent)
    }

    private fun showLocationDetails(location: Location) {
        Toast.makeText(this, "Location: ${location.name}", Toast.LENGTH_SHORT).show()
    }

    private fun loadLocationMap(location: Location) {
        val zoomLevel = viewModel.getMapZoomLevel()
        val mapUrl = "https://www.openstreetmap.org/export/embed.html?bbox=" +
                "${location.longitude - 0.005},${location.latitude - 0.005}," +
                "${location.longitude + 0.005},${location.latitude + 0.005}" +
                "&layer=mapnik&marker=${location.latitude},${location.longitude}"

        // The actual map loading will be handled by the adapter's ImageView
        // This is just for logging/debugging
        android.util.Log.d("MainActivity", "Loading map for ${location.name} at zoom $zoomLevel: $mapUrl")
    }

    private fun showAuthenticationFailedDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Authentication Failed")
            .setMessage("Authentication to OSMAnd failed. Your session has expired or is invalid.")
            .setCancelable(false)
            .setPositiveButton("Close") { _, _ ->
                returnToLogin()
            }
            .create()

        dialog.show()

        // Auto-close after 15 seconds
        lifecycleScope.launch {
            kotlinx.coroutines.delay(15000)
            if (dialog.isShowing) {
                dialog.dismiss()
                returnToLogin()
            }
        }
    }

    private fun returnToLogin() {
        val intent = Intent(this, com.a42r.pevplugshare.ui.auth.LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showPhotoGallery(location: Location) {
        android.util.Log.d("PhotoGallery", "=== showPhotoGallery called ===")
        android.util.Log.d("PhotoGallery", "Location: ${location.name} (ID: ${location.id})")

        val imagePaths = com.a42r.pevplugshare.util.ImageUtils.getLocationImages(this, location.id)
            .map { it.absolutePath }

        android.util.Log.d("PhotoGallery", "Found ${imagePaths.size} images")
        imagePaths.forEachIndexed { index, path ->
            android.util.Log.d("PhotoGallery", "  Image $index: $path")
        }

        if (imagePaths.isEmpty()) {
            android.util.Log.w("PhotoGallery", "No photos found, showing toast")
            Toast.makeText(this, "No photos available for this location", Toast.LENGTH_SHORT).show()
            return
        }

        // Store location reference for download/share operations
        currentGalleryLocation = location

        // Get modal views
        android.util.Log.d("PhotoGallery", "Looking for modal views...")
        val galleryOverlay = binding.imageGalleryOverlay
        val viewPager = binding.imageViewPager
        val imageCounter = binding.imageCounter
        val galleryToolbar = binding.galleryToolbar
        val locationDescView = binding.locationDescription

        android.util.Log.d("PhotoGallery", "Views found:")
        android.util.Log.d("PhotoGallery", "  overlay: ${galleryOverlay != null}")
        android.util.Log.d("PhotoGallery", "  viewPager: ${viewPager != null}")
        android.util.Log.d("PhotoGallery", "  counter: ${imageCounter != null}")
        android.util.Log.d("PhotoGallery", "  toolbar: ${galleryToolbar != null}")
        android.util.Log.d("PhotoGallery", "  description: ${locationDescView != null}")

        if (galleryOverlay == null || viewPager == null) {
            android.util.Log.e("PhotoGallery", "CRITICAL: Modal views not found!")
            Toast.makeText(this, "Error: Modal not initialized", Toast.LENGTH_SHORT).show()
            return
        }

        // Show gallery modal
        android.util.Log.d("PhotoGallery", "Setting modal visibility to VISIBLE")
        galleryOverlay.visibility = android.view.View.VISIBLE
        // Hide FABs when gallery is shown
        binding.addLocationFab.hide()
        binding.quickLocationButton.hide()

        // Enable back button handler for modal
        backPressedCallback.isEnabled = true

        android.util.Log.d("PhotoGallery", "Creating adapter with ${imagePaths.size} images")
        val adapter = com.a42r.pevplugshare.ui.adapter.ImageGalleryAdapter(imagePaths)
        viewPager.adapter = adapter
        viewPager.setCurrentItem(0, false)

        // Set location description
        val description = location.description ?: "No description available"
        locationDescView?.text = description

        // Setup description toggle button
        val toggleButton = binding.toggleDescriptionButton
        var isDescriptionExpanded = false

        // Check if description needs "More" button (more than 2 lines)
        locationDescView?.post {
            val lineCount = locationDescView.lineCount
            if (lineCount > 2 || description.length > 100) {
                toggleButton?.visibility = android.view.View.VISIBLE
            } else {
                toggleButton?.visibility = android.view.View.GONE
            }
        }

        toggleButton?.setOnClickListener {
            isDescriptionExpanded = !isDescriptionExpanded
            if (isDescriptionExpanded) {
                locationDescView?.maxLines = Int.MAX_VALUE
                toggleButton.text = "Less"
            } else {
                locationDescView?.maxLines = 2
                toggleButton.text = "More"
            }
        }

        // Set toolbar title to location name
        galleryToolbar?.title = location.name

        // Update counter
        imageCounter?.text = "1 / ${imagePaths.size}"

        // Track current image position
        var currentImagePosition = 0

        android.util.Log.d("PhotoGallery", "Setting up page change listener")
        // Handle page changes
        viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                android.util.Log.d("PhotoGallery", "Page selected: ${position + 1} / ${imagePaths.size}")
                imageCounter?.text = "${position + 1} / ${imagePaths.size}"
                currentImagePosition = position
            }
        })

        // Download button
        binding.downloadImageButton?.setOnClickListener {
            downloadImage(imagePaths[currentImagePosition])
        }

        // Share button
        binding.shareImageButton?.setOnClickListener {
            shareImage(imagePaths[currentImagePosition])
        }

        // Close button
        android.util.Log.d("PhotoGallery", "Setting up close button")
        galleryToolbar?.setNavigationOnClickListener {
            android.util.Log.d("PhotoGallery", "Close button clicked")
            galleryOverlay.visibility = android.view.View.GONE
            // Show FABs when gallery is closed
            binding.addLocationFab.show()
            binding.quickLocationButton.show()
            // Disable back button handler
            backPressedCallback.isEnabled = false
        }

        // Also allow clicking overlay to close
        galleryOverlay.setOnClickListener {
            android.util.Log.d("PhotoGallery", "Overlay clicked to close")
            it.visibility = android.view.View.GONE
            // Show FABs when gallery is closed
            binding.addLocationFab.show()
            binding.quickLocationButton.show()
            // Disable back button handler
            backPressedCallback.isEnabled = false
        }

        android.util.Log.d("PhotoGallery", "=== Modal opened successfully ===")
    }

    private fun launchArFinder(location: Location) {
        val intent = com.a42r.pevplugshare.ar.ArSocketFinderActivity.createIntent(this, location)
        startActivity(intent)
    }

    private fun downloadImage(imagePath: String) {
        lifecycleScope.launch {
            try {
                val sourceFile = java.io.File(imagePath)

                // Add GPS data to the image if location is available
                if (currentGalleryLocation != null) {
                    withContext(Dispatchers.IO) {
                        com.a42r.pevplugshare.util.ImageUtils.addGpsDataToImage(
                            sourceFile,
                            currentGalleryLocation!!.latitude,
                            currentGalleryLocation!!.longitude
                        )
                    }
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // Use MediaStore for Android 10+
                    val values = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "PEVPlugShare_${System.currentTimeMillis()}.jpg")
                        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/PEVPlugShare")
                        put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                    }

                    val uri = contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    uri?.let { imageUri ->
                        contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                            java.io.FileInputStream(sourceFile).use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }

                        // Clear IS_PENDING flag to make the image visible
                        values.clear()
                        values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                        contentResolver.update(imageUri, values, null, null)

                        Toast.makeText(this@MainActivity, "Image saved to Pictures/PEVPlugShare", Toast.LENGTH_LONG).show()
                    } ?: run {
                        Toast.makeText(this@MainActivity, "Failed to create image file", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Use legacy approach for Android 9 and below (requires permission)
                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        Toast.makeText(this@MainActivity, "Storage permission required", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                    val appDir = java.io.File(picturesDir, "PEVPlugShare")
                    if (!appDir.exists()) {
                        appDir.mkdirs()
                    }

                    val destFile = java.io.File(appDir, "PEVPlugShare_${System.currentTimeMillis()}.jpg")
                    sourceFile.copyTo(destFile, overwrite = true)

                    // Notify media scanner to make image visible in gallery
                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    mediaScanIntent.data = Uri.fromFile(destFile)
                    sendBroadcast(mediaScanIntent)

                    Toast.makeText(this@MainActivity, "Image saved to Pictures/PEVPlugShare", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to save image: ${e.message}", Toast.LENGTH_LONG).show()
                android.util.Log.e("MainActivity", "Error saving image", e)
            }
        }
    }

    private fun shareImage(imagePath: String) {
        try {
            val imageFile = java.io.File(imagePath)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                imageFile
            )

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Charging location image from PEVPlugShare")
                type = "image/jpeg"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share image"))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to share image: ${e.message}", Toast.LENGTH_SHORT).show()
            android.util.Log.e("MainActivity", "Error sharing image", e)
        }
    }

    private fun handleHighlightIntent(intent: Intent) {
        val locationId = intent.getStringExtra("HIGHLIGHT_LOCATION_ID")
        if (!locationId.isNullOrEmpty()) {
            highlightLocation(locationId)
        }
    }

    private fun highlightLocation(locationId: String) {
        lifecycleScope.launch {
            // Wait a bit for the list to be populated
            kotlinx.coroutines.delay(500)

            val currentAdapter = adapter
            if (currentAdapter != null) {
                // Find the position of the location in the list
                val position = currentAdapter.currentList.indexOfFirst { it.id == locationId }

                if (position != -1) {
                    // Scroll to the location and center it on screen
                    val layoutManager = binding.locationRecyclerView.layoutManager as? LinearLayoutManager
                    layoutManager?.scrollToPositionWithOffset(position,
                        binding.locationRecyclerView.height / 2 - 200)

                    // Highlight the location
                    currentAdapter.highlightLocation(locationId)

                    // Remove highlight after 10 seconds
                    lifecycleScope.launch {
                        kotlinx.coroutines.delay(10000)
                        currentAdapter.clearHighlight()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Location not found in list", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showFilterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_filter, null)
        val currentFilter = viewModel.filterState.value

        // Initialize dialog views with current filter state
        val costFree = dialogView.findViewById<android.widget.TextView>(R.id.costFree)
        val costPaid = dialogView.findViewById<android.widget.TextView>(R.id.costPaid)
        val openNowCheckbox = dialogView.findViewById<android.widget.CheckBox>(R.id.openNowCheckbox)
        val socketsRangeSlider = dialogView.findViewById<com.google.android.material.slider.RangeSlider>(R.id.socketsRangeSlider)
        val minSocketsLabel = dialogView.findViewById<android.widget.TextView>(R.id.minSocketsLabel)
        val maxSocketsLabel = dialogView.findViewById<android.widget.TextView>(R.id.maxSocketsLabel)
        val accessPublic = dialogView.findViewById<android.widget.TextView>(R.id.accessPublic)
        val accessPrivate = dialogView.findViewById<android.widget.TextView>(R.id.accessPrivate)

        // Socket type badges (TextViews acting as toggle buttons)
        val socketAS3112 = dialogView.findViewById<android.widget.TextView>(R.id.socketAS3112)
        val socketType2 = dialogView.findViewById<android.widget.TextView>(R.id.socketType2)
        val socketTesla = dialogView.findViewById<android.widget.TextView>(R.id.socketTesla)
        val socketCCS2 = dialogView.findViewById<android.widget.TextView>(R.id.socketCCS2)
        val socketCHAdeMO = dialogView.findViewById<android.widget.TextView>(R.id.socketCHAdeMO)
        val socketType1 = dialogView.findViewById<android.widget.TextView>(R.id.socketType1)
        val socketWallSocket = dialogView.findViewById<android.widget.TextView>(R.id.socketWallSocket)
        val socketCaravan = dialogView.findViewById<android.widget.TextView>(R.id.socketCaravan)
        val socketOther = dialogView.findViewById<android.widget.TextView>(R.id.socketOther)

        // Set current values
        costFree.isSelected = currentFilter.costFilter == CostFilter.FREE
        costPaid.isSelected = currentFilter.costFilter == CostFilter.PAID
        openNowCheckbox.isChecked = currentFilter.openNowOnly
        socketsRangeSlider.values = listOf(currentFilter.minSockets.toFloat(), currentFilter.maxSockets.toFloat())
        accessPublic.isSelected = currentFilter.accessFilter == AccessFilter.PUBLIC
        accessPrivate.isSelected = currentFilter.accessFilter == AccessFilter.PRIVATE

        // Set socket type badges selected state
        socketAS3112.isSelected = com.a42r.pevplugshare.data.model.SocketType.AS3112 in currentFilter.socketTypes
        socketType2.isSelected = com.a42r.pevplugshare.data.model.SocketType.TYPE2 in currentFilter.socketTypes
        socketTesla.isSelected = com.a42r.pevplugshare.data.model.SocketType.TESLA in currentFilter.socketTypes
        socketCCS2.isSelected = com.a42r.pevplugshare.data.model.SocketType.CCS2 in currentFilter.socketTypes
        socketCHAdeMO.isSelected = com.a42r.pevplugshare.data.model.SocketType.CHADEMO in currentFilter.socketTypes
        socketType1.isSelected = com.a42r.pevplugshare.data.model.SocketType.TYPE1 in currentFilter.socketTypes
        socketWallSocket.isSelected = com.a42r.pevplugshare.data.model.SocketType.WALL_SOCKET in currentFilter.socketTypes
        socketCaravan.isSelected = com.a42r.pevplugshare.data.model.SocketType.CARAVAN in currentFilter.socketTypes
        socketOther.isSelected = com.a42r.pevplugshare.data.model.SocketType.OTHER in currentFilter.socketTypes

        // Setup badge click handlers to toggle selection
        costFree.setOnClickListener { it.isSelected = !it.isSelected }
        costPaid.setOnClickListener { it.isSelected = !it.isSelected }
        accessPublic.setOnClickListener { it.isSelected = !it.isSelected }
        accessPrivate.setOnClickListener { it.isSelected = !it.isSelected }
        socketAS3112.setOnClickListener { it.isSelected = !it.isSelected }
        socketType2.setOnClickListener { it.isSelected = !it.isSelected }
        socketTesla.setOnClickListener { it.isSelected = !it.isSelected }
        socketCCS2.setOnClickListener { it.isSelected = !it.isSelected }
        socketCHAdeMO.setOnClickListener { it.isSelected = !it.isSelected }
        socketType1.setOnClickListener { it.isSelected = !it.isSelected }
        socketWallSocket.setOnClickListener { it.isSelected = !it.isSelected }
        socketCaravan.setOnClickListener { it.isSelected = !it.isSelected }
        socketOther.setOnClickListener { it.isSelected = !it.isSelected }

        // Update labels when slider changes
        socketsRangeSlider.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            minSocketsLabel.text = "Min: ${values[0].toInt()}"
            val maxValue = values[1].toInt()
            maxSocketsLabel.text = if (maxValue >= 10) "Max: 10+" else "Max: $maxValue"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Handle button clicks
        dialogView.findViewById<android.widget.Button>(R.id.resetButton).setOnClickListener {
            viewModel.resetFilters()
            dialog.dismiss()
        }

        dialogView.findViewById<android.widget.Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<android.widget.Button>(R.id.applyButton).setOnClickListener {
            // Gather filter settings from badges
            val costFilter = when {
                costFree.isSelected && costPaid.isSelected -> CostFilter.ALL
                costFree.isSelected -> CostFilter.FREE
                costPaid.isSelected -> CostFilter.PAID
                else -> CostFilter.ALL // If none selected, show all
            }

            val accessFilter = when {
                accessPublic.isSelected && accessPrivate.isSelected -> AccessFilter.ALL
                accessPublic.isSelected -> AccessFilter.PUBLIC
                accessPrivate.isSelected -> AccessFilter.PRIVATE
                else -> AccessFilter.ALL // If none selected, show all
            }

            val socketTypes = mutableSetOf<com.a42r.pevplugshare.data.model.SocketType>()
            if (socketAS3112.isSelected) socketTypes.add(com.a42r.pevplugshare.data.model.SocketType.AS3112)
            if (socketType2.isSelected) socketTypes.add(com.a42r.pevplugshare.data.model.SocketType.TYPE2)
            if (socketTesla.isSelected) socketTypes.add(com.a42r.pevplugshare.data.model.SocketType.TESLA)
            if (socketCCS2.isSelected) socketTypes.add(com.a42r.pevplugshare.data.model.SocketType.CCS2)
            if (socketCHAdeMO.isSelected) socketTypes.add(com.a42r.pevplugshare.data.model.SocketType.CHADEMO)
            if (socketType1.isSelected) socketTypes.add(com.a42r.pevplugshare.data.model.SocketType.TYPE1)
            if (socketWallSocket.isSelected) socketTypes.add(com.a42r.pevplugshare.data.model.SocketType.WALL_SOCKET)
            if (socketCaravan.isSelected) socketTypes.add(com.a42r.pevplugshare.data.model.SocketType.CARAVAN)
            if (socketOther.isSelected) socketTypes.add(com.a42r.pevplugshare.data.model.SocketType.OTHER)

            val values = socketsRangeSlider.values
            val newFilter = FilterState(
                costFilter = costFilter,
                openNowOnly = openNowCheckbox.isChecked,
                minSockets = values[0].toInt(),
                maxSockets = values[1].toInt(),
                accessFilter = accessFilter,
                socketTypes = socketTypes,
                favoritesOnly = false
            )

            viewModel.updateFilter(newFilter)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateFilterButtonsAppearance(filterState: FilterState) {
        // Update filter button - icon is already solid white via XML tint
        // Just clear any color filter to keep it pure white
        binding.toggleFilterButton.colorFilter = null

        // Update favorites button icon based on state
        // OFF = white filled with low opacity (almost invisible), ON = solid black filled
        if (filterState.favoritesOnly) {
            // Favorites filter is ON - use solid black filled icon
            binding.filterFavoritesButton.setImageResource(R.drawable.ic_favourite_filled)
            binding.filterFavoritesButton.colorFilter = null
        } else {
            // Favorites filter is OFF - use faded white icon
            binding.filterFavoritesButton.setImageResource(R.drawable.ic_favourite_faded)
            binding.filterFavoritesButton.colorFilter = null
        }
    }
}

