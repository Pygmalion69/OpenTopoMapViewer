package org.nitri.ors.repository

import org.nitri.ors.api.OpenRouteServiceApi
import org.nitri.ors.model.geocode.GeocodeSearchResponse

/**
 * Repository for ORS Geocoding endpoints using GET requests only.
 *
 * Note: ORS Pelias geocoding requires an api_key query parameter even though
 * the client also sends an Authorization header. Therefore, methods here accept
 * apiKey explicitly and pass it through to the API interface.
 */
class GeocodeRepository(private val api: OpenRouteServiceApi) {

    /**
     * Forward geocoding search.
     */
    suspend fun search(
        text: String,
        apiKey: String,
        focusLon: Double? = null,
        focusLat: Double? = null,
        rectMinLon: Double? = null,
        rectMinLat: Double? = null,
        rectMaxLon: Double? = null,
        rectMaxLat: Double? = null,
        circleLon: Double? = null,
        circleLat: Double? = null,
        circleRadiusMeters: Double? = null,
        boundaryGid: String? = null,
        boundaryCountry: String? = null,
        sourcesCsv: String? = null,
        layersCsv: String? = null,
        size: Int? = 10,
    ): GeocodeSearchResponse {
        return api.geocodeSearch(
            text = text,
            focusLon = focusLon,
            focusLat = focusLat,
            rectMinLon = rectMinLon,
            rectMinLat = rectMinLat,
            rectMaxLon = rectMaxLon,
            rectMaxLat = rectMaxLat,
            circleLon = circleLon,
            circleLat = circleLat,
            circleRadiusMeters = circleRadiusMeters,
            boundaryGid = boundaryGid,
            boundaryCountry = boundaryCountry,
            sourcesCsv = sourcesCsv,
            layersCsv = layersCsv,
            size = size,
            apiKey = apiKey
        )
    }

    /**
     * Autocomplete search; returns suggestions for a partial query.
     */
    suspend fun autocomplete(
        apiKey: String,
        text: String,
        focusLon: Double? = null,
        focusLat: Double? = null,
        rectMinLon: Double? = null,
        rectMinLat: Double? = null,
        rectMaxLon: Double? = null,
        rectMaxLat: Double? = null,
        circleLon: Double? = null,
        circleLat: Double? = null,
        circleRadius: Double? = null,
        country: String? = null,
        sources: List<String>? = null,
        layers: List<String>? = null,
        size: Int? = null,
    ): GeocodeSearchResponse {
        return api.autocomplete(
            apiKey = apiKey,
            text = text,
            focusLon = focusLon,
            focusLat = focusLat,
            rectMinLon = rectMinLon,
            rectMinLat = rectMinLat,
            rectMaxLon = rectMaxLon,
            rectMaxLat = rectMaxLat,
            circleLon = circleLon,
            circleLat = circleLat,
            circleRadius = circleRadius,
            country = country,
            sources = sources,
            layers = layers,
            size = size
        )
    }

    /**
     * Structured forward geocoding using address fields.
     */
    suspend fun structured(
        apiKey: String,
        address: String? = null,
        neighbourhood: String? = null,
        borough: String? = null,
        locality: String? = null,
        county: String? = null,
        region: String? = null,
        country: String? = null,
        postalcode: String? = null,
        focusLon: Double? = null,
        focusLat: Double? = null,
        rectMinLon: Double? = null,
        rectMinLat: Double? = null,
        rectMaxLon: Double? = null,
        rectMaxLat: Double? = null,
        circleLon: Double? = null,
        circleLat: Double? = null,
        circleRadiusMeters: Double? = null,
        boundaryCountry: String? = null,
        layers: List<String>? = null,
        sources: List<String>? = null,
        size: Int? = null,
    ): GeocodeSearchResponse {
        return api.geocodeStructured(
            apiKey = apiKey,
            address = address,
            neighbourhood = neighbourhood,
            borough = borough,
            locality = locality,
            county = county,
            region = region,
            country = country,
            postalcode = postalcode,
            focusLon = focusLon,
            focusLat = focusLat,
            rectMinLon = rectMinLon,
            rectMinLat = rectMinLat,
            rectMaxLon = rectMaxLon,
            rectMaxLat = rectMaxLat,
            circleLon = circleLon,
            circleLat = circleLat,
            circleRadiusMeters = circleRadiusMeters,
            boundaryCountry = boundaryCountry,
            layers = layers,
            sources = sources,
            size = size
        )
    }

    /**
     * Reverse geocoding for a point.
     */
    suspend fun reverse(
        apiKey: String,
        lon: Double,
        lat: Double,
        radiusKm: Double? = null,
        size: Int? = null,
        layers: List<String>? = null,
        sources: List<String>? = null,
        boundaryCountry: String? = null,
    ): GeocodeSearchResponse {
        return api.geocodeReverse(
            apiKey = apiKey,
            lon = lon,
            lat = lat,
            radiusKm = radiusKm,
            size = size,
            layers = layers,
            sources = sources,
            boundaryCountry = boundaryCountry
        )
    }
}
