package org.nitri.opentopo.ors

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nitri.ors.OrsClient
import org.nitri.ors.helper.RouteHelper
import org.nitri.opentopo.analytics.OrsErrorCategory
import org.nitri.opentopo.analytics.classifyOrsError

class Directions(val client: OrsClient, private val profile: String) {

    val routeHelper = RouteHelper()

    fun getRouteGpx(coordinates: List<List<Double>>, language: String, result: RouteGpxResult) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val gpxXml = with(routeHelper) { client.getRouteGpx(coordinates, language, profile, true) }
                withContext(Dispatchers.Main) {
                    if (gpxXml.isNotBlank()) {
                        result.onSuccess(gpxXml)
                    } else {
                        result.onEmpty()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    result.onError(classifyOrsError(e))
                }
            }
        }
    }

    interface RouteGpxResult {
        fun onSuccess(gpx: String)
        fun onEmpty()
        fun onError(category: OrsErrorCategory)
    }
}
