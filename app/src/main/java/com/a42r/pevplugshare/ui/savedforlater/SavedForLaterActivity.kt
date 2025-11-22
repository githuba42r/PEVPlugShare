package com.a42r.pevplugshare.ui.savedforlater

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.a42r.pevplugshare.databinding.ActivitySavedForLaterBinding
import com.a42r.pevplugshare.ui.edit.EditLocationActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SavedForLaterActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySavedForLaterBinding
    private val viewModel: SavedForLaterViewModel by viewModels()
    private var adapter: SavedForLaterAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedForLaterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()

        viewModel.loadSavedLocations()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        binding.savedForLaterRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SavedForLaterAdapter(
            onContinueClick = { savedForLater ->
                continueWithLocation(savedForLater)
            },
            onDeleteClick = { savedForLater ->
                confirmDelete(savedForLater)
            }
        )
        binding.savedForLaterRecyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.savedLocationsState.collect { state ->
                when (state) {
                    is SavedForLaterState.Loading -> {
                        binding.loadingProgressBar.visibility = View.VISIBLE
                        binding.savedForLaterRecyclerView.visibility = View.GONE
                        binding.emptyStateText.visibility = View.GONE
                    }
                    is SavedForLaterState.Success -> {
                        binding.loadingProgressBar.visibility = View.GONE
                        if (state.items.isEmpty()) {
                            binding.savedForLaterRecyclerView.visibility = View.GONE
                            binding.emptyStateText.visibility = View.VISIBLE
                        } else {
                            binding.savedForLaterRecyclerView.visibility = View.VISIBLE
                            binding.emptyStateText.visibility = View.GONE
                            adapter?.submitList(state.items)
                        }
                    }
                    is SavedForLaterState.Error -> {
                        binding.loadingProgressBar.visibility = View.GONE
                        binding.savedForLaterRecyclerView.visibility = View.GONE
                        binding.emptyStateText.visibility = View.VISIBLE
                        binding.emptyStateText.text = "Error: ${state.message}"
                        Toast.makeText(this@SavedForLaterActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun continueWithLocation(item: SavedForLaterItem) {
        val intent = Intent(this, EditLocationActivity::class.java).apply {
            putExtra("latitude", item.latitude)
            putExtra("longitude", item.longitude)
            item.bearing?.let { putExtra("bearing", it) }
            putStringArrayListExtra("photo_paths", ArrayList(item.photoPaths))

            // Pass the saved for later ID so it can be deleted after successful save
            putExtra("saved_for_later_id", item.id)

            // Pass focus areas if available
            val focusBundle = Bundle()
            item.photoMetadata.forEach { (path, metadata) ->
                metadata.focusRect?.let { rect ->
                    val focusRectString = "${rect.left},${rect.top},${rect.right},${rect.bottom}"
                    focusBundle.putString(path, focusRectString)
                }
            }
            putExtra("focus_areas", focusBundle)
        }

        startActivity(intent)
        finish()
    }

    private fun confirmDelete(item: SavedForLaterItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Saved Location")
            .setMessage("Are you sure you want to delete this saved location? This will also delete the ${item.photoPaths.size} associated photo(s).")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteSavedForLater(item.id)
                    Toast.makeText(
                        this@SavedForLaterActivity,
                        "Deleted saved location",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the list when returning to this activity
        viewModel.loadSavedLocations()
    }
}
