package org.nitri.ors.model.export

data class TopoJsonExportResponse {
}package org.nitri.ors.model.export

import kotlinx.serialization.Serializable

@Serializable
data class TopoJsonExportResponse(
    val type: String,
    val objects: Map<String, Any>,
    val arcs: List<List<List<Int>>>,
    val transform: Transform
)

@Serializable
data class Transform(
    val scale: List<Double>,
    val translate: List<Double>
)
