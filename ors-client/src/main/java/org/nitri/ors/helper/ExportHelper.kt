package org.nitri.ors.helper

import org.nitri.ors.OrsClient
import org.nitri.ors.Profile
import org.nitri.ors.domain.export.ExportRequest
import org.nitri.ors.domain.export.ExportResponse
import org.nitri.ors.domain.export.TopoJsonExportResponse

/** Helpers for the export endpoints. */
class ExportHelper {

    /** Requests the export endpoint with bounding box [bbox]. */
    suspend fun OrsClient.export(bbox: List<List<Double>>, geometry: Boolean? = null, profile: Profile): ExportResponse {
        val request = ExportRequest(
            bbox = bbox,
            id = "export_request",
            geometry = geometry
        )
        return export(profile, request)
    }

    /** Requests the export endpoint asking explicitly for JSON output. */
    suspend fun OrsClient.exportJson(bbox: List<List<Double>>, geometry: Boolean? = null, profile: Profile): ExportResponse {
        val request = ExportRequest(
            bbox = bbox,
            id = "export_request_json",
            geometry = geometry
        )
        return exportJson(profile, request)
    }

    /** Requests the export endpoint with TopoJSON output. */
    suspend fun OrsClient.exportTopoJson(bbox: List<List<Double>>, geometry: Boolean? = null, profile: Profile): TopoJsonExportResponse {
        val request = ExportRequest(
            bbox = bbox,
            id = "export_request_topo_json",
            geometry = geometry
        )
        return exportTopoJson(profile, request)
    }
}
