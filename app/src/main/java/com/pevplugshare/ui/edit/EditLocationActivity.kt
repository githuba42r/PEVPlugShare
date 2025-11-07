package com.pevplugshare.ui.edit

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
import com.pevplugshare.databinding.ActivityEditLocationBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditLocationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditLocationBinding
    private val viewModel: EditLocationViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationId: String by lazy {
        intent.getStringExtra("location_id") ?: ""
    }

    private val fineLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocation()
        } else {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupUI()
        setupObservers()

        if (locationId.isNotEmpty()) {
            viewModel.loadLocation(locationId)
        } else {
            Toast.makeText(this, "Invalid location ID", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupUI() {
        binding.apply {
            buttonGetLocation.setOnClickListener { requestLocationPermission() }
            buttonSubmit.setOnClickListener { submitUpdates() }
            buttonCancel.setOnClickListener { finish() }

            // Setup socket type spinner options
            val socketTypes = arrayOf("AS3112 (AU GPO)", "Type 2 EV", "Other EV Charger")
            val adapter = android.widget.ArrayAdapter(
                this@EditLocationActivity,
                android.R.layout.simple_spinner_item,
                socketTypes
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            socketTypeSpinner.adapter = adapter
            socketTypeSpinner.setSelection(0)
        }
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
                    is EditLocationState.Loaded -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.formContainer.visibility = android.view.View.VISIBLE
                        binding.buttonSubmit.isEnabled = true
                        populateFormFromLocation(state.location)
                    }
                    is EditLocationState.Updating -> {
                        binding.progressBar.visibility = android.view.View.VISIBLE
                        binding.buttonSubmit.isEnabled = false
                    }
                    is EditLocationState.UpdateSuccess -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        Toast.makeText(this@EditLocationActivity, "Location updated!", Toast.LENGTH_SHORT).show()
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
            gpoCountInput.setText(location.gpoCount.toString())
            descriptionInput.setText(location.description ?: "")
            openingHoursInput.setText(location.openingHours ?: "")
            additionalInfoInput.setText(location.additionalInfo ?: "")

            val socketTypeStr = location.socketTypes.firstOrNull()?.name ?: "AS3112"
            socketTypeSpinner.setSelection(getSocketTypeIndex(socketTypeStr))

            val isPublic = location.publicPrivate == com.pevplugshare.data.model.LocationAccessType.PUBLIC
            // Note: Toggle group needs proper ID setup in layout

            feeCheckbox.isChecked = location.hasFee
            if (location.hasFee) {
                feeDetailsSection.visibility = android.view.View.VISIBLE
                providerNameInput.setText(location.providerName ?: "")
                chargingCostsInput.setText(location.chargingCosts ?: "")
                providerWebsiteInput.setText(location.providerWebsite ?: "")
            }

            locationCoordinates.text = "📍 ${String.format("%.6f, %.6f", location.latitude, location.longitude)}"
            viewModel.updateLocation(location.latitude, location.longitude)
        }

        viewModel.updateName(location.name)
        viewModel.updateGpoCount(location.gpoCount)
        viewModel.updateDescription(location.description ?: "")
        viewModel.updateOpeningHours(location.openingHours ?: "")
        viewModel.updateAdditionalInfo(location.additionalInfo ?: "")
        val isPublic = location.publicPrivate == com.pevplugshare.data.model.LocationAccessType.PUBLIC
        viewModel.updateAccessType(if (isPublic) "public" else "private")
        viewModel.updateHasFee(location.hasFee)
        viewModel.updateProviderName(location.providerName ?: "")
        viewModel.updateChargingCosts(location.chargingCosts ?: "")
        viewModel.updateProviderWebsite(location.providerWebsite ?: "")
    }

    private fun updateUIFromFormData(data: EditLocationFormData) {
        binding.apply {
            nameInput.setOnFocusChangeListener { _, _ ->
                viewModel.updateName(nameInput.text.toString())
            }

            gpoCountInput.setOnFocusChangeListener { _, _ ->
                viewModel.updateGpoCount(gpoCountInput.text.toString().toIntOrNull() ?: 1)
            }

            descriptionInput.setOnFocusChangeListener { _, _ ->
                viewModel.updateDescription(descriptionInput.text.toString())
            }

            openingHoursInput.setOnFocusChangeListener { _, _ ->
                viewModel.updateOpeningHours(openingHoursInput.text.toString())
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

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        } else {
            fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.updateLocation(location.latitude, location.longitude)
                    binding.locationCoordinates.text = "📍 ${String.format("%.6f, %.6f", location.latitude, location.longitude)}"
                    Toast.makeText(this, "Location updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun submitUpdates() {
        viewModel.submitUpdates(locationId)
    }

    private fun getSocketTypeIndex(socketType: String): Int {
        return when (socketType.uppercase()) {
            "TYPE2_EV", "TYPE2" -> 1
            "OTHER_EV", "OTHER" -> 2
            else -> 0
        }
    }
}
