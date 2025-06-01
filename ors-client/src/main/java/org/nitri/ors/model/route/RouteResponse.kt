package org.nitri.ors.model.route

import kotlinx.serialization.Serializable

@Serializable
data class RouteResponse(
    val routes: List<Route>,
    val bbox: List<Double>
)