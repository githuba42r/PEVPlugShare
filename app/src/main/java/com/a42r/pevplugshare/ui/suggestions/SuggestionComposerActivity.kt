package com.a42r.pevplugshare.ui.suggestions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.data.api.ServerLocation
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SuggestionComposerActivity : AppCompatActivity() {

    private val viewModel: SuggestionComposerViewModel by viewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var loadingProgress: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var currentNameLabel: TextView
    private lateinit var newNameInput: TextInputEditText
    private lateinit var currentDescriptionLabel: TextView
    private lateinit var newDescriptionInput: TextInputEditText
    private lateinit var currentOpeningHoursLabel: TextView
    private lateinit var newOpeningHoursInput: TextInputEditText
    private lateinit var currentSocketTypesLabel: TextView
    private lateinit var newSocketTypesInput: TextInputEditText
    private lateinit var currentHasFeeLabel: TextView
    private lateinit var newHasFeeInput: TextInputEditText
    private lateinit var currentChargingCostsLabel: TextView
    private lateinit var newChargingCostsInput: TextInputEditText
    private lateinit var currentPublicPrivateLabel: TextView
    private lateinit var newPublicPrivateInput: TextInputEditText
    private lateinit var messageInput: TextInputEditText
    private lateinit var submitButton: MaterialButton

    private var locationId: String = ""

    companion object {
        const val EXTRA_LOCATION_ID = "locationId"

        fun start(context: Context, locationId: String) {
            val intent = Intent(context, SuggestionComposerActivity::class.java)
            intent.putExtra(EXTRA_LOCATION_ID, locationId)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_suggestion_composer)

        locationId = intent.getStringExtra(EXTRA_LOCATION_ID) ?: ""
        if (locationId.isEmpty()) {
            Toast.makeText(this, "No location specified", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        setSupportActionBar(toolbar)

        loadingProgress = findViewById(R.id.loadingProgress)
        errorText = findViewById(R.id.errorText)
        currentNameLabel = findViewById(R.id.currentNameLabel)
        newNameInput = findViewById(R.id.newNameInput)
        currentDescriptionLabel = findViewById(R.id.currentDescriptionLabel)
        newDescriptionInput = findViewById(R.id.newDescriptionInput)
        currentOpeningHoursLabel = findViewById(R.id.currentOpeningHoursLabel)
        newOpeningHoursInput = findViewById(R.id.newOpeningHoursInput)
        currentSocketTypesLabel = findViewById(R.id.currentSocketTypesLabel)
        newSocketTypesInput = findViewById(R.id.newSocketTypesInput)
        currentHasFeeLabel = findViewById(R.id.currentHasFeeLabel)
        newHasFeeInput = findViewById(R.id.newHasFeeInput)
        currentChargingCostsLabel = findViewById(R.id.currentChargingCostsLabel)
        newChargingCostsInput = findViewById(R.id.newChargingCostsInput)
        currentPublicPrivateLabel = findViewById(R.id.currentPublicPrivateLabel)
        newPublicPrivateInput = findViewById(R.id.newPublicPrivateInput)
        messageInput = findViewById(R.id.messageInput)
        submitButton = findViewById(R.id.submitButton)

        submitButton.setOnClickListener {
            viewModel.submitSuggestion(
                locationId = locationId,
                newName = newNameInput.text?.toString() ?: "",
                newDescription = newDescriptionInput.text?.toString() ?: "",
                newOpeningHours = newOpeningHoursInput.text?.toString() ?: "",
                newSocketTypes = newSocketTypesInput.text?.toString() ?: "",
                newHasFee = newHasFeeInput.text?.toString() ?: "",
                newChargingCosts = newChargingCostsInput.text?.toString() ?: "",
                newPublicPrivate = newPublicPrivateInput.text?.toString() ?: "",
                message = messageInput.text?.toString() ?: ""
            )
        }

        observeState()
        viewModel.loadLocation(locationId)
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.loading.collect { loading ->
                loadingProgress.visibility = if (loading) View.VISIBLE else View.GONE
                submitButton.isEnabled = !loading
            }
        }
        lifecycleScope.launch {
            viewModel.error.collect { error ->
                if (error != null) {
                    errorText.visibility = View.VISIBLE
                    errorText.text = error
                } else {
                    errorText.visibility = View.GONE
                }
            }
        }
        lifecycleScope.launch {
            viewModel.location.collect { location ->
                if (location != null) {
                    populateCurrentValues(location)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.submitted.collect { suggestion ->
                if (suggestion != null) {
                    Toast.makeText(this@SuggestionComposerActivity, "Suggestion submitted!", Toast.LENGTH_SHORT).show()
                    viewModel.clearSubmitted()
                    finish()
                }
            }
        }
    }

    private fun populateCurrentValues(location: ServerLocation) {
        currentNameLabel.text = "Current: ${location.name}"
        currentDescriptionLabel.text = "Current: ${location.description ?: "(none)"}"
        currentOpeningHoursLabel.text = "Current: ${location.openingHours ?: "(none)"}"
        currentSocketTypesLabel.text = "Current: ${location.socketTypes ?: "(none)"}"
        currentHasFeeLabel.text = "Current: ${location.hasFee?.toString() ?: "(none)"}"
        currentChargingCostsLabel.text = "Current: ${location.chargingCosts ?: "(none)"}"
        currentPublicPrivateLabel.text = "Current: ${location.publicPrivate ?: "(none)"}"
    }
}
