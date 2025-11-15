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
import kotlinx.coroutines.*
import java.io.File

/**
 * Adapter for displaying and managing photos in gallery view
 */
class PhotoGalleryAdapter(
    private val photoPaths: MutableList<String>, // Changed to MutableList
    private val focusAreas: MutableMap<String, android.graphics.RectF>, // Changed to MutableMap
    private val onPhotoClick: (String) -> Unit,
    private val onRemoveClick: (Int) -> Unit // New callback for remove button
) : RecyclerView.Adapter<PhotoGalleryAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(private val binding: ItemPhotoThumbnailBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(photoPath: String, position: Int) {
            Log.d("PhotoGalleryAdapter", "bind() called for position $position, photoPath: $photoPath")
            Log.d("PhotoGalleryAdapter", "Total focusAreas in map: ${focusAreas.size}")
            Log.d("PhotoGalleryAdapter", "focusAreas keys: ${focusAreas.keys.joinToString(", ")}")

            // Load thumbnail in background
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 4 // Load smaller sample for performance
                    }
                    val bitmap = PhotoUtils.rotateImageIfNeeded(photoPath, options)

                    withContext(Dispatchers.Main) {
                        if (bitmap != null) {
                            val focusRect = focusAreas[photoPath]
                            Log.d("PhotoGalleryAdapter", "Focus rect for $photoPath: ${focusRect?.let { "${it.left},${it.top},${it.right},${it.bottom}" } ?: "null"}")

                            if (focusRect != null) {
                                Log.d("PhotoGalleryAdapter", "Drawing yellow rectangle for $photoPath")
                                val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                                val canvas = android.graphics.Canvas(mutableBitmap)
                                val paint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.YELLOW
                                    style = android.graphics.Paint.Style.STROKE
                                    strokeWidth = 5f
                                }
                                // Scale the focusRect to the thumbnail size
                                val scaleX = mutableBitmap.width.toFloat() / bitmap.width
                                val scaleY = mutableBitmap.height.toFloat() / bitmap.height
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
