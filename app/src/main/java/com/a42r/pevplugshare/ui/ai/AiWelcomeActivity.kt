package com.a42r.pevplugshare.ui.ai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.a42r.pevplugshare.databinding.ActivityAiWelcomeBinding
import com.a42r.pevplugshare.ui.main.MainActivity
import com.a42r.pevplugshare.ui.settings.SettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AiWelcomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAiWelcomeBinding

    @Inject
    lateinit var tokenManager: com.a42r.pevplugshare.data.remote.TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
    }

    private fun setupButtons() {
        binding.getApiKeyButton.setOnClickListener {
            openOpenRouterSignup()
        }

        binding.setupLaterButton.setOnClickListener {
            skipAiSetup()
        }
    }

    private fun openOpenRouterSignup() {
        // Open Settings activity which now has OAuth flow
        markAiWelcomeAsSeen()
        
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        
        // Proceed to main activity - user can configure API key from settings
        proceedToMainActivity()
    }

    private fun skipAiSetup() {
        markAiWelcomeAsSeen()
        proceedToMainActivity()
    }

    private fun markAiWelcomeAsSeen() {
        // Use TokenManager to track that user has seen this welcome screen
        val prefs = getSharedPreferences("ai_welcome_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("has_seen_ai_welcome", true).apply()
    }

    private fun proceedToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        // Prevent back navigation - user must choose an option
        // Do nothing or show a message
        android.widget.Toast.makeText(
            this,
            "Please choose an option to continue",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}
