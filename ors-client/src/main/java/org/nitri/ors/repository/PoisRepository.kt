package org.nitri.ors.repository

import org.nitri.ors.api.OpenRouteServiceApi
import org.nitri.ors.model.pois.GeoJsonGeometry
import org.nitri.ors.model.pois.Geometry
import org.nitri.ors.model.pois.PoisGeoJsonResponse
import org.nitri.ors.model.pois.PoisRequest

class PoisRepository(private val api: OpenRouteServiceApi) {

    /**
    * Query POIs within a bounding box.
    *
    * @param bbox [[minLon,minLat],[maxLon,maxLat]]
    * @param filters Optional filters map as supported by ORS POIs
    * @param limit Optional limit for number of features returned
    * @param sortby Optional sort field
    * @param buffer Optional buffer in meters applied to the geometry
    */
    suspend fun getPoisByBbox(
        bbox: List<List<Double>>,
        filters: Map<String, String>? = null,
        limit: Int? = null,
        sortby: String? = null,
        buffer: Int? = null
    ): PoisGeoJsonResponse {
        val request = PoisRequest(
            geometry = Geometry(bbox = bbox, buffer = buffer),
            filters = filters,
            limit = limit,
            sortby = sortby
        )
        return api.getPois(request)
    }

    /**
    * Query POIs around a point with a buffer radius.
    *
    * @param point [lon, lat]
    * @param buffer Buffer radius in meters
    */
    suspend fun getPoisByPoint(
        point: List<Double>,
        buffer: Int,
        filters: Map<String, String>? = null,
        limit: Int? = null,
        sortby: String? = null
    ): PoisGeoJsonResponse {
        val request = PoisRequest(
            geometry = Geometry(
                geojson = GeoJsonGeometry(type = "Point", coordinates = point),
                buffer = buffer
            ),
            filters = filters,
            limit = limit,
            sortby = sortby
        )
        return api.getPois(request)
    }
}
