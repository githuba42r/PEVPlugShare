package com.a42r.pevplugshare.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.a42r.pevplugshare.databinding.ActivitySettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    @Inject
    lateinit var tokenManager: com.a42r.pevplugshare.data.remote.TokenManager

    private val okHttpClient = OkHttpClient()
    private var availableModels = listOf<com.a42r.pevplugshare.ui.settings.model.OpenRouterModel>()
    private var modelPricingMap = mutableMapOf<String, com.a42r.pevplugshare.ui.settings.model.ModelPricing>()
    private var modelDataMap = mutableMapOf<String, com.a42r.pevplugshare.ui.settings.model.OpenRouterModel>()
    private var modelChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupUI()
        checkExistingApiKey()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupUI() {
        binding.apply {
            // Get API Key button - opens OpenRouter.ai website
            getApiKeyButton.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://openrouter.ai/keys")
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Could not open browser. Please visit: https://openrouter.ai/keys",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            // Validate & Save button (next to API key input)
            validateSaveButton.setOnClickListener {
                val apiKey = apiKeyInput.text?.toString()?.trim()
                if (apiKey.isNullOrEmpty()) {
                    apiKeyLayout.error = "Please enter an API key"
                    return@setOnClickListener
                }

                apiKeyLayout.error = null
                validateAndSaveApiKey(apiKey)
            }
                            // Disable validate button if API Key is empty
                            apiKeyInput.addTextChangedListener(object : android.text.TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                                override fun afterTextChanged(s: android.text.Editable?) {
                                    validateSaveButton.isEnabled = !s.isNullOrBlank()
                                }
                            })
                            // Initial state
                            validateSaveButton.isEnabled = !apiKeyInput.text.isNullOrBlank()

            // Reset/Update API Key button
            resetApiKeyButton.setOnClickListener {
                showApiKeyEntry()
            }

            // Model selection card click
            selectedModelCard.setOnClickListener {
                if (availableModels.isNotEmpty()) {
                    showModelSelectionDialog()
                }
            }

            // Load saved zoom level
            val savedZoom = tokenManager.getMapZoomLevel()
            zoomLevelSlider.value = savedZoom
            zoomLevelValue.text = String.format("%.1f", savedZoom)

            // Zoom level slider
            zoomLevelSlider.addOnChangeListener { _, value, _ ->
                zoomLevelValue.text = String.format("%.1f", value)
            }

            // Load OsmAnd sync settings
            switchPromptOsmandSync.isChecked = tokenManager.getSyncToOsmandOnUpdate()
            switchVerifyDescription.isChecked = tokenManager.getVerifyDescriptionBeforeSync()

            // Load OsmAnd location match distance
            val savedMatchDistance = tokenManager.getOsmandLocationMatchDistance()
            osmandMatchDistanceSlider.value = savedMatchDistance.toFloat()
            osmandMatchDistanceValue.text = "${savedMatchDistance}m"

            // OsmAnd sync switches
            switchPromptOsmandSync.setOnCheckedChangeListener { _, isChecked ->
                tokenManager.setSyncToOsmandOnUpdate(isChecked)
            }

            switchVerifyDescription.setOnCheckedChangeListener { _, isChecked ->
                tokenManager.setVerifyDescriptionBeforeSync(isChecked)
            }

            // OsmAnd match distance slider
            osmandMatchDistanceSlider.addOnChangeListener { _, value, _ ->
                val meters = value.toInt()
                osmandMatchDistanceValue.text = "${meters}m"
            }

            // Save button
            saveSettingsButton.setOnClickListener {
                saveSettings()
            }

            // Reset button
            resetButton.setOnClickListener {
                resetSettings()
            }
        }
    }

    private fun saveSettings() {
        binding.apply {
            // Save zoom level
            val zoomLevel = zoomLevelSlider.value
            tokenManager.saveMapZoomLevel(zoomLevel)

            // Save OsmAnd match distance
            val matchDistance = osmandMatchDistanceSlider.value.toInt()
            tokenManager.setOsmandLocationMatchDistance(matchDistance)

            Toast.makeText(this@SettingsActivity, "Settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun resetSettings() {
        binding.apply {
            // Reset to default zoom level (18.0)
            zoomLevelSlider.value = 18.0f
            zoomLevelValue.text = "18.0"

            // Reset OsmAnd sync settings to defaults
            switchPromptOsmandSync.isChecked = true
            switchVerifyDescription.isChecked = true
            tokenManager.setSyncToOsmandOnUpdate(true)
            tokenManager.setVerifyDescriptionBeforeSync(true)

            // Reset OsmAnd match distance to default (10m)
            osmandMatchDistanceSlider.value = 10f
            osmandMatchDistanceValue.text = "10m"
            tokenManager.setOsmandLocationMatchDistance(10)

            Toast.makeText(this@SettingsActivity, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showModelSelectionDialog() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this).create()
        val dialogView = layoutInflater.inflate(com.a42r.pevplugshare.R.layout.dialog_model_selection, null)
        dialog.setView(dialogView)

        val searchInput = dialogView.findViewById<android.widget.EditText>(
            com.a42r.pevplugshare.R.id.searchInput
        )
        val clearSearchButton = dialogView.findViewById<android.widget.ImageButton>(
            com.a42r.pevplugshare.R.id.clearSearchButton
        )
        val freeOnlyCheckbox = dialogView.findViewById<android.widget.CheckBox>(
            com.a42r.pevplugshare.R.id.freeOnlyCheckbox
        )
        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(
            com.a42r.pevplugshare.R.id.modelsRecyclerView
        )
        val selectButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(
            com.a42r.pevplugshare.R.id.selectButton
        )
        val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(
            com.a42r.pevplugshare.R.id.cancelButton
        )

        // Convert OpenRouterModel objects to AiModel objects
        val aiModels = availableModels.map { openRouterModel ->
            createAiModelFromId(openRouterModel)
        }

        val currentSelectedModel = tokenManager.getSelectedAiModel()

        val adapter = com.a42r.pevplugshare.ui.settings.model.ModelSelectionAdapter(
            aiModels,
            currentSelectedModel,
            { selectedModel ->
                // Model selected callback
                android.util.Log.d("SettingsActivity", "Model clicked: ${selectedModel.name}")
            },
            { modelId ->
                // Open model page on OpenRouter.ai
                val url = "https://openrouter.ai/models/${modelId.replace("/", "--")}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not open browser", Toast.LENGTH_SHORT).show()
                }
            }
        )

        val layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        // Scroll to selected model if available
        currentSelectedModel?.let { selectedId ->
            val selectedPosition = aiModels.indexOfFirst { it.id == selectedId }
            if (selectedPosition != -1) {
                layoutManager.scrollToPositionWithOffset(selectedPosition, 0)
            }
        }

        // Filter function
        val applyFilter = {
            val query = searchInput.text?.toString() ?: ""
            val freeOnly = freeOnlyCheckbox.isChecked
            adapter.filter(query, freeOnly)

            // Show/hide clear button
            clearSearchButton.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
        }

        // Clear search button
        clearSearchButton.setOnClickListener {
            searchInput.setText("")
        }

        // Search functionality
        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                applyFilter()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Free only checkbox
        freeOnlyCheckbox.setOnCheckedChangeListener { _, _ ->
            applyFilter()
        }

        // Select button
        selectButton.setOnClickListener {
            val selected = adapter.getSelectedModel()
            if (selected != null) {
                tokenManager.saveSelectedAiModel(selected.id)
                updateSelectedModelDisplay(selected)
                Toast.makeText(this, "Model selected: ${selected.name}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please select a model", Toast.LENGTH_SHORT).show()
            }
        }

        // Cancel button
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateBottomButtons() {
        binding.bottomButtons.visibility = if (modelChanged) View.VISIBLE else View.GONE
    }

    private fun checkExistingApiKey() {
        val existingApiKey = tokenManager.getOpenRouterApiKey()
        if (!existingApiKey.isNullOrEmpty()) {
            // Have a key, validate it
            validateAndSaveApiKey(existingApiKey, silent = true)
        } else {
            // No key, show entry form
            showApiKeyEntry()
        }
    }

    private fun showApiKeyEntry() {
        binding.apply {
            apiKeyEntrySection.visibility = View.VISIBLE
            apiKeyValidSection.visibility = View.GONE
            bottomButtons.visibility = View.GONE
            loadingProgressBar.visibility = View.GONE
            statusMessage.text = "Enter your OpenRouter.ai API key to enable AI features"
            statusMessage.setTextColor(getColor(android.R.color.darker_gray))
            apiKeyInput.setText("")

            // Check clipboard for API key
            val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            if (clipboardManager.hasPrimaryClip() && clipboardManager.primaryClipDescription?.hasMimeType(android.content.ClipDescription.MIMETYPE_TEXT_PLAIN) == true) {
                val clipboardText = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
                if (!clipboardText.isNullOrBlank() && clipboardText.startsWith("sk-or-")) {
                    apiKeyInput.setText(clipboardText)
                    Toast.makeText(this@SettingsActivity, "API key pasted from clipboard", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showValidApiKey(models: List<com.a42r.pevplugshare.ui.settings.model.OpenRouterModel>) {
        binding.apply {
            apiKeyEntrySection.visibility = View.GONE
            apiKeyValidSection.visibility = View.VISIBLE
            loadingProgressBar.visibility = View.GONE
            bottomButtons.visibility = View.GONE

            // Store available models
            availableModels = models

            // Update selected model display
            val savedModelId = tokenManager.getSelectedAiModel()
            if (savedModelId != null) {
                val savedOpenRouterModel = modelDataMap[savedModelId]
                if (savedOpenRouterModel != null) {
                    val savedModel = createAiModelFromId(savedOpenRouterModel)
                    updateSelectedModelDisplay(savedModel)
                } else {
                    // Fallback if model not found in map (shouldn't happen if API call is fresh)
                    binding.selectedModelName.text = "Tap to select a model"
                    binding.selectedModelProvider.text = ""
                    binding.selectedModelCost.text = ""
                    binding.selectedModelContext.visibility = View.GONE
                }
            } else {
                // No model selected yet
                binding.selectedModelName.text = "Tap to select a model"
                binding.selectedModelProvider.text = ""
                binding.selectedModelCost.text = ""
                binding.selectedModelContext.visibility = View.GONE
            }

            statusMessage.text = ""
            modelChanged = false
        }
    }

    private fun updateSelectedModelDisplay(model: com.a42r.pevplugshare.ui.settings.model.AiModel) {
        binding.apply {
            selectedModelName.text = model.name
            selectedModelProvider.text = model.provider
            selectedModelCost.text = model.cost

            if (model.contextSize != null) {
                selectedModelContext.text = "• ${model.contextSize}"
                selectedModelContext.visibility = View.VISIBLE
            } else {
                selectedModelContext.visibility = View.GONE
            }
        }
    }

    private fun createAiModelFromId(openRouterModel: com.a42r.pevplugshare.ui.settings.model.OpenRouterModel): com.a42r.pevplugshare.ui.settings.model.AiModel {
        val modelId = openRouterModel.id
        val parts = modelId.split("/")
        val provider = parts.getOrNull(0)?.capitalize() ?: "Unknown"

        val pricing = openRouterModel.pricing
        val costDisplay = if (pricing != null) {
            if (pricing.prompt == "Free" && pricing.completion == "Free") {
                "Free"
            } else {
                "${pricing.prompt}-${pricing.completion}/M tokens"
            }
        } else {
            getModelCost(modelId) // Fallback to hardcoded prices
        }

        val contextSizeDisplay = openRouterModel.context_length?.let { "${it / 1000}K tokens" }

        return com.a42r.pevplugshare.ui.settings.model.AiModel(
            id = modelId,
            name = formatModelDisplayName(modelId).split(" (").first(),
            provider = provider,
            cost = costDisplay,
            contextSize = contextSizeDisplay,
            description = openRouterModel.description, // Pass the description along
            pricing = pricing
        )
    }



    private fun formatModelDisplayName(modelId: String): String {
        // Extract provider and model name
        val parts = modelId.split("/")
        val provider = parts.getOrNull(0)?.capitalize() ?: ""
        val modelName = parts.getOrNull(1) ?: modelId

        // Convert model name to friendly format
        val friendlyName = when {
            modelName.contains("gpt-4o", ignoreCase = true) -> "GPT-4 Omni"
            modelName.contains("gpt-4-turbo", ignoreCase = true) -> "GPT-4 Turbo"
            modelName.contains("gpt-4", ignoreCase = true) -> "GPT-4"
            modelName.contains("gpt-3.5-turbo", ignoreCase = true) -> "GPT-3.5 Turbo"
            modelName.contains("gpt-3.5", ignoreCase = true) -> "GPT-3.5"
            modelName.contains("claude-3-opus", ignoreCase = true) -> "Claude 3 Opus"
            modelName.contains("claude-3-sonnet", ignoreCase = true) -> "Claude 3 Sonnet"
            modelName.contains("claude-3-haiku", ignoreCase = true) -> "Claude 3 Haiku"
            modelName.contains("claude-3.5-sonnet", ignoreCase = true) -> "Claude 3.5 Sonnet"
            modelName.contains("claude-2", ignoreCase = true) -> "Claude 2"
            modelName.contains("gemini-pro", ignoreCase = true) -> "Gemini Pro"
            modelName.contains("gemini-1.5-pro", ignoreCase = true) -> "Gemini 1.5 Pro"
            modelName.contains("gemini-1.5-flash", ignoreCase = true) -> "Gemini 1.5 Flash"
            modelName.contains("gemini-flash", ignoreCase = true) -> "Gemini Flash"
            modelName.contains("llama-3.1-405b", ignoreCase = true) -> "Llama 3.1 405B"
            modelName.contains("llama-3.1-70b", ignoreCase = true) -> "Llama 3.1 70B"
            modelName.contains("llama-3.1-8b", ignoreCase = true) -> "Llama 3.1 8B"
            modelName.contains("llama-3-70b", ignoreCase = true) -> "Llama 3 70B"
            modelName.contains("llama-3-8b", ignoreCase = true) -> "Llama 3 8B"
            modelName.contains("llama-2-70b", ignoreCase = true) -> "Llama 2 70B"
            modelName.contains("mistral-large", ignoreCase = true) -> "Mistral Large"
            modelName.contains("mistral-medium", ignoreCase = true) -> "Mistral Medium"
            modelName.contains("mistral-small", ignoreCase = true) -> "Mistral Small"
            modelName.contains("mixtral-8x7b", ignoreCase = true) -> "Mixtral 8x7B"
            modelName.contains("mixtral-8x22b", ignoreCase = true) -> "Mixtral 8x22B"
            else -> modelName.replace("-", " ").split(" ")
                .joinToString(" ") { it.capitalize() }
        }

        // Get approximate cost
        val cost = getModelCost(modelId)

        return if (provider.isNotEmpty() && provider.lowercase() != "openai" &&
                   provider.lowercase() != "anthropic" && provider.lowercase() != "google" &&
                   provider.lowercase() != "meta" && provider.lowercase() != "mistralai") {
            "$friendlyName by $provider ($cost)"
        } else {
            "$friendlyName ($cost)"
        }
    }

    private fun getModelCost(modelId: String): String {
        val lowerModelId = modelId.lowercase()

        return when {
            // Free models
            lowerModelId.contains("free") -> "Free"

            // Premium models - expensive
            lowerModelId.contains("gpt-4o") && !lowerModelId.contains("mini") -> "$5-15/M tokens"
            lowerModelId.contains("gpt-4-turbo") -> "$10-30/M tokens"
            lowerModelId.contains("gpt-4") && !lowerModelId.contains("turbo") -> "$30-60/M tokens"
            lowerModelId.contains("claude-3-opus") || lowerModelId.contains("claude-opus") -> "$15-75/M tokens"
            lowerModelId.contains("claude-3.5-sonnet") -> "$3-15/M tokens"
            lowerModelId.contains("claude-3-sonnet") || lowerModelId.contains("claude-sonnet") -> "$3-15/M tokens"
            lowerModelId.contains("llama-3.1-405b") -> "$3-5/M tokens"

            // Mid-range models
            lowerModelId.contains("gpt-3.5-turbo") || lowerModelId.contains("gpt-3.5") -> "$0.5-2/M tokens"
            lowerModelId.contains("gpt-4o-mini") -> "$0.15-0.6/M tokens"
            lowerModelId.contains("claude-3-haiku") || lowerModelId.contains("claude-haiku") -> "$0.25-1.25/M tokens"
            lowerModelId.contains("gemini-1.5-pro") || lowerModelId.contains("gemini-pro") -> "$1.25-5/M tokens"
            lowerModelId.contains("gemini-1.5-flash") || lowerModelId.contains("gemini-flash") -> "$0.075-0.3/M tokens"
            lowerModelId.contains("mixtral-8x22b") -> "$0.65-0.65/M tokens"
            lowerModelId.contains("mixtral-8x7b") -> "$0.24-0.24/M tokens"

            // Budget models
            lowerModelId.contains("llama-3.1-70b") -> "$0.35-0.4/M tokens"
            lowerModelId.contains("llama-3.1-8b") -> "$0.06-0.06/M tokens"
            lowerModelId.contains("llama-3-70b") -> "$0.7-0.8/M tokens"
            lowerModelId.contains("llama-3-8b") -> "$0.06-0.2/M tokens"
            lowerModelId.contains("llama-2-70b") -> "$0.7-0.9/M tokens"
            lowerModelId.contains("mistral-large") -> "$2-6/M tokens"
            lowerModelId.contains("mistral-medium") -> "$2.7-8.1/M tokens"
            lowerModelId.contains("mistral-small") -> "$0.2-0.6/M tokens"

            else -> "Varies"
        }
    }

    private fun validateAndSaveApiKey(apiKey: String, silent: Boolean = false) {
        binding.loadingProgressBar.visibility = View.VISIBLE
        binding.statusMessage.text = "Validating API key..."
        binding.statusMessage.setTextColor(getColor(android.R.color.darker_gray))

        lifecycleScope.launch {
            try {
                val models = fetchAvailableModels(apiKey)
                if (models.isNotEmpty()) {
                    // Valid key, save it immediately
                    tokenManager.saveOpenRouterApiKey(apiKey)
                    showValidApiKey(models)
                    if (!silent) {
                        Toast.makeText(
                            this@SettingsActivity,
                            "API key validated and saved successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    showError("API key valid but no models available")
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsActivity", "API key validation failed: ${e.message}", e)
                if (silent) {
                    // If silent check fails, clear the invalid key and show entry
                    tokenManager.saveOpenRouterApiKey("")
                    showApiKeyEntry()
                } else {
                    showError("Invalid or expired API key")
                }
            }
        }
    }

    private fun showError(message: String) {
        binding.apply {
            loadingProgressBar.visibility = View.GONE
            statusMessage.text = "⚠ $message"
            statusMessage.setTextColor(getColor(android.R.color.holo_red_dark))
            apiKeyLayout.error = "Validation failed"

            // Keep showing entry section
            apiKeyEntrySection.visibility = View.VISIBLE
            apiKeyValidSection.visibility = View.GONE
            bottomButtons.visibility = View.GONE
        }
    }

    private suspend fun fetchAvailableModels(apiKey: String): List<com.a42r.pevplugshare.ui.settings.model.OpenRouterModel> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/models")
                .header("Authorization", "Bearer $apiKey")
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}")
            }

            val jsonResponse = response.body?.string() ?: throw Exception("Empty response")
            val json = JSONObject(jsonResponse)
            val dataArray = json.getJSONArray("data")

            val models = mutableListOf<com.a42r.pevplugshare.ui.settings.model.OpenRouterModel>()
            modelPricingMap.clear()
            modelDataMap.clear() // Clear modelDataMap as well

            for (i in 0 until dataArray.length()) {
                val modelJson = dataArray.getJSONObject(i)
                val modelId = modelJson.getString("id")
                val contextLength = modelJson.optInt("context_length", -1).takeIf { it != -1 }
                val description = modelJson.optString("description", null)

                var pricing: com.a42r.pevplugshare.ui.settings.model.ModelPricing? = null
                // Extract pricing information
                if (modelJson.has("pricing")) {
                    val pricingJson = modelJson.getJSONObject("pricing")
                    val promptCost = pricingJson.optString("prompt", "0")
                    val completionCost = pricingJson.optString("completion", "0")

                    // Convert to per million tokens and format
                    val promptPerMillion = (promptCost.toDoubleOrNull() ?: 0.0) * 1000000
                    val completionPerMillion = (completionCost.toDoubleOrNull() ?: 0.0) * 1000000

                    pricing = com.a42r.pevplugshare.ui.settings.model.ModelPricing(
                        prompt = formatCost(promptPerMillion),
                        completion = formatCost(completionPerMillion)
                    )
                    modelPricingMap[modelId] = pricing
                }

                val openRouterModel = com.a42r.pevplugshare.ui.settings.model.OpenRouterModel(
                    id = modelId,
                    context_length = contextLength,
                    description = description,
                    pricing = pricing
                )
                modelDataMap[modelId] = openRouterModel

                // Filter for free or commonly used models
                if (modelId.contains("free", ignoreCase = true) ||
                    modelId.contains("gpt", ignoreCase = true) ||
                    modelId.contains("claude", ignoreCase = true) ||
                    modelId.contains("llama", ignoreCase = true) ||
                    modelId.contains("mistral", ignoreCase = true) ||
                    modelId.contains("gemini", ignoreCase = true)) {
                    models.add(openRouterModel)
                }
            }

            // If no filtered models, return first 30
            if (models.isEmpty()) {
                for (i in 0 until minOf(30, dataArray.length())) {
                    val modelJson = dataArray.getJSONObject(i)
                    val modelId = modelJson.getString("id")
                    val contextLength = modelJson.optInt("context_length", -1).takeIf { it != -1 }
                    val description = modelJson.optString("description", null)

                    var pricing: com.a42r.pevplugshare.ui.settings.model.ModelPricing? = null
                    if (modelJson.has("pricing")) {
                        val pricingJson = modelJson.getJSONObject("pricing")
                        val promptCost = pricingJson.optString("prompt", "0")
                        val completionCost = pricingJson.optString("completion", "0")
                        val promptPerMillion = (promptCost.toDoubleOrNull() ?: 0.0) * 1000000
                        val completionPerMillion = (completionCost.toDoubleOrNull() ?: 0.0) * 1000000
                        pricing = com.a42r.pevplugshare.ui.settings.model.ModelPricing(
                            prompt = formatCost(promptPerMillion),
                            completion = formatCost(completionPerMillion)
                        )
                    }
                    models.add(com.a42r.pevplugshare.ui.settings.model.OpenRouterModel(
                        id = modelId,
                        context_length = contextLength,
                        description = description,
                        pricing = pricing
                    ))
                }
            }

            models
        }
    }

    private fun formatCost(costPerMillion: Double): String {
        return when {
            costPerMillion == 0.0 -> "Free"
            costPerMillion < 0.01 -> String.format("$%.4f", costPerMillion)
            costPerMillion < 1.0 -> String.format("$%.2f", costPerMillion)
            else -> String.format("$%.0f", costPerMillion)
        }
    }
}
