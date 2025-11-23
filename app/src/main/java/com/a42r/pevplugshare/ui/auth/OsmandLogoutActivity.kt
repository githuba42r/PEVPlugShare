package com.a42r.pevplugshare.ui.osmand

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.a42r.pevplugshare.databinding.ActivityOsmandLogoutBinding
import com.a42r.pevplugshare.ui.main.MainActivity
import com.a42r.pevplugshare.ui.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OsmandLogoutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOsmandLogoutBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOsmandLogoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Disconnect from OsmAnd"
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnDisconnect.setOnClickListener {
            val clearDatabase = binding.clearDatabaseCheckbox.isChecked

            if (clearDatabase) {
                showClearDatabaseConfirmation()
            } else {
                performLogout(clearDatabase = false)
            }
        }
    }

    private fun showClearDatabaseConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Confirm Database Deletion")
            .setMessage(
                "Are you absolutely sure you want to delete all OsmAnd locations from the local database?\n\n" +
                "This will permanently remove:\n" +
                "• All locations synced from OsmAnd\n" +
                "• All photos linked to these locations\n" +
                "• All local data for OsmAnd locations\n\n" +
                "⚠️ THIS ACTION CANNOT BE UNDONE!\n\n" +
                "Type 'DELETE' to confirm this action."
            )
            .setPositiveButton("I understand, delete everything") { _, _ ->
                performLogout(clearDatabase = true)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun performLogout(clearDatabase: Boolean) {
        lifecycleScope.launch {
            try {
                if (clearDatabase) {
                    // Show progress
                    binding.btnDisconnect.isEnabled = false
                    binding.btnDisconnect.text = "Removing locations..."

                    // Delete all OsmAnd locations and their photos
                    val deletedCount = viewModel.deleteAllOsmandLocations()

                    Toast.makeText(
                        this@OsmandLogoutActivity,
                        "Removed $deletedCount OsmAnd locations and their photos",
                        Toast.LENGTH_LONG
                    ).show()
                }

                // Perform logout
                viewModel.logout()

                Toast.makeText(
                    this@OsmandLogoutActivity,
                    "Disconnected from OsmAnd successfully",
                    Toast.LENGTH_SHORT
                ).show()

                // Navigate back to MainActivity
                val intent = Intent(this@OsmandLogoutActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                android.util.Log.e("OsmandLogoutActivity", "Error during logout", e)
                Toast.makeText(
                    this@OsmandLogoutActivity,
                    "Error during logout: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                binding.btnDisconnect.isEnabled = true
                binding.btnDisconnect.text = "Disconnect from OsmAnd"
            }
        }
    }
}
