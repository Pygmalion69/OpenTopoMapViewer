package org.nitri.ors.repository

import org.nitri.ors.api.OpenRouteServiceApi
import org.nitri.ors.model.matrix.MatrixRequest
import org.nitri.ors.model.matrix.MatrixResponse

class MatrixRepository(private val api: OpenRouteServiceApi) {

    /**
     * Calls the ORS Matrix endpoint for the given profile.
     *
     * @param locations List of [lon, lat] coordinate pairs.
     * @param profile ORS profile, e.g. "driving-car", "foot-hiking", etc.
     * @param metrics Optional list of metrics to include (e.g., ["duration"], ["distance"], or both).
     * @param sources Optional list of indices into locations used as sources.
     * @param destinations Optional list of indices into locations used as destinations.
     * @param resolveLocations Optional flag to resolve/snaps locations to the network.
     * @param id Optional arbitrary request id.
     */
    suspend fun getMatrix(
        locations: List<List<Double>>,
        profile: String,
        metrics: List<String>? = null,
        sources: List<Int>? = null,
        destinations: List<Int>? = null,
        resolveLocations: Boolean? = null,
        id: String? = null,
    ): MatrixResponse {
        val request = MatrixRequest(
            locations = locations,
            destinations = destinations,
            id = id,
            metrics = metrics,
            resolveLocations = resolveLocations,
            sources = sources
        )
        return api.getMatrix(profile, request)
    }
}