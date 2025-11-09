package com.pevplugshare.ui.quicklocation

import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pevplugshare.databinding.ActivityLocationPickerMapBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.api.IMapController
import org.osmdroid.util.GeoPoint
import org.osmdroid.config.Configuration

/**
 * Map-based location picker for quick location workflow using OSMDroid
 */
@AndroidEntryPoint
class LocationPickerMapActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLocationPickerMapBinding
    private lateinit var mapView: MapView
    private lateinit var mapController: IMapController
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var selectedBearing: Float? = null
    private lateinit var geocoder: Geocoder
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var markerOverlay: ItemizedIconOverlay<OverlayItem>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationPickerMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize OSMDroid
        Configuration.getInstance().userAgentValue = "PEVPlugShare"

        geocoder = Geocoder(this)

        // Get initial location from intent
        selectedLatitude = intent.getDoubleExtra("latitude", -33.8688)
        selectedLongitude = intent.getDoubleExtra("longitude", 151.2093)
        selectedBearing = intent.getFloatExtra("bearing", 0f)

        // Setup map
        mapView = binding.mapView
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        mapController = mapView.controller
        mapController.setZoom(15)
        
        // Set initial location
        val initialPoint = GeoPoint(selectedLatitude!!, selectedLongitude!!)
        mapController.setCenter(initialPoint)
        addMarker(initialPoint)

        // Setup search
        binding.locationSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchLocation(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean = false
        })

        // Setup map click to select location
        mapView.overlays.add(object : org.osmdroid.views.overlay.Overlay(this) {
            override fun onTouchEvent(event: android.view.MotionEvent?, mapView: MapView?): Boolean {
                if (event?.action == android.view.MotionEvent.ACTION_UP && mapView != null) {
                    val projection = mapView.projection
                    val geoPoint = projection.fromPixels(event.x.toInt(), event.y.toInt()) as? GeoPoint
                    if (geoPoint != null) {
                        selectedLatitude = geoPoint.latitude
                        selectedLongitude = geoPoint.longitude
                        mapView.overlays.removeAll { it is ItemizedIconOverlay<*> }
                        addMarker(geoPoint)
                        reverseGeocode(geoPoint)
                        return true
                    }
                }
                return super.onTouchEvent(event, mapView)
            }
        })

        // Setup buttons
        binding.confirmButton.setOnClickListener {
            if (selectedLatitude != null && selectedLongitude != null) {
                val result = Bundle().apply {
                    putDouble("latitude", selectedLatitude!!)
                    putDouble("longitude", selectedLongitude!!)
                    putFloat("bearing", selectedBearing ?: 0f)
                }
                intent.putExtra("result", result)
                setResult(RESULT_OK, intent)
                finish()
            } else {
                Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun addMarker(geoPoint: GeoPoint) {
        val items = arrayOf(
            OverlayItem(
                "Selected Location",
                "${String.format("%.4f", geoPoint.latitude)}, ${String.format("%.4f", geoPoint.longitude)}",
                geoPoint
            )
        )
        
        markerOverlay = ItemizedIconOverlay(
            this,
            items.toList(),
            object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
                override fun onItemSingleTapUp(index: Int, item: OverlayItem): Boolean = true
                override fun onItemLongPress(index: Int, item: OverlayItem): Boolean = false
            }
        )
        mapView.overlays.add(markerOverlay)
        mapView.invalidate()
    }

    private fun searchLocation(query: String) {
        scope.launch {
            try {
                val addresses = withContext(Dispatchers.Default) {
                    geocoder.getFromLocationName(query, 1)
                }

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val geoPoint = GeoPoint(address.latitude, address.longitude)
                    selectedLatitude = address.latitude
                    selectedLongitude = address.longitude

                    withContext(Dispatchers.Main) {
                        mapView.overlays.removeAll { it is ItemizedIconOverlay<*> }
                        mapController.setCenter(geoPoint)
                        mapController.setZoom(15)
                        addMarker(geoPoint)
                        Toast.makeText(
                            this@LocationPickerMapActivity,
                            "Found: ${address.getAddressLine(0)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@LocationPickerMapActivity,
                            "Location not found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("LocationPickerMapActivity", "Error searching location", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@LocationPickerMapActivity,
                        "Error searching location: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun reverseGeocode(geoPoint: GeoPoint) {
        scope.launch {
            try {
                val addresses = withContext(Dispatchers.Default) {
                    geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
                }

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val addressText = address.getAddressLine(0)
                    
                    withContext(Dispatchers.Main) {
                        binding.locationSearch.setQuery(addressText, false)
                    }
                }
            } catch (e: Exception) {
                Log.e("LocationPickerMapActivity", "Error reverse geocoding", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mapView.onDetach()
        super.onDestroy()
        job.cancel()
    }
}
