package org.nitri.ors.helper

import org.nitri.ors.OrsClient
import org.nitri.ors.Profile
import org.nitri.ors.domain.route.GeoJsonRouteResponse
import org.nitri.ors.domain.route.RouteRequest
import org.nitri.ors.domain.route.RouteResponse

/**
 * Convenience extensions for invoking the directions endpoints.
 */
class RouteHelper {

    /** Converts a profile key string to the corresponding [Profile] enum. */
    private fun profileFromKey(key: String): Profile =
        Profile.entries.firstOrNull { it.key == key }
            ?: throw IllegalArgumentException("Unknown profile key: $key")

    /**
     * Retrieves a route between two coordinates.
     */
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

    /**
     * Retrieves a route as GPX between two coordinates.
     */
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

    /**
     * Retrieves a route as GPX for an arbitrary coordinate list.
     */
    suspend fun OrsClient.getRouteGpx(
        coordinates: List<List<Double>>,
        language: String,
        profile: String
    ): String {
        val request = RouteRequest(coordinates = coordinates, language = language)
        return getRouteGpx(profileFromKey(profile), request)
    }

    /**
     * Retrieves a route as GeoJSON feature collection.
     */
    suspend fun OrsClient.getRouteGeoJson(
        start: Pair<Double, Double>,
        end: Pair<Double, Double>,
        profile: Profile
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