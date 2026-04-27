package org.nitri.opentopo.util

import org.nitri.opentopo.model.MarkerModel
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class GpxMarkerImporter {
    data class ImportResult(
        val markers: List<MarkerModel>,
        val skippedCount: Int
    )

    fun import(inputStream: InputStream, baseSeq: Int): ImportResult {
        val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = documentBuilder.parse(inputStream)
        val waypointNodes = document.getElementsByTagName("wpt")

        val markers = mutableListOf<MarkerModel>()
        var skippedCount = 0
        var seq = baseSeq

        for (index in 0 until waypointNodes.length) {
            val node = waypointNodes.item(index)
            if (node !is Element) {
                skippedCount++
                continue
            }

            val latValue = node.getAttribute("lat")
            val lonValue = node.getAttribute("lon")
            val latitude = latValue.toDoubleOrNull()
            val longitude = lonValue.toDoubleOrNull()

            if (latitude == null || longitude == null) {
                skippedCount++
                continue
            }

            seq += 1
            val name = node.getElementsByTagName("name").item(0)?.textContent
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: "Marker $seq"
            val description = node.getElementsByTagName("desc").item(0)?.textContent
                ?.trim()
                .orEmpty()

            markers.add(
                MarkerModel(
                    seq = seq,
                    latitude = latitude,
                    longitude = longitude,
                    name = name,
                    description = description
                )
            )
        }

        return ImportResult(markers, skippedCount)
    }
}
