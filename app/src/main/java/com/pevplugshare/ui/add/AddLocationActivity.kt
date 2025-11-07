package com.pevplugshare.ui.add

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.pevplugshare.R
import com.pevplugshare.data.model.LocationAccessType
import com.pevplugshare.data.model.SocketType
import com.pevplugshare.databinding.ActivityAddLocationBinding
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
                        Toast.makeText(this@AddLocationActivity, "Location added successfully!", Toast.LENGTH_SHORT).show()
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
                    is AddLocationState.Duplicate -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.submitButton.isEnabled = true
                        Toast.makeText(this@AddLocationActivity, "Similar location already exists nearby", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
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
