package org.nitri.ors.domain.pois

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Top-level POIs GeoJSON response:
 * {
 *   "type": "FeatureCollection",
 *   "bbox": [minLon, minLat, maxLon, maxLat],
 *   "features": [...],
 *   "information": {...}
 * }
 */
@Serializable
data class PoisGeoJsonResponse(
    val type: String,                                // "FeatureCollection"
    val bbox: List<Double>? = null,                  // [minLon, minLat, maxLon, maxLat]
    val features: List<PoiFeature>,
    val information: PoisInformation? = null         // metadata (sometimes called "information")
)

/** A single GeoJSON feature (Point) */
@Serializable
data class PoiFeature(
    val type: String,                                // "Feature"
    val geometry: PoiGeometry,
    val properties: PoiProperties
)

@Serializable
data class PoiGeometry(
    val type: String,                                // "Point"
    val coordinates: List<Double>                    // [lon, lat]
)

/**
 * Properties seen in your sample. Some fields are optional across results.
 * - category_ids is an object with integer keys -> map to Map<Int, CategoryInfo>
 * - osm_tags is a free-form OSM tag map (strings)
 */
@Serializable
data class PoiProperties(
    @SerialName("osm_id") val osmId: Long,
    @SerialName("osm_type") val osmType: Int,
    val distance: Double,
    @SerialName("category_ids") val categoryIds: Map<Int, CategoryInfo>? = null,
    @SerialName("osm_tags") val osmTags: Map<String, String>? = null
)

@Serializable
data class CategoryInfo(
    @SerialName("category_name") val categoryName: String,
    @SerialName("category_group") val categoryGroup: String
)

/**
 * The “information” block at the end of the response.
 * Note: ORS uses "information" (not "metadata") here.
 */
@Serializable
data class PoisInformation(
    val attribution: String? = null,
    val version: String? = null,
    val timestamp: Long? = null,
    val query: PoisQueryInfo? = null
)

/** Echo of the request inside the information block */
@Serializable
data class PoisQueryInfo(
    val request: String? = null,                     // "pois"
    val geometry: PoisQueryGeometry? = null
)

/** Mirrors the request’s geometry wrapper */
@Serializable
data class PoisQueryGeometry(
    val bbox: List<List<Double>>? = null,            // [[minLon,minLat],[maxLon,maxLat]]
    val geojson: GeoJsonGeometry? = null,
    val buffer: Int? = null
)

