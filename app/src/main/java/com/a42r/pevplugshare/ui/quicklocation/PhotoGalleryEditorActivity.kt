package com.a42r.pevplugshare.ui.quicklocation

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.databinding.ActivityPhotoGalleryEditorBinding
import com.a42r.pevplugshare.ui.edit.ImageEditorActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File

/**
 * Gallery view for editing multiple photos with focus areas and crop/zoom
 */
@AndroidEntryPoint
class PhotoGalleryEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPhotoGalleryEditorBinding
    private val photoPaths = mutableListOf<String>()
    private val focusAreas = mutableMapOf<String, String>() // photo path -> focus rect serialized
    private lateinit var adapter: PhotoGalleryAdapter
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var selectedPhotoIndex: Int? = null

    private val MAX_IMAGE_COUNT = 5

    // Launcher for editing a single photo
    private val imageEditorLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val editedPath = result.data?.getStringExtra(ImageEditorActivity.EXTRA_OUTPUT_PATH)
            val focusRectSerialized = result.data?.getStringExtra(ImageEditorActivity.EXTRA_FOCUS_RECT)
            val isSaveNew = result.data?.getBooleanExtra(ImageEditorActivity.EXTRA_IS_SAVE_NEW, false) ?: false

            if (editedPath != null) {
                if (isSaveNew) {
                    // Add the new photo to the list
                    if (photoPaths.size < MAX_IMAGE_COUNT) {
                        photoPaths.add(editedPath)
                        if (focusRectSerialized != null) {
                            focusAreas[editedPath] = focusRectSerialized
                        }
                        adapter.notifyItemInserted(photoPaths.size - 1)
                        updateUI()
                        Toast.makeText(this, "New photo added", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Maximum of $MAX_IMAGE_COUNT photos allowed", Toast.LENGTH_SHORT).show()
                    }
                } else if (selectedPhotoIndex != null && selectedPhotoIndex!! >= 0 && selectedPhotoIndex!! < photoPaths.size) {
                    // Update the existing photo
                    val originalPhotoPath = photoPaths[selectedPhotoIndex!!]
                    // Delete the original file if it's different from the edited path
                    if (originalPhotoPath != editedPath) {
                        File(originalPhotoPath).delete()
                    }
                    photoPaths[selectedPhotoIndex!!] = editedPath
                    if (focusRectSerialized != null) {
                        focusAreas[editedPath] = focusRectSerialized
                    } else {
                        focusAreas.remove(editedPath) // Clear focus if none returned
                    }
                    adapter.notifyItemChanged(selectedPhotoIndex!!)
                    updateUI()
                    Toast.makeText(this, "Photo updated", Toast.LENGTH_SHORT).show()
                }
            }
        }
        selectedPhotoIndex = null
    }

    // Launcher for selecting multiple images
    private val selectMultipleImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris?.let {
            val newPhotoPaths = mutableListOf<String>()
            for (uri in it) {
                // Convert URI to file path (this might require more robust handling for different URIs)
                // For simplicity, assuming content URIs that can be resolved to file paths
                val filePath = uri.path // This is a simplified approach, might need actual file utility
                if (filePath != null && photoPaths.size + newPhotoPaths.size < MAX_IMAGE_COUNT) {
                    newPhotoPaths.add(filePath)
                } else if (photoPaths.size + newPhotoPaths.size >= MAX_IMAGE_COUNT) {
                    Toast.makeText(this, "Maximum of $MAX_IMAGE_COUNT photos allowed", Toast.LENGTH_SHORT).show()
                    break
                }
            }
            if (newPhotoPaths.isNotEmpty()) {
                val startInsertPosition = photoPaths.size
                photoPaths.addAll(newPhotoPaths)
                adapter.notifyItemRangeInserted(startInsertPosition, newPhotoPaths.size)
                updateUI()
                Toast.makeText(this, "${newPhotoPaths.size} new photos added", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoGalleryEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get photo paths from intent and filter out deleted/non-existent files
        val incomingPhotoPaths = intent.getStringArrayListExtra("photo_paths") ?: emptyList()
        photoPaths.addAll(incomingPhotoPaths.filter { path ->
            val file = java.io.File(path)
            file.exists()
        }.take(MAX_IMAGE_COUNT)) // Ensure initial load also respects MAX_IMAGE_COUNT

        // Get existing focus areas if any
        val focusAreasBundle = intent.getBundleExtra("focus_areas")
        if (focusAreasBundle != null) {
            for (key in focusAreasBundle.keySet()) {
                focusAreas[key] = focusAreasBundle.getString(key) ?: ""
            }
        }

        setupUI()
        updateUI() // Initial UI update
    }

    private fun setupUI() {
        // Setup gallery recycler
        adapter = PhotoGalleryAdapter(
            photoPaths,
            focusAreas,
            { selectedPath ->
                val index = photoPaths.indexOf(selectedPath)
                if (index != -1) {
                    selectedPhotoIndex = index
                    launchPhotoEditor(selectedPath)
                }
            },
            { position ->
                showRemoveConfirmationDialog(position)
            }
        )

        binding.photoGalleryRecycler.apply {
            layoutManager = GridLayoutManager(this@PhotoGalleryEditorActivity, 3)
            adapter = this@PhotoGalleryEditorActivity.adapter
        }

        // Setup next button
        binding.nextButton.setOnClickListener {
            proceedWithEditedPhotos()
        }

        // Setup back button
        binding.backButton.setOnClickListener {
            finish()
        }

        // Setup add more images button
        binding.addMoreImagesButton.setOnClickListener {
            selectMultipleImagesLauncher.launch("image/*")
        }
    }

    private fun updateUI() {
        binding.titleText.text = "Edit ${photoPaths.size} Photos"
        binding.imageCountTextView.text = "${photoPaths.size}/$MAX_IMAGE_COUNT Images Selected"
        binding.addMoreImagesButton.isEnabled = photoPaths.size < MAX_IMAGE_COUNT
    }

    private fun launchPhotoEditor(photoPath: String) {
        val intent = Intent(this, ImageEditorActivity::class.java).apply {
            putExtra(ImageEditorActivity.EXTRA_IMAGE_PATH, photoPath)
            putExtra(ImageEditorActivity.EXTRA_OUTPUT_PATH, photoPath) // Add output path
            putExtra("is_focus_only", true)
        }
        imageEditorLauncher.launch(intent)
    }

    private fun proceedWithEditedPhotos() {
        // Prepare result with edited photo paths and focus areas
        val resultIntent = Intent().apply {
            putStringArrayListExtra("edited_photo_paths", ArrayList(photoPaths))
            val focusBundle = Bundle()
            for ((path, focusRect) in focusAreas) {
                focusBundle.putString(path, focusRect)
            }
            putExtra("focus_areas", focusBundle)
        }

        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun showRemoveConfirmationDialog(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Remove Photo")
            .setMessage("Are you sure you want to remove this photo?")
            .setPositiveButton("Remove") { dialog, _ ->
                removeImage(position)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun removeImage(position: Int) {
        if (position >= 0 && position < photoPaths.size) {
            val removedPath = photoPaths.removeAt(position)
            focusAreas.remove(removedPath)
            File(removedPath).delete() // Delete the actual file

            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position, photoPaths.size) // Update subsequent items
            updateUI()
            Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
