package org.nitri.ors.domain.export

import kotlinx.serialization.Serializable

/** Request payload for the export endpoint. */
@Serializable
data class ExportRequest(
    /** Bounding box specified as `[[minLon,minLat],[maxLon,maxLat]]`. */
    val bbox: List<List<Double>>,
    /** Client-specified identifier. */
    val id: String,
    /** Whether to include full geometry in the response. */
    val geometry: Boolean? = null
)
