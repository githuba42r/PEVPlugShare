package com.a42r.pevplugshare.util

import android.content.Context
import android.content.SharedPreferences

object QuickLocationPrefs {
    private const val PREFS_NAME = "quick_location_prefs"
    private const val KEY_LAST_LATITUDE = "last_latitude"
    private const val KEY_LAST_LONGITUDE = "last_longitude"

    fun saveLastLocation(context: Context, latitude: Double, longitude: Double) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_LATITUDE, java.lang.Double.doubleToRawLongBits(latitude))
            .putLong(KEY_LAST_LONGITUDE, java.lang.Double.doubleToRawLongBits(longitude))
            .apply()
    }

    fun getLastLocation(context: Context): Pair<Double, Double>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val latBits = prefs.getLong(KEY_LAST_LATITUDE, java.lang.Double.doubleToRawLongBits(Double.NaN))
        val lngBits = prefs.getLong(KEY_LAST_LONGITUDE, java.lang.Double.doubleToRawLongBits(Double.NaN))
        val lat = java.lang.Double.longBitsToDouble(latBits)
        val lng = java.lang.Double.longBitsToDouble(lngBits)
        return if (!lat.isNaN() && !lng.isNaN()) Pair(lat, lng) else null
    }
}
