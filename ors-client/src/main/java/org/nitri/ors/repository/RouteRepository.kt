package org.nitri.ors.repository

import org.nitri.ors.api.OpenRouteServiceApi
import org.nitri.ors.model.RouteRequest
import org.nitri.ors.model.RouteResponse

class RouteRepository(private val api: OpenRouteServiceApi) {
    suspend fun getRoute(start: Pair<Double, Double>, end: Pair<Double, Double>, profile: String): RouteResponse {
        val request = RouteRequest(coordinates = listOf(
            listOf(start.first, start.second),
            listOf(end.first, end.second)
        ))
        return api.getRoute(profile, request)
    }
}