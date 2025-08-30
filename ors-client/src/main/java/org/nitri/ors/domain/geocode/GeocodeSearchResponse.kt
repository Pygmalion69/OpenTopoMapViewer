package org.nitri.ors.domain.geocode

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GeocodeSearchResponse(
    val geocoding: Geocoding? = null,
    val type: String? = null, // "FeatureCollection"
    val features: List<GeocodeFeature> = emptyList(),
    val bbox: List<Double>? = null,
    val errors: List<String>? = null,
    val warnings: List<String>? = null,
    val engine: Engine? = null,
    val timestamp: Long? = null
)

@Serializable
data class Geocoding(
    val version: String? = null,                 // "0.2"
    val attribution: String? = null,
    val query: GeocodeQuery? = null
)

@Serializable
data class Engine(
    val name: String? = null,    // "Pelias"
    val author: String? = null,  // "Mapzen"
    val version: String? = null
)

/**
 * Pelias "query" block varies by endpoint; everything is optional.
 * Fields like "point.lat" are addressed via @SerialName to match JSON.
 */
@Serializable
data class GeocodeQuery(
    // common
    val text: String? = null,
    val size: Int? = null,
    val layers: List<String>? = null,
    val sources: List<String>? = null,
    val private: Boolean? = null,
    val parsed_text: JsonObject? = null, // present on some search/structured responses
    val lang: PeliasLang? = null,
    val querySize: Int? = null,

    // focus point (search/autocomplete)
    @SerialName("focus.point.lon") val focusPointLon: Double? = null,
    @SerialName("focus.point.lat") val focusPointLat: Double? = null,

    // reverse & circle bounds
    @SerialName("point.lon") val pointLon: Double? = null,
    @SerialName("point.lat") val pointLat: Double? = null,
    @SerialName("boundary.circle.lon") val boundaryCircleLon: Double? = null,
    @SerialName("boundary.circle.lat") val boundaryCircleLat: Double? = null,
    @SerialName("boundary.circle.radius") val boundaryCircleRadius: Double? = null,

    // rect bounds (search/autocomplete/structured)
    @SerialName("boundary.rect.min_lon") val rectMinLon: Double? = null,
    @SerialName("boundary.rect.min_lat") val rectMinLat: Double? = null,
    @SerialName("boundary.rect.max_lon") val rectMaxLon: Double? = null,
    @SerialName("boundary.rect.max_lat") val rectMaxLat: Double? = null,

    // country limit (ISO-3166-1 alpha-2)
    @SerialName("boundary.country") val boundaryCountry: String? = null,

    // structured forward fields (all optional; API requires at least one)
    val venue: String? = null,
    val address: String? = null,
    val neighbourhood: String? = null,
    val borough: String? = null,
    val locality: String? = null,
    val county: String? = null,
    val region: String? = null,
    val country: String? = null,
    val postcode: String? = null
)

@Serializable
data class PeliasLang(
    val name: String? = null,     // "English"
    val iso6391: String? = null,  // "en"
    val iso6393: String? = null,  // "eng"
    val via: String? = null,      // "header"
    val defaulted: Boolean? = null
)

@Serializable
data class GeocodeFeature(
    val type: String? = null, // "Feature"
    val geometry: GeocodeGeometry? = null,
    val properties: GeocodeProperties? = null,
    val bbox: List<Double>? = null
)

@Serializable
data class GeocodeGeometry(
    val type: String? = null,             // typically "Point"
    val coordinates: List<Double> = emptyList() // [lon, lat] (+ elevation if provided)
)

/**
 * Properties are rich and vary by source; keep them optional.
 * `addendum` is left as JsonObject to pass through extra provider-specific content (e.g., OSM).
 */
@Serializable
data class GeocodeProperties(
    val id: String? = null,
    val gid: String? = null,
    val layer: String? = null,
    val source: String? = null,
    @SerialName("source_id") val sourceId: String? = null,

    val name: String? = null,
    val confidence: Double? = null,
    val distance: Double? = null,
    val accuracy: String? = null,

    // address-ish
    val label: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val postalcode: String? = null,

    // admin hierarchy
    val country: String? = null,
    @SerialName("country_gid") val countryGid: String? = null,
    @SerialName("country_a") val countryA: String? = null,

    val macroregion: String? = null,
    @SerialName("macroregion_gid") val macroregionGid: String? = null,
    @SerialName("macroregion_a") val macroregionA: String? = null,

    val region: String? = null,
    @SerialName("region_gid") val regionGid: String? = null,
    @SerialName("region_a") val regionA: String? = null,

    val localadmin: String? = null,
    @SerialName("localadmin_gid") val localadminGid: String? = null,

    val locality: String? = null,
    @SerialName("locality_gid") val localityGid: String? = null,

    val borough: String? = null,
    @SerialName("borough_gid") val boroughGid: String? = null,

    val neighbourhood: String? = null,
    @SerialName("neighbourhood_gid") val neighbourhoodGid: String? = null,

    val continent: String? = null,
    @SerialName("continent_gid") val continentGid: String? = null,

    // provider extras: e.g. wheelchair, website, wikidata, etc.
    val addendum: JsonObject? = null
)
