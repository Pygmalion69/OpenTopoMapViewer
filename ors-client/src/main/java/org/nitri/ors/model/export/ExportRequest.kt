package org.nitri.ors.model.export

import kotlinx.serialization.Serializable

@Serializable
data class ExportRequest(
    val bbox: List<List<Double>>, // [minLon, minLat, maxLon, maxLat]
    val id: String,
    val geometry: Boolean? = null
)
