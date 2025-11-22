package com.a42r.pevplugshare.data.api

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

data class OsmandLoginRequest(
    val username: String,
    val password: String,
    val lang: String = "en"
)

data class OsmandLoginResponse(
    val status: String? = null,
    val message: String? = null,
    val userid: String? = null
)

data class OsmandFileInfo(
    val name: String,
    val type: String,
    val clienttime: String? = null,  // API returns date string like "Oct 13, 2025, 10:38:25 AM"
    val updatetime: String? = null,  // API returns date string like "Oct 13, 2025, 10:38:25 AM"
    val updatetimems: Long? = null,  // API also returns millisecond timestamp
    val clienttimems: Long? = null,  // API also returns millisecond timestamp
    val filesize: Long? = null,
    val zipSize: Long? = null,
    val details: OsmandFileDetails? = null
)

data class OsmandFileDetails(
    val analysis: OsmandFileAnalysis? = null
)

data class OsmandFileAnalysis(
    val wptPoints: Int? = null,
    val wptCategories: Map<String, Int>? = null
)

data class OsmandListFilesResponse(
    val uniqueFiles: List<OsmandFileInfo>? = null,
    val totalUniqueFiles: Int? = null,
    val totalFiles: Int? = null
)

// Request for /fav/add endpoint (simpler structure)
data class OsmandAddFavoriteRequest(
    val name: String,
    val address: String?,
    val desc: String?,
    val color: String,
    val background: String,
    val icon: String,
    val hidden: String = "true",  // Note: "true" means hidden by default
    val category: String?,  // Can be null
    val lat: String,  // Note: Web app sends as string
    val lon: String,  // Note: Web app sends as string
    val ext: OsmandFavoriteExtension? = null  // Can be null
)

// Request for /fav/update endpoint (more complex structure)
data class OsmandUpdateFavoriteRequest(
    val name: String,
    val desc: String = "",
    val address: String = "",
    val color: String,
    val background: String = "octagon",
    val icon: String,
    val hidden: String = "true",  // Note: "true" means hidden
    val lat: Double,
    val lon: Double,
    val category: String,  // Must match group name without "favorites-" and ".gpx"
    val ext: OsmandFavoriteExtension = OsmandFavoriteExtension()
)

data class OsmandFavoriteExtension(
    val firstPoint: Boolean = false,
    val lastPoint: Boolean = false,
    val deleted: Boolean = false,
    val time: Long = System.currentTimeMillis(),
    val ele: Double = 6.0,
    val speed: Double = 0.0,
    val hdop: Double? = null,
    val heading: Double? = null,
    val bearing: Double? = null,
    val speedColor: Int = 0,
    val altitudeColor: Int = 0,
    val slopeColor: Int = 0,
    val colourARGB: Int = 0,
    val distance: Double = 0.0,
    val extensions: Map<String, Any> = emptyMap()
)

// Error response from fav/update endpoint
data class OsmandFavUpdateErrorResponse(
    val error: OsmandFavUpdateError? = null
)

data class OsmandFavUpdateError(
    val errorCode: Int,
    val errorMessage: String? = null
)
