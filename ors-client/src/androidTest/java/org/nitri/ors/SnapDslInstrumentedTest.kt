package org.nitri.ors

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.nitri.ors.domain.snap.SnapRequestBuilderJ
import org.nitri.ors.domain.snap.snapRequest

@RunWith(AndroidJUnit4::class)
class SnapDslInstrumentedTest {

    @Test
    fun dsl_buildsSnapRequest_andValidates() {
        val req = snapRequest {
            location(8.681495, 49.41461)
            radius(50)
            id = "snap-1"
        }
        assertEquals(1, req.locations.size)
        assertEquals(50, req.radius)
        assertEquals("snap-1", req.id)
    }

    @Test
    fun dsl_requires_location_and_radius() {
        assertThrows(IllegalArgumentException::class.java) {
            snapRequest { radius(10) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            snapRequest { location(8.0, 48.0) }
        }
    }

    @Test
    fun javaBuilder_buildsSnapRequest_andValidates() {
        val req = SnapRequestBuilderJ()
            .location(8.0, 48.0)
            .radius(25)
            .id("jid")
            .build()
        assertEquals(1, req.locations.size)
        assertEquals(25, req.radius)
        assertEquals("jid", req.id)
    }
}
