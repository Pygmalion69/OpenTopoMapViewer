package org.nitri.ors.domain.route

import kotlinx.serialization.Serializable
import org.nitri.ors.domain.meta.Metadata

/** GeoJSON response for the directions endpoint. */
@Serializable
data class GeoJsonRouteResponse(
    val type: String,
    val bbox: List<Double>,
    val features: List<Feature>,
    val metadata: Metadata
)

/** A single GeoJSON feature within the route response. */
@Serializable
data class Feature(
    val type: String,
    val geometry: Geometry,
    val properties: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap()
)

/** Geometry of a route feature. */
@Serializable
data class Geometry(
    val type: String,
    val coordinates: List<List<Double>> // or List<List<List<Double>>> for MultiLineString, etc.
)