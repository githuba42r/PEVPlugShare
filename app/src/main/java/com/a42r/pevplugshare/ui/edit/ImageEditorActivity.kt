package com.a42r.pevplugshare.ui.edit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Matrix
import android.media.ExifInterface
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View // Import View
import android.widget.ImageView
import android.widget.Toast
import android.widget.Button
import android.widget.TextView // Import TextView
import android.widget.LinearLayout // Import LinearLayout
import android.widget.GridLayout // Import GridLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.a42r.pevplugshare.R
import java.io.File
import java.io.FileOutputStream
import android.graphics.PointF // Import PointF
import android.content.Intent // Import Intent
import androidx.annotation.DrawableRes // Import DrawableRes

/**
 * Activity for editing images: crop, zoom, and define focus areas
 */
class ImageEditorActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var instructionTextView: TextView // New
    private lateinit var mainControlsLayout: GridLayout // Changed from LinearLayout
    private lateinit var cropControlsLayout: LinearLayout // New
    private lateinit var focusControlsLayout: LinearLayout // New

    // Main controls
    private lateinit var zoomInButton: View
    private lateinit var zoomOutButton: View
    private lateinit var rotateLeftButton: View
    private lateinit var rotateRightButton: View
    private lateinit var focusButton: View
    private lateinit var cropButton: View
    private lateinit var resetButton: View
    private lateinit var saveButton: View
    private lateinit var saveNewButton: View
    private lateinit var cancelButton: View

    // Crop sub-menu controls
    private lateinit var cropCancelButton: View
    private lateinit var cropResetButton: View
    private lateinit var cropApplyButton: View

    // Focus sub-menu controls
    private lateinit var focusCancelButton: View
    private lateinit var focusResetButton: View
    private lateinit var focusApplyButton: View

    private lateinit var originalBitmap: Bitmap
    private lateinit var editBitmap: Bitmap
    private var currentZoom = 1.0f
    private var focusRect: RectF? = null
    private var cropRect: RectF? = null // New for crop region
    private var currentMode: EditorMode = EditorMode.MAIN // New state management

    enum class EditorMode {
        MAIN, CROP, FOCUS
    }

    companion object {
        const val EXTRA_IMAGE_PATH = "image_path"
        const val EXTRA_OUTPUT_PATH = "output_path"
        const val EXTRA_FOCUS_RECT = "focus_rect"
        const val EXTRA_METADATA = "metadata"
        const val MAX_IMAGE_TEXTURE_SIZE = 2048 // Maximum dimension for width or height
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_editor)

        try {
            // Find views
            imageView = findViewById(R.id.imageView)
            instructionTextView = findViewById(R.id.instructionTextView)
            mainControlsLayout = findViewById<GridLayout>(R.id.mainControlsLayout)
            cropControlsLayout = findViewById<LinearLayout>(R.id.cropControlsLayout)
            focusControlsLayout = findViewById<LinearLayout>(R.id.focusControlsLayout)

            // Main controls
            zoomInButton = findViewById(R.id.zoomInButton)
            setupIconButton(zoomInButton, R.drawable.ic_zoom_in, "Zoom +")

            zoomOutButton = findViewById(R.id.zoomOutButton)
            setupIconButton(zoomOutButton, R.drawable.ic_zoom_out, "Zoom -")

            rotateLeftButton = findViewById(R.id.rotateLeftButton)
            setupIconButton(rotateLeftButton, R.drawable.ic_rotate_left, "Rotate Left")

            rotateRightButton = findViewById(R.id.rotateRightButton)
            setupIconButton(rotateRightButton, R.drawable.ic_rotate_right, "Rotate Right")

            focusButton = findViewById(R.id.focusButton)
            setupIconButton(focusButton, R.drawable.ic_focus, "Focus")

            cropButton = findViewById(R.id.cropButton)
            setupIconButton(cropButton, R.drawable.ic_crop, "Crop")

            resetButton = findViewById(R.id.resetButton)
            setupIconButton(resetButton, R.drawable.ic_reset, "Reset")

            saveButton = findViewById(R.id.saveButton)
            setupIconButton(saveButton, R.drawable.ic_save, "Save")

            saveNewButton = findViewById(R.id.saveNewButton)
            setupIconButton(saveNewButton, R.drawable.ic_save_new, "Save New")

            cancelButton = findViewById(R.id.cancelButton)
            setupIconButton(cancelButton, R.drawable.ic_cancel, "Cancel")

            // Crop sub-menu controls
            cropCancelButton = findViewById(R.id.cropCancelButton)
            setupIconButton(cropCancelButton, R.drawable.ic_cancel, "Cancel")

            cropResetButton = findViewById(R.id.cropResetButton)
            setupIconButton(cropResetButton, R.drawable.ic_reset, "Reset")

            cropApplyButton = findViewById(R.id.cropApplyButton)
            setupIconButton(cropApplyButton, R.drawable.ic_check, "Apply")

            // Focus sub-menu controls
            focusCancelButton = findViewById(R.id.focusCancelButton)
            setupIconButton(focusCancelButton, R.drawable.ic_cancel, "Cancel")

            focusResetButton = findViewById(R.id.focusResetButton)
            setupIconButton(focusResetButton, R.drawable.ic_reset, "Reset")

            focusApplyButton = findViewById(R.id.focusApplyButton)
            setupIconButton(focusApplyButton, R.drawable.ic_check, "Apply")

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

        loadImageAndOrient(imagePath) // Call the new function
        setupUI()
        updateUIForMode(EditorMode.MAIN) // Set initial UI state
    }

    private fun setupIconButton(view: View, @DrawableRes iconResId: Int, label: String) {
        view.findViewById<ImageView>(R.id.iconImageView).setImageResource(iconResId)
        view.findViewById<TextView>(R.id.labelTextView).text = label
    }



    private fun setupUI() {
        // Zoom controls
        zoomInButton.setOnClickListener {
            zoomImage(1.2f)
        }
        zoomOutButton.setOnClickListener {
            zoomImage(0.8f)
        }

        // Main menu controls
        focusButton.setOnClickListener {
            currentMode = EditorMode.FOCUS
            updateUIForMode(currentMode)
            // Initialize default focus rect
            focusRect = getDefaultRect()
            refreshImageWithOverlay()
        }

        cropButton.setOnClickListener {
            currentMode = EditorMode.CROP
            updateUIForMode(currentMode)
            // Initialize default crop rect
            cropRect = getDefaultRect()
            refreshImageWithOverlay()
        }

        resetButton.setOnClickListener {
            editBitmap = originalBitmap.copy(originalBitmap.config, true)
            focusRect = null
            cropRect = null
            currentZoom = 1.0f
            imageView.scaleX = 1f
            imageView.scaleY = 1f
            refreshImageWithOverlay()
            Toast.makeText(this, "All edits reset", Toast.LENGTH_SHORT).show()
        }

        saveButton.setOnClickListener {
            saveImage(false) // Save and finish
        }

        saveNewButton.setOnClickListener {
            saveImage(true) // Save as new and finish
        }

        cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        // Crop sub-menu controls
        cropCancelButton.setOnClickListener {
            currentMode = EditorMode.MAIN
            cropRect = null // Clear crop selection
            updateUIForMode(currentMode)
            refreshImageWithOverlay()
        }

        cropResetButton.setOnClickListener {
            cropRect = getDefaultRect()
            refreshImageWithOverlay()
        }

        cropApplyButton.setOnClickListener {
            applyCrop()
            currentMode = EditorMode.MAIN
            updateUIForMode(currentMode)
            refreshImageWithOverlay()
        }

        // Focus sub-menu controls
        focusCancelButton.setOnClickListener {
            currentMode = EditorMode.MAIN
            focusRect = null // Clear focus selection
            updateUIForMode(currentMode)
            refreshImageWithOverlay()
        }

        focusResetButton.setOnClickListener {
            focusRect = getDefaultRect()
            refreshImageWithOverlay()
        }

        focusApplyButton.setOnClickListener {
            currentMode = EditorMode.MAIN
            updateUIForMode(currentMode)
            refreshImageWithOverlay()
            Toast.makeText(this, "Focus area saved", Toast.LENGTH_SHORT).show()
        }

        // Image canvas touch handling
        imageView.setOnTouchListener { _, event ->
            when (currentMode) {
                EditorMode.CROP -> handleRectDrawing(event, RectType.CROP)
                EditorMode.FOCUS -> handleRectDrawing(event, RectType.FOCUS)
                else -> false // No drawing in MAIN mode
            }
        }

        addRotationControls()
    }

    private fun updateUIForMode(mode: EditorMode) {
        when (mode) {
            EditorMode.MAIN -> {
                mainControlsLayout.visibility = View.VISIBLE
                cropControlsLayout.visibility = View.GONE
                focusControlsLayout.visibility = View.GONE
                instructionTextView.text = "Edit Image"
            }
            EditorMode.CROP -> {
                mainControlsLayout.visibility = View.GONE
                cropControlsLayout.visibility = View.VISIBLE
                focusControlsLayout.visibility = View.GONE
                instructionTextView.text = "Drag to adjust crop area. Tap Apply when done."
            }
            EditorMode.FOCUS -> {
                mainControlsLayout.visibility = View.GONE
                cropControlsLayout.visibility = View.GONE
                focusControlsLayout.visibility = View.VISIBLE
                instructionTextView.text = "Drag to adjust focus area. Tap Apply when done."
            }
        }
    }

    private fun getDefaultRect(): RectF {
        val imageWidth = editBitmap.width.toFloat()
        val imageHeight = editBitmap.height.toFloat()
        val defaultSize = minOf(imageWidth, imageHeight) * 0.7f
        val left = (imageWidth - defaultSize) / 2
        val top = (imageHeight - defaultSize) / 2
        return RectF(left, top, left + defaultSize, top + defaultSize)
    }

    private enum class RectType {
        CROP, FOCUS
    }

    private var activeRectType: RectType? = null
    private var activeHandle = -1 // 0-TL, 1-TR, 2-BL, 3-BR, 4-Move

    private fun handleRectDrawing(event: MotionEvent, type: RectType): Boolean {
        val currentRect = if (type == RectType.CROP) cropRect else focusRect
        if (currentRect == null) return false

        // Convert touch coordinates to bitmap coordinates
        val bitmapCoords = imageViewToBitmapCoords(event.x, event.y)
        val bitmapX = bitmapCoords.x
        val bitmapY = bitmapCoords.y

        val handleSize = 30f // Larger handle size for easier interaction

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                activeRectType = type
                activeHandle = getActiveHandle(bitmapX, bitmapY, currentRect, handleSize)
                if (activeHandle != -1) {
                    // Change color to indicate active resizing
                    refreshImageWithOverlay(true, type)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeHandle != -1 && activeRectType == type) {
                    val newRect = adjustRect(currentRect, activeHandle, bitmapX, bitmapY)
                    if (type == RectType.CROP) cropRect = newRect else focusRect = newRect
                    refreshImageWithOverlay(true, type)
                }
            }
            MotionEvent.ACTION_UP -> {
                activeHandle = -1
                activeRectType = null
                refreshImageWithOverlay(false, type) // Revert color
            }
        }
        return true
    }

    private fun getActiveHandle(x: Float, y: Float, rect: RectF, handleSize: Float): Int {
        val halfHandle = handleSize / 2
        // Top-Left
        if (x >= rect.left - halfHandle && x <= rect.left + halfHandle &&
            y >= rect.top - halfHandle && y <= rect.top + halfHandle) return 0
        // Top-Right
        if (x >= rect.right - halfHandle && x <= rect.right + halfHandle &&
            y >= rect.top - halfHandle && y <= rect.top + halfHandle) return 1
        // Bottom-Left
        if (x >= rect.left - halfHandle && x <= rect.left + halfHandle &&
            y >= rect.bottom - halfHandle && y <= rect.bottom + halfHandle) return 2
        // Bottom-Right
        if (x >= rect.right - halfHandle && x <= rect.right + halfHandle &&
            y >= rect.bottom - halfHandle && y <= rect.bottom + halfHandle) return 3
        // Move (inside the rect)
        if (x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom) return 4
        return -1
    }

    private fun adjustRect(originalRect: RectF, handle: Int, newX: Float, newY: Float): RectF {
        val minSize = 50f // Minimum size for the rect
        var left = originalRect.left
        var top = originalRect.top
        var right = originalRect.right
        var bottom = originalRect.bottom

        when (handle) {
            0 -> { // Top-Left
                left = newX.coerceAtMost(right - minSize)
                top = newY.coerceAtMost(bottom - minSize)
            }
            1 -> { // Top-Right
                right = newX.coerceAtLeast(left + minSize)
                top = newY.coerceAtMost(bottom - minSize)
            }
            2 -> { // Bottom-Left
                left = newX.coerceAtMost(right - minSize)
                bottom = newY.coerceAtLeast(top + minSize)
            }
            3 -> { // Bottom-Right
                right = newX.coerceAtLeast(left + minSize)
                bottom = newY.coerceAtLeast(top + minSize)
            }
            4 -> { // Move
                val dx = newX - originalRect.centerX()
                val dy = newY - originalRect.centerY()
                left += dx
                top += dy
                right += dx
                bottom += dy
            }
        }

        // Ensure rect stays within image bounds
        left = left.coerceIn(0f, editBitmap.width.toFloat() - minSize)
        top = top.coerceIn(0f, editBitmap.height.toFloat() - minSize)
        right = right.coerceIn(left + minSize, editBitmap.width.toFloat())
        bottom = bottom.coerceIn(top + minSize, editBitmap.height.toFloat())

        return RectF(left, top, right, bottom)
    }

    private fun imageViewToBitmapCoords(x: Float, y: Float): PointF {
        val matrix = Matrix()
        imageView.imageMatrix.invert(matrix)
        val points = floatArrayOf(x, y)
        matrix.mapPoints(points)
        return PointF(points[0], points[1])
    }

    private fun refreshImageWithOverlay(isActive: Boolean = false, type: RectType? = null) {
        val tempBitmap = editBitmap.copy(editBitmap.config, true)
        val canvas = Canvas(tempBitmap)

        // Draw crop rectangle
        cropRect?.let { rect ->
            val paint = Paint().apply {
                color = if (isActive && type == RectType.CROP) 0xFFFF0000.toInt() else 0xFFCC0000.toInt() // Red or darker red
                style = Paint.Style.STROKE
                strokeWidth = 5f
            }
            canvas.drawRect(rect, paint)
            drawHandles(canvas, rect, paint.color)
        }

        // Draw focus rectangle
        focusRect?.let { rect ->
            val paint = Paint().apply {
                color = if (isActive && type == RectType.FOCUS) 0xFFFFFF00.toInt() else 0xFFCCCC00.toInt() // Yellow or darker yellow
                style = Paint.Style.STROKE
                strokeWidth = 5f
            }
            canvas.drawRect(rect, paint)
            drawHandles(canvas, rect, paint.color)
        }

        imageView.setImageBitmap(tempBitmap)
    }

    private fun drawHandles(canvas: Canvas, rect: RectF, color: Int) {
        val handleSize = 20f // Size of the square handles
        val cornerPaint = Paint().apply {
            this.color = color
            style = Paint.Style.FILL
        }

        // Top-Left
        canvas.drawRect(rect.left - handleSize, rect.top - handleSize,
            rect.left + handleSize, rect.top + handleSize, cornerPaint)
        // Top-Right
        canvas.drawRect(rect.right - handleSize, rect.top - handleSize,
            rect.right + handleSize, rect.top + handleSize, cornerPaint)
        // Bottom-Left
        canvas.drawRect(rect.left - handleSize, rect.bottom - handleSize,
            rect.left + handleSize, rect.bottom + handleSize, cornerPaint)
        // Bottom-Right
        canvas.drawRect(rect.right - handleSize, rect.bottom - handleSize,
            rect.right + handleSize, rect.bottom + handleSize, cornerPaint)
    }

    private fun applyCrop() {
        cropRect?.let { rect ->
            val left = rect.left.toInt().coerceIn(0, editBitmap.width - 1)
            val top = rect.top.toInt().coerceIn(0, editBitmap.height - 1)
            val width = (rect.width()).toInt().coerceIn(1, editBitmap.width - left)
            val height = (rect.height()).toInt().coerceIn(1, editBitmap.height - top)

            editBitmap = Bitmap.createBitmap(editBitmap, left, top, width, height)
            cropRect = null // Clear crop selection after applying
            currentZoom = 1.0f
            imageView.scaleX = 1f
            imageView.scaleY = 1f
            Toast.makeText(this, "Image cropped", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "No crop area defined", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImage(saveAsNew: Boolean) {
        try {
            val outputPath = intent.getStringExtra(EXTRA_OUTPUT_PATH) ?: run {
                Toast.makeText(this, "No output path provided", Toast.LENGTH_SHORT).show()
                return
            }

            val outputFile = if (saveAsNew) {
                // Generate a new unique file name for "Save New"
                val originalFile = File(outputPath)
                val parentDir = originalFile.parentFile
                val fileNameWithoutExtension = originalFile.nameWithoutExtension
                val fileExtension = originalFile.extension
                File(parentDir, "${fileNameWithoutExtension}_${System.currentTimeMillis()}.$fileExtension")
            } else {
                File(outputPath)
            }

            outputFile.parentFile?.mkdirs()

            FileOutputStream(outputFile).use { fos ->
                editBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }

            // Extract metadata from original image
            val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH) ?: ""
            val metadata = com.a42r.pevplugshare.util.PhotoUtils.getPhotoMetadata(imagePath)

            // Return focus rect and metadata
            val resultIntent = Intent().apply {
                putExtra(EXTRA_FOCUS_RECT, focusRect)
                putExtra(EXTRA_METADATA, metadata)
                putExtra(EXTRA_OUTPUT_PATH, outputFile.absolutePath) // Return the actual saved path
            }
            setResult(RESULT_OK, resultIntent)

            Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun zoomImage(factor: Float) {
        currentZoom *= factor
        currentZoom = currentZoom.coerceIn(0.5f, 3.0f)
        imageView.scaleX = currentZoom
        imageView.scaleY = currentZoom
    }

    private fun refreshImage() {
        refreshImageWithOverlay()
    }





    private fun rotateBitmapUser(degrees: Float) {
        val matrix = Matrix().apply { postRotate(degrees) }
        editBitmap = Bitmap.createBitmap(editBitmap, 0, 0, editBitmap.width, editBitmap.height, matrix, true)
        refreshImage()
    }

    private fun addRotationControls() {
        rotateLeftButton.setOnClickListener {
            rotateBitmapUser(-90f)
        }

        rotateRightButton.setOnClickListener {
            rotateBitmapUser(90f)
        }
    }

    // New function to handle image loading and auto-orientation
    private fun loadImageAndOrient(imagePath: String) {
        try {
            val file = File(imagePath)
            if (!file.exists()) {
                throw Exception("Image file does not exist: $imagePath")
            }

            // Decode sampled bitmap to prevent OOM errors
            var decodedBitmap = decodeSampledBitmapFromFile(imagePath, MAX_IMAGE_TEXTURE_SIZE, MAX_IMAGE_TEXTURE_SIZE)
                ?: throw Exception("Failed to decode image: $imagePath")

            // Apply EXIF orientation
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            decodedBitmap = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(decodedBitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(decodedBitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(decodedBitmap, 270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipBitmap(decodedBitmap, true, false)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> flipBitmap(decodedBitmap, false, true)
                else -> decodedBitmap
            }

            originalBitmap = decodedBitmap
            editBitmap = originalBitmap.copy(originalBitmap.config, true)
            imageView.setImageBitmap(editBitmap)

        } catch (e: Exception) {
            Log.e("ImageEditorActivity", "Error loading and orienting image", e)
            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun decodeSampledBitmapFromFile(imagePath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        // First decode with inJustDecodeBounds=true to check dimensions
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(imagePath, this)

            // Calculate inSampleSize
            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set
            inJustDecodeBounds = false
            BitmapFactory.decodeFile(imagePath, this)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun rotateBitmap(bitmap: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degree) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun flipBitmap(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) preScale(-1f, 1f, bitmap.width / 2f, 0f)
            if (vertical) preScale(1f, -1f, 0f, bitmap.height / 2f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
