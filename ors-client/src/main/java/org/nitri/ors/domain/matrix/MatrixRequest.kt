package org.nitri.ors.domain.matrix

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a request for the matrix service.
 *
 * @property locations A list of coordinates as `[longitude, latitude]` pairs.
 * @property destinations Optional. A list of indices that refers to the list of locations (starting with `0`).
 *                          `{ "locations": [[9.70093,48.477473],[9.207916,49.153868],[37.573242,55.801281],[115.663757,38.106467]], "destinations": [0,1] }`
 *                          will calculate distances from origin 0 to destinations 0 and 1, and from origin 1 to destinations 0 and 1.
 *                          Default is all locations.
 * @property id Optional. An arbitrary identification string of the request.
 *               This field is not used by the service and only returned in the response.
 * @property metrics Optional. Specifies a list of returned metrics.
 *                   - `distance` - Returns distance matrix in meters.
 *                   - `duration` - Returns duration matrix in seconds.
 *                   Default is `["duration"]`.
 * @property resolveLocations Optional. Specifies whether given locations are resolved or not. If `true`, locations are resolved to the road network,
 *                            if `false` just the locations are used.
 *                            Default is `false`.
 * @property sources Optional. A list of indices that refers to the list of locations (starting with `0`).
 *                     `{ "locations": [[9.70093,48.477473],[9.207916,49.153868],[37.573242,55.801281],[115.663757,38.106467]], "sources": [0,1] }`
 *                     will calculate distances from origin 0 to all destinations and from origin 1 to all destinations.
 *                     Default is all locations.
 */
@Serializable
data class MatrixRequest(

    /** A list of coordinates as `[longitude, latitude]` pairs. */
    val locations: List<List<Double>>,

    /** Optional. A list of indices that refers to the list of locations (starting with `0`). */
    val destinations: List<Int>? = null,

    /** Optional. An arbitrary identification string of the request. */
    val id: String? = null,

    /** Optional. Specifies a list of returned metrics. */
    val metrics: List<String>? = null,

    @SerialName("resolve_locations")
    /** Optional. Specifies whether given locations are resolved or not. */
    val resolveLocations: Boolean? = null,

    /** Optional. A list of indices that refers to the list of locations (starting with `0`). */
    val sources: List<Int>? = null
)