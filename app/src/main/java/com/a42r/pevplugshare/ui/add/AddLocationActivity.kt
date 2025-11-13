package com.a42r.pevplugshare.ui.add

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.data.model.LocationAccessType
import com.a42r.pevplugshare.data.model.SocketType
import com.a42r.pevplugshare.databinding.ActivityAddLocationBinding
import com.a42r.pevplugshare.util.DistanceCalculator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddLocationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddLocationBinding
    private val viewModel: AddLocationViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getLocationAndPopulate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupUI()
        setupObservers()
        requestLocationAndPopulate()
    }

    private fun setupUI() {
        binding.apply {
            // Get current location button
            getLocationButton.setOnClickListener {
                requestLocationAndPopulate()
            }

            // Socket type spinners
            socketTypeSpinner.adapter = android.widget.ArrayAdapter(
                this@AddLocationActivity,
                android.R.layout.simple_spinner_item,
                SocketType.values().map { it.displayName }
            )

            // Access type toggle
            accessTypeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    val accessType = when (checkedId) {
                        R.id.publicButton -> LocationAccessType.PUBLIC
                        R.id.privateButton -> LocationAccessType.PRIVATE
                        else -> LocationAccessType.PUBLIC
                    }
                    viewModel.updateAccessType(accessType)
                }
            }

            // Fee checkbox
            feeCheckbox.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateHasFee(isChecked)
                feeDetailsSection.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
            }

            // Submit button
            submitButton.setOnClickListener {
                collectFormData()
                viewModel.submitLocation()
            }

            // Cancel button
            cancelButton.setOnClickListener {
                finish()
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.addLocationState.collect { state ->
                when (state) {
                    is AddLocationState.Loading -> {
                        binding.submitButton.isEnabled = false
                        binding.progressBar.visibility = android.view.View.VISIBLE
                    }
                    is AddLocationState.Success -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        
                        // Add to OSMAnd favorites
                        state.location?.let { location ->
                            val added = com.a42r.pevplugshare.util.OsmandIntentHelper.addFavorite(
                                this@AddLocationActivity,
                                location
                            )
                            if (added) {
                                Toast.makeText(
                                    this@AddLocationActivity, 
                                    "Location added successfully and exported to OSMAnd!", 
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@AddLocationActivity, 
                                    "Location added successfully!", 
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } ?: run {
                            Toast.makeText(
                                this@AddLocationActivity, 
                                "Location added successfully!", 
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        
                        viewModel.resetForm()
                        finish()
                    }
                    is AddLocationState.Error -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.submitButton.isEnabled = true
                        Toast.makeText(this@AddLocationActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    is AddLocationState.Idle -> {
                        binding.submitButton.isEnabled = true
                        binding.progressBar.visibility = android.view.View.GONE
                    }
                    is AddLocationState.NearbyLocationsFound -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.submitButton.isEnabled = true
                        showNearbyLocationsDialog(state.locations)
                    }
                }
            }
        }
    }
    
    private fun showNearbyLocationsDialog(nearbyLocations: List<Pair<com.a42r.pevplugshare.data.model.Location, Double>>) {
        val locationsList = nearbyLocations.joinToString("\n\n") { (location, distance) ->
            "• ${location.name}\n  Distance: ${DistanceCalculator.formatDistance(distance)}"
        }
        
        AlertDialog.Builder(this)
            .setTitle("Nearby Locations Found")
            .setMessage("The following charging locations are within 250m:\n\n$locationsList\n\nDo you still want to add this location?")
            .setPositiveButton("Yes, Add Anyway") { _, _ ->
                viewModel.saveLocationAnyway()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                viewModel.resetForm()
            }
            .setCancelable(false)
            .show()
    }

    private fun requestLocationAndPopulate() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getLocationAndPopulate()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun getLocationAndPopulate() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location ->
            location?.let {
                viewModel.setCurrentLocation(it.latitude, it.longitude)
                binding.locationCoordinates.text =
                    "📍 ${String.format("%.4f", it.latitude)}, ${String.format("%.4f", it.longitude)}"
            } ?: run {
                Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun collectFormData() {
        val formData = viewModel.formData.value.copy(
            name = binding.locationNameInput.text.toString(),
            gpoCount = binding.gpoCountInput.text.toString().toIntOrNull() ?: 1,
            socketTypes = listOf(
                when (binding.socketTypeSpinner.selectedItemPosition) {
                    0 -> SocketType.AS3112
                    1 -> SocketType.TYPE2
                    else -> SocketType.OTHER
                }
            ),
            description = binding.descriptionInput.text.toString(),
            openingHours = binding.openingHoursInput.text.toString(),
            additionalInfo = binding.additionalInfoInput.text.toString(),
            providerName = binding.providerNameInput.text.toString(),
            providerWebsite = binding.providerWebsiteInput.text.toString(),
            chargingCosts = binding.chargingCostsInput.text.toString()
        )
        viewModel.updateFormData(formData)
    }
}
