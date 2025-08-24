package org.nitri.ors.repository

import kotlinx.serialization.json.JsonElement
import org.nitri.ors.api.OpenRouteServiceApi
import org.nitri.ors.model.optimization.CustomMatrix
import org.nitri.ors.model.optimization.Job
import org.nitri.ors.model.optimization.OptimizationRequest
import org.nitri.ors.model.optimization.OptimizationResponse
import org.nitri.ors.model.optimization.Shipment
import org.nitri.ors.model.optimization.Vehicle

/**
 * Repository for the OpenRouteService Optimization endpoint.
 *
 * This is a thin wrapper around the Retrofit API, similar to other repositories
 * in this package. The repository builds the OptimizationRequest from the
 * provided arguments.
 */
class OptimizationRepository(private val api: OpenRouteServiceApi) {

    /**
     * Calls the ORS Optimization endpoint with provided arguments and builds the request.
     *
     * @param vehicles Required list of vehicles.
     * @param jobs Optional list of jobs.
     * @param shipments Optional list of shipments.
     * @param matrices Optional custom matrices keyed by profile.
     * @param options Optional free-form options.
     */
    suspend fun getOptimization(
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
        return api.getOptimization(request)
    }
}
