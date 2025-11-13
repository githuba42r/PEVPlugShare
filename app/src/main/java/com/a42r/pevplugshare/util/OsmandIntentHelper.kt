package com.a42r.pevplugshare.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.a42r.pevplugshare.data.model.Location

object OsmandIntentHelper {
    const val OSMAND_PACKAGE = "net.osmand.plus"
    const val OSMAND_FREE_PACKAGE = "net.osmand"
    
    /**
     * Check if OSMAnd (Plus or Free) is installed
     */
    fun isOsmandInstalled(context: Context): Boolean {
        return try {
            // First try standard package names
            val plusInstalled = isPackageInstalled(context, OSMAND_PACKAGE)
            val freeInstalled = isPackageInstalled(context, OSMAND_FREE_PACKAGE)
            
            if (plusInstalled || freeInstalled) {
                android.util.Log.d("OsmandHelper", "OSMAnd found via package check")
                return true
            }
            
            // Try intent resolution as fallback
            android.util.Log.d("OsmandHelper", "Package check failed, trying intent resolution")
            if (canResolveOsmandIntent(context)) {
                android.util.Log.d("OsmandHelper", "OSMAnd found via intent resolution")
                return true
            }
            
            // Final fallback: check if we can launch the OSMAnd scheme
            if (canLaunchOsmandScheme(context)) {
                android.util.Log.d("OsmandHelper", "OSMAnd found via scheme launch test")
                return true
            }
            
            android.util.Log.w("OsmandHelper", "OSMAnd NOT found via any method")
            false
        } catch (e: Exception) {
            android.util.Log.e("OsmandHelper", "Error checking if OSMAnd installed: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get the installed OSMAnd package name
     */
    fun getOsmandPackage(context: Context): String? {
        return when {
            isPackageInstalled(context, OSMAND_PACKAGE) -> OSMAND_PACKAGE
            isPackageInstalled(context, OSMAND_FREE_PACKAGE) -> OSMAND_FREE_PACKAGE
            else -> {
                // Fallback: try intent resolution
                android.util.Log.d("OsmandHelper", "Package check failed for getOsmandPackage, trying intent resolution")
                if (canResolveOsmandIntent(context)) OSMAND_PACKAGE else null
            }
        }
    }
    
    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            android.util.Log.d("OsmandHelper", "Package $packageName is installed (via getPackageInfo)")
            true
        } catch (e: PackageManager.NameNotFoundException) {
            android.util.Log.w("OsmandHelper", "Package $packageName NOT found (via getPackageInfo)")
            false
        } catch (e: Exception) {
            android.util.Log.e("OsmandHelper", "Error checking package $packageName: ${e.message}", e)
            false
        }
    }
    
    /**
     * Alternative check using resolveActivity (more reliable for Intent-based apps)
     */
    private fun canResolveOsmandIntent(context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("osmand.api://get_info"))
            val activities = context.packageManager.queryIntentActivities(intent, 0)
            val found = activities.isNotEmpty()
            android.util.Log.d("OsmandHelper", "Can resolve OSMAnd intent: $found (found ${activities.size} activities)")
            found
        } catch (e: Exception) {
            android.util.Log.e("OsmandHelper", "Error checking OSMAnd intent: ${e.message}")
            false
        }
    }
    
    /**
     * Test if we can actually launch an OSMAnd scheme (without actually launching it)
     */
    private fun canLaunchOsmandScheme(context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("osmand.api://show_location?lat=0&lon=0"))
            val componentName = intent.resolveActivity(context.packageManager)
            val canResolve = componentName != null
            android.util.Log.d("OsmandHelper", "Can launch OSMAnd scheme: $canResolve (component: $componentName)")
            canResolve
        } catch (e: Exception) {
            android.util.Log.e("OsmandHelper", "Error checking OSMAnd scheme: ${e.message}")
            false
        }
    }
    
    /**
     * Add a charging location as a favorite in OSMAnd
     * @return true if successful, false if OSMAnd not installed or error
     */
    fun addFavorite(
        context: Context,
        location: Location
    ): Boolean {
        val packageName = getOsmandPackage(context) ?: return false
        
        val description = buildDescription(location)
        
        val uri = Uri.parse("osmand.api://add_favorite" +
            "?lat=${location.latitude}" +
            "&lon=${location.longitude}" +
            "&name=${Uri.encode(location.name)}" +
            "&desc=${Uri.encode(description)}" +
            "&category=${Uri.encode("PEV Charging")}" +
            "&color=green" +
            "&visible=true")
        
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage(packageName)
        
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Navigate to a charging location using OSMAnd
     * @param profile Navigation profile: "bicycle", "car", "pedestrian"
     * @return true if successful, false if OSMAnd not installed or error
     */
    fun navigateToLocation(
        context: Context,
        location: Location,
        profile: String = "bicycle"
    ): Boolean {
        val packageName = getOsmandPackage(context) ?: return false
        
        val uri = Uri.parse("osmand.api://navigate" +
            "?dest_lat=${location.latitude}" +
            "&dest_lon=${location.longitude}" +
            "&dest_name=${Uri.encode(location.name)}" +
            "&profile=$profile")
        
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage(packageName)
        
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Show a charging location on OSMAnd map
     * @return true if successful, false if OSMAnd not installed or error
     */
    fun showLocationOnMap(
        context: Context,
        location: Location
    ): Boolean {
        val packageName = getOsmandPackage(context) ?: return false
        
        val uri = Uri.parse("osmand.api://show_location" +
            "?lat=${location.latitude}" +
            "&lon=${location.longitude}")
        
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage(packageName)
        
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Add a map marker in OSMAnd
     * @return true if successful, false if OSMAnd not installed or error
     */
    fun addMapMarker(
        context: Context,
        location: Location
    ): Boolean {
        val packageName = getOsmandPackage(context) ?: return false
        
        val uri = Uri.parse("osmand.api://add_map_marker" +
            "?lat=${location.latitude}" +
            "&lon=${location.longitude}" +
            "&name=${Uri.encode(location.name)}")
        
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage(packageName)
        
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Open OSMAnd app
     * @return true if successful, false if not installed
     */
    fun openOsmand(context: Context): Boolean {
        val packageName = getOsmandPackage(context) ?: return false
        
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (intent != null) {
            try {
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }
    
    /**
     * Open Play Store to install OSMAnd Plus
     */
    fun openPlayStoreForOsmand(context: Context) {
        val playStoreUri = Uri.parse("market://details?id=$OSMAND_PACKAGE")
        val playStoreIntent = Intent(Intent.ACTION_VIEW, playStoreUri)
        
        try {
            context.startActivity(playStoreIntent)
        } catch (e: Exception) {
            // If Play Store app not available, open in browser
            val browserUri = Uri.parse("https://play.google.com/store/apps/details?id=$OSMAND_PACKAGE")
            val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
            context.startActivity(browserIntent)
        }
    }
    
    /**
     * Build a description string for OSMAnd favorite from location data
     */
    private fun buildDescription(location: Location): String {
        val parts = mutableListOf<String>()
        
        // Socket types
        if (location.socketTypes.isNotEmpty()) {
            parts.add("Sockets: ${location.socketTypes.joinToString(", ") { it.displayName }}")
        }
        
        // GPO count
        if (location.gpoCount > 0) {
            parts.add("GPO: ${location.gpoCount}")
        }
        
        // Access type
        parts.add(location.publicPrivate.displayName)
        
        // Fee status
        if (location.hasFee) {
            parts.add("Paid")
            location.providerName?.let { parts.add("Provider: $it") }
        } else {
            parts.add("Free")
        }
        
        // Hours
        location.openingHours?.let {
            if (it.isNotBlank()) {
                parts.add("Hours: $it")
            }
        }
        
        // User description
        location.description?.let {
            if (it.isNotBlank()) {
                parts.add(it)
            }
        }
        
        return parts.joinToString(" | ")
    }
}
