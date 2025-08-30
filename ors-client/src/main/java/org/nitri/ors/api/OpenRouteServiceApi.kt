package org.nitri.ors.api

import okhttp3.ResponseBody
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
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenRouteServiceApi {

    // Directions
    @GET("v2/directions/{profile}")
    suspend fun getRouteSimple(
        @Path("profile") profile: String,
        @Query("start") start: String,  // e.g. "8.681495,49.41461"
        @Query("end") end: String       // e.g. "8.687872,49.420318"
    ): RouteResponse

    @POST("v2/directions/{profile}")
    suspend fun getRoute(
        @Path("profile") profile: String,
        @Body request: RouteRequest
    ): RouteResponse

    @POST("v2/directions/{profile}/json")
    suspend fun getRouteJson(
        @Path("profile") profile: String,
        @Body request: RouteRequest
    ): RouteResponse

    @POST("v2/directions/{profile}/gpx")
    suspend fun getRouteGpx(
        @Path("profile") profile: String,
        @Body request: RouteRequest
    ): Response<ResponseBody>

    @POST("v2/directions/{profile}/geojson")
    suspend fun getRouteGeoJson(
        @Path("profile") profile: String,
        @Body request: RouteRequest
    ): GeoJsonRouteResponse

    // Export endpoints
    @POST("v2/export/{profile}")
    suspend fun export(
        @Path("profile") profile: String,
        @Body request: ExportRequest
    ): ExportResponse

    @POST("v2/export/{profile}/json")
    suspend fun exportJson(
        @Path("profile") profile: String,
        @Body request: ExportRequest
    ): ExportResponse

    @POST("v2/export/{profile}/topojson")
    suspend fun exportTopoJson(
        @Path("profile") profile: String,
        @Body request: ExportRequest
    ): TopoJsonExportResponse

    // Isochrones endpoint
    @POST("v2/isochrones/{profile}")
    suspend fun getIsochrones(
        @Path("profile") profile: String,
        @Body request: IsochronesRequest
    ): IsochronesResponse

    // Matrix endpoint
    @POST("v2/matrix/{profile}")
    suspend fun getMatrix(
        @Path("profile") profile: String,
        @Body request: MatrixRequest
    ): MatrixResponse

    // Snapping

    @POST("v2/snap/{profile}")
    suspend fun getSnap(
        @Path("profile") profile: String,
        @Body request: SnapRequest
    ): SnapResponse

    @POST("v2/snap/{profile}/json")
    suspend fun getSnapJson(
        @Path("profile") profile: String,
        @Body request: SnapRequest
    ): SnapResponse

    @POST("v2/snap/{profile}/geojson")
    suspend fun getSnapGeoJson(
        @Path("profile") profile: String,
        @Body request: SnapRequest
    ): SnapGeoJsonResponse

    // POIs

    @POST("pois")
    suspend fun getPois(
        @Body request: PoisRequest
    ): PoisGeoJsonResponse

    // Optimization

    @POST("optimization")
    suspend fun getOptimization(
        @Body request: OptimizationRequest
    ): OptimizationResponse

    // Elevation

    @POST("elevation/line")
    suspend fun getElevationLine(
        @Body request: ElevationLineRequest
    ): ElevationLineResponse

    @GET("elevation/point")
    suspend fun getElevationPointSimple(
        @Query("geometry") start: String,  // e.g. "8.681495,49.41461"
    ): RouteResponse

    @POST("elevation/point")
    suspend fun getElevationPoint(
        @Body request: ElevationPointRequest
    ): ElevationPointResponse

    // Geocode

    @GET("geocode/search")
    suspend fun geocodeSearch(
        @Query("text") text: String,

        // Optional focus point
        @Query("focus.point.lon") focusLon: Double? = null,
        @Query("focus.point.lat") focusLat: Double? = null,

        // Optional rectangular boundary
        @Query("boundary.rect.min_lon") rectMinLon: Double? = null,
        @Query("boundary.rect.min_lat") rectMinLat: Double? = null,
        @Query("boundary.rect.max_lon") rectMaxLon: Double? = null,
        @Query("boundary.rect.max_lat") rectMaxLat: Double? = null,

        // Optional circular boundary
        @Query("boundary.circle.lon") circleLon: Double? = null,
        @Query("boundary.circle.lat") circleLat: Double? = null,
        @Query("boundary.circle.radius") circleRadiusMeters: Double? = null,

        // Other optional filters
        @Query("boundary.gid") boundaryGid: String? = null,
        // Pass comma-separated if multiple, e.g. "DE,AT"
        @Query("boundary.country") boundaryCountry: String? = null,
        // Pelias expects comma-separated values; join your list before passing.
        @Query("sources") sourcesCsv: String? = null,     // e.g. "osm,oa,gn,wof"
        @Query("layers") layersCsv: String? = null,       // e.g. "region,country,locality,address"
        @Query("size") size: Int? = 10,

        // Geocoder uses api_key as query
        @Query("api_key") apiKey: String
    ): GeocodeSearchResponse

    @GET("geocode/autocomplete")
    suspend fun autocomplete(
        @Query("api_key") apiKey: String,
        @Query("text") text: String,
        @Query("focus.point.lon") focusLon: Double? = null,
        @Query("focus.point.lat") focusLat: Double? = null,
        @Query("boundary.rect.min_lon") rectMinLon: Double? = null,
        @Query("boundary.rect.min_lat") rectMinLat: Double? = null,
        @Query("boundary.rect.max_lon") rectMaxLon: Double? = null,
        @Query("boundary.rect.max_lat") rectMaxLat: Double? = null,
        @Query("boundary.circle.lon") circleLon: Double? = null,
        @Query("boundary.circle.lat") circleLat: Double? = null,
        @Query("boundary.circle.radius") circleRadius: Double? = null,
        @Query("boundary.country") country: String? = null,
        @Query("sources") sources: List<String>? = null,
        @Query("layers") layers: List<String>? = null,
        @Query("size") size: Int? = null
    ): GeocodeSearchResponse


    @GET("geocode/search/structured")
    suspend fun geocodeStructured(
        @Query("api_key") apiKey: String,

        // Structured query parts (all optional; send only what you have)
        @Query("address") address: String? = null,
        @Query("neighbourhood") neighbourhood: String? = null,
        @Query("borough") borough: String? = null,
        @Query("locality") locality: String? = null,      // e.g., city
        @Query("county") county: String? = null,
        @Query("region") region: String? = null,          // e.g., state/province
        @Query("country") country: String? = null,        // ISO code or name
        @Query("postalcode") postalcode: String? = null,

        // Focus point (used for ranking)
        @Query("focus.point.lon") focusLon: Double? = null,
        @Query("focus.point.lat") focusLat: Double? = null,

        // Bounding rectangle (optional)
        @Query("boundary.rect.min_lon") rectMinLon: Double? = null,
        @Query("boundary.rect.min_lat") rectMinLat: Double? = null,
        @Query("boundary.rect.max_lon") rectMaxLon: Double? = null,
        @Query("boundary.rect.max_lat") rectMaxLat: Double? = null,

        // Bounding circle (optional)
        @Query("boundary.circle.lon") circleLon: Double? = null,
        @Query("boundary.circle.lat") circleLat: Double? = null,
        @Query("boundary.circle.radius") circleRadiusMeters: Double? = null,

        // Limit results to a specific country (ISO code)
        @Query("boundary.country") boundaryCountry: String? = null,

        // Filters
        @Query("layers") layers: List<String>? = null,    // e.g. ["address","venue","street","locality","region","country"]
        @Query("sources") sources: List<String>? = null,  // e.g. ["osm","oa","gn","wof"]

        // Number of results (default 10)
        @Query("size") size: Int? = null
    ): GeocodeSearchResponse


    @GET("geocode/reverse")
    suspend fun geocodeReverse(
        @Query("api_key") apiKey: String,

        // required
        @Query("point.lon") lon: Double,
        @Query("point.lat") lat: Double,

        // optional ranking/filters
        @Query("boundary.circle.radius") radiusKm: Double? = null, // Pelias expects km
        @Query("size") size: Int? = null,                          // default 10
        @Query("layers") layers: List<String>? = null,             // e.g. ["address","venue"]
        @Query("sources") sources: List<String>? = null,           // e.g. ["osm","oa","gn","wof"]
        @Query("boundary.country") boundaryCountry: String? = null // ISO code, e.g. "FR"
    ): GeocodeSearchResponse
}
