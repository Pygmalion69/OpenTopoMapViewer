package org.nitri.opentopo.ors

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.nitri.ors.api.OpenRouteServiceApi
import org.nitri.ors.model.route.RouteRequest
import retrofit2.Response

class Directions(val api: OpenRouteServiceApi, private val profile: String) {

    fun getRouteGpx(coordinates: List<List<Double>>, result: RouteGpResult) {
        CoroutineScope(Dispatchers.IO).launch {
            val request = RouteRequest(coordinates = coordinates)
            try {
                val response : Response<ResponseBody> = api.getRouteGpx(profile, request)
                withContext(Dispatchers.Main) {
                    val gpxXml = response.body()?.string() ?: ""
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