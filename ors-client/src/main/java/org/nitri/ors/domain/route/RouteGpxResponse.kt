package org.nitri.ors.domain.route

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** GPX response returned by the directions endpoint when requesting GPX. */
@Serializable
data class GpxResponse(
    val metadata: GpxMetadata,
    val routes: List<JsonElement> = emptyList(), // route content can vary
    val extensions: JsonElement? = null,
    val gpxRouteElements: List<JsonElement> = emptyList()
)

@Serializable
data class GpxMetadata(
    val name: String? = null,
    val description: String? = null,
    val author: JsonElement? = null,
    val copyright: JsonElement? = null,
    val timeGenerated: String? = null,
    val bounds: GpxBounds? = null,
    val extensions: JsonElement? = null
)

@Serializable
data class GpxBounds(
    val asArray: List<Double>? = null,
    val minLon: Double,
    val maxLon: Double,
    val minLat: Double,
    val maxLat: Double
)
