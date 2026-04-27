package org.nitri.opentopo.util

import org.nitri.opentopo.model.MarkerModel
import org.w3c.dom.Document
import java.io.OutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class GpxMarkerExporter {
    fun export(markers: List<MarkerModel>, outputStream: OutputStream) {
        val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document: Document = documentBuilder.newDocument()

        val gpxElement = document.createElement("gpx").apply {
            setAttribute("version", "1.1")
            setAttribute("creator", "OpenTopoMap Viewer")
            setAttribute("xmlns", "http://www.topografix.com/GPX/1/1")
        }

        markers.forEach { marker ->
            val waypoint = document.createElement("wpt")
            waypoint.setAttribute("lat", marker.latitude.toString())
            waypoint.setAttribute("lon", marker.longitude.toString())

            val name = document.createElement("name")
            name.textContent = marker.name.ifBlank { "${marker.latitude}, ${marker.longitude}" }
            waypoint.appendChild(name)

            if (marker.description.isNotBlank()) {
                val desc = document.createElement("desc")
                desc.textContent = marker.description
                waypoint.appendChild(desc)
            }

            gpxElement.appendChild(waypoint)
        }

        document.appendChild(gpxElement)

        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        }

        transformer.transform(DOMSource(document), StreamResult(outputStream))
    }
}
