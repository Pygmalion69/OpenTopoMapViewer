package org.nitri.ors.domain.optimization

/** DSL for building [OptimizationRequest] payloads. */
inline fun optimizationRequest(build: OptimizationRequestBuilder.() -> Unit): OptimizationRequest =
    OptimizationRequestBuilder().apply(build).build()

/** Builder used by [optimizationRequest]. */
class OptimizationRequestBuilder {
    private val jobs = mutableListOf<Job>()
    private val shipments = mutableListOf<Shipment>()
    private val vehicles = mutableListOf<Vehicle>()
    private var matrices: Map<String, CustomMatrix>? = null
    private var options: Map<String, kotlinx.serialization.json.JsonElement>? = null

    fun job(id: Int, locationLon: Double? = null, locationLat: Double? = null, service: Int? = null, priority: Int? = null) = apply {
        val loc = if (locationLon != null && locationLat != null) listOf(locationLon, locationLat) else null
        jobs.add(Job(id = id, service = service, priority = priority, location = loc))
    }

    fun shipment(
        id: Int,
        pickupLon: Double, pickupLat: Double,
        deliveryLon: Double, deliveryLat: Double,
        servicePickup: Int? = null,
        serviceDelivery: Int? = null
    ) = apply {
        val pickup = ShipmentStep(service = servicePickup, location = listOf(pickupLon, pickupLat))
        val delivery = ShipmentStep(service = serviceDelivery, location = listOf(deliveryLon, deliveryLat))
        shipments.add(Shipment(id = id, amount = null, pickup = pickup, delivery = delivery))
    }

    fun vehicle(id: Int, profile: String, startLon: Double? = null, startLat: Double? = null, endLon: Double? = null, endLat: Double? = null, capacity: List<Int>? = null) = apply {
        val start = if (startLon != null && startLat != null) listOf(startLon, startLat) else null
        val end = if (endLon != null && endLat != null) listOf(endLon, endLat) else null
        vehicles.add(
            Vehicle(
                id = id,
                profile = profile,
                capacity = capacity,
                start = start,
                end = end
            )
        )
    }

    fun matrices(m: Map<String, CustomMatrix>?) = apply { this.matrices = m }
    fun options(o: Map<String, kotlinx.serialization.json.JsonElement>?) = apply { this.options = o }

    fun build(): OptimizationRequest {
        if (vehicles.isEmpty()) {
            throw IllegalStateException("At least one vehicle is required")
        }
        if (jobs.isEmpty() && shipments.isEmpty()) {
            error("Provide at least one job or shipment")
        }
        return OptimizationRequest(
            jobs = jobs.takeIf { it.isNotEmpty() },
            shipments = shipments.takeIf { it.isNotEmpty() },
            vehicles = vehicles.toList(),
            matrices = matrices,
            options = options
        )
    }
}

/** Java-friendly builder counterpart. */
class OptimizationRequestBuilderJ {
    private val dsl = OptimizationRequestBuilder()

    fun addJob(id: Int, locationLon: Double?, locationLat: Double?, service: Int?, priority: Int?) = apply {
        dsl.job(id, locationLon, locationLat, service, priority)
    }

    fun addShipment(id: Int, pickupLon: Double, pickupLat: Double, deliveryLon: Double, deliveryLat: Double, servicePickup: Int?, serviceDelivery: Int?) = apply {
        dsl.shipment(id, pickupLon, pickupLat, deliveryLon, deliveryLat, servicePickup, serviceDelivery)
    }

    fun addVehicle(id: Int, profile: String, startLon: Double?, startLat: Double?, endLon: Double?, endLat: Double?, capacity: List<Int>?) = apply {
        dsl.vehicle(id, profile, startLon, startLat, endLon, endLat, capacity)
    }

    fun matrices(m: Map<String, CustomMatrix>?) = apply { dsl.matrices(m) }
    fun options(o: Map<String, kotlinx.serialization.json.JsonElement>?) = apply { dsl.options(o) }

    fun build(): OptimizationRequest = dsl.build()
}
