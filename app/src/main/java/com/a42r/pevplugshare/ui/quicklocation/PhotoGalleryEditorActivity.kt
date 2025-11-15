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
import javax.inject.Inject
import com.a42r.pevplugshare.domain.repository.PhotoRepository
import com.a42r.pevplugshare.data.db.entity.PhotoEntity
import java.util.UUID

/**
 * Gallery view for editing multiple photos with focus areas and crop/zoom
 */
@AndroidEntryPoint
class PhotoGalleryEditorActivity : AppCompatActivity() {
    @Inject
    lateinit var photoRepository: PhotoRepository
    private lateinit var binding: ActivityPhotoGalleryEditorBinding
    private val photoPaths = mutableListOf<String>()
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
                scope.launch(Dispatchers.IO) {
                    if (isSaveNew) {
                        // Add the new photo to the list
                        if (photoPaths.size < MAX_IMAGE_COUNT) {
                            val newPhotoId = UUID.randomUUID().toString()
                            val newPhotoEntity = PhotoEntity(
                                id = newPhotoId,
                                locationId = "temp_location_id", // Placeholder, will be updated when location is saved
                                filePath = editedPath,
                                timestamp = System.currentTimeMillis(),
                                exifMetadata = null, // Not handled here
                                gpxData = null, // Not handled here
                                focusRectString = focusRectSerialized
                            )
                            photoRepository.insertPhoto(newPhotoEntity)
                            Log.d("PhotoGalleryEditorActivity", "New photo inserted: $editedPath with focusRectString: $focusRectSerialized")

                            // Verify write by re-reading from database
                            val verifyNewEntity = photoRepository.getPhotoByFilePath(editedPath)
                            Log.d("PhotoGalleryEditorActivity", "Verified new photo in database - focusRectString: ${verifyNewEntity?.focusRectString}")

                            withContext(Dispatchers.Main) {
                                photoPaths.add(editedPath)
                                adapter.notifyItemInserted(photoPaths.size - 1)
                                updateUI()
                                Toast.makeText(this@PhotoGalleryEditorActivity, "New photo added", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@PhotoGalleryEditorActivity, "Maximum of $MAX_IMAGE_COUNT photos allowed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else if (selectedPhotoIndex != null && selectedPhotoIndex!! >= 0 && selectedPhotoIndex!! < photoPaths.size) {
                        // Update the existing photo
                        val originalPhotoPath = photoPaths[selectedPhotoIndex!!]
                        val photoEntity = photoRepository.getPhotoByFilePath(originalPhotoPath)

                        if (photoEntity != null) {
                            // Delete the original file if it's different from the edited path
                            if (originalPhotoPath != editedPath) {
                                File(originalPhotoPath).delete()
                            }

                            val updatedPhotoEntity = photoEntity.copy(
                                filePath = editedPath,
                                focusRectString = focusRectSerialized
                            )
                            Log.d("PhotoGalleryEditorActivity", "Updating photo: oldPath=$originalPhotoPath, newPath=$editedPath, focusRectString=$focusRectSerialized")
                            photoRepository.updatePhoto(updatedPhotoEntity)
                            Log.d("PhotoGalleryEditorActivity", "Photo update persisted to database with focusRectString: $focusRectSerialized")

                            // Verify write by re-reading from database
                            val verifyEntity = photoRepository.getPhotoByFilePath(editedPath)
                            Log.d("PhotoGalleryEditorActivity", "Verified in database - focusRectString: ${verifyEntity?.focusRectString}")

                            withContext(Dispatchers.Main) {
                                photoPaths[selectedPhotoIndex!!] = editedPath
                                adapter.notifyItemChanged(selectedPhotoIndex!!)
                                updateUI()
                                Toast.makeText(this@PhotoGalleryEditorActivity, "Photo updated", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Log.e("PhotoGalleryEditorActivity", "PhotoEntity not found for path: $originalPhotoPath. Cannot update photo.")
                                Toast.makeText(this@PhotoGalleryEditorActivity, "Error updating photo", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
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

        // Load focus areas from PhotoRepository
        scope.launch {
            // No need to populate focusAreas map here, as addPhotoThumbnail will fetch it
            withContext(Dispatchers.Main) {
                setupUI()
                updateUI() // Initial UI update
                adapter.notifyDataSetChanged() // Notify adapter after initial data load
            }
        }
    }

    private fun setupUI() {
        // Setup gallery recycler
        adapter = PhotoGalleryAdapter(
            photoPaths,
            { selectedPath ->
                val index = photoPaths.indexOf(selectedPath)
                if (index != -1) {
                    selectedPhotoIndex = index
                    launchPhotoEditor(selectedPath)
                }
            },
            { position ->
                showRemoveConfirmationDialog(position)
            },
            photoRepository // Pass the injected photoRepository
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
        scope.launch(Dispatchers.IO) {
            val focusBundle = Bundle()
            for (path in photoPaths) {
                val photoEntity = photoRepository.getPhotoByFilePath(path)
                photoEntity?.focusRectString?.let { focusRectString ->
                    focusBundle.putString(path, focusRectString)
                }
            }

            withContext(Dispatchers.Main) {
                // Prepare result with edited photo paths and focus areas
                val resultIntent = Intent().apply {
                    putStringArrayListExtra("edited_photo_paths", ArrayList(photoPaths))
                    putExtra("focus_areas", focusBundle)
                }

                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
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
            File(removedPath).delete() // Delete the actual file

            scope.launch(Dispatchers.IO) {
                photoRepository.deletePhotoByFilePath(removedPath)
                withContext(Dispatchers.Main) {
                    adapter.notifyItemRemoved(position)
                    adapter.notifyItemRangeChanged(position, photoPaths.size) // Update subsequent items
                    updateUI()
                    Toast.makeText(this@PhotoGalleryEditorActivity, "Photo removed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
