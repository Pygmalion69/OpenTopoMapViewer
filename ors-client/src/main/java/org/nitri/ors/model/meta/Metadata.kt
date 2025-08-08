package org.nitri.ors.model.meta

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Metadata(
    val id: String? = null,
    val attribution: String,
    val service: String,
    val timestamp: Long,
    val query: JsonElement, // could be String, Map<String, JsonElement>, or a specific model
    val engine: Engine,
    val system_message: String? = null
)

@Serializable
data class Engine(
    val version: String,
    val build_date: String,
    val graph_date: String
)