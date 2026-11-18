package org.nitri.opentopo.util

import org.junit.Assert.*
import org.junit.Test

class LocalMapViewportTest {
    @Test fun sparseCenterUsesKnownTileAtAnAvailableZoom() {
        val tiles = setOf(XyzTile(15, 17846, 12351), XyzTile(18, 142768, 98808))
        val target = LocalMapViewport.choose(tiles, 16.8, 0.0, 0.0)
        assertEquals(15, target.zoom)
        assertTrue(target.fallback in tiles)
    }

    @Test fun visibleEdgeTilesDoNotCauseUnnecessaryRecentering() {
        val tiles = setOf(XyzTile(3, 3, 3), XyzTile(3, 5, 3))
        // The center is a gap (x=4), but the western tile intersects the viewport.
        val target = LocalMapViewport.choose(tiles, 3.0, 10.0, 10.0,
            LocalMapBounds(-40.0, 1.0, 40.0, 40.0))
        assertNull(target.fallback)
    }

    @Test fun wrappedViewportFindsTilesAcrossTheAntimeridian() {
        val target = LocalMapViewport.choose(setOf(XyzTile(3, 0, 3)), 3.0, 10.0, 175.0,
            LocalMapBounds(170.0, 1.0, -170.0, 40.0))
        assertNull(target.fallback)
    }

    @Test fun fitBelowMinimumStillShowsPackage() {
        val tile = XyzTile(18, 142768, 98808)
        assertEquals(LocalMapTarget(18, tile), LocalMapViewport.choose(setOf(tile), 10.0, 0.0, 0.0))
    }

    @Test fun containedCenterAndMissingZoomNeverInventTiles() {
        val tiles = setOf(XyzTile(1, 1, 1), XyzTile(3, 4, 4))
        assertEquals(LocalMapTarget(1, null), LocalMapViewport.choose(tiles, 2.0, -10.0, 10.0))
        assertEquals(3, LocalMapViewport.choose(tiles, 20.0, -10.0, 10.0).zoom)
    }
}
