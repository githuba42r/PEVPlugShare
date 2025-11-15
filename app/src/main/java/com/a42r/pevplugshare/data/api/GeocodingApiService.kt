package com.a42r.pevplugshare.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApiService {
    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("format") format: String = "json",
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("zoom") zoom: Int = 18,
        @Query("addressdetails") addressDetails: Int = 1
    ): Response<NominatimResponse>
}

data class NominatimResponse(
    val place_id: Long? = null,
    val licence: String? = null,
    val osm_type: String? = null,
    val osm_id: Long? = null,
    val lat: String? = null,
    val lon: String? = null,
    val display_name: String? = null,
    val address: NominatimAddress? = null,
    val boundingbox: List<String>? = null
)

data class NominatimAddress(
    val house_number: String? = null,
    val road: String? = null,
    val suburb: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    val country: String? = null,
    val country_code: String? = null,
    val amenity: String? = null,
    val shop: String? = null,
    val tourism: String? = null,
    val building: String? = null,
    val office: String? = null,
    val name: String? = null
)