package org.nitri.ors.model.route

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RouteRequest(
    val coordinates: List<List<Double>>,
    val radiuses: List<Double>? = null,
    val bearings: List<List<Double>>? = null,
    val elevation: Boolean? = null,
    val extra_info: List<String>? = null,
    val instructions: Boolean? = null,
    val instructions_format: String? = null,
    val language: String? = null,
    val preference: String? = null,
    val units: String? = null,
    val geometry: Boolean? = null,
    val geometry_simplify: Boolean? = null,
    val roundabout_exits: Boolean? = null,
    val attributes: List<String>? = null,
    val maneuvers: Boolean? = null,
    val continue_straight: Boolean? = null,
    val options: RouteOptions? = null
)

@Serializable
data class RouteOptions(
    val avoid_features: List<String>? = null,
    val avoid_polygons: AvoidPolygons? = null,
    val profile_params: ProfileParams? = null
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
    val max_height: Double? = null,
    val max_width: Double? = null,
    val max_weight: Double? = null,
    val max_length: Double? = null
)
