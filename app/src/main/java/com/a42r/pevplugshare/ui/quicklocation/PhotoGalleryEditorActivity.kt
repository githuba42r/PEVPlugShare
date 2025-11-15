package com.a42r.pevplugshare.ui.quicklocation

import android.content.Intent
import android.content.Context
import android.graphics.RectF
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
    private val focusAreas = mutableMapOf<String, RectF>() // photo path -> focus rect
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

            Log.d("PhotoGalleryEditorActivity", "Image editor result received:")
            Log.d("PhotoGalleryEditorActivity", "  editedPath: $editedPath")
            Log.d("PhotoGalleryEditorActivity", "  focusRectSerialized: $focusRectSerialized")
            Log.d("PhotoGalleryEditorActivity", "  isSaveNew: $isSaveNew")

            if (editedPath != null) {
                if (isSaveNew) {
                    // Add the new photo to the list
                    if (photoPaths.size < MAX_IMAGE_COUNT) {
                        photoPaths.add(editedPath)
                        if (focusRectSerialized != null) {
                            Log.d("PhotoGalleryEditorActivity", "Parsing focus rect for new photo: $focusRectSerialized")
                            com.a42r.pevplugshare.util.PhotoUtils.parseFocusRectString(focusRectSerialized)?.let { rectF ->
                                Log.d("PhotoGalleryEditorActivity", "Successfully parsed focusRect: ${rectF.left},${rectF.top},${rectF.right},${rectF.bottom}")
                                focusAreas[editedPath] = rectF
                                Log.d("PhotoGalleryEditorActivity", "Added to focusAreas map. Total focus areas: ${focusAreas.size}")
                            } ?: Log.d("PhotoGalleryEditorActivity", "Failed to parse focusRectSerialized")
                        } else {
                            Log.d("PhotoGalleryEditorActivity", "No focus rect returned for new photo")
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
                        Log.d("PhotoGalleryEditorActivity", "Parsing focus rect for updated photo: $focusRectSerialized")
                        com.a42r.pevplugshare.util.PhotoUtils.parseFocusRectString(focusRectSerialized)?.let { rectF ->
                            Log.d("PhotoGalleryEditorActivity", "Successfully parsed focusRect: ${rectF.left},${rectF.top},${rectF.right},${rectF.bottom}")
                            focusAreas[editedPath] = rectF
                            Log.d("PhotoGalleryEditorActivity", "Updated focusAreas map. Total focus areas: ${focusAreas.size}")
                        } ?: Log.d("PhotoGalleryEditorActivity", "Failed to parse focusRectSerialized")
                    } else {
                        Log.d("PhotoGalleryEditorActivity", "No focus rect returned, clearing focus for photo: $editedPath")
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

        // Get existing focus areas if any from intent
        val focusAreasBundle = intent.getBundleExtra("focus_areas")
        if (focusAreasBundle != null) {
            for (key in focusAreasBundle.keySet()) {
                val focusRectString = focusAreasBundle.getString(key)
                if (focusRectString != null) {
                    com.a42r.pevplugshare.util.PhotoUtils.parseFocusRectString(focusRectString)?.let { rectF ->
                        focusAreas[key] = rectF
                    }
                }
            }
        }

        // Also load focus areas from SharedPreferences for persistence
        val sharedPref = getSharedPreferences("focus_areas", Context.MODE_PRIVATE)
        Log.d("PhotoGalleryEditorActivity", "Loading focus areas from SharedPreferences - photoPaths.size=${photoPaths.size}")
        Log.d("PhotoGalleryEditorActivity", "Current photoPaths: $photoPaths")
        for (photoPath in photoPaths) {
            val focusRectString = sharedPref.getString(photoPath, null)
            if (focusRectString != null) {
                Log.d("PhotoGalleryEditorActivity", "Found persisted focus for $photoPath: $focusRectString")
                com.a42r.pevplugshare.util.PhotoUtils.parseFocusRectString(focusRectString)?.let { rectF ->
                    focusAreas[photoPath] = rectF
                    Log.d("PhotoGalleryEditorActivity", "Loaded persisted focus area from SharedPreferences")
                }
            } else {
                Log.d("PhotoGalleryEditorActivity", "No persisted focus found for: $photoPath")
            }
        }
        Log.d("PhotoGalleryEditorActivity", "After loading from SharedPreferences - focusAreas.size=${focusAreas.size}, keys=${focusAreas.keys}")

        setupUI()
        updateUI() // Initial UI update
        adapter.notifyDataSetChanged() // Notify adapter after initial data load
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
        adapter.notifyDataSetChanged() // Notify adapter after setting it to RecyclerView

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
        // Persist all focus areas to SharedPreferences before returning
        val sharedPref = getSharedPreferences("focus_areas", Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            for ((path, rectF) in focusAreas) {
                val focusRectString = "${rectF.left},${rectF.top},${rectF.right},${rectF.bottom}"
                putString(path, focusRectString)
                Log.d("PhotoGalleryEditorActivity", "Persisting focus area to SharedPreferences: $path = $focusRectString")
            }
            apply()
        }

        // Prepare result with edited photo paths and focus areas
        val resultIntent = Intent().apply {
            putStringArrayListExtra("edited_photo_paths", ArrayList(photoPaths))
            val focusBundle = Bundle()
            for ((path, rectF) in focusAreas) {
                val focusRectString = "${rectF.left},${rectF.top},${rectF.right},${rectF.bottom}"
                focusBundle.putString(path, focusRectString)
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
