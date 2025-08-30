package org.nitri.ors.domain.isochrones

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * ORS Isochrones request
 * Docs: /v2/isochrones/{profile}
 *
 * Required: locations, range
 * Optional are nullable and omitted from JSON when null.
 */
@Serializable
data class IsochronesRequest(
    /** [lon, lat] pairs */
    val locations: List<List<Double>>,

    /** In seconds for range_type="time" (default) or in meters for "distance". */
    val range: List<Int>,

    /** "time" (default) or "distance" */
    @SerialName("range_type")
    val rangeType: String? = null,

    /** e.g. ["area"] (see docs for more) */
    val attributes: List<String>? = null,

    /** Optional request id that is echoed back in metadata */
    val id: String? = null,

    /** If true, include intersection info in response */
    val intersections: Boolean? = null,

    /** If set, returns multiple bands every `interval` (same unit as range_type) */
    val interval: Int? = null,

    /** "start" (default) or "destination" */
    @SerialName("location_type")
    val locationType: String? = null,

    /** Smoothing factor, e.g. 25 */
    val smoothing: Double? = null,

    /** Extra tweaks */
    val options: IsochronesOptions? = null
)

/**
 * Subset of useful options. Extend as needed.
 */
@Serializable
data class IsochronesOptions(
    /** "controlled" or "all" */
    @SerialName("avoid_borders")
    val avoidBorders: String? = null,

    /** e.g. ["ferries","tollways"] */
    @SerialName("avoid_features")
    val avoidFeatures: List<String>? = null,

    /**
     * Avoid polygons (GeoJSON geometry). Keep generic to stay flexible:
     * supply a Feature/Geometry object you serialize yourself.
     */
    @SerialName("avoid_polygons")
    val avoidPolygons: JsonElement? = null
)
