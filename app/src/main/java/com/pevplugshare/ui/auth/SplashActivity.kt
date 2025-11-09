package com.pevplugshare.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pevplugshare.databinding.ActivitySplashBinding
import com.pevplugshare.ui.main.MainActivity
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
                val isLoggedIn = viewModel.isUserLoggedIn()
                
                if (isLoggedIn) {
                    android.util.Log.d("SplashActivity", "Session found, validating...")
                    
                    // Validate the session by trying to load categories
                    val isSessionValid = viewModel.validateSession()
                    
                    if (isSessionValid) {
                        android.util.Log.d("SplashActivity", "Session valid, navigating to MainActivity")
                        // Session is valid, go to main activity
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    } else {
                        android.util.Log.d("SplashActivity", "Session invalid, navigating to LoginActivity")
                        // Session expired or invalid, need to login again
                        startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                    }
                } else {
                    android.util.Log.d("SplashActivity", "No session found, navigating to LoginActivity")
                    // No session, user needs to log in
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                }
                finish()
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
