package com.a42r.pevplugshare.ui.edit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Matrix
import android.media.ExifInterface
import android.graphics.RectF
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.a42r.pevplugshare.R
import java.io.File
import java.io.FileOutputStream
import android.graphics.PointF
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog

/**
 * Activity for editing images: crop, zoom, and define focus areas
 */
class ImageEditorActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var instructionTextView: TextView // New
    private lateinit var mainControlsLayout: GridLayout // Changed from LinearLayout
    private lateinit var cropControlsLayout: LinearLayout // New
    private lateinit var focusControlsLayout: LinearLayout // New
    private lateinit var resetZoomIcon: ImageView // New

    // Main controls
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
    private var hasUnsavedChanges: Boolean = false // New flag to track changes

    // Pinch-to-zoom variables
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var matrix = Matrix()
    private var savedMatrix = Matrix()
    private val start = PointF()
    private val mid = PointF()
    private var mode = NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val MIN_ZOOM = 0.5f
    private val MAX_ZOOM = 5.0f

    enum class EditorMode {
        MAIN, CROP, FOCUS
    }

    companion object {
        const val EXTRA_IMAGE_PATH = "image_path"
        const val EXTRA_OUTPUT_PATH = "output_path"
        const val EXTRA_FOCUS_RECT = "focus_rect"
        const val EXTRA_METADATA = "metadata"
        const val MAX_IMAGE_TEXTURE_SIZE = 2048 // Maximum dimension for width or height

        const val NONE = 0
        const val DRAG = 1
        const val ZOOM = 2
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
            resetZoomIcon = findViewById(R.id.resetZoomIcon) // New

            // Main controls
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

    private fun setupIconButton(view: View, iconResId: Int, label: String) {
        view.findViewById<ImageView>(R.id.iconImageView).setImageResource(iconResId)
        view.findViewById<TextView>(R.id.labelTextView).text = label
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var scaleFactor = detector.scaleFactor
            val currentScale = FloatArray(9)
            matrix.getValues(currentScale)
            val prevScale = currentScale[Matrix.MSCALE_X]

            // Limit zoom in/out
            if (prevScale * scaleFactor > MAX_ZOOM) {
                scaleFactor = MAX_ZOOM / prevScale
            }
            if (prevScale * scaleFactor < MIN_ZOOM) {
                scaleFactor = MIN_ZOOM / prevScale
            }

            matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            imageView.imageMatrix = matrix
            currentZoom = prevScale * scaleFactor
            updateResetZoomIconVisibility()
            hasUnsavedChanges = true
            return true
        }
    }

    private fun setupUI() {
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())

        // Main menu controls
        rotateLeftButton.setOnClickListener {
            rotateBitmapUser(-90f)
            hasUnsavedChanges = true
        }

        rotateRightButton.setOnClickListener {
            rotateBitmapUser(90f)
            hasUnsavedChanges = true
        }

        focusButton.setOnClickListener {
            currentMode = EditorMode.FOCUS
            updateUIForMode(currentMode)
            // Initialize default focus rect
            focusRect = getDefaultRect()
            refreshImageWithOverlay()
            hasUnsavedChanges = true
        }

        cropButton.setOnClickListener {
            currentMode = EditorMode.CROP
            updateUIForMode(currentMode)
            // Initialize default crop rect
            cropRect = getDefaultRect()
            refreshImageWithOverlay()
            hasUnsavedChanges = true
        }

        resetButton.setOnClickListener {
            resetAllEdits()
        }

        saveButton.setOnClickListener {
            saveImage(false) // Save and finish
            hasUnsavedChanges = false // Reset changes flag
        }

        saveNewButton.setOnClickListener {
            saveImage(true) // Save as new and finish
            hasUnsavedChanges = false // Reset changes flag
        }

        cancelButton.setOnClickListener {
            if (hasUnsavedChanges) {
                showDiscardChangesDialog()
            } else {
                setResult(RESULT_CANCELED)
                finish()
            }
        }

        // Crop sub-menu controls
        cropCancelButton.setOnClickListener {
            currentMode = EditorMode.MAIN
            cropRect = null // Clear crop selection
            updateUIForMode(currentMode)
            refreshImageWithOverlay()
            // No change to hasUnsavedChanges here, as it's a cancel of a sub-operation
        }

        cropResetButton.setOnClickListener {
            cropRect = getDefaultRect()
            refreshImageWithOverlay()
            hasUnsavedChanges = true
        }

        cropApplyButton.setOnClickListener {
            applyCrop()
            currentMode = EditorMode.MAIN
            updateUIForMode(currentMode)
            refreshImageWithOverlay()
            hasUnsavedChanges = true // Applying crop is a change
        }

        // Focus sub-menu controls
        focusCancelButton.setOnClickListener {
            currentMode = EditorMode.MAIN
            focusRect = null // Clear focus selection
            updateUIForMode(currentMode)
            refreshImageWithOverlay()
            // No change to hasUnsavedChanges here, as it's a cancel of a sub-operation
        }

        focusResetButton.setOnClickListener {
            focusRect = getDefaultRect()
            refreshImageWithOverlay()
            hasUnsavedChanges = true
        }

        focusApplyButton.setOnClickListener {
            currentMode = EditorMode.MAIN
            updateUIForMode(currentMode)
            refreshImageWithOverlay()
            Toast.makeText(this, "Focus area saved", Toast.LENGTH_SHORT).show()
            hasUnsavedChanges = true // Applying focus is a change
        }

        // Reset Zoom Icon
        resetZoomIcon.setOnClickListener {
            resetZoom()
        }

        // Image canvas touch handling
        imageView.setOnTouchListener { v, event ->
            scaleGestureDetector?.onTouchEvent(event)

            val currentScale = FloatArray(9)
            matrix.getValues(currentScale)
            currentZoom = currentScale[Matrix.MSCALE_X]

            val handledByZoom = if (currentMode == EditorMode.MAIN) {
                val x = event.x
                val y = event.y

                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_DOWN -> {
                        savedMatrix.set(matrix)
                        start.set(x, y)
                        mode = DRAG
                        lastTouchX = x
                        lastTouchY = y
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        savedMatrix.set(matrix)
                        start.set(x, y)
                        mode = ZOOM
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                        mode = NONE
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (mode == DRAG) {
                            matrix.set(savedMatrix)
                            matrix.postTranslate(x - start.x, y - start.y)
                            hasUnsavedChanges = true
                        }
                    }
                }
                imageView.imageMatrix = matrix
                true // Event handled by zoom/drag
            } else {
                false // Not handled by zoom/drag in CROP/FOCUS mode
            }

            val handledByRectDrawing = if (!handledByZoom) {
                when (currentMode) {
                    EditorMode.CROP -> handleRectDrawing(event, RectType.CROP)
                    EditorMode.FOCUS -> handleRectDrawing(event, RectType.FOCUS)
                    else -> false
                }
            } else {
                false
            }

            updateResetZoomIconVisibility()
            return@setOnTouchListener handledByZoom || handledByRectDrawing
        }

        addRotationControls()
    }

    private fun showDiscardChangesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Discard Changes?")
            .setMessage("You have unsaved changes. Do you want to discard them?")
            .setPositiveButton("Discard") { dialog, _ ->
                dialog.dismiss()
                setResult(RESULT_CANCELED)
                finish()
            }
            .setNegativeButton("Keep Editing") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
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
        Log.d("RectDrawing", "handleRectDrawing called for type $type, event action: ${event.action}")
        val currentRect = if (type == RectType.CROP) cropRect else focusRect
        if (currentRect == null) {
            Log.d("RectDrawing", "handleRectDrawing: currentRect is null for type $type. Returning false.")
            return false
        }

        val rectHandler = RectSelectionHandler(type)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                activeRectType = type
                Log.d("RectDrawing", "Calling getActiveHandle with eventX=${event.x}, eventY=${event.y}, currentRect=$currentRect")
                activeHandle = rectHandler.getActiveHandle(event.x, event.y, currentRect)
                Log.d("RectDrawing", "ACTION_DOWN: type=$type, touch=(${event.x}, ${event.y}), activeHandle=$activeHandle")
                if (activeHandle != -1) {
                    // Change color to indicate active resizing
                    refreshImageWithOverlay(true, type)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeHandle != -1 && activeRectType == type) {
                    Log.d("RectDrawing", "ACTION_MOVE: type=$type, touch=(${event.x}, ${event.y}), activeHandle=$activeHandle, originalRect=$currentRect")
                    val newRect = rectHandler.adjustRect(currentRect, activeHandle, event.x, event.y)
                    if (type == RectType.CROP) cropRect = newRect else focusRect = newRect
                    refreshImageWithOverlay(true, type)
                    hasUnsavedChanges = true
                    Log.d("RectDrawing", "ACTION_MOVE: newRect=$newRect")
                }
            }
            MotionEvent.ACTION_UP -> {
                Log.d("RectDrawing", "ACTION_UP: type=$type, touch=(${event.x}, ${event.y}), activeHandle=$activeHandle")
                activeHandle = -1
                activeRectType = null
                refreshImageWithOverlay(false, type) // Revert color
                hasUnsavedChanges = true
            }
        }
        return true
    }

    private inner class RectSelectionHandler(private val type: RectType) {
        private val handleHalfSize = 80f // Changed half size of the square handles (total size 160f)
        private val minSize = 50f // Minimum size for the rect

        fun getActiveHandle(x: Float, y: Float, rect: RectF): Int {
            Log.d("RectSelectionHandler", "getActiveHandle: x=$x, y=$y, rect=$rect, handleHalfSize=$handleHalfSize")

            // Convert rect corners to view coordinates
            val rectCorners = floatArrayOf(
                rect.left, rect.top,
                rect.right, rect.top,
                rect.left, rect.bottom,
                rect.right, rect.bottom
            )
            imageView.imageMatrix.mapPoints(rectCorners)

            val viewRect = RectF(
                rectCorners[0], rectCorners[1],
                rectCorners[6], rectCorners[7] // Use bottom-right for width/height
            )

            // Adjust viewRect to be correct
            viewRect.left = minOf(rectCorners[0], rectCorners[2], rectCorners[4], rectCorners[6])
            viewRect.top = minOf(rectCorners[1], rectCorners[3], rectCorners[5], rectCorners[7])
            viewRect.right = maxOf(rectCorners[0], rectCorners[2], rectCorners[4], rectCorners[6])
            viewRect.bottom = maxOf(rectCorners[1], rectCorners[3], rectCorners[5], rectCorners[7])

            val viewHalfHandle = handleHalfSize * imageView.imageMatrix.mapRadius(1f) // Scale handle size by current zoom

            // Top-Left
            if (x >= viewRect.left - viewHalfHandle && x <= viewRect.left + viewHalfHandle &&
                y >= viewRect.top - viewHalfHandle && y <= viewRect.top + viewHalfHandle) {
                Log.d("RectSelectionHandler", "Handle: Top-Left (0) - Detected!")
                return 0
            }
            // Top-Right
            if (x >= viewRect.right - viewHalfHandle && x <= viewRect.right + viewHalfHandle &&
                y >= viewRect.top - viewHalfHandle && y <= viewRect.top + viewHalfHandle) {
                Log.d("RectSelectionHandler", "Handle: Top-Right (1) - Detected!")
                return 1
            }
            // Bottom-Left
            if (x >= viewRect.left - viewHalfHandle && x <= viewRect.left + viewHalfHandle &&
                y >= viewRect.bottom - viewHalfHandle && y <= viewRect.bottom + viewHalfHandle) {
                Log.d("RectSelectionHandler", "Handle: Bottom-Left (2) - Detected!")
                return 2
            }
            // Bottom-Right
            if (x >= viewRect.right - viewHalfHandle && x <= viewRect.right + viewHalfHandle &&
                y >= viewRect.bottom - viewHalfHandle && y <= viewRect.bottom + viewHalfHandle) {
                Log.d("RectSelectionHandler", "Handle: Bottom-Right (3) - Detected!")
                return 3
            }
            Log.d("RectSelectionHandler", "Handle: None (-1)")
            return -1
        }

        fun adjustRect(originalRect: RectF, handle: Int, newX: Float, newY: Float): RectF {
            Log.d("RectSelectionHandler", "adjustRect: originalRect=$originalRect, handle=$handle, newX=$newX, newY=$newY")
            var left = originalRect.left
            var top = originalRect.top
            var right = originalRect.right
            var bottom = originalRect.bottom

            // Convert newX, newY from view coordinates back to bitmap coordinates
            val bitmapCoords = imageViewToBitmapCoords(newX, newY)
            val bitmapNewX = bitmapCoords.x
            val bitmapNewY = bitmapCoords.y

            when (handle) {
                0 -> { // Top-Left
                    left = bitmapNewX.coerceAtMost(right - minSize)
                    top = bitmapNewY.coerceAtMost(bottom - minSize)
                }
                1 -> { // Top-Right
                    right = bitmapNewX.coerceAtLeast(left + minSize)
                    top = bitmapNewY.coerceAtMost(bottom - minSize)
                }
                2 -> { // Bottom-Left
                    left = bitmapNewX.coerceAtMost(right - minSize)
                    bottom = bitmapNewY.coerceAtLeast(top + minSize)
                }
                3 -> { // Bottom-Right
                    right = bitmapNewX.coerceAtLeast(left + minSize)
                    bottom = bitmapNewY.coerceAtLeast(top + minSize)
                }
            }

            // Ensure rect stays within image bounds
            left = left.coerceIn(0f, editBitmap.width.toFloat() - minSize)
            top = top.coerceIn(0f, editBitmap.height.toFloat() - minSize)
            right = right.coerceIn(left + minSize, editBitmap.width.toFloat())
            bottom = bottom.coerceIn(top + minSize, editBitmap.height.toFloat())

            val newRect = RectF(left, top, right, bottom)
            Log.d("RectSelectionHandler", "adjustRect: newRect=$newRect")
            return newRect
        }
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

        val currentImageMatrixValues = FloatArray(9)
        imageView.imageMatrix.getValues(currentImageMatrixValues)
        Log.d("RectDrawing", "refreshImageWithOverlay: Image Matrix = ${currentImageMatrixValues.contentToString()}")

        // Draw crop rectangle
        cropRect?.let { rect ->
            Log.d("RectDrawing", "refreshImageWithOverlay: Drawing Crop Rect = $rect")
            val paint = Paint().apply {
                color = if (isActive && type == RectType.CROP) 0xFFFF0000.toInt() else 0xFFCC0000.toInt() // Red or darker red
                style = Paint.Style.STROKE
                strokeWidth = 10f // Increased border width
            }
            canvas.drawRect(rect, paint)
            drawHandles(canvas, rect, paint.color)
        }

        // Draw focus rectangle
        focusRect?.let { rect ->
            Log.d("RectDrawing", "refreshImageWithOverlay: Drawing Focus Rect = $rect")
            val paint = Paint().apply {
                color = if (isActive && type == RectType.FOCUS) 0xFFFFFF00.toInt() else 0xFFCCCC00.toInt() // Yellow or darker yellow
                style = Paint.Style.STROKE
                strokeWidth = 10f // Increased border width
            }
            canvas.drawRect(rect, paint)
            // Only draw handles if not in MAIN mode
            if (currentMode != EditorMode.MAIN) {
                drawHandles(canvas, rect, paint.color)
            }
        }

        imageView.setImageBitmap(tempBitmap)
        updateResetZoomIconVisibility() // Update visibility after image refresh
    }

    private fun drawHandles(canvas: Canvas, rect: RectF, color: Int) {
        val handleHalfSize = 80f // Changed half size of the square handles (total size 160f)
        val cornerPaint = Paint().apply {
            this.color = color
            style = Paint.Style.FILL
        }

        // Top-Left
        canvas.drawRect(rect.left - handleHalfSize, rect.top - handleHalfSize,
            rect.left + handleHalfSize, rect.top + handleHalfSize, cornerPaint)
        // Top-Right
        canvas.drawRect(rect.right - handleHalfSize, rect.top - handleHalfSize,
            rect.right + handleHalfSize, rect.top + handleHalfSize, cornerPaint)
        // Bottom-Left
        canvas.drawRect(rect.left - handleHalfSize, rect.bottom - handleHalfSize,
            rect.left + handleHalfSize, rect.bottom + handleHalfSize, cornerPaint)
        // Bottom-Right
        canvas.drawRect(rect.right - handleHalfSize, rect.bottom - handleHalfSize,
            rect.right + handleHalfSize, rect.bottom + handleHalfSize, cornerPaint)
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
            matrix.reset() // Reset zoom matrix after crop
            imageView.imageMatrix = matrix
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

    private fun refreshImage() {
        refreshImageWithOverlay()
    }

    private fun updateResetZoomIconVisibility() {
        if (currentZoom > 1.0f + 0.01f || currentZoom < 1.0f - 0.01f) { // Check if zoomed in or out
            resetZoomIcon.visibility = View.VISIBLE
        } else {
            resetZoomIcon.visibility = View.GONE
        }
    }

    private fun resetZoom() {
        matrix.reset()
        imageView.imageMatrix = matrix
        currentZoom = 1.0f
        updateResetZoomIconVisibility()
        hasUnsavedChanges = true
    }

    private fun resetAllEdits() {
        editBitmap = originalBitmap.copy(originalBitmap.config, true)
        focusRect = null
        cropRect = null
        currentZoom = 1.0f
        matrix.reset() // Reset zoom matrix
        imageView.imageMatrix = matrix
        imageView.scaleX = 1f
        imageView.scaleY = 1f
        refreshImageWithOverlay()
        Toast.makeText(this, "All edits reset", Toast.LENGTH_SHORT).show()
        hasUnsavedChanges = false // Reset changes flag
        updateResetZoomIconVisibility()
    }

    private fun rotateBitmapUser(degrees: Float) {
        val matrix = Matrix().apply { postRotate(degrees) }
        editBitmap = Bitmap.createBitmap(editBitmap, 0, 0, editBitmap.width, editBitmap.height, matrix, true)
        refreshImage()
        hasUnsavedChanges = true
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
            imageView.imageMatrix = matrix // Apply initial matrix for zoom
            updateResetZoomIconVisibility()

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
