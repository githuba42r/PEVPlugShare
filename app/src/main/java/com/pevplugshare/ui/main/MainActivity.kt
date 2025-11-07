package com.pevplugshare.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import com.pevplugshare.ui.add.AddLocationActivity
import com.pevplugshare.ui.auth.LoginActivity
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
        viewModel.loadCategories()
    }

    private fun setupUI() {
        binding.apply {
            menuButton.setOnClickListener { showMenu() }
            addLocationFab.setOnClickListener {
                startActivity(Intent(this@MainActivity, AddLocationActivity::class.java))
            }
            refreshButton.setOnClickListener {
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
                    }
                    is LocationListState.Success -> {
                        binding.loadingProgressBar.visibility = android.view.View.GONE
                        binding.locationRecyclerView.visibility = android.view.View.VISIBLE
                        binding.emptyState.visibility = android.view.View.GONE

                        val userLoc = viewModel.userLocation.value ?: Pair(0.0, 0.0)
                        if (adapter == null) {
                            adapter = LocationAdapter(
                                onItemClick = { location -> showLocationDetails(location) },
                                onNavigateClick = { location -> navigateToLocation(location) },
                                onFavouriteClick = { location -> viewModel.toggleFavourite(location) },
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
                        binding.emptyState.text = "No charging locations found"
                    }
                    is LocationListState.Error -> {
                        binding.loadingProgressBar.visibility = android.view.View.GONE
                        binding.locationRecyclerView.visibility = android.view.View.GONE
                        binding.emptyState.visibility = android.view.View.VISIBLE
                        binding.emptyState.text = "Error: ${state.message}"
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isOffline.collect { isOffline ->
                binding.offlineIndicator.apply {
                    if (isOffline) {
                        text = "🔴 Offline"
                        setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red_closed))
                    } else {
                        text = "🟢 Online"
                        setTextColor(ContextCompat.getColor(this@MainActivity, R.color.green_open))
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.selectedCategory.collect { category ->
                binding.categoryName.text = category ?: "Select Category"
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
                R.id.menu_settings -> {
                    Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_search -> {
                    Toast.makeText(this, "Search", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_logout -> {
                    viewModel.logout()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun navigateToLocation(location: Location) {
        val gmmIntentUri = Uri.parse("google.navigation:q=${location.latitude},${location.longitude}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.osmand.plus")

        try {
            startActivity(mapIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "OSMAnd not installed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLocationDetails(location: Location) {
        Toast.makeText(this, "Location: ${location.name}", Toast.LENGTH_SHORT).show()
    }
}
