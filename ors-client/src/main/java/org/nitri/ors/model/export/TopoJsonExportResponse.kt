package org.nitri.ors.model.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TopoJsonExportResponse(
    val type: String, // "Topology"
    val objects: TopoObjects,
    /** Top-level arcs are arrays of [lon, lat] Double coordinates */
    val arcs: List<List<List<Double>>>,
    val bbox: List<Double> // [minLon, minLat, maxLon, maxLat]
)

@Serializable
data class TopoObjects(
    val network: GeometryCollection
)

@Serializable
data class GeometryCollection(
    val type: String, // "GeometryCollection"
    val geometries: List<TopoGeometry>
)

@Serializable
data class TopoGeometry(
    val type: String, // "LineString" (currently)
    /** These are indices into the top-level arcs array (can be negative to indicate reversal) */
    val arcs: List<Int>,
    val properties: GeometryProps? = null // be lenient; fields can vary
)

@Serializable
data class GeometryProps(
    // weight often arrives as a string in this endpoint
    val weight: String? = null,
    @SerialName("node_from") val nodeFrom: Long? = null,
    @SerialName("node_to")   val nodeTo: Long? = null
)

