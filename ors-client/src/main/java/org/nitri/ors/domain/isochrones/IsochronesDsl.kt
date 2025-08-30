package org.nitri.ors.domain.isochrones

/** DSL for constructing [IsochronesRequest] objects. */
inline fun isochronesRequest(build: IsochronesRequestBuilder.() -> Unit): IsochronesRequest =
    IsochronesRequestBuilder().apply(build).build()

/** Builder used by [isochronesRequest]. */
class IsochronesRequestBuilder {
    private val locations = mutableListOf<List<Double>>()
    private var range: MutableList<Int> = mutableListOf()

    var rangeType: String? = null
    var attributes: List<String>? = null
    var id: String? = null
    var intersections: Boolean? = null
    var interval: Int? = null
    var locationType: String? = null
    var smoothing: Double? = null
    var options: IsochronesOptions? = null

    fun location(lon: Double, lat: Double) = apply { locations.add(listOf(lon, lat)) }
    fun rangeSeconds(vararg seconds: Int) = apply { range.addAll(seconds.toList()) }

    fun build(): IsochronesRequest {
        require(locations.isNotEmpty()) { "At least one location is required" }
        require(range.isNotEmpty()) { "At least one range value is required" }
        return IsochronesRequest(
            locations = locations.toList(),
            range = range.toList(),
            rangeType = rangeType,
            attributes = attributes,
            id = id,
            intersections = intersections,
            interval = interval,
            locationType = locationType,
            smoothing = smoothing,
            options = options
        )
    }
}

/** Java-friendly builder counterpart. */
class IsochronesRequestBuilderJ {
    private val locations = mutableListOf<List<Double>>()
    private val range: MutableList<Int> = mutableListOf()

    private var rangeType: String? = null
    private var attributes: List<String>? = null
    private var id: String? = null
    private var intersections: Boolean? = null
    private var interval: Int? = null
    private var locationType: String? = null
    private var smoothing: Double? = null
    private var options: IsochronesOptions? = null

    fun location(lon: Double, lat: Double) = apply { locations.add(listOf(lon, lat)) }
    fun addRange(seconds: Int) = apply { range.add(seconds) }

    fun rangeType(rangeType: String?) = apply { this.rangeType = rangeType }
    fun attributes(attributes: List<String>?) = apply { this.attributes = attributes }
    fun id(id: String?) = apply { this.id = id }
    fun intersections(intersections: Boolean?) = apply { this.intersections = intersections }
    fun interval(interval: Int?) = apply { this.interval = interval }
    fun locationType(locationType: String?) = apply { this.locationType = locationType }
    fun smoothing(smoothing: Double?) = apply { this.smoothing = smoothing }
    fun options(options: IsochronesOptions?) = apply { this.options = options }

    fun build(): IsochronesRequest {
        require(locations.isNotEmpty()) { "At least one location is required" }
        require(range.isNotEmpty()) { "At least one range value is required" }
        return IsochronesRequest(
            locations = locations.toList(),
            range = range.toList(),
            rangeType = rangeType,
            attributes = attributes,
            id = id,
            intersections = intersections,
            interval = interval,
            locationType = locationType,
            smoothing = smoothing,
            options = options
        )
    }
}
