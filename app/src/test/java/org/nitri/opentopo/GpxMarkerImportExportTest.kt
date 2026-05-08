package org.nitri.opentopo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nitri.opentopo.model.MarkerModel
import org.nitri.opentopo.util.GpxMarkerExporter
import org.nitri.opentopo.util.GpxMarkerImporter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class GpxMarkerImportExportTest {
    @Test
    fun exportMarkers_writesWaypoints() {
        val markers = listOf(
            MarkerModel(seq = 1, latitude = 51.0, longitude = 6.6, name = "A & B", description = "note"),
            MarkerModel(seq = 2, latitude = 52.0, longitude = 7.7, name = "", description = "")
        )

        val output = ByteArrayOutputStream()
        GpxMarkerExporter().export(markers, output)
        val xml = output.toString(Charsets.UTF_8)

        assertTrue(xml.contains("<wpt lat=\"51.0\" lon=\"6.6\""))
        assertTrue(xml.contains("<name>A &amp; B</name>"))
        assertTrue(xml.contains("<desc>note</desc>"))
        assertTrue(xml.contains("<wpt lat=\"52.0\" lon=\"7.7\""))
    }

    @Test
    fun importMarkers_readsValidWaypointsAndSkipsInvalid() {
        val gpx = """
            <gpx version="1.1" creator="test" xmlns="http://www.topografix.com/GPX/1/1">
              <wpt lat="51.123" lon="6.456"><name>Point 1</name><desc>Desc 1</desc></wpt>
              <wpt lat="invalid" lon="6.456"><name>Bad Point</name></wpt>
              <wpt lat="50.0" lon="8.0"></wpt>
            </gpx>
        """.trimIndent()

        val result = GpxMarkerImporter().import(ByteArrayInputStream(gpx.toByteArray()), 10)

        assertEquals(2, result.markers.size)
        assertEquals(1, result.skippedCount)
        assertEquals("Point 1", result.markers[0].name)
        assertEquals("Desc 1", result.markers[0].description)
        assertEquals("Marker 12", result.markers[1].name)
    }
}
