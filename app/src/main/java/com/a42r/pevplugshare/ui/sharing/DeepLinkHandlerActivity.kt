package com.a42r.pevplugshare.ui.sharing

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.data.api.ServerLocation
import com.a42r.pevplugshare.data.remote.ServerApiClient
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeepLinkViewModel @Inject constructor(
    private val serverApiClient: ServerApiClient
) : ViewModel() {

    private val _location = MutableStateFlow<ServerLocation?>(null)
    val location: StateFlow<ServerLocation?> = _location.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun resolveToken(token: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val service = serverApiClient.getService()
                val resp = service.resolveDirectShare(token)
                if (resp.isSuccessful) {
                    _location.value = resp.body()
                } else {
                    _error.value = "Failed to resolve link: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to resolve link"
            } finally {
                _loading.value = false
            }
        }
    }
}

@AndroidEntryPoint
class DeepLinkHandlerActivity : AppCompatActivity() {

    private val viewModel: DeepLinkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deep_link_handler)

        val loadingProgress = findViewById<ProgressBar>(R.id.loadingProgress)
        val statusText = findViewById<TextView>(R.id.statusText)

        val data: Uri? = intent?.data
        if (data == null) {
            Toast.makeText(this, "Invalid link", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Extract token from URI path segments
        // e.g. pevplugshare://location/TOKEN
        val token = data.lastPathSegment
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "Invalid share link", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            viewModel.loading.collect { loading ->
                loadingProgress.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }
        lifecycleScope.launch {
            viewModel.error.collect { error ->
                if (error != null) {
                    statusText.text = error
                    statusText.visibility = View.VISIBLE
                }
            }
        }
        lifecycleScope.launch {
            viewModel.location.collect { location ->
                if (location != null) {
                    showLocationDetail(location)
                }
            }
        }

        viewModel.resolveToken(token)
    }

    private fun showLocationDetail(location: ServerLocation) {
        // Show location info in a dialog then finish
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(location.name)
            .setMessage(buildString {
                append(location.description ?: "")
                if (!location.streetAddress.isNullOrBlank()) {
                    if (isNotEmpty()) append("\n")
                    append(location.streetAddress)
                }
                if (!location.suburb.isNullOrBlank()) {
                    append(", ${location.suburb}")
                }
                if (!location.state.isNullOrBlank()) {
                    append(" ${location.state}")
                }
                if (!location.openingHours.isNullOrBlank()) {
                    if (isNotEmpty()) append("\n\n")
                    append("Hours: ${location.openingHours}")
                }
                if (!location.socketTypes.isNullOrBlank()) {
                    if (isNotEmpty()) append("\n")
                    append("Sockets: ${location.socketTypes}")
                }
            })
            .setPositiveButton("OK") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }
}
