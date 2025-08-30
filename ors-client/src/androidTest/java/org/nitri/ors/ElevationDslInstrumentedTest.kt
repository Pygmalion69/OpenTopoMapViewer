package org.nitri.ors

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.ors.domain.elevation.ElevationLineRequestBuilderJ
import org.nitri.ors.domain.elevation.ElevationPointRequestBuilderJ
import org.nitri.ors.domain.elevation.ElevationFormats
import org.nitri.ors.domain.elevation.elevationLineRequest
import org.nitri.ors.domain.elevation.elevationPointRequest

@RunWith(AndroidJUnit4::class)
class ElevationDslInstrumentedTest {

    @Test
    fun dsl_buildsElevationLine_polyline() {
        val req = elevationLineRequest {
            polyline(8.681495 to 49.41461, 8.687872 to 49.420318)
            formatOut = ElevationFormats.GEOJSON
        }
        assertEquals(ElevationFormats.POLYLINE, req.formatIn)
        assertEquals(ElevationFormats.GEOJSON, req.formatOut)
    }

    @Test
    fun dsl_requires_geometry_for_elevationLine() {
        assertThrows(IllegalArgumentException::class.java) {
            elevationLineRequest { formatIn = ElevationFormats.POLYLINE }
        }
    }

    @Test
    fun dsl_buildsElevationPoint_point() {
        val req = elevationPointRequest {
            point(8.681495, 49.41461)
            formatIn = "point"
            formatOut = "geojson"
        }
        assertEquals("geojson", req.formatOut)
        assertEquals("point", req.formatIn)
    }

    @Test
    fun javaBuilder_buildsElevationLine_polyline() {
        val req = ElevationLineRequestBuilderJ()
            .polyline(listOf(listOf(8.0,48.0), listOf(9.0,49.0)))
            .formatOut(ElevationFormats.GEOJSON)
            .build()
        assertEquals(ElevationFormats.POLYLINE, req.formatIn)
    }

    @Test
    fun javaBuilder_buildsElevationPoint_point() {
        val req = ElevationPointRequestBuilderJ()
            .point(8.0, 48.0)
            .formatOut("geojson")
            .build()
        assertEquals("geojson", req.formatOut)
    }
}
