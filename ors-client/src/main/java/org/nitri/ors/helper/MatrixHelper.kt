package org.nitri.ors.helper

import org.nitri.ors.OrsClient
import org.nitri.ors.model.matrix.MatrixRequest
import org.nitri.ors.model.matrix.MatrixResponse

class MatrixHelper(private val orsClient: OrsClient) {

    /**
     * Calls the ORS Matrix endpoint for the given profile.
     */
    suspend fun OrsClient.getMatrix(
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
        return getMatrix(profile, request)
    }
}