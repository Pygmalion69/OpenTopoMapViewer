package org.nitri.ors.domain.pois

/** DSL for constructing [PoisRequest] objects. */
inline fun poisRequest(build: PoisRequestBuilder.() -> Unit): PoisRequest =
    PoisRequestBuilder().apply(build).build()

/** Builder used by [poisRequest]. */
class PoisRequestBuilder {
    private var bbox: List<List<Double>>? = null
    private var geojson: GeoJsonGeometry? = null
    private var buffer: Int? = null

    var filters: Map<String, String>? = null
    var limit: Int? = null
    var sortby: String? = null

    fun bbox(minLon: Double, minLat: Double, maxLon: Double, maxLat: Double) = apply {
        bbox = listOf(listOf(minLon, minLat), listOf(maxLon, maxLat))
    }

    fun point(lon: Double, lat: Double) = apply {
        geojson = GeoJsonGeometry(type = "Point", coordinates = listOf(lon, lat))
    }

    fun buffer(meters: Int?) = apply { this.buffer = meters }

    fun build(): PoisRequest {
        val geom = Geometry(
            bbox = bbox,
            geojson = geojson,
            buffer = buffer
        )
        return PoisRequest(
            geometry = geom,
            filters = filters,
            limit = limit,
            sortby = sortby
        )
    }
}

/** Java-friendly builder counterpart. */
class PoisRequestBuilderJ {
    private var bbox: List<List<Double>>? = null
    private var geojson: GeoJsonGeometry? = null
    private var buffer: Int? = null

    private var filters: Map<String, String>? = null
    private var limit: Int? = null
    private var sortby: String? = null

    fun bbox(minLon: Double, minLat: Double, maxLon: Double, maxLat: Double) = apply {
        bbox = listOf(listOf(minLon, minLat), listOf(maxLon, maxLat))
    }

    fun point(lon: Double, lat: Double) = apply {
        geojson = GeoJsonGeometry(type = "Point", coordinates = listOf(lon, lat))
    }

    fun buffer(meters: Int?) = apply { this.buffer = meters }

    fun filters(filters: Map<String, String>?) = apply { this.filters = filters }
    fun limit(limit: Int?) = apply { this.limit = limit }
    fun sortby(sortby: String?) = apply { this.sortby = sortby }

    fun build(): PoisRequest {
        val geom = Geometry(
            bbox = bbox,
            geojson = geojson,
            buffer = buffer
        )
        return PoisRequest(
            geometry = geom,
            filters = filters,
            limit = limit,
            sortby = sortby
        )
    }
}
