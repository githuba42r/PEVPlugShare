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
import android.content.Context
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.a42r.pevplugshare.domain.repository.PhotoRepository
import com.a42r.pevplugshare.data.db.entity.PhotoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity for editing images: crop, zoom, and define focus areas
 */
@AndroidEntryPoint
class ImageEditorActivity : AppCompatActivity() {
    @Inject
    lateinit var photoRepository: PhotoRepository
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
    private var persistedFocusRectString: String? = null // Store normalized focus string during load

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
        const val EXTRA_IS_SAVE_NEW = "is_save_new" // New constant
        const val MAX_IMAGE_TEXTURE_SIZE = 2048 // Maximum dimension for width or height

        const val EXIF_TAG_PEVPLUGSHARE_FOCUS_RECT = "PEVPlugShare_FocusRect"

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

        // Load focus data from SharedPreferences for the image being edited
        Log.d("ImageEditorActivity", "")
        Log.d("ImageEditorActivity", "📖 IMAGE EDITOR INITIALIZATION")
        Log.d("ImageEditorActivity", "   Image path: $imagePath")

        // Check if there's a saved focus in SharedPreferences
        val sharedPref = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        persistedFocusRectString = sharedPref.getString("focus_$imagePath", null)
        if (persistedFocusRectString != null) {
            Log.d("ImageEditorActivity", "✅ PERSISTED FOCUS FOUND IN SharedPreferences")
            Log.d("ImageEditorActivity", "   Normalized string: $persistedFocusRectString")
        } else {
            Log.d("ImageEditorActivity", "❌ NO PERSISTED FOCUS FOUND")
        }

        Log.d("ImageEditorActivity", "📐 LOADING IMAGE WITH ORIENTATION")
        loadImageAndOrient(imagePath) // Call the new function - sets editBitmap
        setupUI()
        Log.d("ImageEditorActivity", "   Bitmap loaded: ${editBitmap.width}x${editBitmap.height}")

        // If we loaded a persistent focus, denormalize it using the loaded image dimensions
        if (persistedFocusRectString != null) {
            Log.d("ImageEditorActivity", "🔄 DENORMALIZING PERSISTED FOCUS")
            Log.d("ImageEditorActivity", "   Normalized string: $persistedFocusRectString")
            Log.d("ImageEditorActivity", "   Target bitmap size: ${editBitmap.width}x${editBitmap.height}")
            focusRect = com.a42r.pevplugshare.util.PhotoUtils.normalizedStringToFocusRect(
                persistedFocusRectString,
                editBitmap.width,
                editBitmap.height
            )
            Log.d("ImageEditorActivity", "✅ FOCUS LOADED IN EDITOR")
            Log.d("ImageEditorActivity", "   Denormalized to: ${focusRect?.let { "L=${it.left} T=${it.top} R=${it.right} B=${it.bottom}" } ?: "null"}")
            // Do not set currentMode to FOCUS here, only display the rect
            refreshImageWithOverlay()
            Log.d("ImageEditorActivity", "   Status: Focus displayed on overlay")
        } else {
            Log.d("ImageEditorActivity", "ℹ️  NO FOCUS TO LOAD - User will create new focus if needed")
        }
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
            // Initialize default focus rect ONLY if it's null
            if (focusRect == null) {
                focusRect = getDefaultRect()
                Log.d("ImageEditorActivity", "🎯 FOCUS MODE ENTERED - FOCUS INITIALIZED")
                Log.d("ImageEditorActivity", "   Status: New focus rect created")
                Log.d("ImageEditorActivity", "   Coordinates: ${focusRect?.let { "${it.left},${it.top},${it.right},${it.bottom}" } ?: "null"}")
            } else {
                Log.d("ImageEditorActivity", "🎯 FOCUS MODE ENTERED - FOCUS LOADED")
                Log.d("ImageEditorActivity", "   Status: Existing focus retained")
                Log.d("ImageEditorActivity", "   Coordinates: ${focusRect?.let { "${it.left},${it.top},${it.right},${it.bottom}" } ?: "null"}")
            }
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
            Log.d("ImageEditorActivity", "✅ FOCUS APPLY BUTTON CLICKED")
            Log.d("ImageEditorActivity", "   Status: User confirmed focus region")
            Log.d("ImageEditorActivity", "   Coordinates: ${focusRect?.let { "${it.left},${it.top},${it.right},${it.bottom}" } ?: "null"}")
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
        val currentRect = if (type == RectType.CROP) cropRect else focusRect
        if (currentRect == null) {
            return false
        }

        val rectHandler = RectSelectionHandler(type)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                activeRectType = type
                activeHandle = rectHandler.getActiveHandle(event.x, event.y, currentRect)
                if (activeHandle != -1) {
                    // Change color to indicate active resizing
                    refreshImageWithOverlay(true, type)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeHandle != -1 && activeRectType == type) {
                    val newRect = rectHandler.adjustRect(currentRect, activeHandle, event.x, event.y)
                    if (type == RectType.CROP) cropRect = newRect else focusRect = newRect
                    refreshImageWithOverlay(true, type)
                    hasUnsavedChanges = true
                }
            }
            MotionEvent.ACTION_UP -> {
                activeHandle = -1
                activeRectType = null
                refreshImageWithOverlay(false, type) // Revert color
                hasUnsavedChanges = true
            }
        }
        return true
    }

    private inner class RectSelectionHandler(private val type: RectType) {
        private val handleHalfSize = 40f // Changed half size of the square handles (total size 80f)
        private val minSize = 50f // Minimum size for the rect

        fun getActiveHandle(x: Float, y: Float, rect: RectF): Int {
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
                return 0
            }
            // Top-Right
            if (x >= viewRect.right - viewHalfHandle && x <= viewRect.right + viewHalfHandle &&
                y >= viewRect.top - viewHalfHandle && y <= viewRect.top + viewHalfHandle) {
                return 1
            }
            // Bottom-Left
            if (x >= viewRect.left - viewHalfHandle && x <= viewRect.left + viewHalfHandle &&
                y >= viewRect.bottom - viewHalfHandle && y <= viewRect.bottom + viewHalfHandle) {
                return 2
            }
            // Bottom-Right
            if (x >= viewRect.right - viewHalfHandle && x <= viewRect.right + viewHalfHandle &&
                y >= viewRect.bottom - viewHalfHandle && y <= viewRect.bottom + viewHalfHandle) {
                return 3
            }
            return -1
        }

        fun adjustRect(originalRect: RectF, handle: Int, newX: Float, newY: Float): RectF {
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

        // Draw crop rectangle
        cropRect?.let { rect ->
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
        val handleHalfSize = 40f // Changed half size of the square handles (total size 80f)
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
        cropRect?.let { cropArea ->
            val left = cropArea.left.toInt().coerceIn(0, editBitmap.width - 1)
            val top = cropArea.top.toInt().coerceIn(0, editBitmap.height - 1)
            val width = (cropArea.width()).toInt().coerceIn(1, editBitmap.width - left)
            val height = (cropArea.height()).toInt().coerceIn(1, editBitmap.height - top)

            // Calculate relative position and size of focusRect BEFORE cropping
            var newFocusRect: RectF? = null
            focusRect?.let { currentFocus ->
                // Calculate ratios relative to the cropArea
                val relativeLeft = (currentFocus.left - cropArea.left) / cropArea.width()
                val relativeTop = (currentFocus.top - cropArea.top) / cropArea.height()
                val relativeRight = (currentFocus.right - cropArea.left) / cropArea.width()
                val relativeBottom = (currentFocus.bottom - cropArea.top) / cropArea.height()

                // Ensure focus rect is within crop area, if not, adjust or nullify
                if (currentFocus.intersect(cropArea)) {
                    // Apply ratios to the NEWLY cropped bitmap dimensions
                    val croppedBitmapWidth = width.toFloat()
                    val croppedBitmapHeight = height.toFloat()

                    val newLeft = relativeLeft * croppedBitmapWidth
                    val newTop = relativeTop * croppedBitmapHeight
                    val newRight = relativeRight * croppedBitmapWidth
                    val newBottom = relativeBottom * croppedBitmapHeight

                    newFocusRect = RectF(newLeft, newTop, newRight, newBottom)
                } else {
                    // If focusRect is completely outside cropArea, it's gone
                    newFocusRect = null
                }
            }

            editBitmap = Bitmap.createBitmap(editBitmap, left, top, width, height)
            cropRect = null // Clear crop selection after applying
            currentZoom = 1.0f
            matrix.reset() // Reset zoom matrix after crop
            imageView.imageMatrix = matrix
            imageView.scaleX = 1f
            imageView.scaleY = 1f

            focusRect = newFocusRect // Update focusRect with the new, relative position

            Toast.makeText(this, "Image cropped", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "No crop area defined", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImage(saveAsNew: Boolean) {
        try {
            Log.d("ImageEditorActivity", "💾 SAVE IMAGE INITIATED")
            Log.d("ImageEditorActivity", "   Save type: ${if (saveAsNew) "SAVE AS NEW" else "UPDATE EXISTING"}")
            Log.d("ImageEditorActivity", "   Focus rect at save: ${focusRect?.let { "${it.left},${it.top},${it.right},${it.bottom}" } ?: "null"}")

            val outputPath = intent.getStringExtra(EXTRA_OUTPUT_PATH) ?: run {
                Log.e("ImageEditorActivity", "❌ No output path provided")
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
            Log.d("ImageEditorActivity", "Image file saved to: ${outputFile.absolutePath}")

            // Extract metadata from original image
            val originalImagePath = intent.getStringExtra(EXTRA_IMAGE_PATH) ?: ""
            val metadata = com.a42r.pevplugshare.util.PhotoUtils.getPhotoMetadata(originalImagePath)

            // Return focus rect and metadata
            val resultIntent = Intent().apply {
                var normalizedFocusRectString: String? = null
                focusRect?.let { rect ->
                    // Normalize focus rectangle to 0-1 coordinates relative to the edited bitmap
                    normalizedFocusRectString = com.a42r.pevplugshare.util.PhotoUtils.focusRectToNormalizedString(
                        rect,
                        editBitmap.width,
                        editBitmap.height
                    )
                    Log.d("ImageEditorActivity", "")
                    Log.d("ImageEditorActivity", "✏️  FOCUS SERIALIZATION FOR RETURN")
                    Log.d("ImageEditorActivity", "   Pixel coords: L=${rect.left} T=${rect.top} R=${rect.right} B=${rect.bottom}")
                    Log.d("ImageEditorActivity", "   Bitmap size: ${editBitmap.width}x${editBitmap.height}")
                    Log.d("ImageEditorActivity", "   Normalized: $normalizedFocusRectString")
                    Log.d("ImageEditorActivity", "   Storing to SharedPreferences: YES")

                    // Store focus to SharedPreferences for temporary storage
                    androidx.preference.PreferenceManager.getDefaultSharedPreferences(this@ImageEditorActivity)
                        .edit()
                        .putString("focus_${outputFile.absolutePath}", normalizedFocusRectString)
                        .apply()

                    putExtra(EXTRA_FOCUS_RECT, normalizedFocusRectString)
                } ?: run {
                    // If focusRect is null, explicitly put null or an empty string
                    Log.d("ImageEditorActivity", "❌ FOCUS SERIALIZATION: focusRect IS NULL")
                    Log.d("ImageEditorActivity", "   Storing to SharedPreferences: NO")
                    putExtra(EXTRA_FOCUS_RECT, null as String?)
                }
                putExtra(EXTRA_METADATA, metadata)
                putExtra(EXTRA_OUTPUT_PATH, outputFile.absolutePath) // Return the actual saved path
                putExtra(EXTRA_IS_SAVE_NEW, saveAsNew) // Indicate if it was a "Save New" operation
            }
            setResult(RESULT_OK, resultIntent)

            Log.d("ImageEditorActivity", "Image save complete. Output file: ${outputFile.absolutePath}")
            Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Log.e("ImageEditorActivity", "Error saving image", e)
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
