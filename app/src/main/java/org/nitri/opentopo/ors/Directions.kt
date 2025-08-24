package org.nitri.opentopo.ors

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nitri.ors.api.OpenRouteServiceApi
import org.nitri.ors.repository.RouteRepository

class Directions(val api: OpenRouteServiceApi, private val profile: String) {

    val repository = RouteRepository(api)

    fun getRouteGpx(coordinates: List<List<Double>>, language: String, result: RouteGpResult) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val gpxXml = repository.getRouteGpx(coordinates, language, profile)
                withContext(Dispatchers.Main) {
                    if (gpxXml.isNotBlank()) {
                        result.onSuccess(gpxXml)
                    } else {
                        result.onError("Empty response body")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    result.onError("Failed to fetch GPX: ${e.message}")
                }
            }
        }
    }

    interface RouteGpResult {
        fun onSuccess(gpx: String)
        fun onError(message: String)
    }
}