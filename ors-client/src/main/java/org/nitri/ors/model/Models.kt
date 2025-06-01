package org.nitri.ors.model

import kotlinx.serialization.Serializable

@Serializable
data class RouteRequest(
    val coordinates: List<List<Double>>
)

@Serializable
data class RouteResponse(
    val routes: List<Route>,
    val bbox: List<Double>
)

@Serializable
data class Route(
    val summary: RouteSummary,
    val segments: List<Segment>,
    val geometry: String
)

@Serializable
data class RouteSummary(val distance: Double, val duration: Double)

@Serializable
data class Segment(val distance: Double, val duration: Double)
