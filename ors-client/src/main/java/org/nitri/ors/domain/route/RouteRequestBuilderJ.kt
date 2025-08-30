package org.nitri.ors.domain.route

import org.nitri.ors.LonLat

/**
 * Java-friendly builder
 */
class RouteRequestBuilderJ {
    private val coords = mutableListOf<LonLat>()
    private var language: String? = null

    fun start(lon: Double, lat: Double) = apply { coords.add(LonLat(lon, lat)) }
    fun end(lon: Double, lat: Double)   = apply { coords.add(LonLat(lon, lat)) }
    fun add(lon: Double, lat: Double)   = apply { coords.add(LonLat(lon, lat)) }
    fun language(language: String) = apply { this.language = language }

    fun build(): RouteRequest {
        require(coords.size >= 2) { "At least start and end coordinates required" }
        val coordinates = coords.map { listOf(it.lon, it.lat) }
        return RouteRequest(coordinates, language = language)
    }
}