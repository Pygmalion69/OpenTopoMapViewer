package org.nitri.opentopo.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineMapRegionTest {
    @Test fun `location takes precedence over time zone`() {
        assertTrue(OfflineMapRegion.isInEurope(51.79, 6.14, "America/New_York"))
        assertFalse(OfflineMapRegion.isInEurope(40.71, -74.01, "Europe/Berlin"))
        assertFalse(OfflineMapRegion.isInEurope(-33.87, 151.21, "Europe/Berlin"))
        assertFalse(OfflineMapRegion.isInEurope(0.0, 0.0, "Europe/Berlin"))
    }

    @Test fun `rough bounds include European islands and border regions`() {
        for ((lat, lon) in listOf(64.15 to -21.94, 37.74 to -25.67,
            28.12 to -15.43, 78.22 to 15.65, 35.19 to 33.38, 41.01 to 28.98)) {
            assertTrue("$lat, $lon", OfflineMapRegion.isInEurope(lat, lon, "UTC"))
        }
        assertFalse(OfflineMapRegion.isInEurope(55.75, 60.6, "Europe/Moscow"))
    }

    @Test fun `missing or invalid location falls back to time zone`() {
        for ((lat, lon) in listOf(null to null, 51.0 to null, null to 6.0,
            Double.NaN to 6.0, 51.0 to Double.POSITIVE_INFINITY, 91.0 to 6.0, 51.0 to 181.0)) {
            assertTrue(OfflineMapRegion.isInEurope(lat, lon, "Europe/Berlin"))
            assertFalse(OfflineMapRegion.isInEurope(lat, lon, "America/New_York"))
        }
    }

    @Test fun `time zone fallback includes Europe and islands but not other Atlantic zones`() {
        for (zone in listOf("Europe/London", "Atlantic/Reykjavik", "Atlantic/Azores",
            "Atlantic/Madeira", "Atlantic/Canary", "Atlantic/Faroe", "Arctic/Longyearbyen",
            "Asia/Nicosia", "Asia/Famagusta", "Asia/Istanbul")) {
            assertTrue(zone, OfflineMapRegion.isInEurope(null, null, zone))
        }
        for (zone in listOf("UTC", "Atlantic/Bermuda", "Atlantic/Cape_Verde", "Asia/Tokyo", "")) {
            assertFalse(zone, OfflineMapRegion.isInEurope(null, null, zone))
        }
    }
}
