package com.a42r.pevplugshare.ui.main

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.data.model.Location
import com.a42r.pevplugshare.databinding.ItemLocationBinding
import com.a42r.pevplugshare.util.LocationUtils
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.sqrt

class LocationAdapter(
    private val onItemClick: (Location) -> Unit,
    private val onNavigateClick: (Location) -> Unit,
    private val onFavouriteClick: (Location) -> Unit,
    private val onEditClick: (Location) -> Unit,
    private val onInfoClick: (Location) -> Unit,
    private val onMapClick: (Location) -> Unit,
    private val onPhotoClick: (Location) -> Unit,
    private val userLatitude: Double,
    private val userLongitude: Double
) : ListAdapter<Location, LocationAdapter.LocationViewHolder>(LocationDiffCallback()) {

    private fun isValidUserLocation(): Boolean {
        return !(userLatitude == 0.0 && userLongitude == 0.0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val binding = ItemLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LocationViewHolder(binding, onItemClick, onNavigateClick, onFavouriteClick, onEditClick, onInfoClick, onMapClick, onPhotoClick, userLatitude, userLongitude, ::isValidUserLocation)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LocationViewHolder(
        private val binding: ItemLocationBinding,
        private val onItemClick: (Location) -> Unit,
        private val onNavigateClick: (Location) -> Unit,
        private val onFavouriteClick: (Location) -> Unit,
        private val onEditClick: (Location) -> Unit,
        private val onInfoClick: (Location) -> Unit,
        private val onMapClick: (Location) -> Unit,
        private val onPhotoClick: (Location) -> Unit,
        private val userLatitude: Double,
        private val userLongitude: Double,
        private val isValidUserLocation: () -> Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        private var isDescriptionExpanded = false
        private var isMapExpanded = false

        fun bind(location: Location) {
            binding.apply {
                locationName.text = location.name

                // Long press on name to edit
                locationName.setOnLongClickListener {
                    onEditClick(location)
                    true
                }

                // Calculate distance - only display if user location is valid (not 0,0)
                if (isValidUserLocation()) {
                    val distance = LocationUtils.haversineDistance(
                        userLatitude, userLongitude,
                        location.latitude, location.longitude
                    )
                    locationDistance.text = LocationUtils.formatDistance(distance)
                    locationDistance.visibility = android.view.View.VISIBLE
                } else {
                    locationDistance.visibility = android.view.View.GONE
                }

                // Always visible badges
                locationGpoCount.text = "${location.gpoCount} GPO"
                publicPrivateBadge.text = if (location.publicPrivate.displayName == "Public") "🌍 Public" else "🔒 Private"

                feeIndicator.text = if (location.hasFee) "💰 Paid" else "Free"

                // Opening hours badge
                val isOpen = if (!location.openingHours.isNullOrEmpty()) {
                    locationOpeningHours.text = "⏰ ${location.openingHours}"
                    locationOpeningHours.visibility = android.view.View.VISIBLE
                    isLocationOpen(location.openingHours)
                } else {
                    locationOpeningHours.visibility = android.view.View.GONE
                    true // Assume open if no hours specified
                }

                // Highlight card border if open
                root.strokeColor = if (isOpen) {
                    Color.parseColor("#4CAF50") // Green
                } else {
                    Color.TRANSPARENT
                }
                root.strokeWidth = if (isOpen) 4 else 0

                // Info section - description and additional info only
                locationDescription.text = location.description ?: "No description"

                locationAdditionalInfo.apply {
                    if (!location.additionalInfo.isNullOrEmpty()) {
                        text = location.additionalInfo
                        visibility = android.view.View.VISIBLE
                    } else {
                        visibility = android.view.View.GONE
                    }
                }

                // Info button - toggle info section visibility
                infoSection.visibility = if (isDescriptionExpanded) android.view.View.VISIBLE else android.view.View.GONE
                infoButton.setOnClickListener {
                    isDescriptionExpanded = !isDescriptionExpanded
                    infoSection.visibility = if (isDescriptionExpanded) android.view.View.VISIBLE else android.view.View.GONE
                }

                // Map button - toggle map visibility
                mapButton.setOnClickListener {
                    isMapExpanded = !isMapExpanded
                    if (isMapExpanded) {
                        locationMapView.visibility = android.view.View.VISIBLE
                        loadMapInWebView(location, locationMapView)
                        onMapClick(location)
                    } else {
                        locationMapView.visibility = android.view.View.GONE
                    }
                }

                // Favourite button
                favouriteButton.setImageResource(
                    if (location.isFavourite) R.drawable.ic_favourite_filled else R.drawable.ic_favourite_outline
                )
                favouriteButton.setOnClickListener { onFavouriteClick(location) }

                // Navigate button
                navigateButton.setOnClickListener { onNavigateClick(location) }

                // Photos button - show if photos exist in filesystem
                val hasPhotos = com.a42r.pevplugshare.util.ImageUtils.getLocationImages(binding.root.context, location.id).isNotEmpty()
                photosButton.visibility = if (hasPhotos) android.view.View.VISIBLE else android.view.View.GONE
                photosButton.setOnClickListener {
                    onPhotoClick(location)
                }

                // Item click
                root.setOnClickListener { onItemClick(location) }
            }
        }

        private fun isLocationOpen(openingHours: String): Boolean {
            // Simple check for "24/7" or "24 hours"
            if (openingHours.contains("24", ignoreCase = true) ||
                openingHours.contains("always", ignoreCase = true)) {
                return true
            }

            // Try to parse time ranges like "9:00-17:00" or "9am-5pm"
            try {
                val currentTime = LocalTime.now()

                // Match patterns like "9:00-17:00" or "9am-5pm"
                val timePattern = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*([ap]m)?\\s*-\\s*(\\d{1,2})(?::(\\d{2}))?\\s*([ap]m)?", RegexOption.IGNORE_CASE)
                val match = timePattern.find(openingHours)

                if (match != null) {
                    val (startHour, startMin, startAmPm, endHour, endMin, endAmPm) = match.destructured

                    var openHour = startHour.toInt()
                    var closeHour = endHour.toInt()

                    // Handle AM/PM
                    if (startAmPm.equals("pm", ignoreCase = true) && openHour != 12) {
                        openHour += 12
                    } else if (startAmPm.equals("am", ignoreCase = true) && openHour == 12) {
                        openHour = 0
                    }

                    if (endAmPm.equals("pm", ignoreCase = true) && closeHour != 12) {
                        closeHour += 12
                    } else if (endAmPm.equals("am", ignoreCase = true) && closeHour == 12) {
                        closeHour = 0
                    }

                    val openTime = LocalTime.of(openHour, startMin.toIntOrNull() ?: 0)
                    val closeTime = LocalTime.of(closeHour, endMin.toIntOrNull() ?: 0)

                    return currentTime.isAfter(openTime) && currentTime.isBefore(closeTime)
                }
            } catch (e: Exception) {
                // If parsing fails, return true (assume open)
                return true
            }

            // Default to true if we can't determine
            return true
        }

        private fun loadMapInWebView(location: Location, webView: android.webkit.WebView) {
            webView.settings.apply {
                javaScriptEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
            }

            // Create an embedded OSM map iframe
            val mapUrl = "https://www.openstreetmap.org/export/embed.html?bbox=" +
                    "${location.longitude - 0.005},${location.latitude - 0.005}," +
                    "${location.longitude + 0.005},${location.latitude + 0.005}" +
                    "&layer=mapnik&marker=${location.latitude},${location.longitude}"

            webView.loadUrl(mapUrl)
        }
    }

    private class LocationDiffCallback : DiffUtil.ItemCallback<Location>() {
        override fun areItemsTheSame(oldItem: Location, newItem: Location) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Location, newItem: Location) = oldItem == newItem
    }
}
