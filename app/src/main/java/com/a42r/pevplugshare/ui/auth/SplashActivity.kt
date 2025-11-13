package com.a42r.pevplugshare.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.a42r.pevplugshare.databinding.ActivitySplashBinding
import com.a42r.pevplugshare.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            try {
                // Small delay for splash screen visibility
                kotlinx.coroutines.delay(1000)

                // Check if activity is still valid and not finishing
                if (isFinishing || isDestroyed) {
                    return@launch
                }

                // Check if user has session cookies
                val hasSessionCookie = viewModel.hasSessionCookie()
                
                if (hasSessionCookie) {
                    android.util.Log.d("SplashActivity", "Session cookie found, validating in background...")
                    
                    // Validate the session by trying to load categories in background
                    val isSessionValid = viewModel.validateSessionInBackground()
                    
                    if (isSessionValid) {
                        android.util.Log.d("SplashActivity", "Session valid, navigating to MainActivity")
                        // Session is valid, go to main activity
                        if (!isFinishing && !isDestroyed) {
                            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                            finish()
                        }
                    } else {
                        android.util.Log.d("SplashActivity", "Session invalid, checking for stored credentials")
                        
                        // Session invalid - check if we have stored credentials for auto-retry
                        val hasStoredCredentials = viewModel.hasStoredCredentials()
                        if (hasStoredCredentials) {
                            android.util.Log.d("SplashActivity", "Found stored credentials, attempting auto-login...")
                            // Try to auto-login with stored credentials
                            val autoLoginSuccess = viewModel.attemptAutoLoginInBackground()
                            if (autoLoginSuccess) {
                                android.util.Log.d("SplashActivity", "Auto-login successful, navigating to MainActivity")
                                if (!isFinishing && !isDestroyed) {
                                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                                    finish()
                                }
                            } else {
                                android.util.Log.d("SplashActivity", "Auto-login failed, navigating to LoginActivity")
                                if (!isFinishing && !isDestroyed) {
                                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java).apply {
                                        putExtra("showAuthErrorModal", true)
                                    })
                                    finish()
                                }
                            }
                        } else {
                            android.util.Log.d("SplashActivity", "No stored credentials, navigating to LoginActivity")
                            if (!isFinishing && !isDestroyed) {
                                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                                finish()
                            }
                        }
                    }
                } else {
                    android.util.Log.d("SplashActivity", "No session cookie found, navigating to LoginActivity")
                    // No session, user needs to log in
                    if (!isFinishing && !isDestroyed) {
                        startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                        finish()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SplashActivity", "Error during splash screen: ${e.message}", e)
                // Continue to login on error
                if (!isFinishing && !isDestroyed) {
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }
}
