package com.a42r.pevplugshare.ui.quicklocation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.DisplayMetrics
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
    private val focusAreas: MutableMap<String, String>, // Changed to MutableMap
    private val onPhotoClick: (String) -> Unit,
    private val onRemoveClick: (Int) -> Unit // New callback for remove button
) : RecyclerView.Adapter<PhotoGalleryAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(private val binding: ItemPhotoThumbnailBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(photoPath: String, position: Int) {
            // Load thumbnail in background
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 4 // Load smaller sample for performance
                    }
                    val bitmap = PhotoUtils.rotateImageIfNeeded(photoPath, options)
                    
                    withContext(Dispatchers.Main) {
                        if (bitmap != null) {
                            binding.thumbnailImage.setImageBitmap(bitmap)
                        } else {
                            binding.thumbnailImage.setImageResource(R.drawable.ic_add) // Fallback
                        }
                        
                        // Show focus badge if has focus area
                        if (focusAreas.containsKey(photoPath)) {
                            binding.focusIndicator.visibility = View.VISIBLE
                        } else {
                            binding.focusIndicator.visibility = View.GONE
                        }
                    }
                } catch (e: Exception) {
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
