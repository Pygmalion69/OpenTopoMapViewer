package org.nitri.ors.model.export

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class TopoJsonExportResponse(
    val type: String,
    val objects: Map<String, JsonElement> = emptyMap(), // or JsonObject
    val arcs: List<List<List<Int>>> = emptyList(),
    val transform: Transform
)

@Serializable
data class Transform(
    val scale: List<Double>,
    val translate: List<Double>
)
