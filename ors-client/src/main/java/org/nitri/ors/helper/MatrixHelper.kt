package org.nitri.ors.helper

import org.nitri.ors.OrsClient
import org.nitri.ors.Profile
import org.nitri.ors.domain.matrix.MatrixRequest
import org.nitri.ors.domain.matrix.MatrixResponse

/** Helpers for the matrix endpoint. */
class MatrixHelper {

    /** Calls the ORS Matrix endpoint for the given [profile]. */
    suspend fun OrsClient.getMatrix(
        locations: List<List<Double>>,
        profile: Profile,
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