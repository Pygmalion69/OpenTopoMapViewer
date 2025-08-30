package org.nitri.ors.helper

import kotlinx.serialization.json.JsonElement
import org.nitri.ors.OrsClient
import org.nitri.ors.domain.optimization.CustomMatrix
import org.nitri.ors.domain.optimization.Job
import org.nitri.ors.domain.optimization.OptimizationRequest
import org.nitri.ors.domain.optimization.OptimizationResponse
import org.nitri.ors.domain.optimization.Shipment
import org.nitri.ors.domain.optimization.Vehicle

/**
 * Repository for the OpenRouteService Optimization endpoint using [OrsClient].
 */
class OptimizationHelper {

    /**
     * Calls the ORS Optimization endpoint with provided arguments and builds the request.
     */
    suspend fun OrsClient.getOptimization(
        vehicles: List<Vehicle>,
        jobs: List<Job>? = null,
        shipments: List<Shipment>? = null,
        matrices: Map<String, CustomMatrix>? = null,
        options: Map<String, JsonElement>? = null
    ): OptimizationResponse {
        val request = OptimizationRequest(
            jobs = jobs,
            shipments = shipments,
            vehicles = vehicles,
            matrices = matrices,
            options = options
        )
        return getOptimization(request)
    }
}
