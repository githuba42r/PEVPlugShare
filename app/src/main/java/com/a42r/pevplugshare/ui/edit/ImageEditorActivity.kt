package com.a42r.pevplugshare.ui.edit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.Toast
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.a42r.pevplugshare.R
import java.io.File
import java.io.FileOutputStream

/**
 * Activity for editing images: crop, zoom, and define focus areas
 */
class ImageEditorActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var zoomInButton: Button
    private lateinit var zoomOutButton: Button
    private lateinit var toggleFocusButton: Button
    private lateinit var clearFocusButton: Button
    private lateinit var cropButton: Button
    private lateinit var resetButton: Button
    private lateinit var saveButton: Button

    private lateinit var originalBitmap: Bitmap
    private lateinit var editBitmap: Bitmap
    private var currentZoom = 1.0f
    private var focusRect: RectF? = null
    private var isDrawingFocus = false
    private var focusStartX = 0f
    private var focusStartY = 0f

    companion object {
        const val EXTRA_IMAGE_PATH = "image_path"
        const val EXTRA_OUTPUT_PATH = "output_path"
        const val EXTRA_FOCUS_RECT = "focus_rect"
        const val EXTRA_METADATA = "metadata"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_editor)

        try {
            // Find views
            imageView = findViewById(R.id.imageView)
            zoomInButton = findViewById(R.id.zoomInButton)
            zoomOutButton = findViewById(R.id.zoomOutButton)
            toggleFocusButton = findViewById(R.id.toggleFocusButton)
            clearFocusButton = findViewById(R.id.clearFocusButton)
            cropButton = findViewById(R.id.cropButton)
            resetButton = findViewById(R.id.resetButton)
            saveButton = findViewById(R.id.saveButton)
        } catch (e: Exception) {
            Log.e("ImageEditorActivity", "Error finding views", e)
            Toast.makeText(this, "Error initializing UI: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH) ?: run {
            Toast.makeText(this, "No image path provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadImage(imagePath)
        setupUI()
    }

    private fun loadImage(imagePath: String) {
        try {
            val file = File(imagePath)
            if (!file.exists()) {
                throw Exception("Image file does not exist: $imagePath")
            }

            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap == null) {
                throw Exception("Failed to decode image: $imagePath")
            }

            originalBitmap = bitmap
            editBitmap = originalBitmap.copy(originalBitmap.config, true)
            imageView.setImageBitmap(editBitmap)
        } catch (e: Exception) {
            Log.e("ImageEditorActivity", "Error loading image", e)
            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupUI() {
        // Zoom controls
        zoomInButton.setOnClickListener {
            zoomImage(1.2f)
        }
        zoomOutButton.setOnClickListener {
            zoomImage(0.8f)
        }

        // Focus area controls
        toggleFocusButton.setOnClickListener {
            isDrawingFocus = !isDrawingFocus
            toggleFocusButton.text = if (isDrawingFocus) "Stop Focus" else "Add Focus Area"
            toggleFocusButton.setBackgroundColor(
                if (isDrawingFocus) getColor(android.R.color.holo_green_dark)
                else getColor(android.R.color.darker_gray)
            )
        }

        clearFocusButton.setOnClickListener {
            focusRect = null
            isDrawingFocus = false
            toggleFocusButton.text = "Add Focus Area"
            toggleFocusButton.setBackgroundColor(getColor(android.R.color.darker_gray))
            refreshImage()
        }

        // Image canvas touch handling
        imageView.setOnTouchListener { _, event ->
            if (isDrawingFocus) {
                handleFocusDrawing(event)
            }
            true
        }

        // Crop controls
        cropButton.setOnClickListener {
            cropImage()
        }

        // Reset button
        resetButton.setOnClickListener {
            editBitmap = originalBitmap.copy(originalBitmap.config, true)
            focusRect = null
            isDrawingFocus = false
            currentZoom = 1.0f
            toggleFocusButton.text = "Add Focus Area"
            toggleFocusButton.setBackgroundColor(getColor(android.R.color.darker_gray))
            refreshImage()
        }

        // Save button
        saveButton.setOnClickListener {
            saveImage()
        }
    }

    private fun zoomImage(factor: Float) {
        currentZoom *= factor
        currentZoom = currentZoom.coerceIn(0.5f, 3.0f)
        imageView.scaleX = currentZoom
        imageView.scaleY = currentZoom
    }

    private fun handleFocusDrawing(event: MotionEvent) {
        // Get ImageView dimensions and drawable area
        val imageViewWidth = imageView.width.toFloat()
        val imageViewHeight = imageView.height.toFloat()

        // Get the drawable area (where the actual image is displayed)
        // For fitCenter, the image is centered
        val displayedImageWidth = editBitmap.width.toFloat() * currentZoom
        val displayedImageHeight = editBitmap.height.toFloat() * currentZoom

        // Calculate the offset for centered image
        val offsetX = (imageViewWidth - displayedImageWidth) / 2f
        val offsetY = (imageViewHeight - displayedImageHeight) / 2f

        // Convert touch coordinates to bitmap coordinates
        // 1. Remove the centering offset
        // 2. Remove the zoom scaling
        // 3. Clamp to bitmap bounds
        val bitmapX = ((event.x - offsetX) / currentZoom).coerceIn(0f, editBitmap.width.toFloat())
        val bitmapY = ((event.y - offsetY) / currentZoom).coerceIn(0f, editBitmap.height.toFloat())

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                focusStartX = bitmapX
                focusStartY = bitmapY
                Log.d("ImageEditor", "Focus start: ($bitmapX, $bitmapY) - touch: (${event.x}, ${event.y}) - offset: ($offsetX, $offsetY)")
            }
            MotionEvent.ACTION_MOVE -> {
                val rect = RectF(
                    minOf(focusStartX, bitmapX),
                    minOf(focusStartY, bitmapY),
                    maxOf(focusStartX, bitmapX),
                    maxOf(focusStartY, bitmapY)
                )
                focusRect = rect
                refreshImageWithFocus()
            }
            MotionEvent.ACTION_UP -> {
                val rect = RectF(
                    minOf(focusStartX, bitmapX),
                    minOf(focusStartY, bitmapY),
                    maxOf(focusStartX, bitmapX),
                    maxOf(focusStartY, bitmapY)
                )
                focusRect = rect
                isDrawingFocus = false
                toggleFocusButton.text = "Add Focus Area"
                toggleFocusButton.setBackgroundColor(getColor(android.R.color.darker_gray))
                refreshImageWithFocus()
                Log.d("ImageEditor", "Focus end: rect=$rect")
            }
        }
    }

    private fun refreshImageWithFocus() {
        val bitmap = editBitmap.copy(editBitmap.config, true)
        val canvas = Canvas(bitmap)

        // Draw focus rectangle
        focusRect?.let { rect ->
            val paint = Paint().apply {
                color = 0xFFFFD700.toInt() // Gold color
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawRect(rect, paint)

            // Draw corner handles
            val handleSize = 15f
            val cornerPaint = Paint().apply {
                color = 0xFFFFD700.toInt()
                style = Paint.Style.FILL
            }

            // Draw corners
            val corners = listOf(
                Pair(rect.left - handleSize / 2, rect.top - handleSize / 2),
                Pair(rect.right - handleSize / 2, rect.top - handleSize / 2),
                Pair(rect.left - handleSize / 2, rect.bottom - handleSize / 2),
                Pair(rect.right - handleSize / 2, rect.bottom - handleSize / 2)
            )
            corners.forEach { (x, y) ->
                canvas.drawCircle(x + handleSize, y + handleSize, handleSize / 2, cornerPaint)
            }
        }

        imageView.setImageBitmap(bitmap)
    }

    private fun refreshImage() {
        imageView.setImageBitmap(editBitmap)
    }

    private fun cropImage() {
        focusRect?.let { rect ->
            val left = rect.left.toInt().coerceIn(0, editBitmap.width - 1)
            val top = rect.top.toInt().coerceIn(0, editBitmap.height - 1)
            val width = (rect.width()).toInt().coerceIn(1, editBitmap.width - left)
            val height = (rect.height()).toInt().coerceIn(1, editBitmap.height - top)

            editBitmap = Bitmap.createBitmap(editBitmap, left, top, width, height)
            focusRect = null
            isDrawingFocus = false
            toggleFocusButton.text = "Add Focus Area"
            currentZoom = 1.0f
            imageView.scaleX = 1f
            imageView.scaleY = 1f
            refreshImage()
            Toast.makeText(this, "Image cropped", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "Please draw a focus area first to crop", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImage() {
        try {
            val outputPath = intent.getStringExtra(EXTRA_OUTPUT_PATH) ?: run {
                Toast.makeText(this, "No output path provided", Toast.LENGTH_SHORT).show()
                return
            }

            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()

            FileOutputStream(outputFile).use { fos ->
                editBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }

            // Extract metadata from original image
            val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH) ?: ""
            val metadata = com.a42r.pevplugshare.util.PhotoUtils.getPhotoMetadata(imagePath)

            // Return focus rect and metadata
            intent.putExtra(EXTRA_FOCUS_RECT, focusRect)
            intent.putExtra(EXTRA_METADATA, metadata)
            setResult(RESULT_OK, intent)

            Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
