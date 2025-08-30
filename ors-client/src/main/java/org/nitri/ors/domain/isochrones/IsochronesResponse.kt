package org.nitri.ors.domain.isochrones

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GeoJSON response returned by the isochrones endpoint. */
@Serializable
data class IsochronesResponse(
    val type: String,
    val bbox: List<Double>,
    val features: List<IsochroneFeature>,
    val metadata: IsochronesMetadata
)

/** Individual isochrone feature. */
@Serializable
data class IsochroneFeature(
    val type: String,
    val properties: IsochroneProperties,
    val geometry: IsochroneGeometry
)

@Serializable
data class IsochroneProperties(
    @SerialName("group_index") val groupIndex: Int,
    val value: Double,                   // <-- was Int
    val center: List<Double>
)

/** Geometry of an isochrone feature. */
@Serializable
data class IsochroneGeometry(
    val type: String,
    val coordinates: List<List<List<Double>>>
)

@Serializable
data class IsochronesMetadata(
    val attribution: String,
    val service: String,
    val timestamp: Long,
    val query: IsochronesQuery,
    val engine: IsochronesEngine
)

@Serializable
data class IsochronesQuery(
    val profile: String,
    val profileName: String,
    val locations: List<List<Double>>,
    val range: List<Double>,             // <-- was List<Int>
    @SerialName("range_type") val rangeType: String? = null
)

@Serializable
data class IsochronesEngine(
    val version: String,
    @SerialName("build_date") val buildDate: String,
    @SerialName("graph_date") val graphDate: String,
    @SerialName("osm_date") val osmDate: String
)
