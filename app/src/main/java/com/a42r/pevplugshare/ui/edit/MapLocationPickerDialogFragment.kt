package com.a42r.pevplugshare.ui.edit

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.a42r.pevplugshare.R
import com.a42r.pevplugshare.databinding.DialogMapLocationPickerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapLocationPickerDialogFragment : DialogFragment() {

    private var _binding: DialogMapLocationPickerBinding? = null
    private val binding get() = _binding!!

    private var mapView: MapView? = null
    private var locationMarker: Marker? = null
    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0

    private var onLocationSelectedListener: ((Double, Double) -> Unit)? = null

    fun setOnLocationSelectedListener(listener: (Double, Double) -> Unit) {
        onLocationSelectedListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogMapLocationPickerBinding.inflate(inflater, container, false)

        // Initialize osmdroid configuration
        Configuration.getInstance().load(
            requireContext(),
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMap()
        setupButtons()
    }

    private fun setupMap() {
        mapView = binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)

            // Add tap listener to update marker position
            overlays.add(object : org.osmdroid.views.overlay.Overlay() {
                override fun onSingleTapConfirmed(e: android.view.MotionEvent, mapView: MapView): Boolean {
                    val projection = mapView.projection
                    val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                    updateMapMarker(geoPoint.latitude, geoPoint.longitude)
                    return true
                }
            })
        }

        // Initialize with current location if available
        val currentLat = arguments?.getDouble("latitude") ?: 0.0
        val currentLon = arguments?.getDouble("longitude") ?: 0.0

        if (currentLat != 0.0 && currentLon != 0.0) {
            updateMapMarker(currentLat, currentLon)
            mapView?.controller?.setCenter(GeoPoint(currentLat, currentLon))
        }
    }

    private fun setupButtons() {
        binding.buttonAccept.setOnClickListener {
            if (selectedLatitude != 0.0 && selectedLongitude != 0.0) {
                onLocationSelectedListener?.invoke(selectedLatitude, selectedLongitude)
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Please select a location on the map", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun updateMapMarker(latitude: Double, longitude: Double) {
        selectedLatitude = latitude
        selectedLongitude = longitude

        // Remove existing marker
        locationMarker?.let { mapView?.overlays?.remove(it) }

        // Add new marker
        locationMarker = Marker(mapView).apply {
            position = GeoPoint(latitude, longitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Selected Location"
        }
        mapView?.overlays?.add(locationMarker)
        mapView?.invalidate()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDetach()
        _binding = null
    }

    companion object {
        fun newInstance(latitude: Double, longitude: Double): MapLocationPickerDialogFragment {
            return MapLocationPickerDialogFragment().apply {
                arguments = Bundle().apply {
                    putDouble("latitude", latitude)
                    putDouble("longitude", longitude)
                }
            }
        }
    }
}