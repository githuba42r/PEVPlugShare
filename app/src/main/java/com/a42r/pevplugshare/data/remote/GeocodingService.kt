package com.a42r.pevplugshare.data.remote

import android.util.Log
import com.a42r.pevplugshare.data.api.GeocodingApiService
import com.a42r.pevplugshare.data.api.NominatimResponse
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

class GeocodingService {

    companion object {
        private const val TAG = "GeocodingService"
        private const val USER_AGENT = "PEVPlugShare/1.0 (https://github.com/your-repo/pevplugshare)"
        private const val MIN_REQUEST_INTERVAL_MS = 1100L // 1.1 seconds to be safe
    }

    private var lastRequestTime = 0L

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .build()
            chain.proceed(request)
        }
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://nominatim.openstreetmap.org/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(GeocodingApiService::class.java)

    suspend fun reverseGeocode(lat: Double, lon: Double): GeocodingResult {
        // Implement rate limiting - Nominatim requires at least 1 second between requests
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRequest = currentTime - lastRequestTime
        if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
            val delayMs = MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest
            Log.d(TAG, "Rate limiting: delaying request by ${delayMs}ms")
            delay(delayMs)
        }
        lastRequestTime = System.currentTimeMillis()

        Log.d(TAG, "Reverse geocoding request: lat=$lat, lon=$lon")

        return try {
            val response = apiService.reverseGeocode(lat = lat, lon = lon)
            Log.d(TAG, "Reverse geocoding response: code=${response.code()}, message=${response.message()}")

            if (response.isSuccessful) {
                val nominatimResponse = response.body()
                Log.d(TAG, "Reverse geocoding response body: $nominatimResponse")

                nominatimResponse?.let {
                    val result = parseGeocodingResult(it)
                    Log.d(TAG, "Reverse geocoding result: $result")
                    result
                } ?: run {
                    val error = GeocodingResult.Error("Empty response")
                    Log.w(TAG, "Reverse geocoding failed: $error")
                    error
                }
            } else {
                val errorMessage = when (response.code()) {
                    403 -> "HTTP 403: Forbidden - Rate limit exceeded or blocked. Please wait before retrying."
                    429 -> "HTTP 429: Too Many Requests - Rate limit exceeded. Please wait before retrying."
                    else -> "HTTP ${response.code()}: ${response.message()}"
                }
                val error = GeocodingResult.Error(errorMessage)
                Log.w(TAG, "Reverse geocoding failed: $error")
                error
            }
        } catch (e: Exception) {
            val error = GeocodingResult.Error("Network error: ${e.message}")
            Log.e(TAG, "Reverse geocoding exception", e)
            error
        }
    }

    private fun parseGeocodingResult(response: NominatimResponse): GeocodingResult {
        Log.d(TAG, "Parsing geocoding response: $response")

        val displayName = response.display_name
        Log.d(TAG, "Extracted displayName: $displayName")

        val address = response.address ?: run {
            Log.w(TAG, "No address data in response")
            return GeocodingResult.Error("No address data")
        }

        Log.d(TAG, "Address components: amenity=${address.amenity}, shop=${address.shop}, tourism=${address.tourism}, office=${address.office}, name=${address.name}")
        Log.d(TAG, "Address components: house_number=${address.house_number}, road=${address.road}, suburb=${address.suburb}, city=${address.city}, state=${address.state}, postcode=${address.postcode}")

        val businessName = address.amenity ?: address.shop ?: address.tourism ?: address.office ?: address.name
        Log.d(TAG, "Extracted businessName: $businessName")

        val streetAddress = buildString {
            address.house_number?.let { append(it).append(" ") }
            address.road?.let { append(it) }
        }.trim().takeIf { it.isNotEmpty() }
        Log.d(TAG, "Extracted streetAddress: $streetAddress")

        val suburb = address.suburb ?: address.city
        Log.d(TAG, "Extracted suburb: $suburb")

        val state = address.state
        Log.d(TAG, "Extracted state: $state")

        val postcode = address.postcode
        Log.d(TAG, "Extracted postcode: $postcode")

        val result = GeocodingResult.Success(
            displayName = displayName,
            businessName = businessName,
            streetAddress = streetAddress,
            suburb = suburb,
            state = state,
            postcode = postcode
        )
        Log.d(TAG, "Final parsed result: $result")

        return result
    }
}

sealed class GeocodingResult {
    data class Success(
        val displayName: String? = null,
        val businessName: String? = null,
        val streetAddress: String? = null,
        val suburb: String? = null,
        val state: String? = null,
        val postcode: String? = null
    ) : GeocodingResult()

    data class Error(val message: String) : GeocodingResult()
}