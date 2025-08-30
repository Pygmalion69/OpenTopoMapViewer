package org.nitri.ors.domain.snap

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.nitri.ors.domain.meta.Metadata

/**
 * Response for ORS /v2/snap/{profile}[/json]
 */
@Serializable
data class SnapResponse(
    val locations: List<SnapLocation>,
    val metadata: Metadata
)

/**
 * One snapped input coordinate.
 */
@Serializable
data class SnapLocation(
    /** [lon, lat] of the snapped position */
    val location: List<Double>,

    /** Optional street name if available */
    val name: String? = null,

    /** Distance in meters from the input to the snapped position */
    @SerialName("snapped_distance")
    val snappedDistance: Double
)
