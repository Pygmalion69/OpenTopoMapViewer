package org.nitri.ors.domain.elevation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement


@Serializable
data class ElevationLineRequest(
    /** One of: "geojson", "polyline", "encodedpolyline5", "encodedpolyline6" */
    @SerialName("format_in") val formatIn: String,

    /** One of: "geojson", "polyline", "encodedpolyline5", "encodedpolyline6" */
    @SerialName("format_out") val formatOut: String,

    /**
     * Geometry payload. The API accepts different shapes depending on format:
     *  - format_in = "geojson"           -> a GeoJSON LineString object
     *  - format_in = "polyline"          -> [[lon,lat], [lon,lat], ...]
     *  - format_in = "encodedpolyline5/6"-> a single encoded polyline string
     *
     * Using JsonElement keeps this field flexible for all variants.
     */
    val geometry: JsonElement,

    /** Optional: pick a specific elevation dataset (e.g., "SRTM", "COP90", …) */
    val dataset: String? = null
)

object ElevationFormats {
    const val GEOJSON = "geojson"
    const val POLYLINE = "polyline"
    const val ENCODED_5 = "encodedpolyline5"
    const val ENCODED_6 = "encodedpolyline6"
}