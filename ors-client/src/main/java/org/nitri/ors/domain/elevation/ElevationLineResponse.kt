package org.nitri.ors.domain.elevation

import kotlinx.serialization.Serializable

@Serializable
data class ElevationLineResponse(
    val attribution: String? = null,
    val geometry: ElevationLineGeometry,
    val timestamp: Long? = null,
    val version: String? = null
)

@Serializable
data class ElevationLineGeometry(
    val type: String, // "LineString"
    /** Coordinates with elevation: [lon, lat, ele] (API may return [lon,lat] if ele missing) */
    val coordinates: List<List<Double>>
)