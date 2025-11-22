package com.a42r.pevplugshare.ui.gallery

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.a42r.pevplugshare.databinding.ActivityFullImageViewerBinding
import com.a42r.pevplugshare.ui.adapter.ImageGalleryAdapter
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

class FullImageViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFullImageViewerBinding
    private lateinit var imagePaths: Array<String>
    private lateinit var locationName: String
    private var initialPosition: Int = 0
    private var currentPosition: Int = 0

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            downloadCurrentImage()
        } else {
            Toast.makeText(this, "Storage permission required to download images", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imagePaths = intent.getStringArrayExtra(AllImagesGalleryActivity.EXTRA_IMAGE_PATHS) ?: emptyArray()
        locationName = intent.getStringExtra(AllImagesGalleryActivity.EXTRA_LOCATION_NAME) ?: "Unknown"
        initialPosition = intent.getIntExtra(AllImagesGalleryActivity.EXTRA_INITIAL_POSITION, 0)
        currentPosition = initialPosition

        if (imagePaths.isEmpty()) {
            Toast.makeText(this, "No images to display", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupViewPager()
        setupButtons()
        updateUI()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupViewPager() {
        val adapter = ImageGalleryAdapter(imagePaths.toList())
        binding.imageViewPager.adapter = adapter
        binding.imageViewPager.setCurrentItem(initialPosition, false)

        binding.imageViewPager.registerOnPageChangeCallback(
            object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    currentPosition = position
                    updateUI()
                }
            }
        )
    }

    private fun setupButtons() {
        binding.downloadButton.setOnClickListener {
            requestDownloadImage()
        }

        binding.shareButton.setOnClickListener {
            shareCurrentImage()
        }
    }

    private fun updateUI() {
        binding.imageCounter.text = "${currentPosition + 1} / ${imagePaths.size}"
        binding.locationNameText.text = locationName
        binding.toolbar.title = "${currentPosition + 1} / ${imagePaths.size}"
    }

    private fun requestDownloadImage() {
        // Android 10+ doesn't need storage permission for MediaStore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            downloadCurrentImage()
        } else {
            // Android 9 and below need WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                downloadCurrentImage()
            } else {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun downloadCurrentImage() {
        try {
            val sourceFile = File(imagePaths[currentPosition])
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "PEVPlugShare_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PEVPlugShare")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let { imageUri ->
                    contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                        FileInputStream(sourceFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    
                    // Clear IS_PENDING flag to make the image visible in gallery
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(imageUri, values, null, null)
                    
                    Toast.makeText(this, "Image saved to Pictures/PEVPlugShare", Toast.LENGTH_LONG).show()
                } ?: run {
                    Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Use legacy approach for Android 9 and below
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "PEVPlugShare")
                if (!appDir.exists()) {
                    appDir.mkdirs()
                }
                
                val destFile = File(appDir, "PEVPlugShare_${System.currentTimeMillis()}.jpg")
                sourceFile.copyTo(destFile, overwrite = true)
                
                // Notify media scanner to make image visible in gallery
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = Uri.fromFile(destFile)
                sendBroadcast(mediaScanIntent)
                
                Toast.makeText(this, "Image saved to Pictures/PEVPlugShare", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_LONG).show()
            android.util.Log.e("FullImageViewer", "Error saving image", e)
        }
    }

    private fun shareCurrentImage() {
        try {
            val imageFile = File(imagePaths[currentPosition])
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                imageFile
            )

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Charging location image from PEVPlugShare")
                type = "image/jpeg"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share image"))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to share image: ${e.message}", Toast.LENGTH_SHORT).show()
            android.util.Log.e("FullImageViewer", "Error sharing image", e)
        }
    }
}
