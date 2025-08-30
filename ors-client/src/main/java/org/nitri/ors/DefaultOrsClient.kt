package org.nitri.ors

import android.content.Context
import org.nitri.ors.model.elevation.ElevationLineRequest
import org.nitri.ors.model.elevation.ElevationLineResponse
import org.nitri.ors.model.elevation.ElevationPointRequest
import org.nitri.ors.model.elevation.ElevationPointResponse
import org.nitri.ors.model.export.ExportRequest
import org.nitri.ors.model.export.ExportResponse
import org.nitri.ors.model.export.TopoJsonExportResponse
import org.nitri.ors.model.geocode.GeocodeSearchResponse
import org.nitri.ors.model.isochrones.IsochronesRequest
import org.nitri.ors.model.isochrones.IsochronesResponse
import org.nitri.ors.model.matrix.MatrixRequest
import org.nitri.ors.model.matrix.MatrixResponse
import org.nitri.ors.model.optimization.OptimizationRequest
import org.nitri.ors.model.optimization.OptimizationResponse
import org.nitri.ors.model.pois.PoisGeoJsonResponse
import org.nitri.ors.model.pois.PoisRequest
import org.nitri.ors.model.route.GeoJsonRouteResponse
import org.nitri.ors.model.route.RouteRequest
import org.nitri.ors.model.route.RouteResponse
import org.nitri.ors.model.snap.SnapGeoJsonResponse
import org.nitri.ors.model.snap.SnapRequest
import org.nitri.ors.model.snap.SnapResponse
import org.nitri.ors.restclient.OpenRouteServiceRestClient

class DefaultOrsClient(apiKey: String, context: Context) : OrsClient {
    private val api = OpenRouteServiceRestClient.create(apiKey, context)

    override suspend fun getRoute(
        profile: Profile,
        routeRequest: RouteRequest
    ): RouteResponse {
        return api.getRoute(profile.key, routeRequest)
    }

    override suspend fun getRouteGpx(
        profile: Profile,
        routeRequest: RouteRequest
    ): String {
        return api.getRouteGpx(profile.key, routeRequest).body()?.string() ?: ""
    }

    override suspend fun getRouteGeoJson(
        profile: String,
        routeRequest: RouteRequest
    ): GeoJsonRouteResponse {
        return api.getRouteGeoJson(profile, routeRequest)
    }

    override suspend fun export(
        profile: String,
        exportRequest: ExportRequest
    ): ExportResponse {
        return api.export(profile, exportRequest)
    }

    override suspend fun exportJson(
        profile: String,
        exportRequest: ExportRequest
    ): ExportResponse {
        return api.exportJson(profile, exportRequest)
    }

    override suspend fun exportTopoJson(
        profile: String,
        exportRequest: ExportRequest
    ): TopoJsonExportResponse {
        return api.exportTopoJson(profile, exportRequest)
    }

    override suspend fun getIsochrones(
        profile: String,
        isochronesRequest: IsochronesRequest
    ): IsochronesResponse {
        return api.getIsochrones(profile, isochronesRequest)
    }

    override suspend fun getMatrix(
        profile: String,
        matrixRequest: MatrixRequest
    ): MatrixResponse {
        return api.getMatrix(profile, matrixRequest)
    }

    override suspend fun getSnap(
        profile: String,
        snapRequest: SnapRequest
    ): SnapResponse {
        return api.getSnap(profile, snapRequest)
    }

    override suspend fun getSnapJson(
        profile: String,
        snapRequest: SnapRequest
    ): SnapResponse {
        return api.getSnapJson(profile, snapRequest)
    }

    override suspend fun getSnapGeoJson(
        profile: String,
        snapRequest: SnapRequest
    ): SnapGeoJsonResponse {
        return api.getSnapGeoJson(profile, snapRequest)
    }

    override suspend fun getPois(poisRequest: PoisRequest): PoisGeoJsonResponse {
        val raw = api.getPois(poisRequest)
        fun PoisGeoJsonResponse.sanitized(): PoisGeoJsonResponse =
            copy(bbox = bbox?.takeIf { it.size == 4 && it.all(Double::isFinite) })
        return raw.sanitized()
    }

    override suspend fun getOptimization(optimizationRequest: OptimizationRequest): OptimizationResponse {
        return api.getOptimization(optimizationRequest)
    }

    override suspend fun getElevationLine(
        elevationLineRequest: ElevationLineRequest
    ): ElevationLineResponse {
        return api.getElevationLine(elevationLineRequest)
    }

    override suspend fun getElevationPoint(
        elevationPointRequest: ElevationPointRequest
    ): ElevationPointResponse {
        return api.getElevationPoint(elevationPointRequest)
    }

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
