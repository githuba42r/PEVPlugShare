package com.pevplugshare.ui.quicklocation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.pevplugshare.R
import com.pevplugshare.databinding.ActivityQuickLocationBinding
import com.pevplugshare.data.model.PhotoMetadata
import com.pevplugshare.ui.edit.EditLocationActivity
import com.pevplugshare.util.PhotoUtils
import kotlinx.coroutines.*
import java.io.File

/**
 * Quick location workflow: select photos, extract metadata, optionally edit, run AI analysis, then go to add location
 */
class QuickLocationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuickLocationBinding
    private val selectedPhotoPaths = mutableListOf<String>()
    private val photoMetadata = mutableMapOf<String, PhotoMetadata>()
    private var currentStep = Step.SELECT_PHOTOS
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    enum class Step {
        SELECT_PHOTOS,
        EDIT_PHOTOS,
        AI_ANALYSIS,
        ADD_LOCATION
    }

    private val selectPhotos = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            Log.d("QuickLocationActivity", "Selected ${uris.size} photos")
            selectedPhotoPaths.clear()
            
            // Copy photos to app cache and extract metadata
            scope.launch(Dispatchers.Default) {
                uris.forEach { uri ->
                    try {
                        val file = copyPhotoToCache(uri)
                        if (file != null) {
                            selectedPhotoPaths.add(file.absolutePath)
                            val metadata = PhotoUtils.getPhotoMetadata(file.absolutePath)
                            photoMetadata[file.absolutePath] = metadata
                            Log.d("QuickLocationActivity", "Extracted metadata from ${file.name}")
                        }
                    } catch (e: Exception) {
                        Log.e("QuickLocationActivity", "Error processing photo", e)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    if (selectedPhotoPaths.isNotEmpty()) {
                        showPhotoSelectionDialog()
                    } else {
                        Toast.makeText(this@QuickLocationActivity, "Failed to load photos", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuickLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.takePhotosButton.setOnClickListener { selectPhotos.launch("image/*") }
        binding.selectPhotosButton.setOnClickListener { selectPhotos.launch("image/*") }
    }

    private fun copyPhotoToCache(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = "quick_location_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}.jpg"
            val cacheFile = File(cacheDir, fileName)
            
            inputStream.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            cacheFile
        } catch (e: Exception) {
            Log.e("QuickLocationActivity", "Error copying photo", e)
            null
        }
    }

    private fun showPhotoSelectionDialog() {
        val locationInfo = StringBuilder()
        photoMetadata.values.forEach { metadata ->
            if (metadata.hasGpsData) {
                locationInfo.append("Location: ${metadata.getLocationString()}\n")
            }
            if (metadata.hasBearingData) {
                locationInfo.append("Direction: ${metadata.getBearingString()}\n")
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("${selectedPhotoPaths.size} Photos Selected")
            .setMessage(locationInfo.toString().ifEmpty { "No metadata found in photos" })
            .setNegativeButton("Edit Photos") { _, _ ->
                Toast.makeText(this, "Photo editing coming soon", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("AI Analysis") { _, _ ->
                Toast.makeText(this, "AI analysis coming soon", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("Add Location") { _, _ ->
                goToAddLocation()
            }
            .show()
    }

    private fun goToAddLocation() {
        // Extract location from metadata if available
        val latitude = photoMetadata.values.mapNotNull { it.latitude }.firstOrNull()
        val longitude = photoMetadata.values.mapNotNull { it.longitude }.firstOrNull()
        val bearing = photoMetadata.values.mapNotNull { it.bearing }.firstOrNull()
        
        val intent = Intent(this, EditLocationActivity::class.java).apply {
            if (latitude != null && longitude != null) {
                putExtra("latitude", latitude)
                putExtra("longitude", longitude)
            }
            if (bearing != null) {
                putExtra("bearing", bearing)
            }
            putStringArrayListExtra("photo_paths", ArrayList(selectedPhotoPaths))
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
