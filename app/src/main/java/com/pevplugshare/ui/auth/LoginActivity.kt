package com.pevplugshare.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pevplugshare.databinding.ActivityLoginBinding
import com.pevplugshare.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    private val browserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data?.data
        if (data != null) {
            val code = data.getQueryParameter("code")
            if (!code.isNullOrEmpty()) {
                viewModel.exchangeCodeForToken(code)
            } else {
                Toast.makeText(this, "Authorization failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            startOAuthFlow()
        }

        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is LoginState.Loading -> {
                        binding.loginButton.isEnabled = false
                    }
                    is LoginState.Success -> {
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                    is LoginState.Error -> {
                        binding.loginButton.isEnabled = true
                        Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    is LoginState.Idle -> {
                        binding.loginButton.isEnabled = true
                    }
                }
            }
        }
    }

    private fun startOAuthFlow() {
        val clientId = "osmand"
        val redirectUri = "com.pevplugshare://auth/callback"
        val oAuthUrl = Uri.Builder()
            .scheme("https")
            .authority("osmand.net")
            .path("/api/authorize")
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", "read_user")
            .build()
            .toString()

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(oAuthUrl))
        browserLauncher.launch(intent)
    }
}
