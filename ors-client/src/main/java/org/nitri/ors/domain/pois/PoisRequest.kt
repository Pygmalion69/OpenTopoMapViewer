package org.nitri.ors.domain.pois

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

/** Request for the POIs endpoint. */
@Serializable
data class PoisRequest(
    @Required val request: String = "pois",
    val geometry: Geometry,
    val filters: Map<String, String>? = null,
    val limit: Int? = null,
    val sortby: String? = null
)

@Serializable
data class Geometry(
    /** Bounding box `[[minLon,minLat],[maxLon,maxLat]]` if set. */
    val bbox: List<List<Double>>? = null,
    /** Optional GeoJSON geometry. */
    val geojson: GeoJsonGeometry? = null,
    /** Optional buffer in meters applied to the geometry. */
    val buffer: Int? = null
)

@Serializable
data class GeoJsonGeometry(
    val type: String,
    /** `[lon, lat]` pair defining the point location. */
    val coordinates: List<Double>
)
