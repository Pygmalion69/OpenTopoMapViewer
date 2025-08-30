package org.nitri.ors.domain.route

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Payload for the directions `v2/directions/{profile}` endpoint.
 *
 * All parameters map to the official ORS API; nullable properties are omitted
 * from the serialized JSON when not provided.
 */
@Serializable
data class RouteRequest(
    /** List of `[lon, lat]` coordinate pairs in order of travel. */
    val coordinates: List<List<Double>>,
    /** Search radiuses around each coordinate in meters. */
    val radiuses: List<Double>? = null,
    /** List of `[bearing, range]` constraints for each coordinate. */
    val bearings: List<List<Double>>? = null,
    /** Include elevation data in response. */
    val elevation: Boolean? = null,
    @SerialName("extra_info")
    /** Additional information to include, e.g., `waytype`. */
    val extraInfo: List<String>? = null,
    /** Whether to return turn-by-turn instructions. */
    val instructions: Boolean? = null,
    @SerialName("instructions_format")
    /** Format of instructions such as `html` or `text`. */
    val instructionsFormat: String? = null,
    /** Preferred language for textual parts of the response. */
    val language: String? = null,
    /** Routing preference such as `fastest` or `shortest`. */
    val preference: String? = null,
    /** Unit system for distances. */
    val units: String? = null,
    /** Whether to include geometry. */
    val geometry: Boolean? = null,
    @SerialName("geometry_simplify")
    /** Simplify the returned geometry. */
    val geometrySimplify: Boolean? = null,
    @SerialName("roundabout_exits")
    /** Return `exit` indices for roundabouts. */
    val roundaboutExits: Boolean? = null,
    /** Additional attributes to include for each segment. */
    val attributes: List<String>? = null,
    /** Include a list of maneuvers. */
    val maneuvers: Boolean? = null,
    @SerialName("continue_straight")
    /** Force continue straight at waypoints? */
    val continueStraight: Boolean? = null,
    /** Optional advanced options. */
    val options: RouteOptions? = null
)

@Serializable
data class RouteOptions(
    @SerialName("avoid_features")
    /** Features to avoid, e.g., `ferries` or `tollways`. */
    val avoidFeatures: List<String>? = null,
    @SerialName("avoid_polygons")
    /** Optional polygon geometry to avoid. */
    val avoidPolygons: AvoidPolygons? = null,
    @SerialName("profile_params")
    /** Profile specific parameters such as restrictions. */
    val profileParams: ProfileParams? = null
)

@Serializable
data class AvoidPolygons(
    val type: String,
    val coordinates: List<List<List<Double>>>
)

@Serializable
data class ProfileParams(
    val weightings: Weightings? = null,
    val restrictions: Restrictions? = null
)

@Serializable
data class Weightings(
    val green: WeightingFactor? = null,
    val quiet: WeightingFactor? = null,
    val shortest: WeightingFactor? = null
)

@Serializable
data class WeightingFactor(
    val factor: Double
)

@Serializable
data class Restrictions(
    @SerialName("max_height")
    val maxHeight: Double? = null,
    @SerialName("max_width")
    val maxWidth: Double? = null,
    @SerialName("max_weight")
    val maxWeight: Double? = null,
    @SerialName("max_length")
    val maxLength: Double? = null
)
