package org.nitri.ors.model.export

import kotlinx.serialization.Serializable

@Serializable
data class ExportResponse(
    val points: List<Point>,
    val edges: List<Edge>,
    val weights: List<Weight>
)

@Serializable
data class Point(
    val id: Int,
    val coordinates: List<Double> // [lon, lat]
)

@Serializable
data class Edge(
    val id: Int,
    val startPointId: Int,
    val endPointId: Int
)

@Serializable
data class Weight(
    val edgeId: Int,
    val weight: Double
)
