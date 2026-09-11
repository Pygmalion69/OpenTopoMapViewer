package org.nitri.opentopo.ors

import org.nitri.opentopo.model.PlaceSearchResult
import org.nitri.ors.domain.geocode.GeocodeFeature

fun mapGeocodeFeatureToPlaceSearchResult(feature: GeocodeFeature): PlaceSearchResult? {
    val coordinates = feature.geometry?.coordinates ?: return null
    if (coordinates.size < 2) return null

    val lon = coordinates[0]
    val lat = coordinates[1]

    if (!lon.isFinite() || !lat.isFinite()) return null
    if (lon < -180.0 || lon > 180.0) return null
    if (lat < -90.0 || lat > 90.0) return null

    val props = feature.properties
    val rawLabel = props?.label?.trim().orEmpty()
    val rawName = props?.name?.trim().orEmpty()

    val label = rawLabel.ifEmpty { rawName }
    if (label.isEmpty()) return null

    val name = rawName.ifEmpty { label }

    val gid = props?.gid?.trim().orEmpty()
    val id = props?.id?.trim().orEmpty()
    val stableId = when {
        gid.isNotEmpty() -> gid
        id.isNotEmpty() -> id
        else -> "$lon,$lat-$label"
    }

    return PlaceSearchResult(
        stableId = stableId,
        name = name,
        label = label,
        latitude = lat,
        longitude = lon
    )
}

fun mapGeocodeFeaturesToPlaceSearchResults(features: List<GeocodeFeature>?): List<PlaceSearchResult> {
    return features?.mapNotNull { mapGeocodeFeatureToPlaceSearchResult(it) }.orEmpty()
}
