package org.nitri.ors.repository

import org.nitri.ors.api.OpenRouteServiceApi
import org.nitri.ors.model.snap.SnapGeoJsonResponse
import org.nitri.ors.model.snap.SnapRequest
import org.nitri.ors.model.snap.SnapResponse

class SnapRepository(private val api: OpenRouteServiceApi) {

    /**
     * Calls the ORS Snap endpoint for the given profile.
     *
     * @param locations List of [lon, lat] coordinates to snap.
     * @param radius Maximum radius (meters) around given coordinates to search for graph edges.
     * @param profile ORS profile, e.g. "driving-car", "foot-hiking", etc.
     * @param id Optional arbitrary request id echoed back by the service.
     */
    suspend fun getSnap(
        locations: List<List<Double>>,
        radius: Int,
        profile: String,
        id: String? = null,
    ): SnapResponse {
        val request = SnapRequest(
            locations = locations,
            radius = radius,
            id = id
        )
        return api.getSnap(profile, request)
    }

    /**
     * Calls the ORS Snap JSON endpoint.
     */
    suspend fun getSnapJson(
        locations: List<List<Double>>,
        radius: Int,
        profile: String,
        id: String? = null,
    ): SnapResponse {
        val request = SnapRequest(
            locations = locations,
            radius = radius,
            id = id
        )
        return api.getSnapJson(profile, request)
    }

    /**
     * Calls the ORS Snap GeoJSON endpoint.
     */
    suspend fun getSnapGeoJson(
        locations: List<List<Double>>,
        radius: Int,
        profile: String,
        id: String? = null,
    ): SnapGeoJsonResponse {
        val request = SnapRequest(
            locations = locations,
            radius = radius,
            id = id
        )
        return api.getSnapGeoJson(profile, request)
    }
}
