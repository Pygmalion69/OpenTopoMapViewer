package org.nitri.ors.domain.route

import org.nitri.ors.LonLat

/** DSL for constructing [RouteRequest] instances. */
inline fun routeRequest(build: RouteRequestBuilder.() -> Unit): RouteRequest =
    RouteRequestBuilder().apply(build).build()

/** Builder used by [routeRequest]. */
class RouteRequestBuilder {
    private val coords = mutableListOf<LonLat>()
    var language: String? = null

    fun start(lon: Double, lat: Double) = apply { coords.add(LonLat(lon, lat)) }
    fun end(lon: Double, lat: Double) = apply { coords.add(LonLat(lon, lat)) }
    fun coordinate(lon: Double, lat: Double) = apply { coords.add(LonLat(lon, lat)) }

    fun build(): RouteRequest {
        require(coords.size >= 2) { "At least start and end coordinates required" }
        val coordinates = coords.map { listOf(it.lon, it.lat) }
        return RouteRequest(coordinates, language = language)
    }
}
