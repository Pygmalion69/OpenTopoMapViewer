package org.nitri.ors.domain.matrix

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.nitri.ors.domain.meta.Metadata

/**
 * Represents the response from an ORS Matrix API request.
 *
 * The matrix response contains information about travel times or distances
 * between a set of source and destination points.
 *
 * @property durations A list of lists representing the travel duration matrix.
 *                     Present only if 'durations' was requested in the `metrics` parameter.
 *                     The outer list corresponds to sources, and the inner list corresponds to destinations.
 *                     Each value is the duration in seconds.
 * @property distances A list of lists representing the travel distance matrix.
 *                     Present only if 'distances' was requested in the `metrics` parameter.
 *                     The outer list corresponds to sources, and the inner list corresponds to destinations.
 *                     Each value is the distance in meters.
 * @property sources A list of enriched waypoint information for the source locations.
 *                   This is typically present when `resolve_locations=true` is used in the request,
 *                   but might sometimes be included even if `resolve_locations=false`.
 * @property destinations A list of enriched waypoint information for the destination locations.
 *                        This is typically present when `resolve_locations=true` is used in the request,
 *                        but might sometimes be included even if `resolve_locations=false`.
 * @property metadata Standard ORS metadata including information about the service, engine, query, etc.
 */
@Serializable
data class MatrixResponse(
    // Present only if requested in `metrics`
    val durations: List<List<Double>>? = null,
    val distances: List<List<Double>>? = null,

    // Enriched waypoint info when resolve_locations=true (or sometimes even if false)
    val sources: List<MatrixWaypoint>? = null,
    val destinations: List<MatrixWaypoint>? = null,

    // Standard ORS metadata (service, engine, query, etc.)
    val metadata: Metadata? = null
)
/**
 * Represents a waypoint used in the ORS Matrix API response, providing details about a specific location.
 *
 * @property location The coordinates of the waypoint as a list of [longitude, latitude].
 * @property name An optional name associated with the waypoint, which ORS may include.
 * @property snappedDistance The distance in meters from the input coordinate to the snapped network coordinate.
 *                           This is an optional extra that ORS may include.
 */
@Serializable
data class MatrixWaypoint(
    // [lon, lat]
    val location: List<Double>,

    // Optional extras ORS may include
    val name: String? = null,
    @SerialName("snapped_distance")
    val snappedDistance: Double? = null
)