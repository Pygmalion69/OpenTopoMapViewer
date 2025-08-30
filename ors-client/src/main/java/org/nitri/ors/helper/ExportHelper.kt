package org.nitri.ors.helper

import org.nitri.ors.OrsClient
import org.nitri.ors.api.OpenRouteServiceApi
import org.nitri.ors.model.export.ExportRequest
import org.nitri.ors.model.export.ExportResponse
import org.nitri.ors.model.export.TopoJsonExportResponse

class ExportHelper() {

    suspend fun OrsClient.export(bbox: List<List<Double>>, geometry: Boolean? = null, profile: String): ExportResponse {
        val request = ExportRequest(
            bbox = bbox,
            id = "export_request",
            geometry = geometry
        )
        return export(profile, request)
    }

    suspend fun OrsClient.exportJson(bbox: List<List<Double>>, geometry: Boolean? = null, profile: String): ExportResponse {
        val request = ExportRequest(
            bbox = bbox,
            id = "export_request_json",
            geometry = geometry
        )
        return exportJson(profile, request)
    }

    suspend fun OrsClient.exportTopoJson(bbox: List<List<Double>>, geometry: Boolean? = null, profile: String): TopoJsonExportResponse {
        val request = ExportRequest(
            bbox = bbox,
            id = "export_request_topo_json",
            geometry = geometry
        )
        return exportTopoJson(profile, request)
    }
}
