package org.nitri.ors.domain.elevation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Request for the `elevation/point` endpoint. */
@Serializable
data class ElevationPointRequest(
    @SerialName("format_in")
    /** Input format, e.g. `point`. */
    val formatIn: String,

    @SerialName("format_out")
    /** Output format: `geojson` or `point`. */
    val formatOut: String = "geojson",

    /** Optional elevation dataset name, e.g. `srtm`. */
    val dataset: String? = null,

    /** Coordinate as `[lon, lat]`. */
    val geometry: List<Double>
)
