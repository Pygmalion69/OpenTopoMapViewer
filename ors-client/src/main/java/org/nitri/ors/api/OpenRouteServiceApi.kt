package org.nitri.ors.api

import org.nitri.ors.model.RouteRequest
import org.nitri.ors.model.RouteResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenRouteServiceApi {
    @POST("v2/directions/{profile}")
    suspend fun getRoute(
        @Path("profile") profile: String,
        @Body request: RouteRequest
    ): RouteResponse
}