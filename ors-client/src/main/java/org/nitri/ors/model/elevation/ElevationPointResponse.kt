package org.nitri.ors.model.elevation

import kotlinx.serialization.Serializable

@Serializable
data class ElevationPointResponse(
    val attribution: String,
    val geometry: ElevationPointGeometry,
    val timestamp: Long,
    val version: String
)

@Serializable
data class ElevationPointGeometry(
    val coordinates: List<Double>,   // [lon, lat, elevation]
    val type: String                 // "Point"
)
