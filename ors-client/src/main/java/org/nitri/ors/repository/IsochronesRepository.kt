package org.nitri.ors.repository

import org.nitri.ors.api.OpenRouteServiceApi
import org.nitri.ors.model.export.ExportRequest
import org.nitri.ors.model.export.ExportResponse
import org.nitri.ors.model.export.TopoJsonExportResponse
import org.nitri.ors.model.isochrones.IsochronesRequest
import org.nitri.ors.model.isochrones.IsochronesResponse
class IsochronesRepository(private val api: OpenRouteServiceApi) {

    suspend fun getIsochrones(
        locations: List<List<Double>>,
        range: List<Int>,
        profile: String,
        attributes: List<String>? = null,
        rangeType: String? = null,
    ): IsochronesResponse {
        val request = IsochronesRequest(
            locations, range, rangeType, attributes
        )
        return api.getIsochrones(profile, request)
    }

}
