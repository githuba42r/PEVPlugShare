package com.a42r.pevplugshare.ui.gallery

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.databinding.ActivityAllImagesGalleryBinding
import com.a42r.pevplugshare.util.ImageUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import com.a42r.pevplugshare.data.db.dao.LocationDao

data class GalleryImageItem(
    val imagePath: String,
    val locationId: String,
    val locationName: String
)

@AndroidEntryPoint
class AllImagesGalleryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAllImagesGalleryBinding
    private lateinit var adapter: AllImagesAdapter
    
    @Inject
    lateinit var locationDao: LocationDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllImagesGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadImages()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            if (adapter.isSelectionMode) {
                exitSelectionMode()
            } else {
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (adapter.isSelectionMode) {
            menuInflater.inflate(R.menu.menu_selection_mode, menu)
            return true
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_select_all -> {
                adapter.selectAll()
                updateSelectionToolbar()
                true
            }
            R.id.menu_download_selected -> {
                downloadSelectedImages()
                true
            }
            R.id.menu_share_selected -> {
                shareSelectedImages()
                true
            }
            R.id.menu_cancel_selection -> {
                exitSelectionMode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        adapter = AllImagesAdapter(
            onImageClick = { imageItem, position ->
                if (adapter.isSelectionMode) {
                    adapter.toggleSelection(position)
                    updateSelectionToolbar()
                } else {
                    openFullImageViewer(imageItem, position)
                }
            },
            onImageLongClick = { position ->
                if (!adapter.isSelectionMode) {
                    enterSelectionMode(position)
                }
            }
        )
        
        binding.imagesRecyclerView.layoutManager = GridLayoutManager(this, 3)
        binding.imagesRecyclerView.adapter = adapter
    }

    private fun enterSelectionMode(initialPosition: Int) {
        adapter.enterSelectionMode(initialPosition)
        updateSelectionToolbar()
        invalidateOptionsMenu()
    }

    private fun exitSelectionMode() {
        adapter.exitSelectionMode()
        supportActionBar?.title = "All Images"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        invalidateOptionsMenu()
    }

    private fun updateSelectionToolbar() {
        val count = adapter.getSelectedCount()
        if (count == 0) {
            exitSelectionMode()
        } else {
            supportActionBar?.title = "$count selected"
            invalidateOptionsMenu()
        }
    }

    private fun loadImages() {
        binding.loadingProgressBar.visibility = View.VISIBLE
        binding.imagesRecyclerView.visibility = View.GONE
        binding.emptyStateText.visibility = View.GONE

        lifecycleScope.launch {
            val imageItems = withContext(Dispatchers.IO) {
                collectAllImages()
            }

            binding.loadingProgressBar.visibility = View.GONE

            if (imageItems.isEmpty()) {
                binding.emptyStateText.visibility = View.VISIBLE
                binding.imageCountText.text = "No images found"
            } else {
                binding.imagesRecyclerView.visibility = View.VISIBLE
                adapter.submitList(imageItems)
                
                val totalSize = imageItems.sumOf { File(it.imagePath).length() }
                val sizeMB = totalSize / (1024.0 * 1024.0)
                val sizeString = if (sizeMB < 1.0) {
                    String.format("%.2f KB", totalSize / 1024.0)
                } else {
                    String.format("%.2f MB", sizeMB)
                }
                
                binding.imageCountText.text = "${imageItems.size} images • $sizeString"
            }
        }
    }

    private fun downloadSelectedImages() {
        val selectedItems = adapter.getSelectedItems()
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "No images selected", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            var successCount = 0
            var failCount = 0

            withContext(Dispatchers.IO) {
                selectedItems.forEach { item ->
                    try {
                        downloadImageToGallery(File(item.imagePath))
                        successCount++
                    } catch (e: Exception) {
                        e.printStackTrace()
                        failCount++
                    }
                }
            }

            val message = when {
                failCount == 0 -> "Downloaded $successCount image${if (successCount != 1) "s" else ""}"
                successCount == 0 -> "Failed to download images"
                else -> "Downloaded $successCount, failed $failCount"
            }
            Toast.makeText(this@AllImagesGalleryActivity, message, Toast.LENGTH_SHORT).show()
            
            exitSelectionMode()
        }
    }

    private fun shareSelectedImages() {
        val selectedItems = adapter.getSelectedItems()
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "No images selected", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val imageUris = selectedItems.mapNotNull { item ->
                val file = File(item.imagePath)
                if (file.exists()) {
                    FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        file
                    )
                } else {
                    null
                }
            }

            if (imageUris.isEmpty()) {
                Toast.makeText(this, "No valid images to share", Toast.LENGTH_SHORT).show()
                return
            }

            val intent = Intent().apply {
                if (imageUris.size == 1) {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, imageUris[0])
                } else {
                    action = Intent.ACTION_SEND_MULTIPLE
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(imageUris))
                }
                type = "image/jpeg"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Share images"))
            exitSelectionMode()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to share images", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadImageToGallery(imageFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, imageFile.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, 
                    "${Environment.DIRECTORY_PICTURES}/PEVPlugShare")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            uri?.let { imageUri ->
                contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                    FileInputStream(imageFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(imageUri, contentValues, null, null)
            }
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
            val appDir = File(picturesDir, "PEVPlugShare")
            if (!appDir.exists()) {
                appDir.mkdirs()
            }

            val destFile = File(appDir, imageFile.name)
            imageFile.copyTo(destFile, overwrite = true)

            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.fromFile(destFile)
            sendBroadcast(intent)
        }
    }

    private suspend fun collectAllImages(): List<GalleryImageItem> {
        val imageItems = mutableListOf<GalleryImageItem>()
        val imagesDir = ImageUtils.getAppImagesDirectory(this)

        if (imagesDir.exists() && imagesDir.isDirectory) {
            imagesDir.listFiles()?.forEach { locationDir ->
                if (locationDir.isDirectory) {
                    val locationId = locationDir.name
                    val locationEntity = locationDao.getLocationById(locationId)
                    val locationName = locationEntity?.name ?: "Unknown Location"
                    
                    locationDir.listFiles()?.filter { it.extension == "jpg" }
                        ?.sortedBy { it.name }
                        ?.forEach { imageFile ->
                            imageItems.add(
                                GalleryImageItem(
                                    imagePath = imageFile.absolutePath,
                                    locationId = locationId,
                                    locationName = locationName
                                )
                            )
                        }
                }
            }
        }

        return imageItems.sortedBy { it.locationName }
    }

    private fun openFullImageViewer(imageItem: GalleryImageItem, position: Int) {
        // Get all images for the same location
        val locationImages = adapter.currentList.filter { it.locationId == imageItem.locationId }
        val imagePosition = locationImages.indexOf(imageItem)
        
        val intent = Intent(this, FullImageViewerActivity::class.java).apply {
            putExtra(EXTRA_IMAGE_PATHS, locationImages.map { it.imagePath }.toTypedArray())
            putExtra(EXTRA_LOCATION_NAME, imageItem.locationName)
            putExtra(EXTRA_INITIAL_POSITION, imagePosition)
        }
        startActivity(intent)
    }

    companion object {
        const val EXTRA_IMAGE_PATHS = "extra_image_paths"
        const val EXTRA_LOCATION_NAME = "extra_location_name"
        const val EXTRA_INITIAL_POSITION = "extra_initial_position"
    }
}
