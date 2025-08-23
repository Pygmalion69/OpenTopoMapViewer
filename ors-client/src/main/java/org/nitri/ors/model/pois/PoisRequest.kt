package org.nitri.ors.model.pois

import kotlinx.serialization.Serializable

@Serializable
data class PoisRequest(
    val request: String = "pois",
    val geometry: Geometry,
    val filters: Map<String, String>? = null,
    val limit: Int? = null,
    val sortby: String? = null
)

@Serializable
data class Geometry(
    val bbox: List<List<Double>>? = null,        // [[minLon,minLat],[maxLon,maxLat]]
    val geojson: GeoJsonGeometry? = null,        // optional: GeoJSON geometry
    val buffer: Int? = null                      // optional: buffer in meters
)

@Serializable
data class GeoJsonGeometry(
    val type: String,                            // e.g., "Point"
    val coordinates: List<Double>                // [lon, lat]
)
