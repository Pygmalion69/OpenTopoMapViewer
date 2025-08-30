package org.nitri.ors.helper

import org.nitri.ors.OrsClient
import org.nitri.ors.Profile
import org.nitri.ors.domain.snap.SnapGeoJsonResponse
import org.nitri.ors.domain.snap.SnapRequest
import org.nitri.ors.domain.snap.SnapResponse

/** Helpers for the snap endpoints. */
class SnapHelper {

    /** Calls the ORS Snap endpoint for the given [profile]. */
    suspend fun OrsClient.getSnap(
        locations: List<List<Double>>,
        radius: Int,
        profile: Profile,
        id: String? = null,
    ): SnapResponse {
        val request = SnapRequest(
            locations = locations,
            radius = radius,
            id = id
        )
        return getSnap(profile, request)
    }

    /** Calls the ORS Snap JSON endpoint. */
    suspend fun OrsClient.getSnapJson(
        locations: List<List<Double>>,
        radius: Int,
        profile: Profile,
        id: String? = null,
    ): SnapResponse {
        val request = SnapRequest(
            locations = locations,
            radius = radius,
            id = id
        )
        return getSnapJson(profile, request)
    }

    /** Calls the ORS Snap GeoJSON endpoint. */
    suspend fun OrsClient.getSnapGeoJson(
        locations: List<List<Double>>,
        radius: Int,
        profile: Profile,
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
