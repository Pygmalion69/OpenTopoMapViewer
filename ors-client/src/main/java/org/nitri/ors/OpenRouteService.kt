package org.nitri.ors

import org.nitri.ors.client.OpenRouteServiceClient
import org.nitri.ors.repository.RouteRepository

class OpenRouteService(apiKey: String) {
    private val api = OpenRouteServiceClient.create(apiKey)
    val routeRepository = RouteRepository(api)
}
