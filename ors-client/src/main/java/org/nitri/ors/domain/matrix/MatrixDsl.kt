package org.nitri.ors.domain.matrix

/** DSL for building [MatrixRequest] instances. */
inline fun matrixRequest(build: MatrixRequestBuilder.() -> Unit): MatrixRequest =
    MatrixRequestBuilder().apply(build).build()

/** Builder used by [matrixRequest]. */
class MatrixRequestBuilder {
    private val locations = mutableListOf<List<Double>>()
    var destinations: List<Int>? = null
    var id: String? = null
    var metrics: List<String>? = null
    var resolveLocations: Boolean? = null
    var sources: List<Int>? = null

    fun location(lon: Double, lat: Double) = apply { locations.add(listOf(lon, lat)) }

    fun build(): MatrixRequest {
        require(locations.isNotEmpty()) { "At least one location is required" }
        return MatrixRequest(
            locations = locations.toList(),
            destinations = destinations,
            id = id,
            metrics = metrics,
            resolveLocations = resolveLocations,
            sources = sources
        )
    }
}

/** Java-friendly builder counterpart. */
class MatrixRequestBuilderJ {
    private val locations = mutableListOf<List<Double>>()
    private var destinations: List<Int>? = null
    private var id: String? = null
    private var metrics: List<String>? = null
    private var resolveLocations: Boolean? = null
    private var sources: List<Int>? = null

    fun location(lon: Double, lat: Double) = apply { locations.add(listOf(lon, lat)) }
    fun destinations(destinations: List<Int>?) = apply { this.destinations = destinations }
    fun id(id: String?) = apply { this.id = id }
    fun metrics(metrics: List<String>?) = apply { this.metrics = metrics }
    fun resolveLocations(resolveLocations: Boolean?) = apply { this.resolveLocations = resolveLocations }
    fun sources(sources: List<Int>?) = apply { this.sources = sources }

    fun build(): MatrixRequest {
        require(locations.isNotEmpty()) { "At least one location is required" }
        return MatrixRequest(
            locations = locations.toList(),
            destinations = destinations,
            id = id,
            metrics = metrics,
            resolveLocations = resolveLocations,
            sources = sources
        )
    }
}
