package org.nitri.ors

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

/**
 * Supported routing profiles used across the OpenRouteService endpoints.
 *
 * The [key] corresponds to the profile identifier expected by the HTTP API.
 */
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

/** Longitude value wrapper used by the DSL builders. */
@JvmInline
value class Lon(val v: Double)

/** Latitude value wrapper used by the DSL builders. */
@JvmInline
value class Lat(val v: Double)

/** Convenience pair for longitude/latitude coordinates. */
data class LonLat(val lon: Double, val lat: Double)

/**
 * Abstraction over the OpenRouteService HTTP API.
 *
 * Implementations map these suspending functions to the respective REST
 * endpoints as documented in the
 * [OpenRouteService API reference](https://openrouteservice.org/dev/#/api-docs).
 */
interface OrsClient {

    // Directions
    /**
     * Requests a route using the Directions endpoint.
     * `GET /v2/directions/{profile}`
     */
    suspend fun getRoute(profile: Profile, routeRequest: RouteRequest): RouteResponse

    /** Retrieves the route as GPX. */
    suspend fun getRouteGpx(profile: Profile, routeRequest: RouteRequest): String

    /** Retrieves the route as GeoJSON feature collection. */
    suspend fun getRouteGeoJson(profile: Profile, routeRequest: RouteRequest): GeoJsonRouteResponse

    // Export
    /** Calls the export endpoint returning plain JSON. */
    suspend fun export(profile: Profile, exportRequest: ExportRequest): ExportResponse

    /** Same as [export] but explicitly requesting JSON output. */
    suspend fun exportJson(profile: Profile, exportRequest: ExportRequest): ExportResponse

    /** Requests TopoJSON output from the export endpoint. */
    suspend fun exportTopoJson(profile: Profile, exportRequest: ExportRequest): TopoJsonExportResponse

    // Isochrones
    /** Accesses the isochrones endpoint. */
    suspend fun getIsochrones(profile: Profile, isochronesRequest: IsochronesRequest): IsochronesResponse

    // Matrix
    /** Calls the matrix endpoint and returns distance/duration matrices. */
    suspend fun getMatrix(profile: Profile, matrixRequest: MatrixRequest): MatrixResponse

    // Snapping
    /** Snaps coordinates to the road network. */
    suspend fun getSnap(profile: Profile, snapRequest: SnapRequest): SnapResponse

    /** Snaps coordinates and returns the JSON variant of the response. */
    suspend fun getSnapJson(profile: Profile, snapRequest: SnapRequest): SnapResponse

    /** Snaps coordinates and returns a GeoJSON response. */
    suspend fun getSnapGeoJson(profile: Profile, snapRequest: SnapRequest): SnapGeoJsonResponse

    // POIs
    /** Queries points of interest and returns a GeoJSON feature collection. */
    suspend fun getPois(poisRequest: PoisRequest): PoisGeoJsonResponse

    // Optimization
    /** Delegates to the VROOM based optimization service. */
    suspend fun getOptimization(optimizationRequest: OptimizationRequest): OptimizationResponse

    // Elevation
    /** Calls the elevation/line endpoint. */
    suspend fun getElevationLine(elevationLineRequest: ElevationLineRequest): ElevationLineResponse

    /** Calls the elevation/point endpoint. */
    suspend fun getElevationPoint(elevationPointRequest: ElevationPointRequest): ElevationPointResponse

    // Geocode
    /** Forward geocoding search endpoint. */
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

    /** Autocomplete endpoint returning suggestions for a partial query. */
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

    /** Structured forward geocoding using discrete address fields. */
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

    /** Reverse geocoding for a single coordinate. */
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