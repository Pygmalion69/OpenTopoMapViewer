package org.nitri.ors.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.nitri.ors.api.OpenRouteServiceApi
import org.nitri.ors.model.elevation.ElevationLineRequest
import org.nitri.ors.model.elevation.ElevationLineResponse
import org.nitri.ors.model.elevation.ElevationPointRequest
import org.nitri.ors.model.elevation.ElevationPointResponse

class ElevationRepository(private val api: OpenRouteServiceApi) {

    /**
     * Calls the ORS elevation/line POST endpoint with a prepared request.
     */
    suspend fun getElevationLine(request: ElevationLineRequest): ElevationLineResponse =
        api.getElevationLine(request)

    /**
     * Convenience helper to request elevation for a LineString provided as list of [lon, lat] pairs.
     * Builds a GeoJSON LineString payload and requests GeoJSON output.
     */
    suspend fun getElevationLine(
        coordinates: List<List<Double>>, // [[lon,lat], [lon,lat], ...]
        dataset: String? = null,
        formatOut: String = "geojson",
    ): ElevationLineResponse {
        val geometry: JsonElement = JsonObject(
            mapOf(
                "type" to JsonPrimitive("LineString"),
                "coordinates" to JsonArray(
                    coordinates.map { pair ->
                        JsonArray(pair.map { JsonPrimitive(it) })
                    }
                )
            )
        )
        val request = ElevationLineRequest(
            formatIn = "geojson",
            formatOut = formatOut,
            geometry = geometry,
            dataset = dataset
        )
        return api.getElevationLine(request)
    }

    /**
     * Calls the ORS elevation/point POST endpoint for a single coordinate.
     *
     * @param lon longitude
     * @param lat latitude
     * @param formatOut either "geojson" or "point"
     * @param dataset optional dataset (e.g., "srtm")
     */
    suspend fun getElevationPoint(
        lon: Double,
        lat: Double,
        formatOut: String = "geojson",
        dataset: String? = null,
    ): ElevationPointResponse {
        val request = ElevationPointRequest(
            formatIn = "point",
            formatOut = formatOut,
            dataset = dataset,
            geometry = listOf(lon, lat)
        )
        return api.getElevationPoint(request)
    }
}
