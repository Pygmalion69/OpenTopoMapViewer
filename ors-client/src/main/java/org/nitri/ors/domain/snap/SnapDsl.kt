package org.nitri.ors.domain.snap

/** DSL for constructing [SnapRequest] objects. */
inline fun snapRequest(build: SnapRequestBuilder.() -> Unit): SnapRequest =
    SnapRequestBuilder().apply(build).build()

/** Builder used by [snapRequest]. */
class SnapRequestBuilder {
    private val locations = mutableListOf<List<Double>>()
    private var radius: Int? = null
    var id: String? = null

    fun location(lon: Double, lat: Double) = apply { locations.add(listOf(lon, lat)) }
    fun radius(meters: Int) = apply { this.radius = meters }

    fun build(): SnapRequest {
        require(locations.isNotEmpty()) { "At least one location is required" }
        val r = requireNotNull(radius) { "radius is required" }
        return SnapRequest(locations = locations.toList(), radius = r, id = id)
    }
}

/** Java-friendly builder counterpart. */
class SnapRequestBuilderJ {
    private val locations = mutableListOf<List<Double>>()
    private var radius: Int? = null
    private var id: String? = null

    fun location(lon: Double, lat: Double) = apply { locations.add(listOf(lon, lat)) }
    fun radius(meters: Int) = apply { this.radius = meters }
    fun id(id: String?) = apply { this.id = id }

    fun build(): SnapRequest {
        require(locations.isNotEmpty()) { "At least one location is required" }
        val r = requireNotNull(radius) { "radius is required" }
        return SnapRequest(locations = locations.toList(), radius = r, id = id)
    }
}
