package com.a42r.pevplugshare.ui.server

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.a42r.pevplugshare.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ServerAccountActivity : AppCompatActivity() {

    private val viewModel: ServerAccountViewModel by viewModels()

    private lateinit var googleSignInClient: GoogleSignInClient

    // Disconnected state views
    private lateinit var disconnectedContainer: LinearLayout
    private lateinit var serverUrlLayout: TextInputLayout
    private lateinit var serverUrlInput: TextInputEditText
    private lateinit var signInButton: Button

    // Connected state views
    private lateinit var connectedContainer: LinearLayout
    private lateinit var connectedServerUrl: TextView
    private lateinit var connectedEmail: TextView
    private lateinit var connectedDisplayName: TextView
    private lateinit var syncButton: Button
    private lateinit var disconnectButton: Button

    // Loading / Error
    private lateinit var loadingProgress: ProgressBar
    private lateinit var errorContainer: LinearLayout
    private lateinit var errorText: TextView

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(Exception::class.java)
            val idToken = account?.idToken
            val serverUrl = serverUrlInput.text?.toString()?.trim() ?: ""
            if (idToken != null && serverUrl.isNotEmpty()) {
                viewModel.connectWithGoogle(serverUrl, idToken)
            } else {
                showError("Sign-in failed: could not get ID token")
            }
        } catch (e: Exception) {
            showError("Sign-in failed: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_account)

        setupGoogleSignIn()
        bindViews()
        observeState()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_server_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun bindViews() {
        disconnectedContainer = findViewById(R.id.disconnectedContainer)
        serverUrlLayout = findViewById(R.id.serverUrlLayout)
        serverUrlInput = findViewById(R.id.serverUrlInput)
        signInButton = findViewById(R.id.signInButton)

        connectedContainer = findViewById(R.id.connectedContainer)
        connectedServerUrl = findViewById(R.id.connectedServerUrl)
        connectedEmail = findViewById(R.id.connectedEmail)
        connectedDisplayName = findViewById(R.id.connectedDisplayName)
        syncButton = findViewById(R.id.syncButton)
        disconnectButton = findViewById(R.id.disconnectButton)

        loadingProgress = findViewById(R.id.loadingProgress)
        errorContainer = findViewById(R.id.errorContainer)
        errorText = findViewById(R.id.errorText)

        signInButton.setOnClickListener {
            val url = serverUrlInput.text?.toString()?.trim() ?: ""
            if (url.isEmpty()) {
                serverUrlLayout.error = "Server URL is required"
                return@setOnClickListener
            }
            serverUrlLayout.error = null
            val signInIntent = googleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
        }

        syncButton.setOnClickListener {
            startActivity(Intent(this, SyncLocationActivity::class.java))
        }

        disconnectButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Disconnect from server?")
                .setMessage("You will need to sign in again to sync locations.")
                .setPositiveButton("Disconnect") { _, _ ->
                    viewModel.disconnect()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is ServerAccountViewModel.State.Disconnected -> showDisconnected()
                    is ServerAccountViewModel.State.Loading -> showLoading()
                    is ServerAccountViewModel.State.Connected -> showConnected(state)
                    is ServerAccountViewModel.State.Error -> showError(state.message)
                }
            }
        }
    }

    private fun showDisconnected() {
        disconnectedContainer.visibility = View.VISIBLE
        connectedContainer.visibility = View.GONE
        loadingProgress.visibility = View.GONE
        errorContainer.visibility = View.GONE
    }

    private fun showLoading() {
        disconnectedContainer.visibility = View.GONE
        connectedContainer.visibility = View.GONE
        loadingProgress.visibility = View.VISIBLE
        errorContainer.visibility = View.GONE
    }

    private fun showConnected(state: ServerAccountViewModel.State.Connected) {
        disconnectedContainer.visibility = View.GONE
        connectedContainer.visibility = View.VISIBLE
        loadingProgress.visibility = View.GONE
        errorContainer.visibility = View.GONE

        connectedServerUrl.text = state.serverUrl
        connectedEmail.text = state.email
        connectedDisplayName.text = state.displayName
    }

    private fun showError(message: String) {
        loadingProgress.visibility = View.GONE
        errorContainer.visibility = View.VISIBLE
        errorText.text = message
        // Restore disconnected form so user can retry
        disconnectedContainer.visibility = View.VISIBLE
        connectedContainer.visibility = View.GONE
    }
}
