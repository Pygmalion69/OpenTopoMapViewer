package org.nitri.opentopo.nearby

import org.junit.Assert.*
import org.junit.Test
import org.nitri.opentopo.nearby.entity.NearbyItem

class NearbyUiDataTest {
    @Test
    fun sortsFromSuppliedOriginWithoutChangingEntitiesOrInput() {
        val far = place("1", 0.02)
        val near = place("2", 0.001)
        val origin = place("3", 0.0)
        val input = listOf(far, near, origin)

        assertEquals(listOf(origin, near, far), sortNearbyItems(input, 0.0, 0.0))
        assertEquals(listOf(far, near, origin), input)
        assertTrue(input.all { it.distance == 123 })
        assertEquals(listOf(far, near, origin), sortNearbyItems(input, 0.02, 0.0))
    }

    @Test
    fun preservesInputOrderForRoundedDistanceTiesAndHandlesEmptyUpdates() {
        val first = place("1", 0.000003)
        val second = place("2", 0.000001)
        assertEquals(listOf(first, second), sortNearbyItems(listOf(first, second), 0.0, 0.0))
        assertTrue(sortNearbyItems(emptyList(), 0.0, 0.0).isEmpty())
    }

    @Test
    fun normalizesOnlyInsecureAndProtocolRelativeThumbnailUrls() {
        assertEquals("https://example.org/a.jpg", normalizeNearbyThumbnailUrl("http://example.org/a.jpg"))
        assertEquals("https://example.org/a.jpg", normalizeNearbyThumbnailUrl("//example.org/a.jpg"))
        assertEquals("https://example.org/a.jpg", normalizeNearbyThumbnailUrl("https://example.org/a.jpg"))
        assertNull(normalizeNearbyThumbnailUrl(null))
    }

    private fun place(pageId: String, latitude: Double) = NearbyItem().apply {
        pageid = pageId
        lat = latitude
        lon = 0.0
        distance = 123
    }
}
