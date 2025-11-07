package com.pevplugshare.data.api

data class OsmandAuthResponse(
    val access_token: String,
    val token_type: String = "Bearer",
    val expires_in: Int? = null,
    val refresh_token: String? = null
)

data class OsmandUserInfo(
    val userid: String,
    val nickname: String? = null,
    val email: String? = null,
    val login: String? = null
)

data class OsmandCategory(
    val name: String,
    val color: String? = null,
    val icon: String? = null,
    val description: String? = null
)

data class OsmandMarker(
    val id: String? = null,
    val name: String,
    val description: String? = null,
    val latitude: Double,
    val longitude: Double,
    val color: String? = null,
    val icon: String? = null,
    val category: String? = null,
    val timestamp: Long? = null
)

data class OsmandMarkersResponse(
    val markers: List<OsmandMarker>? = null,
    val features: List<OsmandFeature>? = null
)

data class OsmandFeature(
    val type: String = "Feature",
    val geometry: OsmandGeometry,
    val properties: Map<String, Any>
)

data class OsmandGeometry(
    val type: String = "Point",
    val coordinates: List<Double> // [longitude, latitude]
)

data class OsmandCategoriesResponse(
    val categories: List<OsmandCategory>? = null
)

data class OsmandErrorResponse(
    val error: String? = null,
    val error_description: String? = null,
    val message: String? = null
)
