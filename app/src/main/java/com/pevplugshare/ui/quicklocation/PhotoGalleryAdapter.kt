package com.pevplugshare.ui.quicklocation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.pevplugshare.R
import kotlinx.coroutines.*
import java.io.File

/**
 * Adapter for displaying and managing photos in gallery view
 */
class PhotoGalleryAdapter(
    private val photoPaths: List<String>,
    private val focusAreas: Map<String, String>,
    private val onPhotoClick: (String) -> Unit
) : RecyclerView.Adapter<PhotoGalleryAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(private val container: FrameLayout) :
        RecyclerView.ViewHolder(container) {
        
        private val imageView: ImageView = container.findViewById(android.R.id.icon) as? ImageView
            ?: ImageView(container.context).apply {
                container.addView(this, 0)
            }
        
        fun bind(photoPath: String, position: Int) {
            // Load thumbnail in background
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 4 // Load smaller sample for performance
                    }
                    val bitmap = BitmapFactory.decodeFile(photoPath, options)
                    
                    withContext(Dispatchers.Main) {
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap)
                        } else {
                            imageView.setImageResource(R.drawable.ic_add) // Fallback
                        }
                        
                        // Show focus badge if has focus area
                        if (focusAreas.containsKey(photoPath)) {
                            container.foreground = container.context.getDrawable(R.drawable.focus_area_badge)
                        } else {
                            container.foreground = null
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        imageView.setImageResource(R.drawable.ic_add)
                    }
                }
            }
            
            container.setOnClickListener {
                onPhotoClick(photoPath)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val container = FrameLayout(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(300, 300)
        }
        val imageView = ImageView(parent.context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(300, 300)
            setId(android.R.id.icon)
        }
        container.addView(imageView)
        return PhotoViewHolder(container)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photoPaths[position], position)
    }

    override fun getItemCount(): Int = photoPaths.size
}
