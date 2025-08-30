package org.nitri.ors.domain.meta

import kotlinx.serialization.SerialName
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
    @SerialName("system_message")
    val systemMessage: String? = null
)

@Serializable
data class Engine(
    val version: String,
    @SerialName("build_date")
    val buildDate: String,
    @SerialName("graph_date")
    val graphDate: String
)