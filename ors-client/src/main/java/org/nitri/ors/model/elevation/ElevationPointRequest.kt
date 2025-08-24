package org.nitri.ors.model.elevation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ElevationPointRequest(
    @SerialName("format_in")
    val formatIn: String,                    // Input format, must be provided (e.g., "point")

    @SerialName("format_out")
    val formatOut: String = "geojson",       // "geojson" or "point"

    val dataset: String? = null,             // Optional dataset, e.g. "srtm"

    val geometry: List<Double>               // [lon, lat]
)
