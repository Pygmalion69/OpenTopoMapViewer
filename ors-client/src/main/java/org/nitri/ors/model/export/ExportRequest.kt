package org.nitri.ors.model.export

import kotlinx.serialization.Serializable

@Serializable
data class ExportRequest(
    val boundingBox: List<Double>, // [minLon, minLat, maxLon, maxLat]
    val profile: String
)
