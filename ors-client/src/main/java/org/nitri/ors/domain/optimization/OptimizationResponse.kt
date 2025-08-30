package org.nitri.ors.domain.optimization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Top-level result from /optimization */
@Serializable
data class OptimizationResponse(
    val code: Int,
    val summary: OptimizationSummary,
    val unassigned: List<Unassigned> = emptyList(),
    val routes: List<OptimizedRoute> = emptyList()
)

/* -------------------------------- Summary -------------------------------- */

@Serializable
data class OptimizationSummary(
    val cost: Int,
    val routes: Int,
    val unassigned: Int,

    /** Totals – present depending on your payload/constraints */
    val delivery: List<Int>? = null,
    val amount: List<Int>? = null,
    val pickup: List<Int>? = null,

    val setup: Int,
    val service: Int,
    val duration: Int,
    @SerialName("waiting_time") val waitingTime: Int,
    val priority: Int,

    /** Usually empty unless you use hard constraints */
    val violations: List<Violation> = emptyList(),

    @SerialName("computing_times") val computingTimes: ComputingTimes? = null
)

@Serializable
data class ComputingTimes(
    val loading: Int? = null,
    val solving: Int? = null,
    val routing: Int? = null
)

/* ------------------------------ Unassigned ------------------------------- */

@Serializable
data class Unassigned(
    val id: Int,
    val location: LonLat,
    /** e.g. "job" */
    val type: String
)

/* --------------------------------- Routes -------------------------------- */

@Serializable
data class OptimizedRoute(
    val vehicle: Int,
    val cost: Int,

    /** Per-route totals (optional keys depending on your model) */
    val delivery: List<Int>? = null,
    val amount: List<Int>? = null,
    val pickup: List<Int>? = null,

    val setup: Int,
    val service: Int,
    val duration: Int,
    @SerialName("waiting_time") val waitingTime: Int,
    val priority: Int,

    val steps: List<RouteStep> = emptyList(),
    val violations: List<Violation> = emptyList()
)

@Serializable
data class RouteStep(
    /** "start" | "job" | "end" | … */
    val type: String,

    val location: LonLat,

    /** Only for job-like steps */
    val id: Int? = null,
    val job: Int? = null,

    val setup: Int? = null,
    val service: Int? = null,
    @SerialName("waiting_time") val waitingTime: Int? = null,

    /** Vehicle load after performing this step */
    val load: List<Int>? = null,

    /** Timestamps & cumulated travel */
    val arrival: Int? = null,
    val duration: Int? = null,

    val violations: List<Violation> = emptyList()
)

/* ------------------------------- Violations ------------------------------ */
/** Kept flexible; VROOM may return several shapes depending on constraint hit. */
@Serializable
data class Violation(
    val type: String? = null,   // e.g., "capacity", "skills", "time_window", …
    val id: Int? = null,        // job/shipment id if applicable
    val job: Int? = null,
    val description: String? = null
)
