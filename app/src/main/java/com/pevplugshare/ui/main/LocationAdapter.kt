package com.pevplugshare.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pevplugshare.R
import com.pevplugshare.data.model.Location
import com.pevplugshare.databinding.ItemLocationBinding
import com.pevplugshare.util.LocationUtils
import kotlin.math.sqrt

class LocationAdapter(
    private val onItemClick: (Location) -> Unit,
    private val onNavigateClick: (Location) -> Unit,
    private val onFavouriteClick: (Location) -> Unit,
    private val userLatitude: Double,
    private val userLongitude: Double
) : ListAdapter<Location, LocationAdapter.LocationViewHolder>(LocationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val binding = ItemLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LocationViewHolder(binding, onItemClick, onNavigateClick, onFavouriteClick, userLatitude, userLongitude)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LocationViewHolder(
        private val binding: ItemLocationBinding,
        private val onItemClick: (Location) -> Unit,
        private val onNavigateClick: (Location) -> Unit,
        private val onFavouriteClick: (Location) -> Unit,
        private val userLatitude: Double,
        private val userLongitude: Double
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(location: Location) {
            binding.apply {
                locationName.text = location.name
                locationDescription.text = location.description ?: "No description"

                // Calculate distance
                val distance = LocationUtils.haversineDistance(
                    userLatitude, userLongitude,
                    location.latitude, location.longitude
                )
                locationDistance.text = LocationUtils.formatDistance(distance)

                // GPO count
                locationGpoCount.text = "${location.gpoCount} GPO"

                // Public/Private badge
                publicPrivateBadge.text = if (location.publicPrivate.displayName == "Public") "🌍 Public" else "🔒 Private"

                // Fee indicator
                feeIndicator.apply {
                    if (location.hasFee) {
                        text = "💰 Paid"
                        setTextColor(binding.root.context.getColor(R.color.orange_warning))
                    } else {
                        text = "Free"
                        setTextColor(binding.root.context.getColor(R.color.green_open))
                    }
                }

                // Favourite button
                favouriteButton.setImageResource(
                    if (location.isFavourite) R.drawable.ic_favourite_filled else R.drawable.ic_favourite_outline
                )
                favouriteButton.setOnClickListener { onFavouriteClick(location) }

                // Navigate button
                navigateButton.setOnClickListener { onNavigateClick(location) }

                // Item click
                root.setOnClickListener { onItemClick(location) }
            }
        }
    }

    private class LocationDiffCallback : DiffUtil.ItemCallback<Location>() {
        override fun areItemsTheSame(oldItem: Location, newItem: Location) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Location, newItem: Location) = oldItem == newItem
    }
}
