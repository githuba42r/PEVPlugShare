package com.pevplugshare.data.gpx

import com.pevplugshare.data.model.Location
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

class GpxParser {
    fun parse(inputStream: InputStream): List<Location> {
        val locations = mutableListOf<Location>()
        
        try {
            // Read the input stream for debugging
            val content = inputStream.bufferedReader().use { it.readText() }
            android.util.Log.d("GpxParser", "GPX Content length: ${content.length} bytes")
            android.util.Log.d("GpxParser", "GPX Content preview: ${content.take(500)}")
            
            // Parse the XML
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(content.byteInputStream(), null)
            
            var eventType = parser.eventType
            var currentLocation: LocationBuilder? = null
            var currentTag: String? = null
            var wptCount = 0
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        when (currentTag) {
                            "wpt" -> {
                                wptCount++
                                val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                                val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                                android.util.Log.d("GpxParser", "Found wpt #$wptCount: lat=$lat, lon=$lon")
                                if (lat != null && lon != null) {
                                    currentLocation = LocationBuilder(lat, lon)
                                } else {
                                    android.util.Log.w("GpxParser", "Invalid wpt: lat=$lat, lon=$lon")
                                }
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim()
                        if (!text.isNullOrEmpty() && currentLocation != null) {
                            when (currentTag) {
                                "name" -> {
                                    currentLocation.name = text
                                    android.util.Log.d("GpxParser", "  name: $text")
                                }
                                "desc" -> {
                                    currentLocation.description = text
                                    android.util.Log.d("GpxParser", "  desc: $text")
                                }
                                "time" -> currentLocation.timestamp = text
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "wpt" && currentLocation != null) {
                            val location = currentLocation.build()
                            if (location != null) {
                                locations.add(location)
                                android.util.Log.d("GpxParser", "Added location: ${location.name}")
                            } else {
                                android.util.Log.w("GpxParser", "Failed to build location (missing name?)")
                            }
                            currentLocation = null
                        }
                        currentTag = null
                    }
                }
                eventType = parser.next()
            }
            
            android.util.Log.d("GpxParser", "Total wpt tags found: $wptCount")
            android.util.Log.d("GpxParser", "Total locations built: ${locations.size}")
        } catch (e: Exception) {
            android.util.Log.e("GpxParser", "Error parsing GPX: ${e.message}", e)
        }
        
        return locations
    }
    
    private class LocationBuilder(
        val latitude: Double,
        val longitude: Double
    ) {
        var name: String? = null
        var description: String? = null
        var timestamp: String? = null
        
        fun build(): Location? {
            val finalName = name ?: return null
            return Location(
                id = finalName,
                name = finalName,
                description = description,
                latitude = latitude,
                longitude = longitude,
                socketTypes = emptyList(),
                publicPrivate = com.pevplugshare.data.model.LocationAccessType.PUBLIC,
                hasFee = false,
                gpoCount = 0,
                openingHours = null,
                additionalInfo = null,
                providerName = null,
                providerWebsite = null,
                chargingCosts = null,
                color = "#FF0000",
                icon = "special_star",
                lastModified = System.currentTimeMillis(),
                isFavourite = false,
                syncStatus = com.pevplugshare.data.model.SyncStatus.SYNCED,
                photos = emptyList()
            )
        }
    }
}
