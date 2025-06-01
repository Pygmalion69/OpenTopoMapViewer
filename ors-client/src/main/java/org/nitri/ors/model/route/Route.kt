package org.nitri.ors.model.route

import kotlinx.serialization.Serializable

@Serializable
data class Route(
    val summary: Summary,
    val segments: List<Segment>,
    val geometry: String
)