package com.pevplugshare.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pevplugshare.databinding.ItemFullscreenImageBinding
import java.io.File

class ImageGalleryAdapter(private val imagePaths: List<String>) : RecyclerView.Adapter<ImageGalleryAdapter.ImageViewHolder>() {
    
    class ImageViewHolder(val binding: ItemFullscreenImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(imagePath: String) {
            Glide.with(binding.root.context)
                .load(File(imagePath))
                .into(binding.fullscreenImage)
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemFullscreenImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imagePaths[position])
    }
    
    override fun getItemCount() = imagePaths.size
}
