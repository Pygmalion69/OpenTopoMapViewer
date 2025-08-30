package org.nitri.ors.domain.elevation

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray

/** DSL for building [ElevationLineRequest] instances. */
inline fun elevationLineRequest(build: ElevationLineRequestBuilder.() -> Unit): ElevationLineRequest =
    ElevationLineRequestBuilder().apply(build).build()

/** Builder used by [elevationLineRequest]. */
class ElevationLineRequestBuilder {
    var formatIn: String = ElevationFormats.POLYLINE
    var formatOut: String = ElevationFormats.GEOJSON
    var dataset: String? = null

    // flexible geometry holder; convenience helpers below
    private var geometry: JsonElement? = null

    fun geometry(element: JsonElement) = apply { this.geometry = element }

    // Convenience: provide a simple list of [lon,lat] pairs for polyline format
    fun polyline(vararg coords: Pair<Double, Double>) = apply {
        formatIn = ElevationFormats.POLYLINE
        geometry = buildJsonArray {
            coords.forEach { (lon, lat) ->
                add(buildJsonArray { add(JsonPrimitive(lon)); add(JsonPrimitive(lat)) })
            }
        }
    }

    fun build(): ElevationLineRequest {
        val g = requireNotNull(geometry) { "geometry is required" }
        return ElevationLineRequest(
            formatIn = formatIn,
            formatOut = formatOut,
            geometry = g,
            dataset = dataset
        )
    }
}

/** DSL for constructing [ElevationPointRequest] objects. */
inline fun elevationPointRequest(build: ElevationPointRequestBuilder.() -> Unit): ElevationPointRequest =
    ElevationPointRequestBuilder().apply(build).build()

/** Builder used by [elevationPointRequest]. */
class ElevationPointRequestBuilder {
    var formatIn: String = "point"
    var formatOut: String = "geojson"
    var dataset: String? = null
    private var lon: Double? = null
    private var lat: Double? = null

    fun point(lon: Double, lat: Double) = apply { this.lon = lon; this.lat = lat }

    fun build(): ElevationPointRequest {
        val lo = requireNotNull(lon) { "lon is required" }
        val la = requireNotNull(lat) { "lat is required" }
        return ElevationPointRequest(
            formatIn = formatIn,
            formatOut = formatOut,
            dataset = dataset,
            geometry = listOf(lo, la)
        )
    }
}

// Java-friendly builders
class ElevationLineRequestBuilderJ {
    private val dsl = ElevationLineRequestBuilder()
    fun formatIn(v: String) = apply { dsl.formatIn = v }
    fun formatOut(v: String) = apply { dsl.formatOut = v }
    fun dataset(v: String?) = apply { dsl.dataset = v }
    fun geometry(element: JsonElement) = apply { dsl.geometry(element) }
    fun polyline(coords: List<List<Double>>) = apply {
        // Accept list of [lon,lat]; convert to JsonArray
        val arr: JsonArray = buildJsonArray {
            coords.forEach { c ->
                add(buildJsonArray { add(JsonPrimitive(c[0])); add(JsonPrimitive(c[1])) })
            }
        }
        dsl.formatIn = ElevationFormats.POLYLINE
        dsl.geometry(arr)
    }
    fun build(): ElevationLineRequest = dsl.build()
}

class ElevationPointRequestBuilderJ {
    private val dsl = ElevationPointRequestBuilder()
    fun formatIn(v: String) = apply { dsl.formatIn = v }
    fun formatOut(v: String) = apply { dsl.formatOut = v }
    fun dataset(v: String?) = apply { dsl.dataset = v }
    fun point(lon: Double, lat: Double) = apply { dsl.point(lon, lat) }
    fun build(): ElevationPointRequest = dsl.build()
}
