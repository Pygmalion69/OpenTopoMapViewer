package org.nitri.ors.helper

import org.nitri.ors.OrsClient
import org.nitri.ors.Profile
import org.nitri.ors.domain.isochrones.IsochronesRequest
import org.nitri.ors.domain.isochrones.IsochronesResponse

/** Helpers for requesting isochrones. */
class IsochronesHelper {

    /** Calls the isochrones endpoint with the given parameters. */
    suspend fun OrsClient.getIsochrones(
        locations: List<List<Double>>,
        range: List<Int>,
        profile: Profile,
        attributes: List<String>? = null,
        rangeType: String? = null,
    ): IsochronesResponse {
        val request = IsochronesRequest(
            locations, range, rangeType, attributes
        )
        return getIsochrones(profile, request)
    }
}
