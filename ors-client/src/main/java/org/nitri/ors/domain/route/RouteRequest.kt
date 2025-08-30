package org.nitri.ors.model.route

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RouteRequest(
    val coordinates: List<List<Double>>,
    val radiuses: List<Double>? = null,
    val bearings: List<List<Double>>? = null,
    val elevation: Boolean? = null,
    @SerialName("extra_info")
    val extraInfo: List<String>? = null,
    val instructions: Boolean? = null,
    @SerialName("instructions_format")
    val instructionsFormat: String? = null,
    val language: String? = null,
    val preference: String? = null,
    val units: String? = null,
    val geometry: Boolean? = null,
    @SerialName("geometry_simplify")
    val geometrySimplify: Boolean? = null,
    @SerialName("roundabout_exits")
    val roundaboutExits: Boolean? = null,
    val attributes: List<String>? = null,
    val maneuvers: Boolean? = null,
    @SerialName("continue_straight")
    val continueStraight: Boolean? = null,
    val options: RouteOptions? = null
)

@Serializable
data class RouteOptions(
    @SerialName("avoid_features")
    val avoidFeatures: List<String>? = null,
    @SerialName("avoid_polygons")
    val avoidPolygons: AvoidPolygons? = null,
    @SerialName("profile_params")
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
