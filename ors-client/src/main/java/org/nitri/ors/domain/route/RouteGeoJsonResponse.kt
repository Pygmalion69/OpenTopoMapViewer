package org.nitri.ors.domain.route

import kotlinx.serialization.Serializable
import org.nitri.ors.domain.meta.Metadata

@Serializable
data class GeoJsonRouteResponse(
    val type: String,
    val bbox: List<Double>,
    val features: List<Feature>,
    val metadata: Metadata
)

@Serializable
data class Feature(
    val type: String,
    val geometry: Geometry,
    val properties: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap()
)

@Serializable
data class Geometry(
    val type: String,
    val coordinates: List<List<Double>> // or List<List<List<Double>>> for MultiLineString, etc.
)