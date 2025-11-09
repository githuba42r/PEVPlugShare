package com.pevplugshare.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pevplugshare.databinding.ActivityLoginBinding
import com.pevplugshare.ui.main.MainActivity
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

        // Check if we should show auth error modal from splash screen
        if (intent?.getBooleanExtra("showAuthErrorModal", false) == true) {
            showBackgroundAuthenticationFailedModal()
        }

        // Check for existing session token
        lifecycleScope.launch {
            if (viewModel.hasValidSessionToken()) {
                // Skip login screen and go directly to MainActivity
                // Validation will happen in background
                if (!isFinishing && !isDestroyed) {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
                return@launch
            }
            
            // No session token, check for stored credentials
            if (viewModel.hasStoredCredentials()) {
                showAutoLoginState()
                viewModel.attemptAutoLogin()
            }
        }

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
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
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
                "You are about to save your login credentials on this device.\n\n" +
                "SECURITY RISKS:\n" +
                "• Unlocked devices can be accessed by others\n" +
                "• Rooted/compromised devices are at risk\n" +
                "• Use device lock screen protection\n\n" +
                "IMPORTANT:\n" +
                "• Credentials are AES-256 encrypted\n" +
                "• Only enable on personal devices\n" +
                "• Disable if device is lost\n\n" +
                "Do you understand and wish to continue?"
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
            maxLines = 12
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
}
