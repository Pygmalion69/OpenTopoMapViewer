package org.nitri.ors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.nitri.ors.domain.snap.SnapRequestBuilderJ

class SnapRequestBuilderTest {

    @Test
    fun builds_snap_request() {
        val req = SnapRequestBuilderJ()
            .location(8.0, 48.0)
            .radius(25)
            .id("jid")
            .build()
        assertEquals(1, req.locations.size)
        assertEquals(25, req.radius)
        assertEquals("jid", req.id)
    }

    @Test
    fun requires_location_and_radius() {
        assertThrows(IllegalArgumentException::class.java) {
            SnapRequestBuilderJ()
                .radius(10)
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SnapRequestBuilderJ()
                .location(8.0, 48.0)
                .build()
        }
    }
}
