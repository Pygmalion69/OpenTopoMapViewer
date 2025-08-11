package org.nitri.ors.repository

import okhttp3.ResponseBody
import org.nitri.ors.api.OpenRouteServiceApi
import org.nitri.ors.model.route.GeoJsonRouteResponse
import org.nitri.ors.model.route.RouteRequest
import org.nitri.ors.model.route.RouteResponse
import retrofit2.Response

class RouteRepository(private val api: OpenRouteServiceApi) {
    suspend fun getRoute(start: Pair<Double, Double>, end: Pair<Double, Double>, profile: String): RouteResponse {
        val request = RouteRequest(coordinates = listOf(
            listOf(start.first, start.second),
            listOf(end.first, end.second)
        ))
        return api.getRoute(profile, request)
    }

    suspend fun getRouteGpx(start: Pair<Double, Double>, end: Pair<Double, Double>, profile: String): Response<ResponseBody> {
        val request = RouteRequest(coordinates = listOf(
            listOf(start.first, start.second),
            listOf(end.first, end.second)
        ))
        return api.getRouteGpx(profile, request)
    }

    suspend fun getRouteGpx(coordinates: List<List<Double>>, language: String, profile: String): Response<ResponseBody> {
        val request = RouteRequest(coordinates = coordinates, language = language)
        return api.getRouteGpx(profile, request)
    }

    suspend fun getRouteGeoJson(start: Pair<Double, Double>, end: Pair<Double, Double>, profile: String): GeoJsonRouteResponse {
        val request = RouteRequest(coordinates = listOf(
            listOf(start.first, start.second),
            listOf(end.first, end.second)
        ))
        return api.getRouteGeoJson(profile, request)
    }
}