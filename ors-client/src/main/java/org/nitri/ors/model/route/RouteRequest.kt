package org.nitri.ors.model.route

import kotlinx.serialization.Serializable

@Serializable
data class RouteRequest(
    val coordinates: List<List<Double>>
)