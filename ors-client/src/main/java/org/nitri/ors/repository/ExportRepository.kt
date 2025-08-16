package org.nitri.ors.repository

import org.nitri.ors.api.OpenRouteServiceApi
import org.nitri.ors.model.export.ExportRequest
import org.nitri.ors.model.export.ExportResponse
import org.nitri.ors.model.export.TopoJsonExportResponse

class ExportRepository(private val api: OpenRouteServiceApi) {

    suspend fun export(boundingBox: List<Double>, profile: String, geometry: Boolean? = null): ExportResponse {
        val request = ExportRequest(
            boundingBox = boundingBox,
            profile = profile,
            geometry = geometry
        )
        return api.export(profile, request)
    }

    suspend fun export(profile: String, request: ExportRequest): ExportResponse {
        return api.export(profile, request)
    }

    suspend fun exportJson(profile: String, request: ExportRequest): ExportResponse {
        return api.exportJson(profile, request)
    }

    suspend fun exportTopoJson(profile: String, request: ExportRequest): TopoJsonExportResponse {
        return api.exportTopoJson(profile, request)
    }
}
