package org.nitri.ors.model.route

import kotlinx.serialization.Serializable
import org.nitri.ors.model.meta.Metadata

@Serializable
data class RouteResponse(
    val routes: List<Route>,
    val bbox: List<Double>,
    val metadata: Metadata? = null
)

@Serializable
data class Route(
    val summary: RouteSummary,
    val segments: List<Segment>,
    val geometry: String? = null,
    val way_points: List<Int>? = null
)

@Serializable
data class RouteSummary(
    val distance: Double,
    val duration: Double,
    val ascent: Double? = null,
    val descent: Double? = null
)

@Serializable
data class Segment(
    val distance: Double,
    val duration: Double,
    val steps: List<Step>? = null,
    val ascent: Double? = null,
    val descent: Double? = null,
    val detourfactor: Double? = null,
    val percentage: Double? = null
)

@Serializable
data class Step(
    val distance: Double,
    val duration: Double,
    val instruction: String,
    val name: String,
    val type: Int,
    val way_points: List<Int>,
    val exit_number: Int? = null
)

