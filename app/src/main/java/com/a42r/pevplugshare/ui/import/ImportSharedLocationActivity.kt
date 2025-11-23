package com.a42r.pevplugshare.ui.import

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.a42r.pevplugshare.data.model.Location
import com.a42r.pevplugshare.data.model.LocationAccessType
import com.a42r.pevplugshare.data.model.SocketType
import com.a42r.pevplugshare.data.model.SyncStatus
import com.a42r.pevplugshare.data.model.OsmandSyncStatus
import com.a42r.pevplugshare.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

@AndroidEntryPoint
class ImportSharedLocationActivity : AppCompatActivity() {
    
    private val viewModel: com.a42r.pevplugshare.viewmodel.MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                handleSharedLocation(intent)
            }
            else -> {
                Toast.makeText(this, "No shared location data found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun handleSharedLocation(intent: Intent) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        val sharedJson = intent.getStringExtra("PEVPLUGSHARE_JSON")
        
        if (sharedJson != null) {
            // Try to parse JSON first (machine-interpretable format)
            try {
                val location = parseLocationFromJson(sharedJson)
                showImportDialog(location, isFromJson = true)
            } catch (e: Exception) {
                android.util.Log.e("ImportSharedLocation", "Failed to parse JSON", e)
                Toast.makeText(this, "Failed to import location: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        } else if (sharedText != null) {
            // Fallback: Try to parse from human-readable text
            try {
                val location = parseLocationFromText(sharedText)
                showImportDialog(location, isFromJson = false)
            } catch (e: Exception) {
                android.util.Log.e("ImportSharedLocation", "Failed to parse text", e)
                Toast.makeText(this, "Unable to import location from shared text", Toast.LENGTH_LONG).show()
                finish()
            }
        } else {
            Toast.makeText(this, "No location data found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun parseLocationFromJson(json: String): Location {
        val jsonObj = JSONObject(json)
        
        // Parse socket types
        val socketTypesArray = jsonObj.optJSONArray("socketTypes")
        val socketTypes = mutableListOf<SocketType>()
        if (socketTypesArray != null) {
            for (i in 0 until socketTypesArray.length()) {
                try {
                    socketTypes.add(SocketType.valueOf(socketTypesArray.getString(i)))
                } catch (e: Exception) {
                    // Skip invalid socket types
                }
            }
        }
        
        // If no valid socket types, add a default
        if (socketTypes.isEmpty()) {
            socketTypes.add(SocketType.AS3112)
        }
        
        return Location(
            id = UUID.randomUUID().toString(), // Generate new ID for imported location
            name = jsonObj.getString("name"),
            description = jsonObj.optString("description", null),
            latitude = jsonObj.getDouble("latitude"),
            longitude = jsonObj.getDouble("longitude"),
            socketTypes = socketTypes,
            publicPrivate = try {
                LocationAccessType.valueOf(jsonObj.getString("publicPrivate"))
            } catch (e: Exception) {
                LocationAccessType.PUBLIC
            },
            hasFee = jsonObj.optBoolean("hasFee", false),
            gpoCount = jsonObj.optInt("gpoCount", 1),
            openingHours = jsonObj.optString("openingHours", null),
            additionalInfo = jsonObj.optString("additionalInfo", null),
            providerName = jsonObj.optString("providerName", null),
            providerWebsite = jsonObj.optString("providerWebsite", null),
            chargingCosts = jsonObj.optString("chargingCosts", null),
            color = "blue",
            icon = "charging_station",
            lastModified = System.currentTimeMillis(),
            isFavourite = false,
            syncStatus = SyncStatus.PENDING,
            businessName = jsonObj.optString("businessName", null),
            streetAddress = jsonObj.optString("streetAddress", null),
            suburb = jsonObj.optString("suburb", null),
            state = jsonObj.optString("state", null),
            postcode = jsonObj.optString("postcode", null),
            osmandSyncStatus = OsmandSyncStatus.LOCAL_ONLY
        )
    }
    
    private fun parseLocationFromText(text: String): Location {
        // Extract coordinates from text (looking for pattern like "lat, lon" or OpenStreetMap URL)
        val coordsPattern = Regex("""(-?\d+\.\d+),\s*(-?\d+\.\d+)""")
        val coordsMatch = coordsPattern.find(text)
        
        val osmUrlPattern = Regex("""mlat=(-?\d+\.\d+)&mlon=(-?\d+\.\d+)""")
        val osmUrlMatch = osmUrlPattern.find(text)
        
        val (latitude, longitude) = when {
            coordsMatch != null -> {
                Pair(coordsMatch.groupValues[1].toDouble(), coordsMatch.groupValues[2].toDouble())
            }
            osmUrlMatch != null -> {
                Pair(osmUrlMatch.groupValues[1].toDouble(), osmUrlMatch.groupValues[2].toDouble())
            }
            else -> throw IllegalArgumentException("No coordinates found in shared text")
        }
        
        // Extract name (first line typically)
        val lines = text.lines().filter { it.isNotBlank() }
        val name = lines.firstOrNull()?.replace("🔌", "")?.trim() ?: "Imported Charging Location"
        
        // Extract GPO count
        val gpoPattern = Regex("""(\d+)\s+GPO""")
        val gpoMatch = gpoPattern.find(text)
        val gpoCount = gpoMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
        
        // Determine if public/private
        val isPublic = text.contains("Public", ignoreCase = true)
        val publicPrivate = if (isPublic) LocationAccessType.PUBLIC else LocationAccessType.PRIVATE
        
        // Determine if has fee
        val hasFee = text.contains("Paid", ignoreCase = true) || text.contains("Fee", ignoreCase = true)
        
        return Location(
            id = UUID.randomUUID().toString(),
            name = name,
            description = "Imported from shared location",
            latitude = latitude,
            longitude = longitude,
            socketTypes = listOf(SocketType.AS3112), // Default
            publicPrivate = publicPrivate,
            hasFee = hasFee,
            gpoCount = gpoCount,
            openingHours = null,
            additionalInfo = null,
            providerName = null,
            providerWebsite = null,
            chargingCosts = null,
            color = "blue",
            icon = "charging_station",
            lastModified = System.currentTimeMillis(),
            isFavourite = false,
            syncStatus = SyncStatus.PENDING,
            businessName = null,
            streetAddress = null,
            suburb = null,
            state = null,
            postcode = null,
            osmandSyncStatus = OsmandSyncStatus.LOCAL_ONLY
        )
    }
    
    private fun showImportDialog(location: Location, isFromJson: Boolean) {
        val message = buildString {
            append("Import this charging location?\n\n")
            append("Name: ${location.name}\n")
            append("Location: ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}\n")
            append("GPO Count: ${location.gpoCount}\n")
            append("Access: ${location.publicPrivate.displayName}\n")
            append("Cost: ${if (location.hasFee) "Paid" else "Free"}\n")
            
            if (isFromJson) {
                append("\n✓ Full location details available")
            } else {
                append("\n⚠ Imported from text (limited details)")
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("Import Shared Location")
            .setMessage(message)
            .setPositiveButton("Import") { _, _ ->
                importLocation(location)
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .setOnCancelListener {
                finish()
            }
            .show()
    }
    
    private fun importLocation(location: Location) {
        lifecycleScope.launch {
            try {
                // Save to local database
                viewModel.addLocation(location)
                
                Toast.makeText(
                    this@ImportSharedLocationActivity,
                    "Location imported successfully",
                    Toast.LENGTH_SHORT
                ).show()
                
                // Navigate to main activity
                val intent = Intent(this@ImportSharedLocationActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("HIGHLIGHT_LOCATION_ID", location.id)
                }
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@ImportSharedLocationActivity,
                    "Failed to import location: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                android.util.Log.e("ImportSharedLocation", "Failed to save location", e)
                finish()
            }
        }
    }
}
