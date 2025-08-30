package org.nitri.ors.domain.optimization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Root payload sent to /optimization
 * See: https://github.com/VROOM-Project/vroom/blob/master/docs/API.md
 */
@Serializable
data class OptimizationRequest(
    /** Required: either jobs and/or shipments, and vehicles */
    val jobs: List<Job>? = null,
    val shipments: List<Shipment>? = null,
    val vehicles: List<Vehicle>,

    /** Optional custom matrices keyed by routing profile (e.g. "driving-car") */
    val matrices: Map<String, CustomMatrix>? = null,

    /** Optional free-form options bag passed to the optimization engine */
    val options: Map<String, JsonElement>? = null
)

/* ---------------------------- Basics & helpers ---------------------------- */

@Serializable
data class TimeWindow(
    /** seconds since midnight */
    val start: Int,
    /** seconds since midnight */
    val end: Int
)

typealias LonLat = List<Double>          // [lon, lat]
typealias Amount = List<Int>             // capacities/quantities
typealias Skills = List<Int>             // integers as in VROOM API

/* --------------------------------- Jobs ---------------------------------- */

@Serializable
data class Job(
    /** Unique job id (required) */
    val id: Int,

    /** Service time in seconds spent on site (optional) */
    val service: Int? = null,

    /** Demand to be delivered/picked as a quantity vector (optional) */
    val delivery: Amount? = null,
    val pickup: Amount? = null,
    val amount: Amount? = null,          // generic synonym supported by VROOM

    /** Location as coordinates or index into the matrix, provide one of them */
    val location: LonLat? = null,
    @SerialName("location_index") val locationIndex: Int? = null,

    /** Allowed vehicles list (restrict which vehicles may handle this job) */
    @SerialName("allowed_vehicles") val allowedVehicles: List<Int>? = null,

    /** Disallowed vehicles list */
    @SerialName("disallowed_vehicles") val disallowedVehicles: List<Int>? = null,

    /** Skills required for this job */
    val skills: Skills? = null,

    /** 0..100 (higher = more important) */
    val priority: Int? = null,

    /** Multiple time windows supported */
    @SerialName("time_windows") val timeWindows: List<TimeWindow>? = null
)

/* ------------------------------- Shipments -------------------------------- */

@Serializable
data class Shipment(
    /** Unique shipment id (required) */
    val id: Int,

    /** Overall amount carried by the shipment */
    val amount: Amount? = null,

    /** Pickup and delivery legs (each looks like a job) */
    val pickup: ShipmentStep,
    val delivery: ShipmentStep,

    /** Skills & priority rules apply to the *whole* shipment */
    val skills: Skills? = null,
    val priority: Int? = null
)

@Serializable
data class ShipmentStep(
    /** Service time in seconds at this step */
    val service: Int? = null,

    /** Coordinates or index, provide one of them */
    val location: LonLat? = null,
    @SerialName("location_index") val locationIndex: Int? = null,

    /** Time windows at this step */
    @SerialName("time_windows") val timeWindows: List<TimeWindow>? = null
)

/* -------------------------------- Vehicles -------------------------------- */

@Serializable
data class Vehicle(
    /** Unique vehicle id (required) */
    val id: Int,

    /** Routing profile, e.g., "driving-car" (required) */
    val profile: String,

    /** Vehicle capacity vector */
    val capacity: Amount? = null,

    /** Start/end positions as coordinates or as matrix indices (pick one style) */
    val start: LonLat? = null,
    val end: LonLat? = null,
    @SerialName("start_index") val startIndex: Int? = null,
    @SerialName("end_index") val endIndex: Int? = null,

    /** Skills the vehicle provides */
    val skills: Skills? = null,

    /** When the vehicle is available to operate */
    @SerialName("time_window") val timeWindow: TimeWindow? = null,

    /** Optional list of breaks (each with service & time windows) */
    val breaks: List<VehicleBreak>? = null,

    /** Earliest/latest start/end (advanced; seconds since midnight) */
    @SerialName("earliest_start") val earliestStart: Int? = null,
    @SerialName("latest_end") val latestEnd: Int? = null,

    /** Max tasks or max travel time limits (seconds) */
    @SerialName("max_tasks") val maxTasks: Int? = null,
    @SerialName("max_travel_time") val maxTravelTime: Int? = null,

    /** Optional arbitrary metadata to round-trip through the solver */
    val description: String? = null
)

@Serializable
data class VehicleBreak(
    /** Service time in seconds spent for the break */
    val service: Int? = null,
    /** Allowed time windows for this break */
    @SerialName("time_windows") val timeWindows: List<TimeWindow>
)

/* ------------------------------ Custom matrix ----------------------------- */

@Serializable
data class CustomMatrix(
    /** Square matrix in seconds; required if provided */
    val durations: List<List<Int>>? = null,
    /** Optional distances in meters */
    val distances: List<List<Int>>? = null
)
