package org.nitri.ors.domain.route

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.nitri.ors.domain.meta.Metadata

/** Response from the directions endpoint. */
@Serializable
data class RouteResponse(
    val routes: List<Route>,
    val bbox: List<Double>,
    val metadata: Metadata? = null
)

/** Single route variant returned by the API. */
@Serializable
data class Route(
    val summary: RouteSummary,
    val segments: List<Segment>,
    val geometry: String? = null,
    @SerialName("way_points")
    val wayPoints: List<Int>? = null
)

/** Aggregated summary of a route. */
@Serializable
data class RouteSummary(
    val distance: Double,
    val duration: Double,
    val ascent: Double? = null,
    val descent: Double? = null
)

/** One segment between intermediate waypoints. */
@Serializable
data class Segment(
    val distance: Double,
    val duration: Double,
    val steps: List<Step>? = null,
    val ascent: Double? = null,
    val descent: Double? = null,
    @SerialName("detour_factor")
    val detourFactor: Double? = null,
    val percentage: Double? = null
)

/** Turn instruction within a segment. */
@Serializable
data class Step(
    val distance: Double,
    val duration: Double,
    val instruction: String,
    val name: String,
    val type: Int,
    @SerialName("way_points")
    val wayPoints: List<Int>,
    @SerialName("exit_number")
    val exitNumber: Int? = null
)

