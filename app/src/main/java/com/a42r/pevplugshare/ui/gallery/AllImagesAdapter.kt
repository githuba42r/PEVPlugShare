package com.a42r.pevplugshare.ui.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.databinding.ItemGalleryThumbnailBinding
import java.io.File

class AllImagesAdapter(
    private val onImageClick: (GalleryImageItem, Int) -> Unit,
    private val onImageLongClick: (Int) -> Unit
) : ListAdapter<GalleryImageItem, AllImagesAdapter.ImageViewHolder>(ImageDiffCallback()) {

    private val selectedItems = mutableSetOf<Int>()
    var isSelectionMode = false
        private set

    fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }
        notifyItemChanged(position)
    }

    fun enterSelectionMode(initialPosition: Int) {
        isSelectionMode = true
        selectedItems.add(initialPosition)
        notifyDataSetChanged()
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<GalleryImageItem> {
        return selectedItems.mapNotNull { position ->
            if (position < currentList.size) getItem(position) else null
        }
    }

    fun getSelectedCount() = selectedItems.size

    fun selectAll() {
        selectedItems.clear()
        selectedItems.addAll(0 until currentList.size)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemGalleryThumbnailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ImageViewHolder(
        private val binding: ItemGalleryThumbnailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GalleryImageItem, position: Int) {
            binding.locationNameText.text = item.locationName
            
            Glide.with(binding.root.context)
                .load(File(item.imagePath))
                .centerCrop()
                .into(binding.thumbnailImage)

            // Show/hide selection UI
            val isSelected = selectedItems.contains(position)
            if (isSelectionMode) {
                binding.selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
                binding.selectionCheckbox.visibility = View.VISIBLE
                binding.selectionCheckbox.isChecked = isSelected
            } else {
                binding.selectionOverlay.visibility = View.GONE
                binding.selectionCheckbox.visibility = View.GONE
            }
            
            // Update card appearance based on selection
            if (isSelected) {
                binding.root.strokeWidth = 4
                binding.root.strokeColor = binding.root.context.getColor(R.color.md_theme_primary)
            } else {
                binding.root.strokeWidth = 0
            }

            binding.root.setOnClickListener {
                onImageClick(item, position)
            }

            binding.root.setOnLongClickListener {
                onImageLongClick(position)
                true
            }
        }
    }

    class ImageDiffCallback : DiffUtil.ItemCallback<GalleryImageItem>() {
        override fun areItemsTheSame(oldItem: GalleryImageItem, newItem: GalleryImageItem): Boolean {
            return oldItem.imagePath == newItem.imagePath
        }

        override fun areContentsTheSame(oldItem: GalleryImageItem, newItem: GalleryImageItem): Boolean {
            return oldItem == newItem
        }
    }
}
