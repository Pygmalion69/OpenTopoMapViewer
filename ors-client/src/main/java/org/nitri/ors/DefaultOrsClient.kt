package org.nitri.ors

import android.content.Context
import org.nitri.ors.domain.elevation.ElevationLineRequest
import org.nitri.ors.domain.elevation.ElevationLineResponse
import org.nitri.ors.domain.elevation.ElevationPointRequest
import org.nitri.ors.domain.elevation.ElevationPointResponse
import org.nitri.ors.domain.export.ExportRequest
import org.nitri.ors.domain.export.ExportResponse
import org.nitri.ors.domain.export.TopoJsonExportResponse
import org.nitri.ors.domain.geocode.GeocodeSearchResponse
import org.nitri.ors.domain.isochrones.IsochronesRequest
import org.nitri.ors.domain.isochrones.IsochronesResponse
import org.nitri.ors.domain.matrix.MatrixRequest
import org.nitri.ors.domain.matrix.MatrixResponse
import org.nitri.ors.domain.optimization.OptimizationRequest
import org.nitri.ors.domain.optimization.OptimizationResponse
import org.nitri.ors.domain.pois.PoisGeoJsonResponse
import org.nitri.ors.domain.pois.PoisRequest
import org.nitri.ors.domain.route.GeoJsonRouteResponse
import org.nitri.ors.domain.route.RouteRequest
import org.nitri.ors.domain.route.RouteResponse
import org.nitri.ors.domain.snap.SnapGeoJsonResponse
import org.nitri.ors.domain.snap.SnapRequest
import org.nitri.ors.domain.snap.SnapResponse
import org.nitri.ors.restclient.OpenRouteServiceRestClient

/**
 * Default implementation of [OrsClient] using the Retrofit based
 * [OpenRouteServiceRestClient].
 */
class DefaultOrsClient(apiKey: String, context: Context) : OrsClient {
    private val api = OpenRouteServiceRestClient.create(apiKey, context)

    /** @inheritDoc */
    override suspend fun getRoute(
        profile: Profile,
        routeRequest: RouteRequest
    ): RouteResponse {
        return api.getRoute(profile.key, routeRequest)
    }

    /** @inheritDoc */
    override suspend fun getRouteGpx(
        profile: Profile,
        routeRequest: RouteRequest
    ): String {
        return api.getRouteGpx(profile.key, routeRequest).body()?.string() ?: ""
    }

    /** @inheritDoc */
    override suspend fun getRouteGeoJson(
        profile: Profile,
        routeRequest: RouteRequest
    ): GeoJsonRouteResponse {
        return api.getRouteGeoJson(profile.key, routeRequest)
    }

    /** @inheritDoc */
    override suspend fun export(
        profile: Profile,
        exportRequest: ExportRequest
    ): ExportResponse {
        return api.export(profile.key, exportRequest)
    }

    /** @inheritDoc */
    override suspend fun exportJson(
        profile: Profile,
        exportRequest: ExportRequest
    ): ExportResponse {
        return api.exportJson(profile.key, exportRequest)
    }

    /** @inheritDoc */
    override suspend fun exportTopoJson(
        profile: Profile,
        exportRequest: ExportRequest
    ): TopoJsonExportResponse {
        return api.exportTopoJson(profile.key, exportRequest)
    }

    /** @inheritDoc */
    override suspend fun getIsochrones(
        profile: Profile,
        isochronesRequest: IsochronesRequest
    ): IsochronesResponse {
        return api.getIsochrones(profile.key, isochronesRequest)
    }

    /** @inheritDoc */
    override suspend fun getMatrix(
        profile: Profile,
        matrixRequest: MatrixRequest
    ): MatrixResponse {
        return api.getMatrix(profile.key, matrixRequest)
    }

    /** @inheritDoc */
    override suspend fun getSnap(
        profile: Profile,
        snapRequest: SnapRequest
    ): SnapResponse {
        return api.getSnap(profile.key, snapRequest)
    }

    /** @inheritDoc */
    override suspend fun getSnapJson(
        profile: Profile,
        snapRequest: SnapRequest
    ): SnapResponse {
        return api.getSnapJson(profile.key, snapRequest)
    }

    /** @inheritDoc */
    override suspend fun getSnapGeoJson(
        profile: Profile,
        snapRequest: SnapRequest
    ): SnapGeoJsonResponse {
        return api.getSnapGeoJson(profile.key, snapRequest)
    }

    /** @inheritDoc */
    override suspend fun getPois(poisRequest: PoisRequest): PoisGeoJsonResponse {
        val raw = api.getPois(poisRequest)
        fun PoisGeoJsonResponse.sanitized(): PoisGeoJsonResponse =
            copy(bbox = bbox?.takeIf { it.size == 4 && it.all(Double::isFinite) })
        return raw.sanitized()
    }

    /** @inheritDoc */
    override suspend fun getOptimization(optimizationRequest: OptimizationRequest): OptimizationResponse {
        return api.getOptimization(optimizationRequest)
    }

    /** @inheritDoc */
    override suspend fun getElevationLine(
        elevationLineRequest: ElevationLineRequest
    ): ElevationLineResponse {
        return api.getElevationLine(elevationLineRequest)
    }

    /** @inheritDoc */
    override suspend fun getElevationPoint(
        elevationPointRequest: ElevationPointRequest
    ): ElevationPointResponse {
        return api.getElevationPoint(elevationPointRequest)
    }

    /** @inheritDoc */
    override suspend fun geocodeSearch(
        text: String,
        apiKey: String,
        focusLon: Double?,
        focusLat: Double?,
        rectMinLon: Double?,
        rectMinLat: Double?,
        rectMaxLon: Double?,
        rectMaxLat: Double?,
        circleLon: Double?,
        circleLat: Double?,
        circleRadiusMeters: Double?,
        boundaryGid: String?,
        boundaryCountry: String?,
        sourcesCsv: String?,
        layersCsv: String?,
        size: Int?
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

    /** @inheritDoc */
    override suspend fun geocodeAutocomplete(
        apiKey: String,
        text: String,
        focusLon: Double?,
        focusLat: Double?,
        rectMinLon: Double?,
        rectMinLat: Double?,
        rectMaxLon: Double?,
        rectMaxLat: Double?,
        circleLon: Double?,
        circleLat: Double?,
        circleRadius: Double?,
        country: String?,
        sources: List<String>?,
        layers: List<String>?,
        size: Int?
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

    /** @inheritDoc */
    override suspend fun geocodeStructured(
        apiKey: String,
        address: String?,
        neighbourhood: String?,
        borough: String?,
        locality: String?,
        county: String?,
        region: String?,
        country: String?,
        postalcode: String?,
        focusLon: Double?,
        focusLat: Double?,
        rectMinLon: Double?,
        rectMinLat: Double?,
        rectMaxLon: Double?,
        rectMaxLat: Double?,
        circleLon: Double?,
        circleLat: Double?,
        circleRadiusMeters: Double?,
        boundaryCountry: String?,
        layers: List<String>?,
        sources: List<String>?,
        size: Int?
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

    /** @inheritDoc */
    override suspend fun geocodeReverse(
        apiKey: String,
        lon: Double,
        lat: Double,
        radiusKm: Double?,
        size: Int?,
        layers: List<String>?,
        sources: List<String>?,
        boundaryCountry: String?
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
