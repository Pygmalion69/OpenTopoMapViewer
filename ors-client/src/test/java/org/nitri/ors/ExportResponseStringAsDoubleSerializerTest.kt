package org.nitri.ors

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import org.nitri.ors.domain.export.ExportResponse

class ExportResponseStringAsDoubleSerializerTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parses_weight_when_number_or_string() {
        val payload = """
            {
              "nodes": [],
              "edges": [
                {"fromId":1, "toId":2, "weight": "12.34"},
                {"fromId":2, "toId":3, "weight": 56.78}
              ]
            }
        """.trimIndent()

        val resp = json.decodeFromString<ExportResponse>(payload)
        assertEquals(2, resp.edges.size)
        assertEquals(12.34, resp.edges[0].weight, 1e-9)
        assertEquals(56.78, resp.edges[1].weight, 1e-9)
    }

    @Test
    fun serializes_weight_as_number() {
        val payload = """
            {
              "nodes": [],
              "edges": [
                {"fromId":1, "toId":2, "weight": "10"}
              ]
            }
        """.trimIndent()
        val resp = json.decodeFromString<ExportResponse>(payload)

        val out = json.encodeToString(resp)
        // The serializer writes numbers, so we expect \"weight\":10.0 (or 10) in output
        // We assert that there is no quoted weight in the serialized JSON.
        assert(!out.contains("\"weight\": \""))
    }
}
