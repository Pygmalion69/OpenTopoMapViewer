package org.nitri.ors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.nitri.ors.domain.route.RouteRequestBuilderJ

class RouteRequestBuilderJTest {

    @Test
    fun builds_coordinates_and_language() {
        val req = RouteRequestBuilderJ()
            .start(8.68, 49.41)
            .add(8.69, 49.42)
            .end(8.70, 49.43)
            .language("de")
            .build()

        assertEquals(3, req.coordinates.size)
        assertEquals(listOf(8.68, 49.41), req.coordinates.first())
        assertEquals(listOf(8.70, 49.43), req.coordinates.last())
        assertEquals("de", req.language)
    }

    @Test
    fun throws_when_less_than_two_points() {
        assertThrows(IllegalArgumentException::class.java) {
            RouteRequestBuilderJ()
                .start(8.68, 49.41)
                .build()
        }
    }
}
