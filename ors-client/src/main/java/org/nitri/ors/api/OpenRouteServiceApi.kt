package org.nitri.ors.api

import org.nitri.ors.model.route.RouteRequest
import org.nitri.ors.model.route.RouteResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface OpenRouteServiceApi {
    @POST("v2/directions/{profile}")
    suspend fun getRoute(
        @Path("profile") profile: String,
        @Body request: RouteRequest
    ): RouteResponse
}