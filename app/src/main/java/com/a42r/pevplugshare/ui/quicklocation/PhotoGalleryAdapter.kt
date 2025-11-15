package com.a42r.pevplugshare.ui.quicklocation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.databinding.ItemPhotoThumbnailBinding
import com.a42r.pevplugshare.util.PhotoUtils
import com.a42r.pevplugshare.domain.repository.PhotoRepository
import com.a42r.pevplugshare.data.db.entity.PhotoEntity
import kotlinx.coroutines.*
import java.io.File

/**
 * Adapter for displaying and managing photos in gallery view
 */
class PhotoGalleryAdapter(
    private val photoPaths: MutableList<String>, // Changed to MutableList
    private val onPhotoClick: (String) -> Unit,
    private val onRemoveClick: (Int) -> Unit, // New callback for remove button
    private val photoRepository: PhotoRepository // Injected PhotoRepository
) : RecyclerView.Adapter<PhotoGalleryAdapter.PhotoViewHolder>() {

    // Track currently rendering items to avoid race conditions
    private val renderingJobs = mutableMapOf<String, Job>()

    inner class PhotoViewHolder(private val binding: ItemPhotoThumbnailBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(photoPath: String, position: Int) {
            Log.d("PhotoGalleryAdapter", "bind() called for position $position, photoPath: $photoPath")

            // Cancel any previous rendering job for this photo
            renderingJobs[photoPath]?.cancel()

            // Clear the image view immediately to show loading state
            binding.thumbnailImage.setImageResource(R.drawable.ic_add)

            // Load thumbnail in background
            val job = CoroutineScope(Dispatchers.Default).launch {
                try {
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 4 // Load smaller sample for performance
                    }
                    val bitmap = PhotoUtils.rotateImageIfNeeded(photoPath, options)

                    // Fetch focusRect from PhotoRepository
                    val photoEntity = photoRepository.getPhotoByFilePath(photoPath)
                    val focusRectString = photoEntity?.focusRectString

                    // Denormalize focus rect from normalized coordinates
                    val focusRect = focusRectString?.let { rectString ->
                        // Get the actual image dimensions to denormalize
                        val imageOptions = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeFile(photoPath, imageOptions)
                        val imageWidth = imageOptions.outWidth
                        val imageHeight = imageOptions.outHeight

                        Log.d("PhotoGalleryAdapter", "Image dimensions for denormalization: $imageWidth x $imageHeight")

                        // Denormalize the focus rectangle using actual image dimensions
                        PhotoUtils.normalizedStringToFocusRect(rectString, imageWidth, imageHeight)
                    }

                    withContext(Dispatchers.Main) {
                        if (bitmap != null) {
                            Log.d("PhotoGalleryAdapter", "Focus rect for $photoPath: ${focusRect?.let { "${it.left},${it.top},${it.right},${it.bottom}" } ?: "null"}")

                            if (focusRect != null) {
                                Log.d("PhotoGalleryAdapter", "Drawing yellow rectangle for $photoPath")
                                val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                                val canvas = android.graphics.Canvas(mutableBitmap)
                                val paint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.YELLOW
                                    style = android.graphics.Paint.Style.STROKE
                                    strokeWidth = 8f // Increased visibility for small thumbnails
                                    isAntiAlias = true
                                }
                                // Scale the focusRect to the thumbnail size
                                // The focusRect is based on original image, but bitmap is downsampled by inSampleSize
                                val inSampleSize = 4 // Must match the inSampleSize above
                                val scaleX = 1.0f / inSampleSize
                                val scaleY = 1.0f / inSampleSize
                                val scaledRect = android.graphics.RectF(
                                    focusRect.left * scaleX,
                                    focusRect.top * scaleY,
                                    focusRect.right * scaleX,
                                    focusRect.bottom * scaleY
                                )
                                Log.d("PhotoGalleryAdapter", "Scaled rect: ${scaledRect.left},${scaledRect.top},${scaledRect.right},${scaledRect.bottom} (scale: $scaleX x $scaleY)")
                                canvas.drawRect(scaledRect, paint)
                                binding.thumbnailImage.setImageBitmap(mutableBitmap)
                            } else {
                                Log.d("PhotoGalleryAdapter", "No focus rect for $photoPath, displaying plain bitmap")
                                binding.thumbnailImage.setImageBitmap(bitmap)
                            }
                        } else {
                            Log.w("PhotoGalleryAdapter", "Bitmap is null for $photoPath")
                            binding.thumbnailImage.setImageResource(R.drawable.ic_add) // Fallback
                        }
                        binding.focusIndicator.visibility = View.GONE // Always hide, as we draw directly
                    }
                } catch (e: Exception) {
                    Log.e("PhotoGalleryAdapter", "Error loading thumbnail for $photoPath", e)
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        binding.thumbnailImage.setImageResource(R.drawable.ic_add)
                    }
                }
            }
            // Track this rendering job
            renderingJobs[photoPath] = job

            binding.root.setOnClickListener {
                onPhotoClick(photoPath)
            }

            binding.removePhotoButton.setOnClickListener {
                onRemoveClick(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoThumbnailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photoPaths[position], position)
    }

    override fun getItemCount(): Int = photoPaths.size
}
