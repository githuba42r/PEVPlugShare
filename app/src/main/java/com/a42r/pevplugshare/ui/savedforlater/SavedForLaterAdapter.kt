package com.a42r.pevplugshare.ui.savedforlater

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.a42r.pevplugshare.databinding.ItemSavedForLaterBinding
import com.bumptech.glide.Glide
import java.io.File

class SavedForLaterAdapter(
    private val onContinueClick: (SavedForLaterItem) -> Unit,
    private val onDeleteClick: (SavedForLaterItem) -> Unit
) : ListAdapter<SavedForLaterItem, SavedForLaterAdapter.ViewHolder>(SavedForLaterDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSavedForLaterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onContinueClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemSavedForLaterBinding,
        private val onContinueClick: (SavedForLaterItem) -> Unit,
        private val onDeleteClick: (SavedForLaterItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SavedForLaterItem) {
            binding.locationInfoText.text = item.locationDisplayName
            binding.coordinatesText.text = item.coordinatesText
            binding.createdAtText.text = item.createdAtText

            // Setup thumbnail gallery
            val thumbnailAdapter = ThumbnailAdapter(item.photoPaths)
            binding.thumbnailRecyclerView.layoutManager = LinearLayoutManager(
                binding.root.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            binding.thumbnailRecyclerView.adapter = thumbnailAdapter

            binding.continueButton.setOnClickListener {
                onContinueClick(item)
            }

            binding.deleteButton.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    private class SavedForLaterDiffCallback : DiffUtil.ItemCallback<SavedForLaterItem>() {
        override fun areItemsTheSame(oldItem: SavedForLaterItem, newItem: SavedForLaterItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SavedForLaterItem, newItem: SavedForLaterItem): Boolean {
            return oldItem == newItem
        }
    }
}

class ThumbnailAdapter(
    private val photoPaths: List<String>
) : RecyclerView.Adapter<ThumbnailAdapter.ThumbnailViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(com.a42r.pevplugshare.R.layout.item_thumbnail, parent, false)
        return ThumbnailViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        holder.bind(photoPaths[position])
    }

    override fun getItemCount(): Int = photoPaths.size

    class ThumbnailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(com.a42r.pevplugshare.R.id.thumbnailImage)

        fun bind(photoPath: String) {
            val file = File(photoPath)
            if (file.exists()) {
                Glide.with(itemView.context)
                    .load(file)
                    .centerCrop()
                    .into(imageView)
            }
        }
    }
}
