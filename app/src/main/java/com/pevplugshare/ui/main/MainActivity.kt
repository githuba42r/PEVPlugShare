package com.pevplugshare.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import com.pevplugshare.R
import com.pevplugshare.data.model.Location
import com.pevplugshare.databinding.ActivityMainBinding
import com.pevplugshare.ui.edit.EditLocationActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var adapter: LocationAdapter? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            updateUserLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupUI()
        setupObservers()
        requestLocationPermission()
        
        // Validate session token in background and load data
        lifecycleScope.launch {
            val isValid = viewModel.validateSessionAndLoadData()
            if (!isValid) {
                showAuthenticationFailedDialog()
            }
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
                startActivity(Intent(this@MainActivity, com.pevplugshare.ui.quicklocation.QuickLocationActivity::class.java))
            }
            swipeRefreshLayout.setOnRefreshListener {
                viewModel.syncLocations()
            }
            locationRecyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.locationListState.collect { state ->
                when (state) {
                    is LocationListState.Loading -> {
                        binding.loadingProgressBar.visibility = android.view.View.VISIBLE
                        binding.locationRecyclerView.visibility = android.view.View.GONE
                        binding.emptyState.visibility = android.view.View.GONE
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                    is LocationListState.Success -> {
                        binding.loadingProgressBar.visibility = android.view.View.GONE
                        binding.locationRecyclerView.visibility = android.view.View.VISIBLE
                        binding.emptyState.visibility = android.view.View.GONE
                        binding.swipeRefreshLayout.isRefreshing = false

                        val userLoc = viewModel.userLocation.value ?: Pair(0.0, 0.0)
                        if (adapter == null) {
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
                        }
                        adapter?.submitList(state.locations)
                    }
                    is LocationListState.Empty -> {
                        binding.loadingProgressBar.visibility = android.view.View.GONE
                        binding.locationRecyclerView.visibility = android.view.View.GONE
                        binding.emptyState.visibility = android.view.View.VISIBLE
                        binding.emptyState.text = "No charging locations found.\n\nLog in to OSMAnd to sync your favorites, or tap + to add locations manually."
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                    is LocationListState.Error -> {
                        binding.loadingProgressBar.visibility = android.view.View.GONE
                        binding.locationRecyclerView.visibility = android.view.View.GONE
                        binding.emptyState.visibility = android.view.View.VISIBLE
                        binding.emptyState.text = "Error: ${state.message}"
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.selectedCategory.collect { category ->
                if (category != null) {
                    binding.categoryBadge.text = category
                    binding.categoryBadge.visibility = android.view.View.VISIBLE
                } else {
                    binding.categoryBadge.visibility = android.view.View.GONE
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
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_select_category -> {
                    showCategorySelectionDialog()
                    true
                }
                R.id.menu_settings -> {
                    startActivity(Intent(this, com.pevplugshare.ui.settings.SettingsActivity::class.java))
                    true
                }
                R.id.menu_search -> {
                    Toast.makeText(this, "Search", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_logout -> {
                    showLogoutConfirmation()
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
            val intent = Intent(this@MainActivity, com.pevplugshare.ui.auth.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showCategorySelectionDialog(mandatory: Boolean = false) {
        lifecycleScope.launch {
            binding.loadingProgressBar.visibility = android.view.View.VISIBLE
            
            // Refresh categories from server if not mandatory (mandatory means we already have them)
            if (!mandatory) {
                viewModel.refreshCategories()
            }
            
            // Wait for categories to be available (up to 5 seconds)
            var categories = viewModel.categories.value
            var attempts = 0
            while (categories.isEmpty() && attempts < 50) {
                kotlinx.coroutines.delay(100)
                categories = viewModel.categories.value
                attempts++
                android.util.Log.d("MainActivity", "Waiting for categories... attempt $attempts, current size: ${categories.size}")
            }
            
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
        
        val message = "A charging location finder for Personal Electric Vehicles.\n\n" +
                "Version: $versionName\n" +
                "Integration: OSMAnd (via Intent API)\n" +
                "Storage: Local database\n\n" +
                "Features:\n" +
                "• Find nearby charging locations\n" +
                "• Add new charging spots\n" +
                "• Navigate with OSMAnd\n" +
                "• Offline support"
        
        AlertDialog.Builder(this)
            .setTitle("PEVPlugShare")
            .setIcon(R.mipmap.ic_launcher)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun navigateToLocation(location: Location) {
        val success = com.pevplugshare.util.OsmandIntentHelper.navigateToLocation(
            this, 
            location,
            "bicycle" // Default profile for PEV
        )
        
        if (!success) {
            Toast.makeText(this, R.string.osmand_not_installed, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun editLocation(location: Location) {
        val intent = Intent(this, com.pevplugshare.ui.edit.EditLocationActivity::class.java)
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
        val intent = Intent(this, com.pevplugshare.ui.auth.LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun showPhotoGallery(location: Location) {
        android.util.Log.d("PhotoGallery", "=== showPhotoGallery called ===")
        android.util.Log.d("PhotoGallery", "Location: ${location.name} (ID: ${location.id})")
        
        val imagePaths = com.pevplugshare.util.ImageUtils.getLocationImages(this, location.id)
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
        
        android.util.Log.d("PhotoGallery", "Creating adapter with ${imagePaths.size} images")
        val adapter = com.pevplugshare.ui.adapter.ImageGalleryAdapter(imagePaths)
        viewPager.adapter = adapter
        viewPager.setCurrentItem(0, false)
        
        // Set location description
        locationDescView?.text = location.description ?: "No description available"
        
        // Set toolbar title to location name
        galleryToolbar?.title = location.name
        
        // Update counter
        imageCounter?.text = "1 / ${imagePaths.size}"
        
        android.util.Log.d("PhotoGallery", "Setting up page change listener")
        // Handle page changes
        viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                android.util.Log.d("PhotoGallery", "Page selected: ${position + 1} / ${imagePaths.size}")
                imageCounter?.text = "${position + 1} / ${imagePaths.size}"
            }
        })
        
        // Close button
        android.util.Log.d("PhotoGallery", "Setting up close button")
        galleryToolbar?.setNavigationOnClickListener {
            android.util.Log.d("PhotoGallery", "Close button clicked")
            galleryOverlay.visibility = android.view.View.GONE
            // Show FABs when gallery is closed
            binding.addLocationFab.show()
            binding.quickLocationButton.show()
        }
        
        // Also allow clicking overlay to close
        galleryOverlay.setOnClickListener {
            android.util.Log.d("PhotoGallery", "Overlay clicked to close")
            it.visibility = android.view.View.GONE
            // Show FABs when gallery is closed
            binding.addLocationFab.show()
            binding.quickLocationButton.show()
        }
        
        android.util.Log.d("PhotoGallery", "=== Modal opened successfully ===")
    }
}
