package com.a42r.pevplugshare.ui.edit

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.ai.LocationDescriptionGenerator
import com.a42r.pevplugshare.data.model.LocationAccessType
import com.a42r.pevplugshare.databinding.DialogAiDescriptionBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AiDescriptionDialogFragment : DialogFragment() {

    @Inject
    lateinit var descriptionGenerator: LocationDescriptionGenerator

    private var _binding: DialogAiDescriptionBinding? = null
    private val binding get() = _binding!!

    private var currentDescription: String = ""
    private val conversationHistory = mutableListOf<Pair<String, String>>()

    private var locationName: String = ""
    private var accessType: LocationAccessType = LocationAccessType.PUBLIC
    private var hasFee: Boolean = false
    private var chargingPoints: Int = 1
    private var socketTypes: List<String> = emptyList()
    private var openingHours: String? = null
    private var capacity: String? = null
    private var additionalInfo: String? = null
    private var originalDescription: String? = null

    private var onDescriptionAccepted: ((String) -> Unit)? = null

    companion object {
        private const val TAG = "AiDescriptionDialog"

        fun newInstance(
            locationName: String,
            accessType: LocationAccessType,
            hasFee: Boolean,
            chargingPoints: Int,
            socketTypes: List<String>,
            openingHours: String?,
            capacity: String?,
            additionalInfo: String?,
            originalDescription: String?,
            onAccept: (String) -> Unit
        ): AiDescriptionDialogFragment {
            return AiDescriptionDialogFragment().apply {
                this.locationName = locationName
                this.accessType = accessType
                this.hasFee = hasFee
                this.chargingPoints = chargingPoints
                this.socketTypes = socketTypes
                this.openingHours = openingHours
                this.capacity = capacity
                this.additionalInfo = additionalInfo
                this.originalDescription = originalDescription
                this.onDescriptionAccepted = onAccept
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAiDescriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), R.style.Theme_PEVPlugShare).apply {
            setCancelable(true)
            setCanceledOnTouchOutside(false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        generateInitialDescription()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.9).toInt()
        )
    }

    private fun setupUI() {
        binding.apply {
            oldDescriptionText.text = originalDescription ?: "No description"

            cancelButton.setOnClickListener {
                dismiss()
            }

            acceptButton.setOnClickListener {
                val finalDescription = newDescriptionText.text.toString()
                if (finalDescription.isNotBlank()) {
                    onDescriptionAccepted?.invoke(finalDescription)
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "Description cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }

            sendButton.setOnClickListener {
                val userMessage = chatInput.text.toString()
                if (userMessage.isNotBlank()) {
                    refineDescription(userMessage)
                    chatInput.text?.clear()
                }
            }

            // Enable send button only when there's text
            chatInput.doAfterTextChanged { text ->
                sendButton.isEnabled = !text.isNullOrBlank()
            }
            sendButton.isEnabled = false
        }
    }

    private fun generateInitialDescription() {
        binding.progressBar.visibility = View.VISIBLE
        binding.contentScrollView.visibility = View.GONE
        binding.chatInputContainer.visibility = View.GONE
        binding.buttonContainer.visibility = View.GONE

        Log.d(TAG, "=== GENERATING INITIAL DESCRIPTION ===")
        Log.d(TAG, "Location: $locationName")
        Log.d(TAG, "Access Type: $accessType")
        Log.d(TAG, "Has Fee: $hasFee")
        Log.d(TAG, "Charging Points: $chargingPoints")
        Log.d(TAG, "Socket Types: $socketTypes")
        Log.d(TAG, "Opening Hours: $openingHours")
        Log.d(TAG, "Capacity: $capacity")
        Log.d(TAG, "Additional Info: $additionalInfo")

        lifecycleScope.launch {
            val result = descriptionGenerator.generateDescription(
                locationName = locationName,
                accessType = accessType,
                hasFee = hasFee,
                chargingPoints = chargingPoints,
                socketTypes = socketTypes,
                openingHours = openingHours,
                capacity = capacity,
                additionalInfo = additionalInfo,
                description = originalDescription,
                conversationHistory = emptyList()
            )

            result.onSuccess { description ->
                Log.d(TAG, "Initial description generated successfully")
                Log.d(TAG, "Generated Description: $description")
                currentDescription = description
                conversationHistory.clear()
                binding.newDescriptionText.setText(description)
                showContent()
            }.onFailure { error ->
                Log.e(TAG, "Failed to generate initial description", error)
                Log.e(TAG, "Error message: ${error.message}")
                binding.progressBar.visibility = View.GONE
                binding.contentScrollView.visibility = View.VISIBLE
                binding.buttonContainer.visibility = View.VISIBLE
                binding.chatInputContainer.visibility = View.GONE

                binding.newDescriptionText.setText("Error: ${error.message ?: "Unknown error"}\n\nPlease check your OpenRouter.ai settings and try again.")

                Toast.makeText(
                    requireContext(),
                    "Failed to generate: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun refineDescription(userMessage: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.sendButton.isEnabled = false

        Log.d(TAG, "=== REFINING DESCRIPTION ===")
        Log.d(TAG, "User Message: $userMessage")
        Log.d(TAG, "Current Description: $currentDescription")

        // Add current description and user request to history
        conversationHistory.add(Pair(userMessage, currentDescription))

        lifecycleScope.launch {
            val result = descriptionGenerator.generateDescription(
                locationName = locationName,
                accessType = accessType,
                hasFee = hasFee,
                chargingPoints = chargingPoints,
                socketTypes = socketTypes,
                openingHours = openingHours,
                capacity = capacity,
                additionalInfo = additionalInfo,
                description = originalDescription,
                conversationHistory = conversationHistory
            )

            result.onSuccess { description ->
                Log.d(TAG, "Description refined successfully")
                Log.d(TAG, "Refined Description: $description")
                currentDescription = description
                binding.newDescriptionText.setText(description)
                binding.progressBar.visibility = View.GONE
                binding.sendButton.isEnabled = true
            }.onFailure { error ->
                Log.e(TAG, "Failed to refine description", error)
                Log.e(TAG, "Error message: ${error.message}")
                Toast.makeText(
                    requireContext(),
                    "Failed to refine description: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
                conversationHistory.removeLastOrNull() // Remove failed request
                binding.progressBar.visibility = View.GONE
                binding.sendButton.isEnabled = true
            }
        }
    }

    private fun showContent() {
        binding.progressBar.visibility = View.GONE
        binding.contentScrollView.visibility = View.VISIBLE
        binding.chatInputContainer.visibility = View.VISIBLE
        binding.buttonContainer.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
