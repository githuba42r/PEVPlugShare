package com.a42r.pevplugshare.data.model

import java.util.UUID

data class LocationPhoto(
    val id: String = UUID.randomUUID().toString(),
    val locationId: String,
    val photoPath: String,
    val thumbnailPath: String? = null,
    val caption: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val uploadedToOsmand: Boolean = false,
    val uploadTimestamp: Long? = null,
    val fileSize: Long = 0,
    val mimeType: String = "image/jpeg",
    val exifData: String? = null // JSON string of EXIF metadata
)
