package org.nitri.ors.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.nitri.ors.model.export.ExportRequest
import org.nitri.ors.model.export.ExportResponse
import org.nitri.ors.model.export.TopoJsonExportResponse
import org.nitri.ors.model.route.GeoJsonRouteResponse
import org.nitri.ors.model.route.GpxResponse
import org.nitri.ors.model.route.RouteRequest
import org.nitri.ors.model.route.RouteResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenRouteServiceApi {

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

    
}
