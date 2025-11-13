package com.a42r.pevplugshare.ui.photo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.databinding.ActivityPhotoCaptureBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class PhotoCaptureActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPhotoCaptureBinding
    private val viewModel: PhotoCaptureViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var cameraExecutor: ExecutorService? = null
    private var imageCapture: ImageCapture? = null
    private val locationId: String by lazy {
        intent.getStringExtra("location_id") ?: ""
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setupUI()
        setupObservers()
        requestCameraPermissionAndStart()
    }

    private fun setupUI() {
        binding.apply {
            captureButton.setOnClickListener { takePhoto() }
            cancelButton.setOnClickListener { finish() }
            acceptButton.setOnClickListener {
                collectPhotoData()
                viewModel.savePhoto(locationId)
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.photoCaptureState.collect { state ->
                when (state) {
                    is PhotoCaptureState.PhotoCaptured -> {
                        binding.previewView.visibility = android.view.View.GONE
                        binding.capturedPhotoView.visibility = android.view.View.VISIBLE
                        binding.capturedPhotoView.setImageBitmap(state.bitmap)
                        binding.captureButton.visibility = android.view.View.GONE
                        binding.acceptButton.visibility = android.view.View.VISIBLE
                    }
                    is PhotoCaptureState.PhotoSaved -> {
                        Toast.makeText(this@PhotoCaptureActivity, "Photo saved!", Toast.LENGTH_SHORT).show()
                        intent.putExtra("photo_saved", true)
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                    is PhotoCaptureState.Loading -> {
                        binding.progressBar.visibility = android.view.View.VISIBLE
                    }
                    is PhotoCaptureState.Error -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        Toast.makeText(this@PhotoCaptureActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun requestCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

                imageCapture = ImageCapture.Builder()
                    .setTargetRotation(windowManager.defaultDisplay.rotation)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture
                    )
                } catch (exc: Exception) {
                    Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            File(cacheDir, "temp_photo.jpg")
        ).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor!!,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(outputFileResults.savedUri?.path)
                        if (bitmap != null) {
                            viewModel.setPhotoBitmap(bitmap)
                            getCurrentLocation()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@PhotoCaptureActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@PhotoCaptureActivity, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                location?.let {
                    viewModel.updateLocation(it.latitude, it.longitude)
                }
            }
        }
    }

    private fun collectPhotoData() {
        val caption = binding.captionInput.text.toString()
        viewModel.updateCaption(caption)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
    }
}

