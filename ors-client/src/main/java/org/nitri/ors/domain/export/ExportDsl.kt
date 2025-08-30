package org.nitri.ors.domain.export

// Kotlin DSL for ExportRequest
inline fun exportRequest(build: ExportRequestBuilder.() -> Unit): ExportRequest =
    ExportRequestBuilder().apply(build).build()

class ExportRequestBuilder {
    private var bbox: List<List<Double>>? = null
    private var id: String? = null
    private var geometry: Boolean? = null

    fun bbox(minLon: Double, minLat: Double, maxLon: Double, maxLat: Double) = apply {
        bbox = listOf(listOf(minLon, minLat), listOf(maxLon, maxLat))
    }

    fun id(id: String) = apply { this.id = id }
    fun geometry(geometry: Boolean?) = apply { this.geometry = geometry }

    fun build(): ExportRequest {
        val b = requireNotNull(bbox) { "bbox is required" }
        val i = id ?: "export_request"
        return ExportRequest(bbox = b, id = i, geometry = geometry)
    }
}

// Java-friendly builder
class ExportRequestBuilderJ {
    private var bbox: List<List<Double>>? = null
    private var id: String? = null
    private var geometry: Boolean? = null

    fun bbox(minLon: Double, minLat: Double, maxLon: Double, maxLat: Double) = apply {
        bbox = listOf(listOf(minLon, minLat), listOf(maxLon, maxLat))
    }

    fun id(id: String) = apply { this.id = id }
    fun geometry(geometry: Boolean?) = apply { this.geometry = geometry }

    fun build(): ExportRequest {
        val b = requireNotNull(bbox) { "bbox is required" }
        val i = id ?: "export_request"
        return ExportRequest(bbox = b, id = i, geometry = geometry)
    }
}