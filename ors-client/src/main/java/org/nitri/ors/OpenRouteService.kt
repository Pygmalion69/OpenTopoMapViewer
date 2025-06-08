package org.nitri.ors

import android.content.Context
import org.nitri.ors.client.OpenRouteServiceClient
import org.nitri.ors.repository.RouteRepository

class OpenRouteService(apiKey: String, context: Context) {
    private val api = OpenRouteServiceClient.create(apiKey, context)
    val routeRepository = RouteRepository(api)
}
