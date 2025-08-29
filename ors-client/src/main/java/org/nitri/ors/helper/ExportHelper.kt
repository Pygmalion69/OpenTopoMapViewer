package org.nitri.ors.helper

import org.nitri.ors.api.OpenRouteServiceApi
import org.nitri.ors.model.export.ExportRequest
import org.nitri.ors.model.export.ExportResponse
import org.nitri.ors.model.export.TopoJsonExportResponse

class ExportHelper(private val api: OpenRouteServiceApi) {

    suspend fun export(bbox: List<List<Double>>, geometry: Boolean? = null, profile: String): ExportResponse {
        val request = ExportRequest(
            bbox = bbox,
            id = "export_request",
            geometry = geometry
        )
        return api.export(profile, request)
    }

    suspend fun exportJson(bbox: List<List<Double>>, geometry: Boolean? = null, profile: String): ExportResponse {
        val request = ExportRequest(
            bbox = bbox,
            id = "export_request_json",
            geometry = geometry
        )
        return api.exportJson(profile, request)
    }

    suspend fun exportTopoJson(bbox: List<List<Double>>, geometry: Boolean? = null, profile: String): TopoJsonExportResponse {
        val request = ExportRequest(
            bbox = bbox,
            id = "export_request_topo_json",
            geometry = geometry
        )
        return api.exportTopoJson(profile, request)
    }
}
