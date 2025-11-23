package com.a42r.pevplugshare.ui.oauth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.databinding.ActivityOauthBinding
import com.a42r.pevplugshare.viewmodel.OpenRouterOAuthViewModel
import com.a42r.pevplugshare.data.remote.TokenManager
import com.a42r.pevplugshare.ui.settings.model.AiModel
import com.a42r.pevplugshare.ui.settings.model.ModelSelectionAdapter
import com.a42r.pevplugshare.ui.settings.model.OpenRouterModel
import com.a42r.pevplugshare.ui.settings.model.ModelPricing
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject

/**
 * Activity for OpenRouter OAuth2 authentication with PKCE
 *
 * This activity demonstrates the complete OAuth flow:
 * 1. Register session with callback service
 * 2. Open OpenRouter authorization page
 * 3. Handle callback with authorization code
 * 4. Exchange code for API key
 * 5. Select AI model for location descriptions
 */
@AndroidEntryPoint
class OAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOauthBinding
    private val viewModel: OpenRouterOAuthViewModel by viewModels()
    private val okHttpClient = OkHttpClient()
    private var modelDataMap = mutableMapOf<String, OpenRouterModel>()

    @Inject
    lateinit var tokenManager: TokenManager

    companion object {
        private const val DEFAULT_MODEL = "google/gemini-2.0-flash-exp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOauthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
        setupModelSelection()

        // Handle deep link callback if this is a callback intent
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "AI Settings"
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnStartOauth.setOnClickListener {
            viewModel.startOAuthFlow()
        }

        binding.btnDisconnect.setOnClickListener {
            showDisconnectConfirmationDialog()
        }

        binding.tvLearnMore.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://openrouter.ai"))
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open browser", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.oauthState.observe(this) { state ->
            updateUI(state)

            when (state) {
                is OpenRouterOAuthViewModel.OAuthState.ReadyToAuthorize -> {
                    // Automatically open authorization URL
                    viewModel.openAuthorizationUrl(state.authUrl)
                }
                is OpenRouterOAuthViewModel.OAuthState.Success -> {
                    Toast.makeText(this, "✅ Connected to OpenRouter successfully!", Toast.LENGTH_LONG).show()
                }
                is OpenRouterOAuthViewModel.OAuthState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }

        viewModel.isAuthenticated.observe(this) { isAuthenticated ->
            updateAuthenticationUI(isAuthenticated)
        }
    }

    private fun updateUI(state: OpenRouterOAuthViewModel.OAuthState) {
        when (state) {
            is OpenRouterOAuthViewModel.OAuthState.Idle -> {
                binding.tvStatus.text = "Not connected"
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnStartOauth.isEnabled = true
            }
            is OpenRouterOAuthViewModel.OAuthState.Registering -> {
                binding.tvStatus.text = "Registering OAuth session..."
                binding.progressBar.visibility = android.view.View.VISIBLE
                binding.btnStartOauth.isEnabled = false
            }
            is OpenRouterOAuthViewModel.OAuthState.ReadyToAuthorize -> {
                binding.tvStatus.text = "Opening browser for authorization..."
                binding.progressBar.visibility = android.view.View.VISIBLE
            }
            is OpenRouterOAuthViewModel.OAuthState.ExchangingCode -> {
                binding.tvStatus.text = "Exchanging authorization code..."
                binding.progressBar.visibility = android.view.View.VISIBLE
            }
            is OpenRouterOAuthViewModel.OAuthState.Success -> {
                binding.tvStatus.text = "✅ Connected to OpenRouter"
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnStartOauth.isEnabled = false
            }
            is OpenRouterOAuthViewModel.OAuthState.Authenticated -> {
                binding.tvStatus.text = "✅ Connected to OpenRouter"
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnStartOauth.isEnabled = false
            }
            is OpenRouterOAuthViewModel.OAuthState.Error -> {
                binding.tvStatus.text = "❌ Error: ${state.message}"
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnStartOauth.isEnabled = true
            }
        }
    }

    private fun updateAuthenticationUI(isAuthenticated: Boolean) {
        if (isAuthenticated) {
            binding.btnStartOauth.visibility = android.view.View.GONE
            binding.btnDisconnect.visibility = android.view.View.VISIBLE
            binding.modelSelectionSection.visibility = android.view.View.VISIBLE
            binding.tvDescription.text = "AI-powered features enabled:\n• Automatic location descriptions\n• Smart charging location summaries\n• Natural language location details"

            // Set default model if none selected
            if (tokenManager.getSelectedAiModel().isNullOrEmpty()) {
                tokenManager.saveSelectedAiModel(DEFAULT_MODEL)
            }

            // Fetch models and update display
            lifecycleScope.launch {
                try {
                    val apiKey = tokenManager.getOpenRouterApiKey()
                    if (!apiKey.isNullOrEmpty()) {
                        val models = fetchAvailableModels(apiKey)
                        models.forEach { modelDataMap[it.id] = it }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("OAuthActivity", "Failed to fetch models for display", e)
                }
                updateSelectedModelDisplay()
            }
        } else {
            binding.btnStartOauth.visibility = android.view.View.VISIBLE
            binding.btnDisconnect.visibility = android.view.View.GONE
            binding.modelSelectionSection.visibility = android.view.View.GONE
            binding.tvDescription.text = "AI-powered features:\n• Automatic location descriptions\n• Smart charging location summaries\n• Natural language location details"
        }
    }

    private fun setupModelSelection() {
        // Click listener to open model selection dialog
        binding.selectedModelCard.setOnClickListener {
            showModelSelectionDialog()
        }

        // Initial display update
        updateSelectedModelDisplay()
    }

    private fun updateSelectedModelDisplay() {
        val selectedModelId = tokenManager.getSelectedAiModel()

        if (selectedModelId != null) {
            val modelData = modelDataMap[selectedModelId]
            
            // Parse model ID to extract provider and name
            val parts = selectedModelId.split("/")
            val provider = if (parts.size >= 2) parts[0] else ""
            val modelName = if (parts.size >= 2) parts[1] else selectedModelId

            // Format display names
            val displayName = modelName.replace("-", " ").split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            val displayProvider = provider.replaceFirstChar { it.uppercase() }

            binding.selectedModelName.text = displayName
            binding.selectedModelProvider.text = "Provider: $displayProvider"

            // Show cost and context info from model data
            if (modelData != null) {
                val pricing = modelData.pricing
                val costDisplay = if (pricing?.prompt == "Free" && pricing.completion == "Free") {
                    "Free"
                } else if (pricing != null) {
                    "Cost: ${pricing.prompt}-${pricing.completion}/M tokens"
                } else {
                    ""
                }
                binding.selectedModelCost.text = costDisplay
                
                val contextDisplay = modelData.context_length?.let { "Context: ${it / 1000}K tokens" } ?: ""
                binding.selectedModelContext.text = contextDisplay
            } else {
                binding.selectedModelCost.text = ""
                binding.selectedModelContext.text = ""
            }
        } else {
            binding.selectedModelName.text = "Tap to select a model"
            binding.selectedModelProvider.text = ""
            binding.selectedModelCost.text = ""
            binding.selectedModelContext.text = ""
        }
    }

    private fun showModelSelectionDialog() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_model_selection, null)
        dialog.setView(dialogView)

        val searchInput = dialogView.findViewById<android.widget.EditText>(R.id.searchInput)
        val clearSearchButton = dialogView.findViewById<android.widget.ImageButton>(R.id.clearSearchButton)
        val freeOnlyCheckbox = dialogView.findViewById<android.widget.CheckBox>(R.id.freeOnlyCheckbox)
        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.modelsRecyclerView)
        val selectButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.selectButton)
        val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancelButton)

        val currentSelectedModel = tokenManager.getSelectedAiModel()

        // Fetch models from API
        lifecycleScope.launch {
            try {
                val apiKey = tokenManager.getOpenRouterApiKey()
                if (apiKey.isNullOrEmpty()) {
                    Toast.makeText(this@OAuthActivity, "API key not found", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    return@launch
                }

                val openRouterModels = fetchAvailableModels(apiKey)
                // Store models in map for later use
                openRouterModels.forEach { modelDataMap[it.id] = it }
                val aiModels = openRouterModels.map { createAiModelFromOpenRouterModel(it) }

                setupModelDialog(
                    dialog,
                    searchInput,
                    clearSearchButton,
                    freeOnlyCheckbox,
                    recyclerView,
                    selectButton,
                    cancelButton,
                    aiModels,
                    currentSelectedModel
                )
            } catch (e: Exception) {
                android.util.Log.e("OAuthActivity", "Failed to fetch models", e)
                Toast.makeText(this@OAuthActivity, "Failed to load models: ${e.message}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()

        // Make dialog full screen
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
    }

    private fun setupModelDialog(
        dialog: AlertDialog,
        searchInput: android.widget.EditText,
        clearSearchButton: android.widget.ImageButton,
        freeOnlyCheckbox: android.widget.CheckBox,
        recyclerView: androidx.recyclerview.widget.RecyclerView,
        selectButton: com.google.android.material.button.MaterialButton,
        cancelButton: com.google.android.material.button.MaterialButton,
        aiModels: List<AiModel>,
        currentSelectedModel: String?
    ) {

        val adapter = ModelSelectionAdapter(
            aiModels,
            currentSelectedModel,
            { selectedModel ->
                // Model clicked callback
                android.util.Log.d("OAuthActivity", "Model clicked: ${selectedModel.name}")
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

        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        // Scroll to selected model if available
        currentSelectedModel?.let { selectedId ->
            val selectedPosition = aiModels.indexOfFirst { it.id == selectedId }
            if (selectedPosition != -1) {
                layoutManager.scrollToPositionWithOffset(selectedPosition, 200)
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
                updateSelectedModelDisplay()
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
    }

    private suspend fun fetchAvailableModels(apiKey: String): List<OpenRouterModel> {
        return withContext(Dispatchers.IO) {
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

            val models = mutableListOf<OpenRouterModel>()

            for (i in 0 until dataArray.length()) {
                val modelJson = dataArray.getJSONObject(i)
                val modelId = modelJson.getString("id")
                val contextLength = modelJson.optInt("context_length", -1).takeIf { it != -1 }
                val description = modelJson.optString("description", null)

                var pricing: ModelPricing? = null
                if (modelJson.has("pricing")) {
                    val pricingJson = modelJson.getJSONObject("pricing")
                    val promptCost = pricingJson.optString("prompt", "0")
                    val completionCost = pricingJson.optString("completion", "0")

                    val promptPerMillion = (promptCost.toDoubleOrNull() ?: 0.0) * 1000000
                    val completionPerMillion = (completionCost.toDoubleOrNull() ?: 0.0) * 1000000

                    pricing = ModelPricing(
                        prompt = formatCost(promptPerMillion),
                        completion = formatCost(completionPerMillion)
                    )
                }

                models.add(
                    OpenRouterModel(
                        id = modelId,
                        context_length = contextLength,
                        description = description,
                        pricing = pricing
                    )
                )
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

    private fun createAiModelFromOpenRouterModel(openRouterModel: OpenRouterModel): AiModel {
        val modelId = openRouterModel.id
        val parts = modelId.split("/")
        val provider = if (parts.size >= 2) parts[0] else ""
        val modelName = if (parts.size >= 2) parts[1] else modelId

        val pricing = openRouterModel.pricing
        val pricingDisplay = if (pricing?.prompt == "Free" && pricing.completion == "Free") {
            "Free"
        } else if (pricing != null) {
            "${pricing.prompt}-${pricing.completion}/M tokens"
        } else {
            "Pricing unavailable"
        }

        val contextSizeDisplay = openRouterModel.context_length?.let { "${it / 1000}K tokens" }
            ?: "Unknown context"

        val displayName = modelName.replace("-", " ").split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        val displayProvider = provider.replaceFirstChar { it.uppercase() }

        return AiModel(
            id = modelId,
            name = displayName,
            provider = displayProvider,
            cost = pricingDisplay,
            contextSize = contextSizeDisplay,
            description = openRouterModel.description
        )
    }

    private fun createDefaultModelList(): List<AiModel> {
        // Hardcoded list of popular models
        // TODO: Fetch from OpenRouter API
        return listOf(
            AiModel("anthropic/claude-3.5-sonnet", "Claude 3.5 Sonnet", "Anthropic", "$3-15/M tokens", "200K tokens", "Most intelligent model"),
            AiModel("anthropic/claude-3-opus", "Claude 3 Opus", "Anthropic", "$15-75/M tokens", "200K tokens", "Powerful model for hard tasks"),
            AiModel("anthropic/claude-3-sonnet", "Claude 3 Sonnet", "Anthropic", "$3-15/M tokens", "200K tokens", "Balance of intelligence and speed"),
            AiModel("anthropic/claude-3-haiku", "Claude 3 Haiku", "Anthropic", "$0.25-1.25/M tokens", "200K tokens", "Fastest and most compact"),
            AiModel("openai/gpt-4o", "GPT-4 Omni", "OpenAI", "$5-15/M tokens", "128K tokens", "High-intelligence flagship model"),
            AiModel("openai/gpt-4-turbo", "GPT-4 Turbo", "OpenAI", "$10-30/M tokens", "128K tokens", "Latest GPT-4 with vision"),
            AiModel("openai/gpt-4", "GPT-4", "OpenAI", "$30-60/M tokens", "8K tokens", "Original GPT-4"),
            AiModel("openai/gpt-3.5-turbo", "GPT-3.5 Turbo", "OpenAI", "$0.50-1.50/M tokens", "16K tokens", "Fast and inexpensive"),
            AiModel("google/gemini-pro-1.5", "Gemini Pro 1.5", "Google", "$1.25-5/M tokens", "1M tokens", "Huge context window"),
            AiModel("google/gemini-pro", "Gemini Pro", "Google", "$0.50-1.50/M tokens", "32K tokens", "Efficient and capable"),
            AiModel("meta-llama/llama-3.1-70b-instruct", "Llama 3.1 70B", "Meta", "Free", "128K tokens", "70B parameters, open source"),
            AiModel("meta-llama/llama-3.1-8b-instruct", "Llama 3.1 8B", "Meta", "Free", "128K tokens", "8B parameters, open source"),
            AiModel("mistralai/mistral-large", "Mistral Large", "Mistral", "$4-12/M tokens", "32K tokens", "Top tier reasoning"),
            AiModel("mistralai/mistral-medium", "Mistral Medium", "Mistral", "$2.7-8.1/M tokens", "32K tokens", "Balanced performance"),
            AiModel("cohere/command-r-plus", "Command R+", "Cohere", "$3-15/M tokens", "128K tokens", "Advanced RAG capabilities")
        )
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return

        // Check if this is our OAuth callback
        if (uri.scheme == "pevplugshare" && uri.host == "oauth" && uri.path == "/callback") {
            val code = uri.getQueryParameter("code")
            val error = uri.getQueryParameter("error")
            val errorDescription = uri.getQueryParameter("error_description")
            val provider = uri.getQueryParameter("provider")

            when {
                code != null && provider != null -> {
                    // Success - exchange code for API key
                    viewModel.handleCallback(code, provider)
                }
                error != null -> {
                    // Error from OAuth provider
                    viewModel.handleError(error, errorDescription)
                }
                else -> {
                    viewModel.handleError("invalid_callback", "Missing required parameters")
                }
            }
        }
    }

    private fun showDisconnectConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Disconnect OpenRouter")
            .setMessage("Are you sure you want to disconnect your OpenRouter account? This will disable all AI features.")
            .setPositiveButton("Disconnect") { _, _ ->
                viewModel.disconnect()
                Toast.makeText(this, "Disconnected from OpenRouter", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
