package org.nitri.ors.helper

import org.nitri.ors.OrsClient
import org.nitri.ors.Profile
import org.nitri.ors.model.route.GeoJsonRouteResponse
import org.nitri.ors.model.route.RouteRequest
import org.nitri.ors.model.route.RouteResponse

class RouteHelper() {

    private fun profileFromKey(key: String): Profile =
        Profile.values().firstOrNull { it.key == key }
            ?: throw IllegalArgumentException("Unknown profile key: $key")

    suspend fun OrsClient.getRoute(
        start: Pair<Double, Double>,
        end: Pair<Double, Double>,
        profile: String
    ): RouteResponse {
        val request = RouteRequest(
            coordinates = listOf(
                listOf(start.first, start.second),
                listOf(end.first, end.second)
            )
        )
        return getRoute(profileFromKey(profile), request)
    }

    suspend fun OrsClient.getRouteGpx(
        start: Pair<Double, Double>,
        end: Pair<Double, Double>,
        profile: String
    ): String {
        val request = RouteRequest(
            coordinates = listOf(
                listOf(start.first, start.second),
                listOf(end.first, end.second)
            )
        )
        return getRouteGpx(profileFromKey(profile), request)
    }

    suspend fun OrsClient.getRouteGpx(
        coordinates: List<List<Double>>,
        language: String,
        profile: String
    ): String {
        val request = RouteRequest(coordinates = coordinates, language = language)
        return getRouteGpx(profileFromKey(profile), request)
    }

    suspend fun OrsClient.getRouteGeoJson(
        start: Pair<Double, Double>,
        end: Pair<Double, Double>,
        profile: String
    ): GeoJsonRouteResponse {
        val request = RouteRequest(
            coordinates = listOf(
                listOf(start.first, start.second),
                listOf(end.first, end.second)
            )
        )
        return getRouteGeoJson(profile, request)
    }
}