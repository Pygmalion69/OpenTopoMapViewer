package org.nitri.ors.domain.snap

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.nitri.ors.domain.meta.Metadata

/**
 * Snap response in GeoJSON format
 */
@Serializable
data class SnapGeoJsonResponse(
    val type: String, // "FeatureCollection"
    val features: List<SnapFeature>,
    val metadata: Metadata,
    val bbox: List<Double>? = null
)

/**
 * A GeoJSON Feature with snapped location info
 */
@Serializable
data class SnapFeature(
    val type: String, // "Feature"
    val properties: SnapProperties,
    val geometry: SnapGeometry
)

@Serializable
data class SnapProperties(
    val name: String? = null,

    @SerialName("snapped_distance")
    val snappedDistance: Double,

    @SerialName("source_id")
    val sourceId: Int
)

@Serializable
data class SnapGeometry(
    val type: String, // "Point"
    val coordinates: List<Double> // [lon, lat]
)
