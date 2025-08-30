package org.nitri.ors

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.ors.domain.isochrones.IsochronesRequestBuilderJ
import org.nitri.ors.domain.isochrones.isochronesRequest

@RunWith(AndroidJUnit4::class)
class IsochronesDslInstrumentedTest {

    @Test
    fun dsl_buildsIsochronesRequest_andValidates() {
        val req = isochronesRequest {
            location(8.681495, 49.41461)
            rangeSeconds(600)
            rangeType = "time"
            intersections = true
        }
        assertEquals(listOf(listOf(8.681495,49.41461)), req.locations)
        assertEquals(listOf(600), req.range)
        assertEquals("time", req.rangeType)
        assertEquals(true, req.intersections)
    }

    @Test
    fun dsl_requires_location_and_range() {
        assertThrows(IllegalArgumentException::class.java) {
            isochronesRequest { rangeSeconds(300) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            isochronesRequest { location(8.0, 48.0) }
        }
    }

    @Test
    fun javaBuilder_buildsIsochronesRequest_andValidates() {
        val req = IsochronesRequestBuilderJ()
            .location(8.0, 48.0)
            .addRange(900)
            .rangeType("time")
            .intersections(false)
            .build()
        assertEquals(listOf(listOf(8.0,48.0)), req.locations)
        assertEquals(listOf(900), req.range)
    }
}
