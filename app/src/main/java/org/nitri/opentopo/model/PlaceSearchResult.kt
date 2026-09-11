package org.nitri.opentopo.model

data class PlaceSearchResult(
    val stableId: String,
    val name: String,
    val label: String,
    val latitude: Double,
    val longitude: Double
)
