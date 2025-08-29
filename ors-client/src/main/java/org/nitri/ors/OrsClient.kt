package org.nitri.ors

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

enum class Profile(val key: String) {
    DRIVING_CAR("driving-car"),
    DRIVING_HGV("driving-hgv"),
    CYCLING_REGULAR("cycling-regular"),
    CYCLING_ROAD("cycling-road"),
    CYCLING_MOUNTAIN("cycling-mountain"),
    CYCLING_ELECTRIC("cycling-electric"),
    FOOT_WALKING("foot-walking"),
    FOOT_HIKING("foot-hiking"),
    WHEELCHAIR("wheelchair")
}


interface OrsClient {

    // Directions
    suspend fun getRoute(profile: Profile, routeRequest: RouteRequest): RouteResponse
    suspend fun getRouteGpx(profile: Profile, routeRequest: RouteRequest): String
    suspend fun getRouteGeoJson(profile: String, routeRequest: RouteRequest): GeoJsonRouteResponse

    // Export
    suspend fun export(profile: String, exportRequest: ExportRequest): ExportResponse
    suspend fun exportJson(profile: String, exportRequest: ExportRequest): ExportResponse
    suspend fun exportTopoJson(profile: String, exportRequest: ExportRequest): TopoJsonExportResponse

    // Isochrones
    suspend fun getIsochrones(profile: String, isochronesRequest: IsochronesRequest): IsochronesResponse

    // Matrix
    suspend fun getMatrix(profile: String, matrixRequest: MatrixRequest): MatrixResponse

    // Snapping
    suspend fun getSnap(profile: String, snapRequest: SnapRequest): SnapResponse
    suspend fun getSnapJson(profile: String, snapRequest: SnapRequest): SnapResponse
    suspend fun getSnapGeoJson(profile: String, snapRequest: SnapRequest): SnapGeoJsonResponse

    // POIs
    suspend fun getPois(poisRequest: PoisRequest): PoisGeoJsonResponse

    // Optimization
    suspend fun getOptimization(optimizationRequest: OptimizationRequest): OptimizationResponse

    // Elevation
    suspend fun getElevationLine(elevationLineRequest: ElevationLineRequest): ElevationLineResponse
    suspend fun getElevationPoint(elevationPointRequest: ElevationPointRequest): ElevationPointResponse

    // Geocode
    suspend fun geocodeSearch(
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
    ): GeocodeSearchResponse

    suspend fun geocodeAutocomplete(
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
    ): GeocodeSearchResponse

    suspend fun geocodeStructured(
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
    ): GeocodeSearchResponse

    suspend fun geocodeReverse(
        apiKey: String,
        lon: Double,
        lat: Double,
        radiusKm: Double? = null,
        size: Int? = null,
        layers: List<String>? = null,
        sources: List<String>? = null,
        boundaryCountry: String? = null,
    ): GeocodeSearchResponse
}