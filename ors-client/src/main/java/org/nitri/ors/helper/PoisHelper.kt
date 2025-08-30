package org.nitri.ors.helper

import org.nitri.ors.OrsClient
import org.nitri.ors.domain.pois.GeoJsonGeometry
import org.nitri.ors.domain.pois.Geometry
import org.nitri.ors.domain.pois.PoisGeoJsonResponse
import org.nitri.ors.domain.pois.PoisRequest

/** Convenience extensions for the POIs endpoints. */
class PoisHelper {

    /**
    * Query POIs within a bounding box.
    *
    * @param bbox [[minLon,minLat],[maxLon,maxLat]]
    * @param filters Optional filters map as supported by ORS POIs
    * @param limit Optional limit for number of features returned
    * @param sortby Optional sort field
    * @param buffer Optional buffer in meters applied to the geometry
    */
    suspend fun OrsClient.getPoisByBbox(
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
        return getPois(request)
    }

    /**
    * Query POIs around a point with a buffer radius.
    *
    * @param point [lon, lat]
    * @param buffer Buffer radius in meters
    */
    suspend fun OrsClient.getPoisByPoint(
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
        return getPois(request)
    }
}
