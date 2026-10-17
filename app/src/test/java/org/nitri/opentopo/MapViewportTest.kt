package org.nitri.opentopo

import org.junit.Assert.*
import org.junit.Test

class MapViewportTest {
    @Test fun `legacy float preferences accept all hemispheres and zero`() {
        for ((lat, lon) in listOf(-33f to -70f, 0f to 6f, 51f to 0f, 0f to 0f)) {
            val viewport = MapViewport.fromPreferences(mapOf("latitude" to lat, "longitude" to lon))!!
            assertEquals(lat.toDouble(), viewport.latitude, 0.0)
            assertEquals(lon.toDouble(), viewport.longitude, 0.0)
            assertEquals(15.0, viewport.zoom, 0.0)
        }
    }

    @Test fun `missing and invalid coordinates are rejected`() {
        assertNull(MapViewport.fromPreferences(emptyMap<String, Float>()))
        assertNull(MapViewport.fromPreferences(mapOf("latitude" to 0f)))
        assertNull(MapViewport.fromPreferences(mapOf("longitude" to 0f)))
        for ((lat, lon) in listOf(Float.NaN to 0f, 0f to Float.POSITIVE_INFINITY,
            -91f to 0f, 91f to 0f, 0f to -181f, 0f to 181f)) {
            assertNull(MapViewport.fromPreferences(mapOf("latitude" to lat, "longitude" to lon)))
        }
    }

    @Test fun `persisted zoom is restored and constrained to supported limits`() {
        val viewport = MapViewport.fromPreferences(mapOf("latitude" to 51f, "longitude" to 6f, "zoom" to 16.5f))!!
        assertEquals(16.5, viewport.clampedZoom(1.0, 17.0), 0.0)
        assertEquals(14.0, viewport.clampedZoom(1.0, 14.0), 0.0)
        assertEquals(1.0, viewport.copy(zoom = -2.0).clampedZoom(1.0, 17.0), 0.0)
        for (zoom in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertEquals(15.0, viewport.copy(zoom = zoom).clampedZoom(1.0, 17.0), 0.0)
        }
    }

    @Test fun `selection prefers restored viewport then preferences then location then fallback`() {
        val state = MapViewport(1.0, 2.0, 10.0)
        val prefs = MapViewport(3.0, 4.0, 11.0)
        val location = MapViewport(5.0, 6.0, 15.0)
        assertEquals(state, MapViewport.select(state, prefs, location))
        assertEquals(prefs, MapViewport.select(null, prefs, location))
        assertEquals(location, MapViewport.select(null, null, location))
        assertEquals(MapViewport(51.78962, 6.14120, 15.0), MapViewport.select(null, null, null))
    }
}
