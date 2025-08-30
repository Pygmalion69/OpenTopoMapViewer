package org.nitri.ors

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.ors.domain.pois.PoisRequestBuilderJ
import org.nitri.ors.domain.pois.poisRequest

@RunWith(AndroidJUnit4::class)
class PoisDslInstrumentedTest {

    @Test
    fun dsl_buildsPoisRequest_bbox() {
        val req = poisRequest {
            bbox(8.67, 49.40, 8.70, 49.43)
            limit = 10
            sortby = "distance"
        }
        assertEquals(10, req.limit)
        assertEquals("distance", req.sortby)
        requireNotNull(req.geometry.bbox)
    }

    @Test
    fun javaBuilder_buildsPoisRequest_point() {
        val req = PoisRequestBuilderJ()
            .point(8.68, 49.41)
            .limit(5)
            .build()
        assertEquals(5, req.limit)
        requireNotNull(req.geometry.geojson)
    }
}
