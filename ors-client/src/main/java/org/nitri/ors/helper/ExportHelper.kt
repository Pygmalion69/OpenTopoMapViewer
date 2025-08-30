package org.nitri.ors.helper

import org.nitri.ors.OrsClient
import org.nitri.ors.Profile
import org.nitri.ors.domain.export.ExportRequest
import org.nitri.ors.domain.export.ExportResponse
import org.nitri.ors.domain.export.TopoJsonExportResponse

class ExportHelper() {

    suspend fun OrsClient.export(bbox: List<List<Double>>, geometry: Boolean? = null, profile: Profile): ExportResponse {
        val request = ExportRequest(
            bbox = bbox,
            id = "export_request",
            geometry = geometry
        )
        return export(profile, request)
    }

    suspend fun OrsClient.exportJson(bbox: List<List<Double>>, geometry: Boolean? = null, profile: Profile): ExportResponse {
        val request = ExportRequest(
            bbox = bbox,
            id = "export_request_json",
            geometry = geometry
        )
        return exportJson(profile, request)
    }

    suspend fun OrsClient.exportTopoJson(bbox: List<List<Double>>, geometry: Boolean? = null, profile: Profile): TopoJsonExportResponse {
        val request = ExportRequest(
            bbox = bbox,
            id = "export_request_topo_json",
            geometry = geometry
        )
        return exportTopoJson(profile, request)
    }
}
