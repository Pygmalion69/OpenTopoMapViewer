package org.nitri.ors.domain.snap

import kotlinx.serialization.Serializable

/**
 * Snap request for ORS /v2/snap/{profile}/json
 *
 * @param locations List of [lon, lat] coordinates to snap.
 * @param radius Maximum radius (meters) around given coordinates to search for graph edges.
 * @param id Optional client-provided identifier.
 */
@Serializable
data class SnapRequest(
    val locations: List<List<Double>>,
    val radius: Int,
    val id: String? = null
)