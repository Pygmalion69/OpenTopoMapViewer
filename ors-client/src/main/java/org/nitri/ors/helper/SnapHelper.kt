package org.nitri.ors.helper

import org.nitri.ors.OrsClient
import org.nitri.ors.model.snap.SnapGeoJsonResponse
import org.nitri.ors.model.snap.SnapRequest
import org.nitri.ors.model.snap.SnapResponse

class SnapHelper(private val orsClient: OrsClient) {

    /**
     * Calls the ORS Snap endpoint for the given profile.
     */
    suspend fun OrsClient.getSnap(
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
        return getSnap(profile, request)
    }

    /**
     * Calls the ORS Snap JSON endpoint.
     */
    suspend fun OrsClient.getSnapJson(
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
        return getSnapJson(profile, request)
    }

    /**
     * Calls the ORS Snap GeoJSON endpoint.
     */
    suspend fun OrsClient.getSnapGeoJson(
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
        return getSnapGeoJson(profile, request)
    }
}
