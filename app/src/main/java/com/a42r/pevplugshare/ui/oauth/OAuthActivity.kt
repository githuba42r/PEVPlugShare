package com.a42r.pevplugshare.ui.oauth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.a42r.pevplugshare.databinding.ActivityOauthBinding
import com.a42r.pevplugshare.viewmodel.OpenRouterOAuthViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity for OpenRouter OAuth2 authentication with PKCE
 *
 * This activity demonstrates the complete OAuth flow:
 * 1. Register session with callback service
 * 2. Open OpenRouter authorization page
 * 3. Handle callback with authorization code
 * 4. Exchange code for API key
 */
@AndroidEntryPoint
class OAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOauthBinding
    private val viewModel: OpenRouterOAuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOauthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()

        // Handle deep link callback if this is a callback intent
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun setupUI() {
        binding.btnStartOauth.setOnClickListener {
            viewModel.startOAuthFlow()
        }

        binding.btnReset.setOnClickListener {
            viewModel.reset()
            updateUI(OpenRouterOAuthViewModel.OAuthState.Idle)
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
                    Toast.makeText(this, "API Key obtained successfully!", Toast.LENGTH_LONG).show()
                }
                is OpenRouterOAuthViewModel.OAuthState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }

        viewModel.apiKey.observe(this) { apiKey ->
            apiKey?.let {
                binding.tvApiKey.text = "API Key: ${it.take(20)}..."
            }
        }
    }

    private fun updateUI(state: OpenRouterOAuthViewModel.OAuthState) {
        when (state) {
            is OpenRouterOAuthViewModel.OAuthState.Idle -> {
                binding.tvStatus.text = "Ready to authenticate with OpenRouter"
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
                binding.tvStatus.text = "Exchanging authorization code for API key..."
                binding.progressBar.visibility = android.view.View.VISIBLE
            }
            is OpenRouterOAuthViewModel.OAuthState.Success -> {
                binding.tvStatus.text = "✅ Authentication successful!"
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
}
