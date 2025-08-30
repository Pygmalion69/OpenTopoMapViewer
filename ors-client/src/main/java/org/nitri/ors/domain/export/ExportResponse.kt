package org.nitri.ors.domain.export

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/** Graph export response containing nodes and edges. */
@Serializable
data class ExportResponse(
    val nodes: List<Node> = emptyList(),
    val edges: List<GraphEdge> = emptyList()
)

/** Node entry in an [ExportResponse]. */
@Serializable
data class Node(
    @SerialName("nodeId") val nodeId: Long,
    /** `[lon, lat]` coordinate. */
    val location: List<Double>
)

/** Graph edge entry in an [ExportResponse]. */
@Serializable
data class GraphEdge(
    @SerialName("fromId") val fromId: Long,
    @SerialName("toId") val toId: Long,
    @Serializable(with = StringAsDoubleSerializer::class)
    val weight: Double
)

/**
 * ORS sometimes returns numeric fields (e.g., "weight") as strings.
 * This serializer accepts either a JSON number or a string number.
 */
object StringAsDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringAsDouble", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double {
        return if (decoder is JsonDecoder) {
            val prim = decoder.decodeJsonElement() as JsonPrimitive
            prim.doubleOrNull ?: prim.content.toDouble()
        } else {
            decoder.decodeDouble()
        }
    }

    override fun serialize(encoder: Encoder, value: Double) {
        encoder.encodeDouble(value)
    }
}
