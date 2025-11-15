package com.a42r.pevplugshare.data.model

import android.graphics.RectF
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

import com.google.gson.Gson

/**
 * Photo metadata extracted from EXIF data
 */
@Parcelize
data class PhotoMetadata(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val bearing: Float? = null,
    val dateTime: String? = null,
    val make: String? = null,
    val model: String? = null,
    val hasGpsData: Boolean = false,
    val hasBearingData: Boolean = false,
    val focusRect: RectF? = null
) : Parcelable {
    fun getLocationString(): String {
        return if (latitude != null && longitude != null) {
            String.format("%.6f, %.6f", latitude, longitude)
        } else {
            "No GPS data"
        }
    }

    fun getBearingString(): String {
        return bearing?.let {
            val direction = when {
                it < 22.5f || it >= 337.5f -> "N"
                it < 67.5f -> "NE"
                it < 112.5f -> "E"
                it < 157.5f -> "SE"
                it < 202.5f -> "S"
                it < 247.5f -> "SW"
                it < 292.5f -> "W"
                it < 337.5f -> "NW"
                else -> "N"
            }
            String.format("%.1f° (%s)", it, direction)
        } ?: "No bearing data"
    }

    fun toJsonString(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromJsonString(jsonString: String): PhotoMetadata {
            return Gson().fromJson(jsonString, PhotoMetadata::class.java)
        }
    }
}
