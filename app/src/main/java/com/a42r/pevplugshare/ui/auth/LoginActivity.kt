package com.a42r.pevplugshare.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.a42r.pevplugshare.databinding.ActivityLoginBinding
import com.a42r.pevplugshare.ui.main.MainActivity
import com.a42r.pevplugshare.ui.ai.AiWelcomeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private var rememberMeConfirmed = false
    private var authErrorDialog: AlertDialog? = null
    private var authErrorDismissTimer: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if this is a manual reconnection attempt from the menu
        val isReconnecting = intent?.getBooleanExtra("isReconnecting", false) == true

        // Check if we should show auth error modal from splash screen
        if (intent?.getBooleanExtra("showAuthErrorModal", false) == true) {
            showBackgroundAuthenticationFailedModal()
        }

        // Check for existing session token (but not if user explicitly wants to reconnect)
        if (!isReconnecting) {
            lifecycleScope.launch {
                if (viewModel.hasValidSessionToken()) {
                    // Check if user has seen AI welcome screen
                    if (!hasSeenAiWelcome()) {
                        // Show AI welcome screen first
                        if (!isFinishing && !isDestroyed) {
                            startActivity(Intent(this@LoginActivity, AiWelcomeActivity::class.java))
                            finish()
                        }
                    } else {
                        // Skip login screen and go directly to MainActivity
                        // Validation will happen in background
                        if (!isFinishing && !isDestroyed) {
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        }
                    }
                    return@launch
                }

                // No session token, check for stored credentials
                if (viewModel.hasStoredCredentials()) {
                    showAutoLoginState()
                    viewModel.attemptAutoLogin()
                }
            }
        }
        // If isReconnecting is true, just show the login screen without auto-navigating

        // Handle Remember Me checkbox - show warning when checked
        binding.rememberMeCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !rememberMeConfirmed) {
                // Disable login button until user confirms
                binding.loginButton.isEnabled = false
                showRememberMeWarning()
            } else if (!isChecked) {
                // Re-enable login if unchecked
                binding.loginButton.isEnabled = true
                rememberMeConfirmed = false
            }
        }

        binding.loginButton.setOnClickListener {
            val username = binding.usernameInput.text?.toString()?.trim()
            val password = binding.passwordInput.text?.toString()
            val rememberMe = binding.rememberMeCheckbox.isChecked

            if (username.isNullOrEmpty()) {
                binding.usernameLayout.error = "Email is required"
                return@setOnClickListener
            }

            if (password.isNullOrEmpty()) {
                binding.passwordLayout.error = "Password is required"
                return@setOnClickListener
            }

            binding.usernameLayout.error = null
            binding.passwordLayout.error = null

            // Login with remember me setting
            viewModel.loginWithCredentials(username, password, rememberMe)
        }

        binding.skipLoginButton.setOnClickListener {
            // Skip login and go to MainActivity with local database only
            showSkipLoginConfirmation()
        }

        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is LoginState.Loading -> {
                        binding.loginButton.isEnabled = false
                        binding.usernameInput.isEnabled = false
                        binding.passwordInput.isEnabled = false
                        binding.rememberMeCheckbox.isEnabled = false
                        binding.progressBar.visibility = android.view.View.VISIBLE
                        binding.statusText.visibility = android.view.View.GONE
                    }
                    is LoginState.Success -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.statusText.visibility = android.view.View.GONE
                        if (!isFinishing && !isDestroyed) {
                            // Check if user has seen AI welcome screen
                            val nextActivity = if (hasSeenAiWelcome()) {
                                MainActivity::class.java
                            } else {
                                AiWelcomeActivity::class.java
                            }
                            startActivity(Intent(this@LoginActivity, nextActivity))
                            finish()
                        }
                    }
                    is LoginState.Error -> {
                        binding.loginButton.isEnabled = true
                        binding.usernameInput.isEnabled = true
                        binding.passwordInput.isEnabled = true
                        binding.rememberMeCheckbox.isEnabled = true
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.statusText.visibility = android.view.View.GONE
                        Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()

                        // Show login form if auto-login failed
                        showLoginForm()
                    }
                    is LoginState.Idle -> {
                        binding.loginButton.isEnabled = true
                        binding.usernameInput.isEnabled = true
                        binding.passwordInput.isEnabled = true
                        binding.rememberMeCheckbox.isEnabled = true
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.statusText.visibility = android.view.View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        authErrorDismissTimer?.cancel()
        authErrorDialog?.dismiss()
    }

    private fun showRememberMeWarning() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("⚠️ Security Warning")
            .setMessage(
                "IMPORTANT SECURITY NOTICE\n\n" +
                "You have chosen to save your login credentials.\n\n" +
                "What this means:\n" +
                "• Your username and password will be stored on this device\n" +
                "• The app will automatically log you in when opened\n" +
                "• Credentials are encrypted using AES-256 encryption\n" +
                "• Data is stored in secure Android storage\n\n" +
                "SECURITY RISKS:\n" +
                "• If someone gains physical access to your unlocked device, they may be able to access your account\n" +
                "• If your device is rooted or compromised, stored credentials may be at risk\n" +
                "• You should only enable this on devices you personally control and trust\n\n" +
                "RECOMMENDATIONS:\n" +
                "• Use device lock screen protection (PIN/Pattern/Biometric)\n" +
                "• Do not enable on shared or public devices\n" +
                "• Disable if device is lost or stolen (change password)\n\n" +
                "Do you understand the risks and wish to continue?"
            )
            .setPositiveButton("I Understand, Continue") { _, _ ->
                // User confirmed, enable login button
                rememberMeConfirmed = true
                binding.loginButton.isEnabled = true
                Toast.makeText(this, "Remember me enabled. You can now login.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { _, _ ->
                // User canceled, uncheck the checkbox and keep login disabled
                binding.rememberMeCheckbox.isChecked = false
                rememberMeConfirmed = false
                binding.loginButton.isEnabled = true
            }
            .setOnCancelListener {
                // User dismissed dialog, uncheck the checkbox
                binding.rememberMeCheckbox.isChecked = false
                rememberMeConfirmed = false
                binding.loginButton.isEnabled = true
            }
            .setCancelable(true)
            .create()

        dialog.show()
        dialog.findViewById<android.widget.TextView>(android.R.id.message)?.apply {
            maxLines = 8
            setVerticalScrollBarEnabled(true)
            movementMethod = android.text.method.ScrollingMovementMethod()
        }
    }

    private fun showBackgroundAuthenticationFailedModal() {
        authErrorDialog = AlertDialog.Builder(this)
            .setTitle("Authentication Failed")
            .setMessage("Background authentication failed. Please log in again.")
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
                authErrorDismissTimer?.cancel()
            }
            .setCancelable(false)
            .create()

        authErrorDialog?.show()

        authErrorDismissTimer = lifecycleScope.launch {
            delay(15000)
            if (!isFinishing && !isDestroyed && authErrorDialog?.isShowing == true) {
                authErrorDialog?.dismiss()
                showLoginForm()
            }
        }
    }

    private fun showAutoLoginState() {
        binding.usernameLayout.visibility = android.view.View.GONE
        binding.passwordLayout.visibility = android.view.View.GONE
        binding.rememberMeCheckbox.visibility = android.view.View.GONE
        binding.loginButton.visibility = android.view.View.GONE
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.statusText.visibility = android.view.View.VISIBLE
        binding.statusText.text = "Logging in..."
    }

    private fun showLoginForm() {
        binding.usernameLayout.visibility = android.view.View.VISIBLE
        binding.passwordLayout.visibility = android.view.View.VISIBLE
        binding.rememberMeCheckbox.visibility = android.view.View.VISIBLE
        binding.loginButton.visibility = android.view.View.VISIBLE
        binding.statusText.visibility = android.view.View.GONE
    }

    private fun showSkipLoginConfirmation() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Use Local Database Only?")
            .setMessage(
                "You are about to skip OSMAnd login and use the app with local database only.\n\n" +
                "Features available:\n" +
                "✓ Add and edit charging locations\n" +
                "✓ View locations on map\n" +
                "✓ Navigate to locations\n" +
                "✓ All data stored locally on your device\n\n" +
                "Features NOT available without OSMAnd:\n" +
                "✗ Sync locations across devices\n" +
                "✗ Cloud backup\n" +
                "✗ Share with other users\n\n" +
                "You can connect to OSMAnd later from the menu."
            )
            .setPositiveButton("Continue Without OSMAnd") { _, _ ->
                // Proceed to MainActivity or AI welcome screen
                if (!isFinishing && !isDestroyed) {
                    val nextActivity = if (hasSeenAiWelcome()) {
                        MainActivity::class.java
                    } else {
                        AiWelcomeActivity::class.java
                    }
                    startActivity(Intent(this@LoginActivity, nextActivity))
                    finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun hasSeenAiWelcome(): Boolean {
        val prefs = getSharedPreferences("ai_welcome_prefs", MODE_PRIVATE)
        return prefs.getBoolean("has_seen_ai_welcome", false)
    }
}
