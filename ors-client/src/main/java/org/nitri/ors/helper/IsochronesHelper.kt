package org.nitri.ors.helper

import org.nitri.ors.OrsClient
import org.nitri.ors.model.isochrones.IsochronesRequest
import org.nitri.ors.model.isochrones.IsochronesResponse

class IsochronesHelper() {

    suspend fun OrsClient.getIsochrones(
        locations: List<List<Double>>,
        range: List<Int>,
        profile: String,
        attributes: List<String>? = null,
        rangeType: String? = null,
    ): IsochronesResponse {
        val request = IsochronesRequest(
            locations, range, rangeType, attributes
        )
        return getIsochrones(profile, request)
    }
}
