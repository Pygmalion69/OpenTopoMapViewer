package org.nitri.ors.domain.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** TopoJSON variant of the export response. */
@Serializable
data class TopoJsonExportResponse(
    val type: String,
    val objects: TopoObjects,
    /** Top-level arcs represented as `[lon, lat]` coordinate pairs. */
    val arcs: List<List<List<Double>>>,
    val bbox: List<Double>
)

@Serializable
data class TopoObjects(
    val network: GeometryCollection
)

@Serializable
data class GeometryCollection(
    val type: String,
    val geometries: List<TopoGeometry>
)

@Serializable
data class TopoGeometry(
    val type: String,
    /** Indices into the top-level [TopoJsonExportResponse.arcs] array. */
    val arcs: List<Int>,
    val properties: GeometryProps? = null
)

@Serializable
data class GeometryProps(
    /** Edge weight often supplied as a string. */
    val weight: String? = null,
    @SerialName("node_from") val nodeFrom: Long? = null,
    @SerialName("node_to") val nodeTo: Long? = null
)

