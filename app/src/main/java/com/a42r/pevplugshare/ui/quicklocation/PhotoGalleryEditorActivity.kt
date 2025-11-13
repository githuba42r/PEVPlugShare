package com.a42r.pevplugshare.ui.quicklocation

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoGalleryEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get photo paths from intent
        photoPaths.addAll(intent.getStringArrayListExtra("photo_paths") ?: emptyList())
        
        // Get existing focus areas if any
        val focusAreasBundle = intent.getBundleExtra("focus_areas")
        if (focusAreasBundle != null) {
            for (key in focusAreasBundle.keySet()) {
                focusAreas[key] = focusAreasBundle.getString(key) ?: ""
            }
        }

        setupUI()
    }

    private fun setupUI() {
        // Setup title
        binding.titleText.text = "Edit ${photoPaths.size} Photos"
        
        // Setup gallery recycler
        adapter = PhotoGalleryAdapter(photoPaths, focusAreas) { selectedPath ->
            launchPhotoEditor(selectedPath)
        }
        
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
    }

    private fun launchPhotoEditor(photoPath: String) {
        val intent = Intent(this, ImageEditorActivity::class.java).apply {
            putExtra(ImageEditorActivity.EXTRA_IMAGE_PATH, photoPath)
            putExtra("is_focus_only", true) // Only allow focus area, no crop/zoom for now
        }
        startActivity(intent)
        
        // After returning, check for focus area result
        // This would be handled via activity result in a full implementation
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

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
